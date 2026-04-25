package com.nigdroid.quantummessenger

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.nigdroid.quantummessenger.presentation.navigation.AppNavigation
import com.nigdroid.quantummessenger.presentation.navigation.AuthRoute
import com.nigdroid.quantummessenger.presentation.ui.screen.LockedScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.VaultCompromisedScreen
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.viewmodel.LockState
import com.nigdroid.quantummessenger.presentation.viewmodel.MainViewModel
import com.nigdroid.quantummessenger.security.BiometricPromptManager
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
        enableEdgeToEdge()

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

                    // ── Screen stack ──────────────────────────────────────────
                    AnimatedContent(
                        targetState   = lockState,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                        },
                        label = "lockStateTransition"
                    ) { state ->
                        when (state) {
                            is LockState.Locked -> {
                                LockedScreen(
                                    onUnlockClick = { triggerBiometricUnlock() }
                                )
                            }

                            is LockState.BiometricError -> {
                                LockedScreen(
                                    onUnlockClick = {
                                        viewModel.retryBiometric()
                                        triggerBiometricUnlock()
                                    },
                                    errorMessage = state.message
                                )
                            }

                            is LockState.VaultCompromised -> {
                                VaultCompromisedScreen(
                                    onWipeAndReRegister = { viewModel.executeVaultWipe() },
                                    isWiping = false
                                )
                            }

                            is LockState.Wiping -> {
                                VaultCompromisedScreen(
                                    onWipeAndReRegister = {},
                                    isWiping = true
                                )
                            }

                            is LockState.WipeComplete -> {
                                // After wipe, show AuthScreen for re-registration
                                val navController = rememberNavController()
                                AppNavigation(
                                    navController    = navController,
                                    startDestination = AuthRoute
                                )
                            }

                            is LockState.Unlocked -> {
                                if (startDestination != null) {
                                    val navController = rememberNavController()
                                    AppNavigation(
                                        navController    = navController,
                                        startDestination = startDestination!!
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
    }

    // ── Biometric Prompt ──────────────────────────────────────────────────────

    private fun triggerBiometricUnlock() {
        biometricPromptManager.showBiometricPrompt(
            activity = this,
            title    = "Unlock Quantum Messenger",
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
