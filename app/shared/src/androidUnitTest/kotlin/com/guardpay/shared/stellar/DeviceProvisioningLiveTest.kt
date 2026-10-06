package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import com.soneso.stellar.sdk.rpc.SorobanServer
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Deploys a testnet account for the phone's own Keystore keys. The app writes their
 * G addresses (public) to `keys.json`; pass them as
 * `-Pgp.owner=G… -Pgp.guardian=G…` with `-PliveTestnet`. The result is
 * `app/shared/build/testnet.json`, pushed to the phone with adb. No seed is ever
 * involved: only public addresses go in and out.
 *
 * Skipped unless both properties are given.
 */
class DeviceProvisioningLiveTest {
    private fun log(msg: String) = println("[live] $msg")

    @Test
    fun provisionForThePhonesKeys() = runBlocking {
        val owner = System.getProperty("gp.owner").orEmpty()
        val guardian = System.getProperty("gp.guardian").orEmpty()
        assumeTrue("needs -Pgp.owner and -Pgp.guardian", owner.isNotEmpty() && guardian.isNotEmpty())

        val server = SorobanServer(TestnetProvisioner.RPC)
        val p = TestnetProvisioner(server, ::log).provision(owner, guardian, contacts = 1, strangers = 1)
        p.txHashes.forEach { (label, hash) -> log("setup $label: $hash") }
        p.strangers.forEach { log("stranger (held lane): $it") }

        // The phone's account: one rule, the phone's owner key its only signer.
        val shape = KmpStellarGateway(p.config.network(), server).readAccountShape(StellarAddress(p.config.account))
        log("shape: $shape")
        assertEquals(listOf(owner), shape.signers.map { it.value })
        assertFalse(StellarAddress(guardian) in shape.signers, "the guardian must never be an account signer")

        val out = File(System.getProperty("gp.out") ?: "build/testnet.json")
        out.writeText(p.config.encode())
        log("wrote ${out.absolutePath}")
    }
}
