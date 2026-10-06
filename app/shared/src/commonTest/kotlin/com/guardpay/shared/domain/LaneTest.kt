package com.guardpay.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LaneTest {
    private fun rules(cap: Long = 50, spent: Long = 10) =
        LaneRules(trustedContacts = listOf(ANA), dailyCap = usdc(cap), spentToday = usdc(spent))

    @Test
    fun laneHasExactlyTwoValues() {
        assertEquals(listOf(Lane.Immediate, Lane.MustHold), Lane.entries)
    }

    @Test
    fun contactUnderCapIsImmediate() {
        val p = predictLane(PaymentIntent(ANA.address, usdc(30)), rules())
        assertIs<LanePrediction.Immediate>(p)
        assertEquals(ANA, p.contact)
        assertEquals(usdc(10), p.remainingAfter)
    }

    @Test
    fun contactExactlyAtCapIsImmediate() {
        val p = predictLane(PaymentIntent(ANA.address, usdc(40)), rules())
        assertIs<LanePrediction.Immediate>(p)
        assertEquals(UsdcAmount.ZERO, p.remainingAfter)
    }

    @Test
    fun contactOneUnitOverCapMustHold() {
        val p = predictLane(PaymentIntent(ANA.address, UsdcAmount(usdc(40).units + 1)), rules())
        assertEquals(LanePrediction.MustHold(HoldReason.OverDailyCap), p)
    }

    @Test
    fun capAlreadyUsedUpMustHold() {
        val p = predictLane(PaymentIntent(ANA.address, UsdcAmount(1)), rules(cap = 50, spent = 50))
        assertEquals(LanePrediction.MustHold(HoldReason.OverDailyCap), p)
    }

    @Test
    fun overflowingSumMustHold() {
        val r = LaneRules(listOf(ANA), dailyCap = UsdcAmount(Long.MAX_VALUE), spentToday = UsdcAmount(Long.MAX_VALUE - 1))
        val p = predictLane(PaymentIntent(ANA.address, UsdcAmount(2)), r)
        assertEquals(LanePrediction.MustHold(HoldReason.OverDailyCap), p)
    }

    @Test
    fun strangerAlwaysMustHold() {
        val p = predictLane(PaymentIntent(STRANGER, UsdcAmount(1)), rules(cap = 1_000, spent = 0))
        assertEquals(LanePrediction.MustHold(HoldReason.NotTrustedContact), p)
    }

    @Test
    fun ownerCanChooseHoldForContact() {
        val p = predictLane(PaymentIntent(ANA.address, usdc(5)), rules(), ownerChoseHold = true)
        assertEquals(LanePrediction.MustHold(HoldReason.OwnerChoseHold), p)
    }
}
