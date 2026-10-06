package com.guardpay.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValuesTest {
    @Test
    fun amountsAreNeverNegative() {
        assertFailsWith<IllegalArgumentException> { UsdcAmount(-1) }
    }

    @Test
    fun checkedSumReturnsNullOnOverflow() {
        assertNull(UsdcAmount(Long.MAX_VALUE).plusOrNull(UsdcAmount(1)))
        assertEquals(usdc(3), usdc(1).plusOrNull(usdc(2)))
    }

    @Test
    fun paymentNeedsPositiveAmount() {
        assertFailsWith<IllegalArgumentException> { PaymentIntent(STRANGER, UsdcAmount.ZERO) }
    }

    @Test
    fun addressAcceptsOnlyPlainGAndC() {
        assertTrue(StellarAddress.isValid("G" + "A".repeat(55)))
        assertTrue(StellarAddress.isValid("C" + "A".repeat(55)))
        listOf(
            "M" + "A".repeat(55), // muxed: never matched against contacts
            "G" + "A".repeat(54),
            "G" + "A".repeat(56),
            "g" + "a".repeat(55),
            "G" + "1".repeat(55),
            "",
        ).forEach { assertFailsWith<IllegalArgumentException>(it) { StellarAddress(it) } }
    }

    @Test
    fun shortFormKeepsFirstAndLastFour() {
        val a = StellarAddress("GABC" + "D".repeat(48) + "WXYZ")
        assertEquals("GABC…WXYZ", a.short())
    }

    @Test
    fun txHashMustBe64LowercaseHex() {
        assertFailsWith<IllegalArgumentException> { TxHash("abc") }
        assertFailsWith<IllegalArgumentException> { TxHash("G".repeat(64)) }
        TxHash("2c7c671fc37dbf919613e3bca699d223582c40afe49d4a176e5168d551ab2d26")
    }

    @Test
    fun holdRecordInvariants() {
        assertFailsWith<IllegalArgumentException> { hold(readyAt = 1_000) }
        assertFailsWith<IllegalArgumentException> { hold(expiresAt = 1_120) }
        assertFailsWith<IllegalArgumentException> { hold(status = HoldStatus.Cancelled) }
        assertFailsWith<IllegalArgumentException> { hold(status = HoldStatus.Held, cancelledBy = Party.Guardian) }
    }
}
