package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.stellar.ContractAbi.AuthPayload
import com.guardpay.shared.stellar.ContractAbi.Config
import com.guardpay.shared.stellar.ContractAbi.Hold
import com.guardpay.shared.stellar.ContractAbi.Status
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.SCValXdr

/** The RPC could not be read (transport, simulation error, missing value). */
class ChainReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A chain value did not have the shape the app expects. */
class ChainDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** guardian_hold's stored config for one account. Set once at install; the app never writes it. */
data class PolicyConfig(
    val owner: StellarAddress,
    val usdc: StellarAddress,
    val trustedContacts: List<StellarAddress>,
    val registry: StellarAddress,
    val dailyCap: UsdcAmount,
)

/**
 * Pure decoders from contract values (SCVal) to domain types. Strict on purpose:
 * an unexpected shape throws instead of guessing, because a misread record could
 * show "Retenido" with nothing on chain behind it.
 */
internal object ChainDecoders {
    private val maxLong = BigInteger.fromLong(Long.MAX_VALUE)

    /** An i128 token amount. Negative or above Long.MAX_VALUE is rejected, never clamped. */
    fun amount(v: SCValXdr): UsdcAmount = decoding("i128 amount") {
        val n = Scv.fromInt128(v)
        if (n < BigInteger.ZERO || n > maxLong) throw ChainDecodeException("amount out of range: $n")
        UsdcAmount(n.longValue(exactRequired = true))
    }

    /** A plain G or C address. Muxed (M) and other address kinds are rejected. */
    fun address(v: SCValXdr): StellarAddress = decoding("address") {
        StellarAddress(Address.fromSCVal(v).getEncodedAddress())
    }

    fun u64(v: SCValXdr): Long = decoding("u64") {
        val n = Scv.fromUint64(v)
        if (n > Long.MAX_VALUE.toULong()) throw ChainDecodeException("u64 above Long.MAX_VALUE")
        n.toLong()
    }

    fun u32(v: SCValXdr): Int = decoding("u32") {
        val n = Scv.fromUint32(v)
        if (n > Int.MAX_VALUE.toUInt()) throw ChainDecodeException("u32 above Int.MAX_VALUE")
        n.toInt()
    }

    /** A `#[contracttype]` struct: an ScMap keyed by field-name symbols. */
    fun struct(v: SCValXdr): Map<String, SCValXdr> = decoding("struct") {
        Scv.fromMap(v).entries.associate { (k, value) -> Scv.fromSymbol(k) to value }
    }

    /** A unit `#[contracttype]` enum variant: `Vec[Symbol(name)]`. */
    fun variant(v: SCValXdr): String = decoding("enum variant") {
        val items = Scv.fromVec(v)
        if (items.size != 1) throw ChainDecodeException("enum variant with ${items.size} items")
        Scv.fromSymbol(items[0])
    }

    /**
     * One registry record, stored under `Hold(account, id)`. It must be in [token].
     * `Stopped` is attributed to the guardian: on the deployed registry `cancel`
     * requires the guardian's auth, so nobody else can stop a payment. The registry
     * has no expiry, so `expiresAt` is always null.
     */
    fun hold(id: Long, v: SCValXdr, account: StellarAddress, token: StellarAddress): HeldPayment {
        val f = struct(v)
        fun req(name: String) = f[name] ?: throw ChainDecodeException("hold: missing field $name")
        if (address(req(Hold.TOKEN)) != token) throw ChainDecodeException("hold is in another token")
        val status = when (val s = variant(req(Hold.STATUS))) {
            Status.RETAINED -> HoldStatus.Held
            Status.EXECUTED -> HoldStatus.Executed
            Status.STOPPED -> HoldStatus.Cancelled
            else -> throw ChainDecodeException("unknown hold status $s")
        }
        return decoding("hold record") {
            HeldPayment(
                id = id,
                account = account,
                destination = address(req(Hold.DESTINATION)),
                amount = amount(req(Hold.AMOUNT)),
                createdAt = LedgerTime(u64(req(Hold.CREATED_AT))),
                readyAt = LedgerTime(u64(req(Hold.READY_AT))),
                expiresAt = null,
                status = status,
                cancelledBy = if (status == HoldStatus.Cancelled) Party.Guardian else null,
            )
        }
    }

