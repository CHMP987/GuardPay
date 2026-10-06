package com.guardpay.shared.ui

import com.guardpay.shared.ui.guardian.GuardianChain
import kotlin.test.Test
import kotlin.test.assertEquals

/** The guardian's screens can read and stop, nothing else: no transfer, no queue, no signer. */
class GuardianSurfaceTest {
    @Test
    fun guardianChainHasExactlyReadAndStop() {
        // Functions taking or returning a value class get a "-hash" suffix on the JVM.
        val methods = GuardianChain::class.java.declaredMethods.map { it.name.substringBefore('-') }.toSet()
        assertEquals(setOf("latestLedgerTime", "readHolds", "readTrustedContacts", "submitCancel"), methods)
    }
}
