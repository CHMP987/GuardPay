package com.guardpay.android.signing

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.guardpay.shared.signing.Signer
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * P6: an ed25519 key made inside the Android Keystore. The private key never
 * leaves it: this class only ever holds a handle, signs through [Signature], and
 * nothing here logs or exports key material.
 *
 * Ed25519 in the Keystore needs Android 13 (API 33). With a [prompt], each
 * signature needs the person's fingerprint or screen lock (timeout 0: one
 * authentication, one signature). Without one the key needs no authentication;
 * only the instrumented tests do that, under their own alias.
 *
 * What the person approves is a 32-byte hash. The prompt cannot show destination
 * or amount, so it is no defence on its own: the hold and the guardian's view are.
 */
@RequiresApi(33)
class KeystoreSigner(
    private val alias: String,
    private val prompt: SigningPrompt?,
) : Signer {
    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override val publicKey: ByteArray = rawPublicKey(keyStore.getCertificate(alias)?.publicKey?.encoded ?: create())

    override suspend fun signHash(hash: ByteArray): ByteArray {
        require(hash.size == 32) { "only 32-byte hashes are signed" }
        val key = keyStore.getKey(alias, null) as PrivateKey
        val signature = Signature.getInstance(ALGORITHM).apply { initSign(key) }
        val authorized = prompt?.authorize(signature) ?: signature
        authorized.update(hash)
        return authorized.sign()
    }

    private fun create(): ByteArray {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("ed25519"))
            .setDigests(KeyProperties.DIGEST_NONE)
            .apply {
                if (prompt != null) {
                    setUserAuthenticationRequired(true)
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                }
            }
            .build()
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER)
        generator.initialize(spec)
        return generator.generateKeyPair().public.encoded
    }

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val ALGORITHM = "Ed25519"

        /** X.509 SubjectPublicKeyInfo prefix of an ed25519 key: the raw 32 bytes follow it. */
        private val SPKI_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)

        fun rawPublicKey(spki: ByteArray): ByteArray {
            require(spki.size == 44 && spki.copyOfRange(0, 12).contentEquals(SPKI_PREFIX)) { "not an ed25519 public key" }
            return spki.copyOfRange(12, 44)
        }

        /** Removes the key. Only for the instrumented tests' own alias. */
        fun delete(alias: String) {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(alias)
        }
    }
}

/** Unlocks one [Signature] with the person's fingerprint or screen lock. */
fun interface SigningPrompt {
    /** Throws `SigningCancelledException` when the person dismisses it. */
    suspend fun authorize(signature: Signature): Signature
}
