package com.nigdroid.quantummessenger

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nigdroid.quantummessenger.presentation.navigation.AppNavigation
import com.nigdroid.quantummessenger.presentation.ui.screen.LockedScreen
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.viewmodel.MainViewModel
import com.nigdroid.quantummessenger.security.BiometricPromptManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var biometricPromptManager: BiometricPromptManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            QuantumMessengerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isUnlocked by viewModel.isUnlocked.collectAsState()
                    val startDestination by viewModel.startDestination.collectAsState()
                    val navController = rememberNavController()

                    if (!isUnlocked) {
                        LockedScreen(
                            onUnlockClick = { triggerUnlock() }
                        )
                    } else if (startDestination != null) {
                        AppNavigation(
                            navController = navController,
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

    private fun triggerUnlock() {
        biometricPromptManager.showBiometricPrompt(
            activity = this,
            onResult = { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.Success -> {
                        viewModel.setUnlocked(true)
                    }
                    is BiometricPromptManager.BiometricResult.Error -> {
                        Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
                    }
                    is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                        // Fallback or bypass if device doesn't support biometrics
                        viewModel.setUnlocked(true)
                    }
                    else -> {}
                }
            }
        )
    }
}
