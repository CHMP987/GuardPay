package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.domain.usdc
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.SCValXdr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Parsing of chain reads: contract return values (SCVal) into domain types. */
class ChainDecodersTest {
    private val account = cKey(1)
    private val token = cKey(2)
    private val guardian = gKey(3)
    private val destination = gKey(4)
    private val stranger = gKey(5)

    private fun record(
        account: SCValXdr = sv(this.account),
        token: SCValXdr = sv(this.token),
        amount: SCValXdr = i128(usdc(150).units),
        status: String = "Held",
        createdAt: Long = 1_000,
        readyAt: Long = 1_120,
        expiresAt: SCValXdr? = u64(1_720),
        cancelledBy: SCValXdr? = none(),
        cancelledAt: SCValXdr? = none(),
    ): SCValXdr = struct(
        *listOfNotNull(
            "id" to u64(7),
            "account" to account,
            "token" to token,
            "destination" to sv(destination),
            "amount" to amount,
            "created_at" to u64(createdAt),
            "ready_at" to u64(readyAt),
            expiresAt?.let { "expires_at" to it },
            "status" to variant(status),
            cancelledBy?.let { "cancelled_by" to it },
            cancelledAt?.let { "cancelled_at" to it },
        ).toTypedArray(),
    )

    private fun decode(v: SCValXdr) = ChainDecoders.hold(v, account, token, guardian)

    private fun rejects(v: SCValXdr) {
        assertFailsWith<ChainDecodeException> { decode(v) }
    }

    @Test
    fun heldRecordDecodesEveryField() {
        val h = decode(record())
        assertEquals(7, h.id)
        assertEquals(account, h.account)
        assertEquals(destination, h.destination)
        assertEquals(usdc(150), h.amount)
        assertEquals(LedgerTime(1_000), h.createdAt)
        assertEquals(LedgerTime(1_120), h.readyAt)
        assertEquals(LedgerTime(1_720), h.expiresAt)
        assertEquals(HoldStatus.Held, h.status)
        assertNull(h.cancelledBy)
    }

    @Test
    fun cancellationIsAttributedOnlyToGuardianOrOwner() {
        val byGuardian = decode(record(status = "Cancelled", cancelledBy = sv(guardian), cancelledAt = u64(1_060)))
        assertEquals(Party.Guardian, byGuardian.cancelledBy)
        assertEquals(LedgerTime(1_060), byGuardian.cancelledAt)
        assertEquals(Party.Owner, decode(record(status = "Cancelled", cancelledBy = sv(account))).cancelledBy)
        rejects(record(status = "Cancelled", cancelledBy = sv(stranger)))
    }

    @Test
    fun optionalFieldsMayBeVoidOrAbsent() {
        assertNull(decode(record(expiresAt = none())).expiresAt)
        val bare = decode(record(expiresAt = null, cancelledBy = null, cancelledAt = null))
        assertNull(bare.expiresAt)
        assertNull(bare.cancelledAt)
    }

    @Test
    fun missingRequiredFieldIsAnError() {
        val m = Scv.fromMap(record())
        m.remove(sym("ready_at"))
        rejects(Scv.toMap(m))
    }

    @Test
    fun recordFromAnotherAccountOrTokenIsAnError() {
        rejects(record(account = sv(cKey(9))))
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
    fun u64AboveLongMaxIsRejected() {
        assertFailsWith<ChainDecodeException> { ChainDecoders.u64(Scv.toUint64(ULong.MAX_VALUE)) }
        assertEquals(Long.MAX_VALUE, ChainDecoders.u64(Scv.toUint64(Long.MAX_VALUE.toULong())))
    }

    @Test
    fun muxedAddressIsRejected() {
        val muxed = Address.fromMuxedAccount(ByteArray(40) { 4 }).toSCVal()
        assertFailsWith<ChainDecodeException> { ChainDecoders.address(muxed) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.contacts(vec(sv(destination), muxed)) }
    }

    @Test
    fun unknownOrMalformedStatusIsRejected() {
        rejects(record(status = "Released"))
        val m = Scv.fromMap(record())
        m[sym("status")] = Scv.toVec(listOf(sym("Held"), u64(1)))
        rejects(Scv.toMap(m))
    }

    @Test
    fun inconsistentRecordsAreRejected() {
        rejects(record(status = "Held", cancelledBy = sv(guardian)))
        rejects(record(status = "Cancelled"))
        rejects(record(readyAt = 1_000))
        rejects(record(expiresAt = u64(1_120)))
    }

    @Test
    fun wrongValueTypesAreDecodeErrors() {
        rejects(record(amount = sym("150")))
        assertFailsWith<ChainDecodeException> { ChainDecoders.u64(i128(5)) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.struct(vec()) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.holds(u64(1), account, token, guardian) }
    }

    @Test
    fun holdListDecodesInOrder() {
        val list = ChainDecoders.holds(vec(record(), record(status = "Executed")), account, token, guardian)
        assertEquals(listOf(HoldStatus.Held, HoldStatus.Executed), list.map { it.status })
        assertEquals(emptyList(), ChainDecoders.holds(vec(), account, token, guardian))
    }

    @Test
    fun contactsAreDistinctAndShownByShortAddress() {
        val contacts = ChainDecoders.contacts(vec(sv(destination), sv(stranger), sv(destination)))
        assertEquals(listOf(destination, stranger), contacts.map { it.address })
        assertEquals(destination.short(), contacts[0].name)
    }

    @Test
    fun accountRulesDecode() {
        val cfg = struct(
            "guardian" to sv(guardian),
            "daily_cap" to i128(usdc(50).units),
            "hold_secs" to u64(120),
            "expiry_secs" to u64(600),
        )
        assertEquals(AccountRules(guardian, usdc(50), 120, 600), ChainDecoders.accountRules(cfg))

        val noExpiry = struct("guardian" to sv(guardian), "daily_cap" to i128(1), "hold_secs" to u64(120), "expiry_secs" to none())
        assertNull(ChainDecoders.accountRules(noExpiry).expiryWindowSeconds)

        val zeroHold = struct("guardian" to sv(guardian), "daily_cap" to i128(1), "hold_secs" to u64(0))
        assertFailsWith<ChainDecodeException> { ChainDecoders.accountRules(zeroHold) }
        assertFailsWith<ChainDecodeException> { ChainDecoders.accountRules(struct("guardian" to sv(guardian))) }
    }
}
