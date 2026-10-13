package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.UNIT
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.Transaction
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.xdr.ConfigSettingIDXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyConfigSettingXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyXdr
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.TimeSource

/**
 * F-02 on Stellar TESTNET (B-2): a fresh account with the indexed registry gets
 * `gp.records` (default 400) holds, each queued by the owner and stopped by the
 * guardian, through the app's gateway. Then a contact payment, a queue, a cancel
 * and a matured release must still land. Before the index, the local bench put the
 * limit near 390 records. Also prints the network's Soroban limits (ConfigSetting).
 *
 * Opt-in (`-PliveTestnet`). Keys are generated per run, stay in memory and are never
 * printed. Sequential, about two transactions per record: expect over an hour.
 */
class RegistryScaleLiveTest {
    private fun log(msg: String) = println("[live] $msg")

    private fun keySigner(kp: KeyPair) = object : Signer {
        override val publicKey: ByteArray = kp.getPublicKey()
        override suspend fun signHash(hash: ByteArray): ByteArray = kp.sign(hash)
    }

    private fun confirmed(label: String, r: SubmitResult): TxHash {
        if (r !is SubmitResult.Confirmed) log("$label: $r")
        return assertIs<SubmitResult.Confirmed>(r, label).txHash
    }

    @Test
    fun manyFinishedHoldsDoNotBlockTheAccount() = runBlocking {
        val records = System.getProperty("gp.records")?.toInt() ?: 400
        val server = SorobanServer(TestnetProvisioner.RPC)
        logNetworkLimits(server)

        val owner = KeyPair.random()
        val guardian = KeyPair.random()
        val p = TestnetProvisioner(server, ::log).provision(owner.getAccountId(), guardian.getAccountId(), contacts = 1, strangers = 1)
        p.txHashes.forEach { (label, hash) -> log("setup $label: $hash") }
        val c = p.config
        val gw = KmpStellarGateway(c.network(), server)
        val account = StellarAddress(c.account)
        val contact = StellarAddress(c.contacts.single())
        val stranger = StellarAddress(p.strangers.single())
        val held = PaymentIntent(stranger, UsdcAmount(1 * UNIT))

        // Baseline, with no records.
        resources(server, "contact payment, 0 records", confirmed("pay", gw.submitTransfer(account, PaymentIntent(contact, UsdcAmount(10 * UNIT)), keySigner(owner))))

        val clock = TimeSource.Monotonic.markNow()
        for (id in 0L until records) {
            val queued = confirmed("queue $id", gw.submitQueue(account, held, keySigner(owner)))
            val cancelled = confirmed("cancel $id", gw.submitCancel(account, id, keySigner(guardian)))
            if (id == 0L) {
                resources(server, "queue, 0 records", queued)
                resources(server, "cancel, 0 records", cancelled)
            }
            if ((id + 1) % 25 == 0L) log("${id + 1} records, ${clock.elapsedNow().inWholeSeconds} s")
        }

        val holds = gw.readHolds(account)
        assertEquals(records, holds.size)
        assertEquals(setOf(HoldStatus.Cancelled), holds.map { it.status }.toSet())
        log("readHolds: ${holds.size} records, all Cancelled")

        // The same operations with `records` finished records behind them.
        resources(server, "contact payment, $records records", confirmed("pay", gw.submitTransfer(account, PaymentIntent(contact, UsdcAmount(10 * UNIT)), keySigner(owner))))
        resources(server, "queue, $records records", confirmed("queue", gw.submitQueue(account, held, keySigner(owner))))
        resources(server, "cancel, ${records + 1} records", confirmed("cancel", gw.submitCancel(account, records.toLong(), keySigner(guardian))))
        val release = PaymentIntent(stranger, UsdcAmount(150 * UNIT))
        resources(server, "queue, ${records + 1} records", confirmed("queue", gw.submitQueue(account, release, keySigner(owner))))
        val last = gw.readHolds(account).last()
        while (gw.latestLedgerTime() < last.readyAt) delay(5_000)
        resources(server, "release, ${records + 2} records", confirmed("release", gw.submitTransfer(account, release, keySigner(owner))))

        val statuses = gw.readHolds(account).takeLast(2).map { it.status }
        assertEquals(listOf(HoldStatus.Cancelled, HoldStatus.Executed), statuses)
        assertEquals(UsdcAmount(150 * UNIT), gw.readBalance(stranger))
        assertEquals(UsdcAmount(330 * UNIT), gw.readBalance(account))
        log("done: ${records + 2} records, balance 330")
        server.close()
    }

    /** The declared resources of a landed transaction: the simulation's footprint plus the gateway's leeway. */
    private suspend fun resources(server: SorobanServer, label: String, hash: TxHash) {
        val r = server.getTransaction(hash.hex)
        val tx = AbstractTransaction.fromEnvelopeXdr(r.envelopeXdr!!, Network.TESTNET) as Transaction
        val res = tx.sorobanData!!.resources
        val ro = res.footprint.readOnly.size
        val rw = res.footprint.readWrite.size
        log(
            "resources $label: ${hash.hex} ledger=${r.ledger} instructions=${res.instructions.value} " +
                "footprint=${ro + rw} (readOnly=$ro readWrite=$rw) diskReadBytes=${res.diskReadBytes.value} writeBytes=${res.writeBytes.value}",
        )
    }

    private suspend fun logNetworkLimits(server: SorobanServer) {
        val ids = listOf(
            ConfigSettingIDXdr.CONFIG_SETTING_CONTRACT_COMPUTE_V0,
            ConfigSettingIDXdr.CONFIG_SETTING_CONTRACT_LEDGER_COST_V0,
            ConfigSettingIDXdr.CONFIG_SETTING_CONTRACT_LEDGER_COST_EXT_V0,
            ConfigSettingIDXdr.CONFIG_SETTING_CONTRACT_BANDWIDTH_V0,
        )
        val res = server.getLedgerEntries(ids.map { LedgerKeyXdr.ConfigSetting(LedgerKeyConfigSettingXdr(it)) })
        log("network limits at ledger ${res.latestLedger}:")
        res.entries.orEmpty().forEach { log("  ${it.parseXdr().toXdrJson()}") }
    }
}
