package com.nigdroid.quantummessenger

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.nigdroid.quantummessenger.presentation.navigation.AppNavigation
import com.nigdroid.quantummessenger.presentation.navigation.AuthRoute
import com.nigdroid.quantummessenger.presentation.navigation.HomeRoute
import com.nigdroid.quantummessenger.presentation.ui.screen.LockedScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.VaultCompromisedScreen
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.viewmodel.LockState
import com.nigdroid.quantummessenger.presentation.viewmodel.MainViewModel
import com.nigdroid.quantummessenger.security.BiometricPromptManager
import com.nigdroid.quantummessenger.security.NotificationPermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity — the single-Activity entry point.
 *
 * Security flow:
 *   1. App starts → LockedScreen (biometric gate)
 *   2. Biometric success → validate Keystore key integrity
 *   3. Key intact → Unlocked → AppNavigation
 *   4. Key invalidated (KPIE) → VaultCompromisedScreen → wipe → AuthScreen
 *   5. App backgrounded (ON_STOP) → lock immediately
 *   6. App resumed (ON_START) → LockedScreen + biometric prompt again
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var biometricPromptManager: BiometricPromptManager

    // Track if we've already triggered biometric this lifecycle to avoid double-prompts
    private var biometricTriggeredThisResume = false

    // ── Lifecycle observer for lock-on-background ─────────────────────────────
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // Lock immediately when app goes to background
            viewModel.lock()
            biometricTriggeredThisResume = false
        }

        override fun onStart(owner: LifecycleOwner) {
            // Auto-trigger biometric when app returns to foreground
            if (!biometricTriggeredThisResume && viewModel.lockState.value == LockState.Locked) {
                biometricTriggeredThisResume = true
                triggerBiometricUnlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Disable screenshots and screen recording
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()

        // Request notification permission (Android 13+)
        NotificationPermissionManager.requestNotificationPermission(this)

        // Register process-level lifecycle observer (survives config changes)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        setContent {
            QuantumMessengerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val lockState by viewModel.lockState.collectAsState()
                    val startDestination by viewModel.startDestination.collectAsState()

                    // Auto-trigger biometric on first composition if locked
                    LaunchedEffect(lockState) {
                        if (lockState == LockState.Locked && !biometricTriggeredThisResume) {
                            biometricTriggeredThisResume = true
                            triggerBiometricUnlock()
                        }
                    }

                    // ── Screen stack ─────────────────────────────────────────────
                    Box(modifier = Modifier.fillMaxSize()) {
                        val isLocked = lockState != LockState.Unlocked && lockState != LockState.WipeComplete

                        if (startDestination != null || lockState is LockState.WipeComplete) {
                            val navController = rememberNavController()
                            AppNavigation(
                                navController    = navController,
                                startDestination = if (lockState is LockState.WipeComplete) AuthRoute else (startDestination ?: HomeRoute)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = QuantumColors.Primary)
                            }
                        }

                        // Authentication Overlay (LockedScreen)
                        if (isLocked) {
                            // Semi-transparent background to hide the UI beneath efficiently
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                            )

                            when (lockState) {
                                is LockState.VaultCompromised, is LockState.Wiping -> {
                                    VaultCompromisedScreen(
                                        onWipeAndReRegister = { viewModel.executeVaultWipe() },
                                        isWiping = lockState is LockState.Wiping
                                    )
                                }
                                is LockState.Locked -> {
                                    LockedScreen(
                                        onUnlockClick = { triggerBiometricUnlock() }
                                    )
                                }
                                is LockState.BiometricError -> {
                                    LockedScreen(
                                        onUnlockClick = { triggerBiometricUnlock() },
                                        errorMessage = (lockState as LockState.BiometricError).message
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    // Removed AuthenticationDialog (reverting to LockedScreen)

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
    }

    // ── Biometric Prompt ──────────────────────────────────────────────────────

    private fun triggerBiometricUnlock() {
        biometricPromptManager.showBiometricPrompt(
            activity = this,
            title    = "Unlock Quantum Safe",
            subtitle = "Use your biometric to access your secure vault",
            onResult = { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.Success -> {
                        viewModel.onBiometricSuccess()
                    }
                    is BiometricPromptManager.BiometricResult.Error -> {
                        viewModel.onBiometricError(result.error)
                    }
                    is BiometricPromptManager.BiometricResult.TemporaryLockout -> {
                        viewModel.onBiometricError(result.message)
                    }
                    is BiometricPromptManager.BiometricResult.PermanentLockout -> {
                        viewModel.onBiometricError(result.message)
                    }
                    is BiometricPromptManager.BiometricResult.Failed -> {
                        // Single attempt failed — prompt retries automatically, no state change
                    }
                    is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                        // No biometric hardware — skip gate, validate key directly
                        viewModel.onBiometricUnavailable()
                    }
                }
            }
        )
    }
}
