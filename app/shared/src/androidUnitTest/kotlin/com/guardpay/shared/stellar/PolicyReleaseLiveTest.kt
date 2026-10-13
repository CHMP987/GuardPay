package com.guardpay.shared.stellar

import com.soneso.stellar.sdk.rpc.SorobanServer
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uploads a contract release to TESTNET and deploys the shared `guardian_hold` from it.
 * Pass the directory of CI's `wasm` artifact as `-Pgp.wasmDir=…` with `-PliveTestnet`.
 * Each file must match the artifact's `wasm.sha256`. The source key is generated per
 * run and never printed. Copy the logged ids into [TestnetProvisioner]'s constants.
 *
 * Skipped unless the property is given.
 */
class PolicyReleaseLiveTest {
    private fun log(msg: String) = println("[live] $msg")

    @Test
    fun uploadReleaseAndDeployPolicy() = runBlocking {
        val dir = File(System.getProperty("gp.wasmDir").orEmpty())
        assumeTrue("needs -Pgp.wasmDir", dir.isDirectory)

        val expected = File(dir, "wasm.sha256").readLines().filter { it.isNotBlank() }
            .associate { line -> line.trim().split(Regex("\\s+")).let { it[1].removePrefix("*") to it[0] } }
        val wasm = listOf("account.wasm", "guardian_hold.wasm", "hold_registry.wasm").associateWith { File(dir, it).readBytes() }
        wasm.forEach { (name, bytes) -> assertEquals(expected[name], TestnetProvisioner.sha256Hex(bytes), name) }

        val server = SorobanServer(TestnetProvisioner.RPC)
        val r = TestnetProvisioner(server, ::log).uploadRelease(wasm)
        r.txHashes.forEach { (label, hash) -> log("release $label: $hash") }
        r.wasmIds.forEach { (name, id) -> log("wasm $name: $id") }
        log("guardian_hold: ${r.guardianHold}")
        server.close()
    }
}
