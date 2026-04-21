package com.nigdroid.quantummessenger.presentation.ui.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthState
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthViewModel
import kotlin.math.sin

/**
 * Authentication Screen - Premium Glassmorphism UI
 *
 * Features:
 * - Animated mesh gradient background
 * - Glass-morphic input fields and buttons
 * - Fluid state transitions (Loading, Success, Error)
 * - Cryptographic mesh animation during key generation
 * - Edge-to-edge layout with system insets
 *
 * Usage:
 *   AuthScreen(
 *       onAuthSuccess = { userId -> navigateToChat(userId) }
 *   )
 */
@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Navigate on success
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val userId = (authState as AuthState.Success).userId
            onAuthSuccess(userId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Animated background
        AnimatedMeshGradientBackground(
            modifier = Modifier.fillMaxSize(),
            isDarkTheme = false
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            AuthHeader()

            Spacer(modifier = Modifier.height(48.dp))

            // Content based on state
            when (authState) {
                is AuthState.Idle -> {
                    IdleContent(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it },
                        onLoginClick = {
                            focusManager.clearFocus()
                            viewModel.secureLogin(phoneNumber)
                        }
                    )
                }

                is AuthState.GeneratingKeys -> {
                    GeneratingKeysContent()
                }

                is AuthState.Uploading -> {
                    UploadingContent()
                }

                is AuthState.Success -> {
                    SuccessContent(userId = (authState as AuthState.Success).userId)
                }

                is AuthState.Error -> {
                    val errorState = authState as AuthState.Error
                    ErrorContent(
                        message = errorState.message,
                        onRetry = {
                            viewModel.retryLogin(phoneNumber)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Header with app title and description
 */
@Composable
private fun AuthHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quantum Messenger",
            fontSize = 32.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Post-Quantum Secure Messaging",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Idle state - Phone number input and login button
 */
@Composable
private fun IdleContent(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Phone input field with glassmorphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .glassmorphism(
                    blurRadius = 20f,
                    cornerRadius = 16,
                    isDarkTheme = false
                )
                .background(Color(0x4DFFFFFF))
                .padding(4.dp)
        ) {
            TextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+1 (234) 567-8900", fontSize = 14.sp) },
                label = { Text("Phone Number", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onLoginClick() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secure Login button with glow effect
        GlowingButton(
            text = "Secure Login",
            onClick = onLoginClick,
            enabled = phoneNumber.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info text
        Text(
            text = "Your cryptographic keys will be generated on-device and never shared with our servers.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Loading state - Cryptographic key generation animation
 */
@Composable
private fun GeneratingKeysContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Cryptographic mesh animation
        CryptographicMeshAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Generating Cryptographic Identity",
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Generating ML-KEM & ML-DSA keypairs\nPlease wait, this may take a moment...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Uploading state - Registering with server
 */
@Composable
private fun UploadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Registering with Server",
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Uploading public keys securely...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Success state - Registration complete
 */
@Composable
private fun SuccessContent(userId: String) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "✓",
                fontSize = 72.sp,
                color = Color.Green
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Identity Created",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your cryptographic identity is ready",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Error state - Show error and retry option
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Authentication Error",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlowingButton(
                text = "Retry",
                onClick = onRetry
            )
        }
    }
}

/**
 * Glowing button with animation
 */
@Composable
private fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366FF).copy(alpha = glowAlpha),
                        Color(0xFF8E6FA8).copy(alpha = glowAlpha * 0.7f)
                    )
                )
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

/**
 * Cryptographic mesh animation for key generation
 */
@Composable
private fun CryptographicMeshAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshAnimation")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 6000, easing = LinearEasing)
        ),
        label = "offset2"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x4D6366FF).copy(alpha = 0.6f + sin((offset1 * Math.PI).toFloat() / 180f) * 0.2f),
                        Color(0x4D00BCD4).copy(alpha = 0.4f + sin((offset2 * Math.PI).toFloat() / 180f) * 0.2f),
                        Color(0x4D8E6FA8).copy(alpha = 0.3f)
                    ),
                    center = Offset(
                        0.5f + sin((offset1 * Math.PI / 180.0)).toFloat() * 0.3f,
                        0.5f + sin((offset2 * Math.PI / 180.0)).toFloat() * 0.3f
                    ),
                    radius = 400f
                )
            )
    ) {
        CircularProgressIndicator(
            color = Color.Transparent,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(0.6f)
        )
    }
}