    /** guardian_hold's `GuardianHoldConfig`. The cap must be positive and there are 1 to 3 contacts. */
    fun policyConfig(v: SCValXdr): PolicyConfig {
        val f = struct(v)
        fun req(name: String) = f[name] ?: throw ChainDecodeException("config: missing field $name")
        val contacts = decoding("contact list") { Scv.fromVec(req(Config.TRUSTED_CONTACTS)) }.map(::address)
        if (contacts.size !in 1..3 || contacts.toSet().size != contacts.size) {
            throw ChainDecodeException("config: ${contacts.size} contacts, expected 1 to 3 distinct")
        }
        val cap = amount(req(Config.DAILY_CAP))
        if (!cap.isPositive) throw ChainDecodeException("config: daily cap must be positive")
        return PolicyConfig(
            owner = address(req(Config.OWNER)),
            usdc = address(req(Config.USDC)),
            trustedContacts = contacts,
            registry = address(req(Config.REGISTRY)),
            dailyCap = cap,
        )
    }

    /** Contacts are bare addresses on chain; the name shown is the short address unless the app has a label. */
    fun contacts(addresses: List<StellarAddress>): List<TrustedContact> = addresses.map { TrustedContact(it.short(), it) }

    /**
     * An OZ `Signer` as a G address: `Delegated(address)` is that address, and an
     * `External(verifier, key)` with a 32-byte ed25519 key is that key's G strkey.
     */
    fun signer(v: SCValXdr): StellarAddress = decoding("signer") {
        val items = Scv.fromVec(v)
        when (Scv.fromSymbol(items[0])) {
            AuthPayload.DELEGATED -> {
                if (items.size != 2) throw ChainDecodeException("Delegated signer with ${items.size} items")
                address(items[1])
            }
            AuthPayload.EXTERNAL -> {
                val key = Scv.fromBytes(items.getOrNull(2) ?: throw ChainDecodeException("External signer without key"))
                if (key.size != 32) throw ChainDecodeException("External signer key is not ed25519")
                ed25519AccountId(key)
            }
            else -> throw ChainDecodeException("unknown signer kind")
        }
    }

    /** One unit-variant key (`Vec[Symbol(name)]`) from a contract's instance storage. */
    fun instanceValue(v: SCValXdr, key: String): SCValXdr = decoding("instance storage") {
        val storage = Scv.fromContractInstance(v).storage?.value ?: throw ChainDecodeException("instance has no storage")
        storage.firstOrNull { (it.key as? SCValXdr.Vec)?.let { k -> variantOrNull(k) } == key }?.`val`
            ?: throw ChainDecodeException("instance storage has no $key")
    }

    /** OZ `ContextRuleEntry`: the global signer and policy ids the rule references. */
    fun ruleIds(v: SCValXdr): Pair<List<Int>, List<Int>> {
        val f = struct(v)
        fun ids(name: String) = decoding("rule $name") {
            Scv.fromVec(f[name] ?: throw ChainDecodeException("rule: missing field $name")).map(::u32)
        }
        return ids(AuthPayload.SIGNER_IDS) to ids(AuthPayload.POLICY_IDS)
    }

    /** OZ `SignerEntry { signer, count }`. */
    fun signerEntry(v: SCValXdr): StellarAddress =
        signer(struct(v)[AuthPayload.SIGNER_FIELD] ?: throw ChainDecodeException("signer entry: missing signer"))

    /** OZ `PolicyEntry { policy, count }`. */
    fun policyEntry(v: SCValXdr): StellarAddress =
        address(struct(v)[AuthPayload.POLICY_FIELD] ?: throw ChainDecodeException("policy entry: missing policy"))

    private fun variantOrNull(v: SCValXdr): String? = try {
        variant(v)
    } catch (e: ChainDecodeException) {
        null
    }

    private inline fun <T> decoding(what: String, block: () -> T): T = try {
        block()
    } catch (e: ChainDecodeException) {
        throw e
    } catch (e: Exception) {
        throw ChainDecodeException("bad $what: ${e.message}", e)
    }
}
