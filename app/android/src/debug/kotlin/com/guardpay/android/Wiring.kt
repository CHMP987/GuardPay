package com.guardpay.android

import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.contractIdOf
import com.guardpay.shared.stellar.ed25519AccountId
import com.guardpay.shared.ui.MessageReader
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.guardian.GuardianConfig
import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.ProofLinks
import com.guardpay.shared.ui.owner.OwnerConfig
import com.guardpay.shared.ui.owner.OwnerSession
import com.guardpay.shared.ui.wiring.GatewayGuardianChain
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random

/**
 * Debug: both sides of the demo on one phone, against [SimulatedStellarGateway].
 * Every address is 32 random bytes made on launch, kept in memory only: no secret
 * key exists at all, since the simulated signer verifies nothing. Every screen
 * carries the "Simulación" banner and shows no hashes or proofs.
 */
object Wiring {
    private const val OWNER = "Laura"
    private const val GUARDIAN = "Diego"

    fun sessions(nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions {
        val ownerKey = Random.nextBytes(32)
        val guardianKey = Random.nextBytes(32)
        val account = contractIdOf(Random.nextBytes(32))
        val policy = contractIdOf(Random.nextBytes(32))
        val guardian = ed25519AccountId(guardianKey)
        val contacts = listOf("Mamá", "Andrés", "Sofía").map { TrustedContact(it, ed25519AccountId(Random.nextBytes(32))) }
        val rules = AccountRules(
            guardian = guardian,
            dailyCap = UsdcAmount.ofWhole(100),
            holdDurationSeconds = 120,
            expiryWindowSeconds = 900,
        )
        val gateway = SimulatedStellarGateway(account, ownerKey, guardianKey, policy, rules, contacts, UsdcAmount.ofWhole(500))
        val names = contacts.associate { it.address to it.name } + (guardian to GUARDIAN)
        val links = ProofLinks(policy = policy)

        val owner = OwnerSession(
            gateway, SimulatedSigner(ownerKey),
            OwnerConfig(account, OWNER, GUARDIAN, DataSource.Simulation, links, names),
            reader, nav, scope,
        )
        val guardianSide = GuardianSession(
            GatewayGuardianChain(gateway, account, SimulatedSigner(guardianKey)),
            GuardianConfig(OWNER, GUARDIAN, DataSource.Simulation, links, names),
            scope,
        )
        return Sessions(owner, guardianSide, simulation = true)
    }
}
