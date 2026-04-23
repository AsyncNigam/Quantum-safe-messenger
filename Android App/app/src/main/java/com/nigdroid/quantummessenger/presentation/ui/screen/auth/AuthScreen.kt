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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthState
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthViewModel
import kotlin.math.sin

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

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
        AnimatedMeshGradientBackground(
            modifier = Modifier.fillMaxSize(),
            isDarkTheme = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader()

            Spacer(modifier = Modifier.height(48.dp))

            when (val state = authState) {
                is AuthState.Idle -> {
                    IdleContent(
                        email = email,
                        password = password,
                        isSignUp = isSignUp,
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onToggleMode = { isSignUp = !isSignUp },
                        onActionClick = {
                            focusManager.clearFocus()
                            if (isSignUp) {
                                viewModel.signUpWithEmail(email, password)
                            } else {
                                viewModel.loginWithEmail(email, password)
                            }
                        }
                    )
                }

                is AuthState.Authenticating -> {
                    AuthenticatingContent()
                }

                is AuthState.WaitingForEmailConfirmation -> {
                    EmailConfirmationContent(
                        email = state.email,
                        onConfirmed = { viewModel.onEmailConfirmed(state.email) }
                    )
                }

                is AuthState.GeneratingKeys -> {
                    GeneratingKeysContent()
                }

                is AuthState.Uploading -> {
                    UploadingContent()
                }

                is AuthState.Success -> {
                    SuccessContent(userId = state.userId)
                }

                is AuthState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.retryLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quantum Messenger", fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Post-Quantum Secure Messaging", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun IdleContent(
    email: String,
    password: String,
    isSignUp: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onActionClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).glassmorphism(20f, 16, false).background(Color(0x4DFFFFFF)).padding(4.dp)) {
            TextField(value = email, onValueChange = onEmailChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("email@example.com") }, label = { Text("Email") }, singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).glassmorphism(20f, 16, false).background(Color(0x4DFFFFFF)).padding(4.dp)) {
            TextField(value = password, onValueChange = onPasswordChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("••••••••") }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
        }
        Spacer(modifier = Modifier.height(24.dp))
        GlowingButton(text = if (isSignUp) "Create Account" else "Secure Login", onClick = onActionClick, enabled = email.isNotBlank() && password.length >= 6)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up", modifier = Modifier.clickable { onToggleMode() }, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmailConfirmationContent(email: String, onConfirmed: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Email Confirmation Sent", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please check $email and confirm your email address.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        GlowingButton(text = "I've Confirmed", onClick = onConfirmed)
    }
}

@Composable
private fun AuthenticatingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connecting to Supabase...")
    }
}

@Composable
private fun GeneratingKeysContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CryptographicMeshAnimation(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Generating Cryptographic Identity...", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun UploadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Uploading keys to your backend...")
    }
}

@Composable
private fun SuccessContent(userId: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", fontSize = 72.sp, color = Color.Green)
        Text("Identity Ready", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", fontSize = 64.sp)
        Text("Authentication Error", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.Red)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(24.dp))
        GlowingButton(text = "Back to Login", onClick = onRetry)
    }
}

@Composable
private fun GlowingButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(1500, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse))
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF6366FF).copy(alpha = glowAlpha), Color(0xFF8E6FA8).copy(alpha = glowAlpha * 0.7f))))) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
            Text(text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

@Composable
private fun CryptographicMeshAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset1 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)))
    val offset2 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)))
    Box(modifier = modifier.background(Brush.radialGradient(listOf(Color(0x4D6366FF).copy(alpha = 0.6f + sin((offset1 * Math.PI).toFloat() / 180f) * 0.2f), Color(0x4D00BCD4).copy(alpha = 0.4f + sin((offset2 * Math.PI).toFloat() / 180f) * 0.2f), Color(0x4D8E6FA8).copy(alpha = 0.3f)), center = Offset(0.5f + sin((offset1 * Math.PI / 180.0)).toFloat() * 0.3f, 0.5f + sin((offset2 * Math.PI / 180.0)).toFloat() * 0.3f), radius = 400f)))
}
