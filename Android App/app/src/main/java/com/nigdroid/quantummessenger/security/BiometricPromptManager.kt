package com.nigdroid.quantummessenger.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

/**
 * BiometricPromptManager — production-grade biometric authentication.
 *
 * Features:
 *  - BIOMETRIC_STRONG only (no weak biometrics like face recognition without IR)
 *  - DEVICE_CREDENTIAL as fallback on API 30+
 *  - Distinguishes lockout from soft failure for precise UI messaging
 *  - Singleton via Hilt — safe to inject into Activity
 */
class BiometricPromptManager @Inject constructor() {

    // ── Result sealed class ───────────────────────────────────────────────────

    sealed class BiometricResult {
        /** User authenticated successfully */
        object Success : BiometricResult()

        /** Single attempt failed — prompt retries automatically */
        object Failed : BiometricResult()

        /** Non-recoverable error or user cancelled */
        data class Error(
            val errorCode: Int,
            val error: String
        ) : BiometricResult()

        /** Device has no biometric hardware or none enrolled */
        object FeatureUnavailable : BiometricResult()

        /** Biometric temporarily locked due to too many attempts */
        data class TemporaryLockout(val message: String) : BiometricResult()

        /** Biometric permanently locked — device credential required to reset */
        data class PermanentLockout(val message: String) : BiometricResult()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun showBiometricPrompt(
        activity   : FragmentActivity,
        title      : String = "Unlock Quantum Messenger",
        subtitle   : String = "Use your biometric to access your secure vault",
        onResult   : (BiometricResult) -> Unit
    ) {
        // API 30+ allows BIOMETRIC_STRONG + DEVICE_CREDENTIAL together
        // Below API 30, BIOMETRIC_STRONG only (device credential handled via fallback)
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

    // ── Internal ──────────────────────────────────────────────────────────────

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
                // Single attempt failed — prompt automatically shows "Try again"
                onResult(BiometricResult.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        // Too many attempts — temporary lockout (30s)
                        onResult(
                            BiometricResult.TemporaryLockout(
                                "Too many attempts. Please wait 30 seconds and try again."
                            )
                        )
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        // Permanent lockout — must use device PIN/password to reset
                        onResult(
                            BiometricResult.PermanentLockout(
                                "Biometric locked. Unlock with your device PIN to reset."
                            )
                        )
                    }
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        // User dismissed — treat as soft error, allow retry
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
            // Note: setNegativeButtonText cannot be used with DEVICE_CREDENTIAL
            .build()

        prompt.authenticate(promptInfo)
    }
}
