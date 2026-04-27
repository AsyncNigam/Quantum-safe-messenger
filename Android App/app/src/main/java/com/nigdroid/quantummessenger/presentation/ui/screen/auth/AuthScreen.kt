package com.nigdroid.quantummessenger.presentation.ui.screen.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthState
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Root Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess((authState as AuthState.Success).textFingerprint)
        }
    }

    AuthScreenContent(
        authState      = authState,
        onGenerateTap  = { viewModel.generateIdentity() },
        onRetry        = { viewModel.retry() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen layout (decoupled for preview)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuthScreenContent(
    authState: AuthState,
    onGenerateTap: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Aurora / mesh gradient background ───────────────────────────────
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        // ── Main content ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Hero section ─────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuantumOrb()
                Spacer(Modifier.height(32.dp))
                Text(
                    text       = "Quantum Safe",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = QuantumColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text      = "Post-quantum secure messaging.\nYour identity is purely cryptographic.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = QuantumColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            // ── Auth state panel ─────────────────────────────────────────────
            AnimatedContent(
                targetState   = authState,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                label = "authStateTransition"
            ) { state ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is AuthState.Idle           -> IdlePanel(onGenerateTap = onGenerateTap)
                        is AuthState.GeneratingMLKem -> StepLoadingPanel("⚛️  Generating ML-KEM-768 keys…", 0.33f)
                        is AuthState.GeneratingX25519 -> StepLoadingPanel("🔑  Generating X25519 keys…", 0.66f)
                        is AuthState.Registering    -> StepLoadingPanel("🌐  Registering anonymous identity…", 0.90f)
                        is AuthState.Success        -> SuccessPanel(state.textFingerprint)
                        is AuthState.Error          -> ErrorPanel(state.message, onRetry)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Quantum Orb
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuantumOrb() {
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.30f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(2_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val particleAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "particleAngle"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow halo
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = glow * 0.5f),
                            QuantumColors.Accent.copy(alpha  = glow * 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Orbiting particle ring (Canvas)
        Canvas(modifier = Modifier.size(150.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbRadius = size.width / 2f * 0.82f
            for (i in 0 until 8) {
                val angle = (particleAngle + i * 45f) * (PI / 180f).toFloat()
                val x = center.x + orbRadius * cos(angle)
                val y = center.y + orbRadius * sin(angle)
                val alpha = 0.3f + (i % 3) * 0.2f
                val r = (2.5f + (i % 3)).dp.toPx()
                drawCircle(
                    color  = QuantumColors.Primary.copy(alpha = alpha),
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }
        // Glass orb core
        Box(
            modifier = Modifier
                .size(96.dp)
                .glassmorphism(cornerRadius = 48, overlayAlpha = 0.20f, usePrimaryTint = true)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = 0.45f),
                            QuantumColors.Accent.copy(alpha  = 0.20f)
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
                contentDescription = "Quantum Security",
                tint               = Color.White,
                modifier           = Modifier.size(44.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Idle state — "Generate Anonymous Identity" button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdlePanel(onGenerateTap: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Security badge row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            SecurityBadge("🔐 Zero-Knowledge")
            SecurityBadge("⚛️ Post-Quantum")
            SecurityBadge("🫥 Anonymous")
        }

        // ── PRIMARY CTA — Generate Anonymous Identity ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .glassmorphism(cornerRadius = 20, overlayAlpha = 0.12f, usePrimaryTint = true)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QuantumColors.Primary,
                            QuantumColors.PrimaryDark
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Button(
                onClick  = onGenerateTap,
                modifier = Modifier.fillMaxSize(),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text       = "Generate Anonymous Identity",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }
        }

        // Subtle reassurance text
        Text(
            text      = "Your identity lives only on this device.\nNo email. No account. No tracking.",
            style     = MaterialTheme.typography.bodySmall,
            color     = QuantumColors.TextTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step-based loading panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepLoadingPanel(stepLabel: String, progress: Float) {
    val infinite = rememberInfiniteTransition(label = "loading")
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing)),
        label = "rotation"
    )
    val cipherOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse),
        label = "cipher"
    )

    // Animated ciphertext rain characters
    val chars = remember { (0 until 16).map { ('A'..'Z').random().toString() + ('0'..'9').random().toString() } }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Spinning gradient ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .rotate(rotation)
                .glassmorphism(cornerRadius = 36, overlayAlpha = 0.18f, usePrimaryTint = true)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            QuantumColors.Primary,
                            QuantumColors.Accent,
                            QuantumColors.Teal,
                            Color.Transparent,
                            QuantumColors.Primary
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
        )

        // Progress bar
        val animatedProgress by animateFloatAsState(
            targetValue   = progress,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label         = "progress"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(3.dp)
                .background(QuantumColors.GlassWhite08, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(QuantumColors.Primary, QuantumColors.Accent)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Text(
            text      = stepLabel,
            style     = MaterialTheme.typography.bodyLarge,
            color     = QuantumColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        // Scrolling cipher text — purely decorative
        Text(
            text       = chars.take(8).joinToString(" "),
            style      = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize   = 9.sp
            ),
            color      = QuantumColors.Primary.copy(alpha = 0.45f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.offset(y = cipherOffset.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessPanel(fingerprint: String) {
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "successScale"
    )
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        listOf(QuantumColors.Success.copy(alpha = 0.3f), Color.Transparent)
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
            color = QuantumColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        // Display the first 16 chars of the fingerprint as a visual "ID card"
        Box(
            modifier = Modifier
                .glassmorphism(cornerRadius = 12, overlayAlpha = 0.10f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text  = fingerprint.take(16).chunked(4).joinToString(" ").uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                color = QuantumColors.Primary
            )
        }
        Text(
            text  = "Redirecting you now…",
            style = MaterialTheme.typography.bodyMedium,
            color = QuantumColors.TextTertiary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚠️", fontSize = 48.sp)
        Text(
            text  = "Identity Generation Failed",
            style = MaterialTheme.typography.titleLarge,
            color = QuantumColors.Error,
            fontWeight = FontWeight.Bold
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
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Security badge chip
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Idle")
@Composable
private fun PreviewAuthIdle() {
    QuantumMessengerTheme {
        AuthScreenContent(authState = AuthState.Idle, onGenerateTap = {}, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — ML-KEM")
@Composable
private fun PreviewAuthMLKem() {
    QuantumMessengerTheme {
        AuthScreenContent(authState = AuthState.GeneratingMLKem, onGenerateTap = {}, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Registering")
@Composable
private fun PreviewAuthRegistering() {
    QuantumMessengerTheme {
        AuthScreenContent(authState = AuthState.Registering, onGenerateTap = {}, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Success")
@Composable
private fun PreviewAuthSuccess() {
    QuantumMessengerTheme {
        AuthScreenContent(
            authState = AuthState.Success(
                textFingerprint = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890",
                identity = com.nigdroid.quantummessenger.domain.model.Identity(
                    textFingerprint = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890",
                    mlKemPublicKey  = ByteArray(0),
                    x25519PublicKey = ByteArray(0)
                )
            ),
            onGenerateTap = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Auth — Error")
@Composable
private fun PreviewAuthError() {
    QuantumMessengerTheme {
        AuthScreenContent(
            authState = AuthState.Error("Network timeout — could not reach the backend"),
            onGenerateTap = {},
            onRetry = {}
        )
    }
}
