package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress

/**
 * Where the app talks to. Testnet only: there is deliberately no passphrase field,
 * [KmpStellarGateway] always signs for `Network.TESTNET`, so a mainnet RPC URL
 * would only get signatures the mainnet rejects.
 *
 * The contract addresses arrive from Persona 1 at Sync 2 (day 4). Nothing here is
 * hard-coded.
 */
data class GuardPayNetwork(
    val rpcUrl: String,
    val usdc: StellarAddress,
    val holdRegistry: StellarAddress,
    val guardianHold: StellarAddress,
    /** OZ ed25519 verifier contract used by the owner's External signer. */
    val ed25519Verifier: StellarAddress,
    /** [INFERENCE] The account's single Default rule has id 0 (true for Spike C's account). */
    val contextRuleIds: List<UInt> = listOf(0u),
) {
    init {
        require(rpcUrl.startsWith("https://")) { "RPC must be https" }
        require(listOf(usdc, holdRegistry, guardianHold, ed25519Verifier).all { it.isContract }) {
            "contract addresses must be C strkeys"
        }
        require(contextRuleIds.isNotEmpty()) { "at least one context rule id" }
    }
}
