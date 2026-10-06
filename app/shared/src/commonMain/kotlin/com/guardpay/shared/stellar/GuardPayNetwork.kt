package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress

/**
 * Where the app talks to. Testnet only: there is deliberately no passphrase field,
 * [KmpStellarGateway] always signs for `Network.TESTNET`, so a mainnet RPC URL
 * would only get signatures the mainnet rejects.
 *
 * Nothing here is hard-coded: the debug app reads it from `testnet.json`, written
 * by the provisioning tool with public addresses only.
 */
data class GuardPayNetwork(
    val rpcUrl: String,
    /** The token the policy enforces (a test SAC on testnet, not Circle's USDC). */
    val usdc: StellarAddress,
    /** This account's registry. The gateway refuses an account whose policy points at another. */
    val holdRegistry: StellarAddress,
    val guardianHold: StellarAddress,
    /** The account's single Default rule is id 0 (`evidence/stellar/deployment.md`). */
    val contextRuleIds: List<UInt> = listOf(0u),
) {
    init {
        require(rpcUrl.startsWith("https://")) { "RPC must be https" }
        require(listOf(usdc, holdRegistry, guardianHold).all { it.isContract }) {
            "contract addresses must be C strkeys"
        }
        require(contextRuleIds.isNotEmpty()) { "at least one context rule id" }
    }
}
