package com.guardpay.android.signing

import android.app.KeyguardManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.guardpay.shared.stellar.ed25519AccountId
import com.soneso.stellar.sdk.KeyPair
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.PrivateKey
import kotlin.random.Random

/**
 * P6 on a real Keystore (Android 13+). Uses its own aliases, deleted after each
 * test, never the app's.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSignerTest {
    private val open = "guardpay-test-open"
    private val locked = "guardpay-test-locked"

    @After
    fun cleanUp() {
        KeystoreSigner.delete(open)
        KeystoreSigner.delete(locked)
    }

    @Test
    fun signaturesVerifyAgainstTheStellarAddressOfTheKey() = runBlocking {
        val signer = KeystoreSigner(open, prompt = null)
        val hash = Random.nextBytes(32)
        val sig = signer.signHash(hash)

        assertEquals(64, sig.size)
        val stellar = KeyPair.fromPublicKey(signer.publicKey)
        assertTrue("an independent ed25519 check accepts it", stellar.verify(hash, sig))
        assertFalse("and rejects it for another hash", stellar.verify(Random.nextBytes(32), sig))
        assertEquals(stellar.getAccountId(), ed25519AccountId(signer.publicKey).value)
    }

    @Test
    fun theSameAliasIsTheSameKeyAfterARestart() {
        val first = KeystoreSigner(open, prompt = null).publicKey
        assertTrue(first.contentEquals(KeystoreSigner(open, prompt = null).publicKey))
    }

    @Test
    fun thePrivateKeyCannotBeExported() {
        KeystoreSigner(open, prompt = null)
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(open, null) as PrivateKey
        assertNull("no encoded form leaves the Keystore", key.encoded)
    }

    @Test
    fun onlyHashesAreSigned() = runBlocking {
        val signer = KeystoreSigner(open, prompt = null)
        val refused = runCatching { signer.signHash(ByteArray(64)) }
        assertTrue(refused.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun aKeyThatNeedsThePersonDoesNotSignWithoutThem() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("needs a screen lock", context.getSystemService(KeyguardManager::class.java).isDeviceSecure)
        // A prompt that skips the person: hands the Signature back unauthenticated.
        val signer = KeystoreSigner(locked, prompt = { it })
        val refused = runCatching { signer.signHash(Random.nextBytes(32)) }
        assertTrue("the Keystore refuses: ${refused.exceptionOrNull()}", refused.isFailure)
    }
}
