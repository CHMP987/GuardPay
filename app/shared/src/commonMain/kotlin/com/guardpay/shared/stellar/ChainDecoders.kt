package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.stellar.ProvisionalAbi.Config
import com.guardpay.shared.stellar.ProvisionalAbi.Hold
import com.guardpay.shared.stellar.ProvisionalAbi.Status
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.SCValXdr

/** The RPC could not be read (transport, simulation error, missing value). */
class ChainReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A chain value did not have the shape the app expects. */
class ChainDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Pure decoders from contract return values (SCVal) to domain types. Strict on
 * purpose: an unexpected shape throws instead of guessing, because a misread
 * record could show "Retenido" with nothing on chain behind it.
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

    /** A `#[contracttype]` struct: an ScMap keyed by field-name symbols. */
    fun struct(v: SCValXdr): Map<String, SCValXdr> = decoding("struct") {
        Scv.fromMap(v).entries.associate { (k, value) -> Scv.fromSymbol(k) to value }
    }

    /** Soroban `Option<T>`: `Void` (or an absent field) is `None`. */
    fun <T> option(v: SCValXdr?, decode: (SCValXdr) -> T): T? =
        if (v == null || v is SCValXdr.Void) null else decode(v)

    /** A unit `#[contracttype]` enum variant: `Vec[Symbol(name)]`. */
    fun variant(v: SCValXdr): String = decoding("enum variant") {
        val items = Scv.fromVec(v)
        if (items.size != 1) throw ChainDecodeException("enum variant with ${items.size} items")
        Scv.fromSymbol(items[0])
    }

    /**
     * One registry record. It must belong to [account] and be in [token]; a
     * cancellation is attributed by comparing `cancelled_by` with the guardian and
     * the account, and anyone else is a decode error.
     */
    fun hold(v: SCValXdr, account: StellarAddress, token: StellarAddress, guardian: StellarAddress): HeldPayment {
        val f = struct(v)
        fun req(name: String) = f[name] ?: throw ChainDecodeException("hold: missing field $name")
        if (address(req(Hold.ACCOUNT)) != account) throw ChainDecodeException("hold belongs to another account")
        if (address(req(Hold.TOKEN)) != token) throw ChainDecodeException("hold is in another token")
        val status = when (val s = variant(req(Hold.STATUS))) {
            Status.HELD -> HoldStatus.Held
            Status.EXECUTED -> HoldStatus.Executed
            Status.CANCELLED -> HoldStatus.Cancelled
            else -> throw ChainDecodeException("unknown hold status $s")
        }
        val cancelledBy = option(f[Hold.CANCELLED_BY], ::address)?.let {
            when (it) {
                guardian -> Party.Guardian
                account -> Party.Owner
                else -> throw ChainDecodeException("hold cancelled by an unknown party")
            }
        }
        return decoding("hold record") {
            HeldPayment(
                id = u64(req(Hold.ID)),
                account = account,
                destination = address(req(Hold.DESTINATION)),
                amount = amount(req(Hold.AMOUNT)),
                createdAt = LedgerTime(u64(req(Hold.CREATED_AT))),
                readyAt = LedgerTime(u64(req(Hold.READY_AT))),
                expiresAt = option(f[Hold.EXPIRES_AT], ::u64)?.let(::LedgerTime),
                status = status,
                cancelledBy = cancelledBy,
                cancelledAt = option(f[Hold.CANCELLED_AT], ::u64)?.let(::LedgerTime),
            )
        }
    }

    fun holds(v: SCValXdr, account: StellarAddress, token: StellarAddress, guardian: StellarAddress): List<HeldPayment> =
        decoding("hold list") { Scv.fromVec(v) }.map { hold(it, account, token, guardian) }

    /** Contacts are bare addresses on chain; the name shown is the short address until P6 adds local labels. */
    fun contacts(v: SCValXdr): List<TrustedContact> =
        decoding("contact list") { Scv.fromVec(v) }.map(::address).distinct().map { TrustedContact(it.short(), it) }

    fun accountRules(v: SCValXdr): AccountRules {
        val f = struct(v)
        fun req(name: String) = f[name] ?: throw ChainDecodeException("config: missing field $name")
        val holdSecs = u64(req(Config.HOLD_SECS))
        if (holdSecs <= 0) throw ChainDecodeException("hold duration must be positive")
        return AccountRules(
            guardian = address(req(Config.GUARDIAN)),
            dailyCap = amount(req(Config.DAILY_CAP)),
            holdDurationSeconds = holdSecs,
            expiryWindowSeconds = option(f[Config.EXPIRY_SECS], ::u64),
        )
    }

    private inline fun <T> decoding(what: String, block: () -> T): T = try {
        block()
    } catch (e: ChainDecodeException) {
        throw e
    } catch (e: Exception) {
        throw ChainDecodeException("bad $what: ${e.message}", e)
    }
}
