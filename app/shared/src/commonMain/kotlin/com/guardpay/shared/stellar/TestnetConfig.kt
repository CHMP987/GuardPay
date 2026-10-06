package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `testnet.json`: what the provisioning test deployed, public addresses only. The
 * debug app reads it to talk to real contracts. It never holds a seed: the owner's
 * and the guardian's keys live in the phone's Keystore and only their G addresses
 * appear here.
 */
@Serializable
data class TestnetConfig(
    val rpcUrl: String,
    val usdc: String,
    val holdRegistry: String,
    val guardianHold: String,
    val account: String,
    val owner: String,
    val guardian: String,
    val contacts: List<String>,
) {
    init {
        val all = listOf(usdc, holdRegistry, guardianHold, account, owner, guardian) + contacts
        require(all.all(StellarAddress::isValid)) { "testnet.json holds an invalid address" }
        require(listOf(usdc, holdRegistry, guardianHold, account).all { it.startsWith("C") }) { "contracts must be C strkeys" }
        require(owner.startsWith("G") && guardian.startsWith("G")) { "owner and guardian must be G accounts" }
        require(owner != guardian) { "the guardian must not be the owner" }
        require(contacts.size in 1..3 && contacts.distinct().size == contacts.size) { "1 to 3 distinct contacts" }
    }

    fun network(): GuardPayNetwork =
        GuardPayNetwork(rpcUrl, StellarAddress(usdc), StellarAddress(holdRegistry), StellarAddress(guardianHold))

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { prettyPrint = true }

        /** Throws on malformed JSON or on any address that fails the checks above. */
        fun parse(text: String): TestnetConfig = Json.decodeFromString(serializer(), text)
    }
}
