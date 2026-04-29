package com.nigdroid.quantummessenger.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

class BiometricPromptManager @Inject constructor() {

    sealed class BiometricResult {
        object Success : BiometricResult()
        object Failed : BiometricResult()
        data class Error(val errorCode: Int, val error: String) : BiometricResult()
        object FeatureUnavailable : BiometricResult()
        data class TemporaryLockout(val message: String) : BiometricResult()
        data class PermanentLockout(val message: String) : BiometricResult()
    }

    fun showBiometricPrompt(
        activity   : FragmentActivity,
        title      : String = "Unlock Quantum Messenger",
        subtitle   : String = "Use your biometric to access your secure vault",
        onResult   : (BiometricResult) -> Unit
    ) {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val manager = BiometricManager.from(activity)
        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                launchPrompt(activity, title, subtitle, authenticators, onResult)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onResult(BiometricResult.FeatureUnavailable)
            }
            else -> {
                onResult(BiometricResult.FeatureUnavailable)
            }
        }
    }

    private fun launchPrompt(
        activity       : FragmentActivity,
        title          : String,
        subtitle       : String,
        authenticators : Int,
        onResult       : (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricResult.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        onResult(
                            BiometricResult.TemporaryLockout(
                                "Too many attempts. Please wait 30 seconds and try again."
                            )
                        )
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onResult(
                            BiometricResult.PermanentLockout(
                                "Biometric locked. Unlock with your device PIN to reset."
                            )
                        )
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                    else -> {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
