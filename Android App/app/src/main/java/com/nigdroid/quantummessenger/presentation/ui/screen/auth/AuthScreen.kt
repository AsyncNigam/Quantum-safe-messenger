package com.nigdroid.quantummessenger.presentation.ui.screen.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthState
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthViewModel
import com.nigdroid.quantummessenger.util.Constants
import kotlinx.coroutines.launch
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Root Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState      by viewModel.authState.collectAsState()
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess((authState as AuthState.Success).userId)
        }
    }

    val handleGoogleSignIn = {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(Constants.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                handleSignInResult(result, viewModel)
            } catch (e: GetCredentialException) {
                Log.e("AuthScreen", "Google Error: ${e.message}")
                val errorMsg = when {
                    e.message?.contains("7")  == true -> "Network Error — check connection"
                    e.message?.contains("10") == true -> "Config Error — check SHA-1"
                    e.message?.contains("16") == true -> "Cancelled"
                    else -> e.message ?: "Sign-in failed"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("AuthScreen", "General Error: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AuthScreenContent(
        authState      = authState,
        onGoogleClick  = { handleGoogleSignIn() },
        onRetry        = { viewModel.retryLogin() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen Layout (decoupled for preview)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuthScreenContent(
    authState: AuthState,
    onGoogleClick: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Aurora background ────────────────────────────────────────────────
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        // ── Main content ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(0.dp))

            // ── Hero section ─────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuantumOrb()
                Spacer(Modifier.height(36.dp))
                Text(
                    text       = "Quantum Messenger",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = QuantumColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text      = "Post-quantum secure messaging.\nNobody sees your conversations.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = QuantumColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            // ── Auth state content ───────────────────────────────────────────
            AnimatedContent(
                targetState   = authState,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(200))
                },
                label = "authStateTransition"
            ) { state ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is AuthState.Idle         -> IdleContent(onGoogleClick = onGoogleClick)
                        is AuthState.Authenticating -> AuthLoadingContent("Authenticating with Google…")
                        is AuthState.GeneratingKeys -> AuthLoadingContent("Generating your quantum keys…")
                        is AuthState.Uploading      -> AuthLoadingContent("Securing your identity…")
                        is AuthState.Success        -> SuccessContent()
                        is AuthState.Error          -> ErrorContent(state.message, onRetry)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

/** Animated glowing orb — the visual centerpiece of the auth screen */
@Composable
private fun QuantumOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue  = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 0.65f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbGlow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulse)
                .background(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = glow * 0.5f),
                            QuantumColors.Accent.copy(alpha  = glow * 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Glass orb
        Box(
            modifier = Modifier
                .size(100.dp)
                .glassmorphism(cornerRadius = 50, overlayAlpha = 0.18f, usePrimaryTint = true)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = 0.4f),
                            QuantumColors.Accent.copy(alpha  = 0.25f)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(100f, 100f)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Security,
                contentDescription = "Quantum Shield",
                tint               = Color.White,
                modifier           = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun IdleContent(onGoogleClick: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Security badges row ──────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            SecurityBadge("🔐 E2E Encrypted")
            SecurityBadge("⚛️ Post-Quantum")
            SecurityBadge("🔒 Zero-Knowledge")
        }

        // ── Google sign-in button ────────────────────────────────────────────
        Button(
            onClick  = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape  = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor   = Color(0xFF1F1F1F)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            )
        ) {
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.Center
            ) {
                // Google "G" colored logo approximation
                Text(
                    text  = "G",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 20.sp,
                    color      = Color(0xFF4285F4)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text       = "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    color      = Color(0xFF1F1F1F)
                )
            }
        }

        // ── Separator ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color    = QuantumColors.GlassBorder,
                thickness = 0.5.dp
            )
            Text(
                text     = "  End-to-end encrypted  ",
                style    = MaterialTheme.typography.labelSmall,
                color    = QuantumColors.TextTertiary
            )
            Divider(
                modifier = Modifier.weight(1f),
                color    = QuantumColors.GlassBorder,
                thickness = 0.5.dp
            )
        }

        Text(
            text      = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
            style     = MaterialTheme.typography.labelSmall,
            color     = QuantumColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SecurityBadge(label: String) {
    Box(
        modifier = Modifier
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(
                color = QuantumColors.GlassWhite08,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = QuantumColors.TextSecondary
        )
    }
}

@Composable
private fun AuthLoadingContent(message: String) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animated progress ring
        val infiniteTransition = rememberInfiniteTransition(label = "progress")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue  = 360f,
            animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
            label = "rotation"
        )
        Box(
            modifier         = Modifier
                .size(64.dp)
                .rotate(rotation)
                .glassmorphism(cornerRadius = 32, overlayAlpha = 0.15f, usePrimaryTint = true)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            QuantumColors.Primary,
                            QuantumColors.Accent,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
        )

        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyLarge,
            color     = QuantumColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent() {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val scale by animateFloatAsState(
            targetValue   = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "successScale"
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(QuantumColors.Success.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 40.sp, color = QuantumColors.Success, fontWeight = FontWeight.Bold)
        }
        Text(
            text  = "Identity Secured",
            style = MaterialTheme.typography.headlineSmall,
            color = QuantumColors.TextPrimary
        )
        Text(
            text  = "Redirecting you now…",
            style = MaterialTheme.typography.bodyMedium,
            color = QuantumColors.TextTertiary
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚠️", fontSize = 48.sp)
        Text(
            text  = "Authentication Failed",
            style = MaterialTheme.typography.titleLarge,
            color = QuantumColors.Error
        )
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = QuantumColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick  = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QuantumColors.Primary
            )
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun handleSignInResult(result: GetCredentialResponse, viewModel: AuthViewModel) {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val token = GoogleIdTokenCredential.createFrom(credential.data)
        viewModel.signInWithGoogle(idToken = token.idToken, email = token.id, nonce = null)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Idle")
@Composable
private fun PreviewAuthIdle() {
    QuantumMessengerTheme {
        AuthScreenContent(
            authState    = AuthState.Idle,
            onGoogleClick = {},
            onRetry      = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Loading")
@Composable
private fun PreviewAuthLoading() {
    QuantumMessengerTheme {
        AuthScreenContent(
            authState    = AuthState.Authenticating,
            onGoogleClick = {},
            onRetry      = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Error")
@Composable
private fun PreviewAuthError() {
    QuantumMessengerTheme {
        AuthScreenContent(
            authState    = AuthState.Error("Developer Console Error: Check SHA-1 and Package Name", null),
            onGoogleClick = {},
            onRetry      = {}
        )
    }
}
