package com.guardpay.shared.signing

/**
 * Signs 32-byte hashes with an ed25519 key that never leaves the device (P6:
 * Android Keystore; passkey is SHOULD). The owner's key is the account's only
 * signer, as `Signer::Delegated(owner G)`; the guardian's key signs `cancel` on the
 * registry and is never an account signer.
 *
 * The key's G account is also the transaction source, so the one hash it signs is
 * the transaction hash. That envelope signature also authorizes the source-account
 * entry `account.__check_auth(digest)`, which is how a `Delegated` signer proves
 * itself to the account. The gateway builds the hash from the transaction it
 * checked; a signer never chooses what it signs.
 *
 * A common interface rather than `expect`: platform implementations need platform
 * objects (Keystore alias, Activity for a passkey prompt) in their constructors.
 */
interface Signer {
    /** 32-byte ed25519 public key. */
    val publicKey: ByteArray

    /**
     * Signs [hash] (32 bytes) and returns the raw 64-byte ed25519 signature.
     * Throws [SigningCancelledException] if the person dismisses the prompt.
     */
    suspend fun signHash(hash: ByteArray): ByteArray
}

class SigningCancelledException : Exception("signing cancelled by the user")

/**
 * Signs [hash], or returns null when the key itself failed (permission, invalidated
 * key, Keystore error) so callers can tell that apart from a network failure.
 * A dismissed prompt still throws [SigningCancelledException].
 */
suspend fun signWith(signer: Signer, hash: ByteArray): ByteArray? = try {
    signer.signHash(hash)
} catch (e: SigningCancelledException) {
    throw e
} catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
