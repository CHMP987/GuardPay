package com.guardpay.android.signing

import android.app.Activity
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.guardpay.shared.signing.SigningCancelledException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.Signature
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The system's own prompt (fingerprint, or PIN/pattern), bound to one [Signature]
 * through a CryptoObject: the Keystore releases that one signature and no other.
 * [activity] is the visible screen, or null when the app is in the background.
 */
@RequiresApi(33)
class BiometricSigningPrompt(
    private val activity: () -> Activity?,
    private val title: String,
) : SigningPrompt {
    override suspend fun authorize(signature: Signature): Signature = withContext(Dispatchers.Main) {
        val host = activity() ?: throw SigningCancelledException()
        suspendCancellableCoroutine { cont ->
            val cancel = CancellationSignal()
            cont.invokeOnCancellation { cancel.cancel() }
            BiometricPrompt.Builder(host)
                .setTitle(title)
                .setSubtitle("Confirma con tu huella o tu bloqueo de pantalla")
                .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
                .build()
                .authenticate(
                    BiometricPrompt.CryptoObject(signature),
                    cancel,
                    host.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            val unlocked = result.cryptoObject?.signature
                            if (unlocked == null) cont.resumeWithException(SigningCancelledException()) else cont.resume(unlocked)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            if (cont.isActive) cont.resumeWithException(SigningCancelledException())
                        }
                    },
                )
        }
    }
}
