package com.guardpay.shared.stellar

import com.soneso.stellar.sdk.rpc.SorobanServer
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uploads only `hold_registry.wasm` to TESTNET (B-2: the indexed registry). The account
 * and the policy wasm did not change, so the shared `guardian_hold` stays; each new
 * account deploys its own registry from this wasm. Pass CI's `wasm` artifact as
 * `-Pgp.wasmDir=…` with `-PliveTestnet`; the file must match its `wasm.sha256`.
 * The source key is generated per run and never printed. Copy the logged id into
 * [TestnetProvisioner.HOLD_REGISTRY_WASM].
 *
 * Skipped unless the property is given.
 */
class RegistryReleaseLiveTest {
    private fun log(msg: String) = println("[live] $msg")

    @Test
    fun uploadRegistry() = runBlocking {
        val dir = File(System.getProperty("gp.wasmDir").orEmpty())
        assumeTrue("needs -Pgp.wasmDir", dir.isDirectory)

        val expected = File(dir, "wasm.sha256").readLines().filter { it.isNotBlank() }
            .associate { line -> line.trim().split(Regex("\\s+")).let { it[1].removePrefix("*") to it[0] } }
        val name = "hold_registry.wasm"
        val bytes = File(dir, name).readBytes()
        assertEquals(expected[name], TestnetProvisioner.sha256Hex(bytes), name)

        val server = SorobanServer(TestnetProvisioner.RPC)
        val provisioner = TestnetProvisioner(server, ::log)
        val ids = provisioner.uploadOnly(mapOf(name to bytes))
        provisioner.uploadHashes.forEach { (label, hash) -> log("release $label: $hash") }
        ids.forEach { (file, id) -> log("wasm $file: $id") }
        server.close()
    }
}
