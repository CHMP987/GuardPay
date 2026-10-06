package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.domain.usdc
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.ContractExecutableXdr
import com.soneso.stellar.sdk.xdr.SCContractInstanceXdr
import com.soneso.stellar.sdk.xdr.SCMapEntryXdr
import com.soneso.stellar.sdk.xdr.SCMapXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Parsing of chain reads into domain types, with the shapes the deployed contracts
 * store (`contracts/hold_registry`, `contracts/guardian_hold`, the OZ smart account).
 */
class ChainDecodersTest {
    private val account = cKey(1)
    private val token = cKey(2)
    private val registry = cKey(3)
    private val destination = gKey(4)
    private val owner = gKey(5)

    private fun record(
        token: SCValXdr = sv(this.token),
        amount: SCValXdr = i128(usdc(150).units),
        status: SCValXdr = variant("Retained"),
        createdAt: Long = 1_000,
        readyAt: Long = 1_120,
        drop: String? = null,
    ): SCValXdr = struct(
        *listOf(
            "token" to token,
            "destination" to sv(destination),
            "amount" to amount,
            "created_at" to u64(createdAt),
            "ready_at" to u64(readyAt),
            "status" to status,
        ).filter { it.first != drop }.toTypedArray(),
    )

    private fun decode(v: SCValXdr) = ChainDecoders.hold(7, v, account, token)

    private fun rejects(v: SCValXdr) {
        assertFailsWith<ChainDecodeException> { decode(v) }
    }

    @Test
    fun retainedRecordDecodesEveryField() {
        val h = decode(record())
        assertEquals(7, h.id)
        assertEquals(account, h.account)
        assertEquals(destination, h.destination)
        assertEquals(usdc(150), h.amount)
        assertEquals(LedgerTime(1_000), h.createdAt)
        assertEquals(LedgerTime(1_120), h.readyAt)
        assertNull(h.expiresAt) // the registry has no expiry
        assertEquals(HoldStatus.Held, h.status)
        assertNull(h.cancelledBy)
    }

    @Test
    fun statusesMapAndOnlyTheGuardianStops() {
        assertEquals(HoldStatus.Executed, decode(record(status = variant("Executed"))).status)
        val stopped = decode(record(status = variant("Stopped")))
        assertEquals(HoldStatus.Cancelled, stopped.status)
        assertEquals(Party.Guardian, stopped.cancelledBy)
    }

    @Test
    fun unknownOrMalformedStatusIsRejected() {
        rejects(record(status = variant("Released")))
        rejects(record(status = variant("Held"))) // the old provisional name
        rejects(record(status = sym("Retained")))
        rejects(record(status = vec(sym("Retained"), u64(1))))
    }

    @Test
    fun missingFieldIsAnError() {
        listOf("token", "destination", "amount", "created_at", "ready_at", "status").forEach { rejects(record(drop = it)) }
    }

    @Test
    fun recordInAnotherTokenIsAnError() {
        rejects(record(token = sv(cKey(9))))
    }

    @Test
    fun amountsOutsideZeroToLongMaxAreRejectedNotClamped() {
        rejects(record(amount = i128(-1)))
        rejects(record(amount = i128(BigInteger.fromLong(Long.MAX_VALUE) + BigInteger.ONE)))
        rejects(record(amount = i128(0))) // a hold of nothing breaks the record invariant
        assertEquals(UsdcAmount(Long.MAX_VALUE), ChainDecoders.amount(i128(Long.MAX_VALUE)))
        assertEquals(UsdcAmount.ZERO, ChainDecoders.amount(i128(0)))
    }

    @Test
    fun inconsistentTimesAreRejected() {
        rejects(record(createdAt = 1_120, readyAt = 1_000))
    }

    @Test
    fun integerRangesAreChecked() {
        assertFailsWith<ChainDecodeException> { ChainDecoders.u64(Scv.toUint64(ULong.MAX_VALUE)) }
        assertEquals(Long.MAX_VALUE, ChainDecoders.u64(Scv.toUint64(Long.MAX_VALUE.toULong())))
        assertFailsWith<ChainDecodeException> { ChainDecoders.u32(Scv.toUint32(UInt.MAX_VALUE)) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.u32(u64(1)) }
    }

