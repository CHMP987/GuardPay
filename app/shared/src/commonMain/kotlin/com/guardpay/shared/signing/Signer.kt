package com.guardpay.shared.signing

/**
 * Signs Soroban auth digests with a key that never leaves the device (P6: Android
 * Keystore; passkey is SHOULD). The owner's key is the account's only signer; the
 * guardian's key signs `cancel` on the registry and is never an account signer.
 *
 * A common interface rather than `expect`: platform implementations need platform
 * objects (Keystore alias, Activity for a passkey prompt) in their constructors.
 */
interface Signer {
    /** 32-byte ed25519 public key. */
    val publicKey: ByteArray

    /**
     * Signs the 32-byte digest `sha256(signature_payload ‖ xdr(context_rule_ids))`
     * (verified on testnet in Spike C) and returns the raw 64-byte signature.
     * Throws [SigningCancelledException] if the person dismisses the prompt.
     */
    suspend fun signAuthDigest(digest: ByteArray): ByteArray
}

class SigningCancelledException : Exception("signing cancelled by the user")
