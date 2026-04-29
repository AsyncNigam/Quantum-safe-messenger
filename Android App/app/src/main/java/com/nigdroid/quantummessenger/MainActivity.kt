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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var biometricPromptManager: BiometricPromptManager

    private var biometricTriggeredThisResume = false

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            viewModel.lock()
            biometricTriggeredThisResume = false
        }

        override fun onStart(owner: LifecycleOwner) {
            if (!biometricTriggeredThisResume && viewModel.lockState.value == LockState.Locked) {
                biometricTriggeredThisResume = true
                triggerBiometricUnlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        NotificationPermissionManager.requestNotificationPermission(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        setContent {
            QuantumMessengerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val lockState by viewModel.lockState.collectAsState()
                    val startDestination by viewModel.startDestination.collectAsState()

                    LaunchedEffect(lockState) {
                        if (lockState == LockState.Locked && !biometricTriggeredThisResume) {
                            biometricTriggeredThisResume = true
                            triggerBiometricUnlock()
                        }
                    }

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

                        if (isLocked) {
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

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
    }

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
                    is BiometricPromptManager.BiometricResult.Failed -> {}
                    is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                        viewModel.onBiometricUnavailable()
                    }
                }
            }
        )
    }
}
