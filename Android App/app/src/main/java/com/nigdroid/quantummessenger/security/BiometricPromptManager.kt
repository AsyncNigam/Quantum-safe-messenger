package com.nigdroid.quantummessenger.security

import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import javax.inject.Inject

/**
 * Manager class to handle Biometric Authentication requests.
 */
class BiometricPromptManager @Inject constructor() {

    sealed class BiometricResult {
        object Success : BiometricResult()
        object Failed : BiometricResult()
        data class Error(val error: String) : BiometricResult()
        object FeatureUnavailable : BiometricResult()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Quantum Messenger",
        subtitle: String = "Authenticate to access your secure messages",
        onResult: (BiometricResult) -> Unit
    ) {
        val manager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onResult(BiometricResult.Success)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onResult(BiometricResult.Error(errString.toString()))
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onResult(BiometricResult.Failed)
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators)
                    .build()

                prompt.authenticate(promptInfo)
            }
            else -> onResult(BiometricResult.FeatureUnavailable)
        }
    }
}
