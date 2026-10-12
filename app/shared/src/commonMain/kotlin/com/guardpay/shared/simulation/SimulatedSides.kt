package com.guardpay.shared.simulation

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

class SimulatedSides(val owner: OwnerSession, val guardian: GuardianSession)

/**
 * Both sides of the demo over [SimulatedStellarGateway], with random addresses, under
 * the "Simulación" banner and with no hashes or proofs. Used by Android debug builds
 * without `testnet.json` and by the iOS app.
 */
fun simulatedSides(
    nav: Navigator,
    scope: CoroutineScope,
    reader: MessageReader,
    ownerName: String = "Laura",
    guardianName: String = "Diego",
    contactNames: List<String> = listOf("Mamá", "Andrés", "Sofía"),
): SimulatedSides {
    val ownerKey = Random.nextBytes(32)
    val guardianKey = Random.nextBytes(32)
    val account = contractIdOf(Random.nextBytes(32))
    val policy = contractIdOf(Random.nextBytes(32))
    val guardian = ed25519AccountId(guardianKey)
    val contacts = contactNames.map { TrustedContact(it, ed25519AccountId(Random.nextBytes(32))) }
    val rules = AccountRules(
        guardian = guardian,
        dailyCap = UsdcAmount.ofWhole(100),
        holdDurationSeconds = 120,
        expiryWindowSeconds = 900,
    )
    val gateway = SimulatedStellarGateway(account, ownerKey, guardianKey, policy, rules, contacts, UsdcAmount.ofWhole(500))
    val names = contacts.associate { it.address to it.name } + (guardian to guardianName)
    val links = ProofLinks(policy = policy)

    val owner = OwnerSession(
        gateway, SimulatedSigner(ownerKey),
        OwnerConfig(account, ownerName, guardianName, DataSource.Simulation, links, names),
        reader, nav, scope,
    )
    val guardianSide = GuardianSession(
        GatewayGuardianChain(gateway, account, SimulatedSigner(guardianKey)),
        GuardianConfig(ownerName, guardianName, DataSource.Simulation, links, names),
        scope,
    )
    return SimulatedSides(owner, guardianSide)
}
