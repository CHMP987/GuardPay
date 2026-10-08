package com.guardpay.android

import android.content.Context
import android.os.Build
import android.util.Log
import com.guardpay.android.signing.BiometricSigningPrompt
import com.guardpay.android.signing.KeystoreSigner
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.simulation.simulatedSides
import com.guardpay.shared.stellar.KmpStellarGateway
import com.guardpay.shared.stellar.TestnetConfig
import com.guardpay.shared.stellar.ed25519AccountId
import com.guardpay.shared.ui.MessageReader
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.guardian.GuardianChain
import com.guardpay.shared.ui.guardian.GuardianConfig
import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.ProofLinks
import com.guardpay.shared.ui.owner.OwnerConfig
import com.guardpay.shared.ui.owner.OwnerSession
import com.guardpay.shared.ui.wiring.GatewayGuardianChain
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Debug: the demo's two sides, on one phone or on two.
 *
 * Testnet (P6): on Android 13+ the owner's and the guardian's ed25519 keys are made
 * in the Keystore under their own aliases, and every signature asks for the
 * fingerprint or screen lock. Their G addresses (public) are written to
 * `keys.json` in the app's files dir; the provisioning test deploys an account for
 * them, and its `testnet.json` is pushed back there. With that file the app talks
 * to those contracts. The guardian's key only ever signs `cancel`.
 *
 * The role comes from which of this phone's keys `testnet.json` names: both (the two
 * sides on one phone), only the owner's (the owner's phone; Soy guardián is off) or
 * only the guardian's (the guardian's phone; it polls for holds, Entrar is off). For
 * two phones, provision with the owner G of one and the guardian G of the other and
 * push the same file to both.
 *
 * Otherwise: an in-memory simulation with random addresses, under the "Simulación"
 * banner and with no hashes or proofs.
 */
object Wiring {
    private const val TAG = "GuardPayWiring"
    private const val OWNER = "Laura"
    private const val GUARDIAN = "Diego"
    private const val OWNER_ALIAS = "guardpay-owner"
    private const val GUARDIAN_ALIAS = "guardpay-guardian"
    private val CONTACT_NAMES = listOf("Mamá", "Andrés", "Sofía")

    class Watched(val account: String, val chain: GuardianChain)

    fun sessions(context: Context, nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions {
        val real = realSetup(context)
        return if (real != null) chain(real, nav, scope, reader) else simulated(nav, scope, reader)
    }

    /** What [com.guardpay.android.notifications.HoldWatchService] polls; null in simulation. */
    fun watchedChain(context: Context): Watched? {
        val real = realSetup(context) ?: return null
        return Watched(real.config.account, guardianChain(real) ?: return null)
    }

    /** At least one of [owner] / [guardian] is set: the roles this phone holds. */
    private class Real(val config: TestnetConfig, val owner: Signer?, val guardian: Signer?)

    private fun realSetup(context: Context): Real? {
        if (Build.VERSION.SDK_INT < 33) return null
        val dir = context.getExternalFilesDir(null) ?: return null
        val (owner, guardian) = try {
            KeystoreSigner(OWNER_ALIAS, BiometricSigningPrompt({ VisibleActivity.current }, "Firmar en Stellar")) to
                KeystoreSigner(GUARDIAN_ALIAS, BiometricSigningPrompt({ VisibleActivity.current }, "Detener el pago"))
        } catch (e: Exception) {
            // Keys that need a fingerprint or screen lock cannot exist without one.
            Log.w(TAG, "Keystore keys unavailable (${e::class.simpleName}); is a screen lock set?")
            return null
        }
        val ownerId = ed25519AccountId(owner.publicKey).value
        val guardianId = ed25519AccountId(guardian.publicKey).value
        File(dir, "keys.json").writeText("{\n  \"owner\": \"$ownerId\",\n  \"guardian\": \"$guardianId\"\n}\n")

        val file = File(dir, "testnet.json")
        if (!file.exists()) return null
        val config = try {
            TestnetConfig.parse(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "testnet.json rejected: ${e.message}")
            return null
        }
        val isOwner = config.owner == ownerId
        val isGuardian = config.guardian == guardianId
        if (!isOwner && !isGuardian) {
            Log.w(TAG, "testnet.json was provisioned for other keys; provision again")
            return null
        }
        if (!isOwner || !isGuardian) Log.i(TAG, if (isOwner) "owner's phone" else "guardian's phone")
        return Real(config, owner.takeIf { isOwner }, guardian.takeIf { isGuardian })
    }

    private fun gateway(c: TestnetConfig) = KmpStellarGateway(c.network())

    private fun guardianChain(real: Real) = real.guardian?.let {
        GatewayGuardianChain(gateway(real.config), StellarAddress(real.config.account), it)
    }

    private fun chain(real: Real, nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions {
        val c = real.config
        val account = StellarAddress(c.account)
        val names = c.contacts.zip(CONTACT_NAMES).associate { (a, n) -> StellarAddress(a) to n } +
            (StellarAddress(c.guardian) to GUARDIAN)
        val links = ProofLinks(
            explorerBase = "https://stellar.expert/explorer/testnet",
            registry = StellarAddress(c.holdRegistry),
            policy = StellarAddress(c.guardianHold),
        )
        val owner = real.owner?.let {
            OwnerSession(
                gateway(c), it,
                // The deployed registry lets only the guardian cancel.
                OwnerConfig(account, OWNER, GUARDIAN, DataSource.Chain, links, names, ownerMayStop = false),
                reader, nav, scope,
            )
        }
        val guardianSide = guardianChain(real)?.let {
            GuardianSession(it, GuardianConfig(OWNER, GUARDIAN, DataSource.Chain, links, names), scope)
        }
        return Sessions(owner, guardianSide, simulation = false, watchable = guardianSide != null)
    }

    private fun simulated(nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions {
        val sides = simulatedSides(nav, scope, reader, OWNER, GUARDIAN, CONTACT_NAMES)
        return Sessions(sides.owner, sides.guardian, simulation = true)
    }
}
