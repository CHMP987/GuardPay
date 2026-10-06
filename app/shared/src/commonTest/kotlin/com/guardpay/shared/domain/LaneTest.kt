package com.guardpay.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LaneTest {
    @Test
    fun thereAreExactlyTwoLanes() {
        assertEquals(listOf(Lane.Immediate, Lane.MustHold), Lane.entries)
    }
}