    @Test
    fun muxedAddressIsRejected() {
        val muxed = Address.fromMuxedAccount(ByteArray(40) { 4 }).toSCVal()
        assertFailsWith<ChainDecodeException> { ChainDecoders.address(muxed) }
    }

    // --- guardian_hold config ---------------------------------------------------

    private fun config(
        contacts: List<SCValXdr> = listOf(sv(destination), sv(gKey(6))),
        cap: SCValXdr = i128(usdc(100).units),
    ): SCValXdr = struct(
        "owner" to sv(owner),
        "usdc" to sv(token),
        "trusted_contacts" to Scv.toVec(contacts),
        "registry" to sv(registry),
        "daily_cap" to cap,
        "spent" to i128(0),
        "spent_day" to u64(0),
    )

    @Test
    fun policyConfigDecodes() {
        val c = ChainDecoders.policyConfig(config())
        assertEquals(owner, c.owner)
        assertEquals(token, c.usdc)
        assertEquals(registry, c.registry)
        assertEquals(usdc(100), c.dailyCap)
        assertEquals(listOf(destination, gKey(6)), c.trustedContacts)
        assertEquals(destination.short(), ChainDecoders.contacts(c.trustedContacts).first().name)
    }

    @Test
    fun policyConfigOutsideTheInstallRulesIsRejected() {
        assertFailsWith<ChainDecodeException> { ChainDecoders.policyConfig(config(contacts = emptyList())) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.policyConfig(config(contacts = List(4) { sv(gKey(10 + it)) })) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.policyConfig(config(contacts = listOf(sv(destination), sv(destination)))) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.policyConfig(config(cap = i128(0))) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.policyConfig(struct("owner" to sv(owner))) }
    }

    // --- OZ account storage ----------------------------------------------------

    @Test
    fun signersDecodeToGAddresses() {
        assertEquals(owner, ChainDecoders.signer(vec(sym("Delegated"), sv(owner))))
        assertEquals(owner, ChainDecoders.signer(vec(sym("External"), sv(cKey(8)), Scv.toBytes(ByteArray(32) { 5 }))))
        assertFailsWith<ChainDecodeException> { ChainDecoders.signer(vec(sym("External"), sv(cKey(8)), Scv.toBytes(ByteArray(65)))) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.signer(vec(sym("Delegated"))) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.signer(vec(sym("Webauthn"), sv(owner))) }
    }

    @Test
    fun ruleAndEntriesDecode() {
        val rule = struct(
            "name" to Scv.toString("default"),
            "context_type" to variant("Default"),
            "valid_until" to none(),
            "signer_ids" to vec(Scv.toUint32(0u)),
            "policy_ids" to vec(Scv.toUint32(0u), Scv.toUint32(2u)),
        )
        assertEquals(listOf(0) to listOf(0, 2), ChainDecoders.ruleIds(rule))
        assertEquals(owner, ChainDecoders.signerEntry(struct("signer" to vec(sym("Delegated"), sv(owner)), "count" to Scv.toUint32(1u))))
        assertEquals(cKey(9), ChainDecoders.policyEntry(struct("policy" to sv(cKey(9)), "count" to Scv.toUint32(1u))))
        assertFailsWith<ChainDecodeException> { ChainDecoders.ruleIds(struct("signer_ids" to vec())) }
    }

    @Test
    fun instanceStorageIsReadByVariantKey() {
        val instance = Scv.toContractInstance(
            SCContractInstanceXdr(
                ContractExecutableXdr.Void,
                SCMapXdr(listOf(SCMapEntryXdr(variant("Count"), Scv.toUint32(1u)), SCMapEntryXdr(sym("Other"), u64(3)))),
            ),
        )
        assertEquals(1, ChainDecoders.u32(ChainDecoders.instanceValue(instance, "Count")))
        assertFailsWith<ChainDecodeException> { ChainDecoders.instanceValue(instance, "Missing") }
        assertFailsWith<ChainDecodeException> { ChainDecoders.instanceValue(u64(1), "Count") }
    }
}
