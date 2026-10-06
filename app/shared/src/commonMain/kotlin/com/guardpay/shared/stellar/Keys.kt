package com.guardpay.shared.stellar

import com.guardpay.shared.domain.StellarAddress
import com.soneso.stellar.sdk.StrKey

/** The G strkey of a 32-byte ed25519 public key (an account signer or the guardian's key). */
fun ed25519AccountId(publicKey: ByteArray): StellarAddress {
    require(publicKey.size == 32) { "ed25519 public keys are 32 bytes" }
    return StellarAddress(StrKey.encodeEd25519PublicKey(publicKey))
}

/** A `C…` strkey for 32 contract-id bytes. */
fun contractIdOf(bytes: ByteArray): StellarAddress = StellarAddress(StrKey.encodeContract(bytes))

