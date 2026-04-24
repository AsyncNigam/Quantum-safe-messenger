package com.nigdroid.quantummessenger.presentation.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    LockedScreenContent(onUnlockClick = onUnlockClick)
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LockedScreenContent(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(0.dp))

            // ── Centre section ────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LockOrb()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "Quantum Messenger",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = QuantumColors.TextPrimary,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text      = "Encrypted & Locked",
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = QuantumColors.TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Security status chips ─────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusChip(icon = Icons.Default.Shield, label = "Quantum Safe")
                    StatusChip(icon = Icons.Default.Lock,   label = "E2E Encrypted")
                }
            }

            // ── Unlock button ─────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Biometric unlock button — large tap target
                UnlockButton(onClick = onUnlockClick)

                Text(
                    text  = "Use biometric authentication\nto unlock your messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = QuantumColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LockOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "lockOrb")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lockPulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20_000, easing = LinearEasing)
        ),
        label = "orbRotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        // Rotating dashed ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulse)
                .rotate(rotation)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = ringAlpha),
                            QuantumColors.Accent.copy(alpha  = ringAlpha * 0.6f),
                            Color.Transparent,
                            QuantumColors.Teal.copy(alpha    = ringAlpha * 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Outer glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glass orb
        Box(
            modifier = Modifier
                .size(120.dp)
                .glassmorphism(cornerRadius = 60, overlayAlpha = 0.18f, usePrimaryTint = true)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = 0.50f),
                            QuantumColors.PrimaryDark.copy(alpha = 0.70f)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(120f, 120f)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = "Locked",
                tint               = Color.White,
                modifier           = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    label : String
) {
    Row(
        modifier = Modifier
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = QuantumColors.Primary,
            modifier           = Modifier.size(14.dp)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = QuantumColors.TextSecondary
        )
    }
}

@Composable
private fun UnlockButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "unlockBtn")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = LinearEasing)
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.12f, usePrimaryTint = true)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        QuantumColors.Primary,
                        QuantumColors.Accent.copy(alpha = 0.85f),
                        QuantumColors.Primary
                    ),
                    start = Offset(shimmer * 600f, 0f),
                    end   = Offset(shimmer * 600f + 300f, 100f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Fingerprint,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(26.dp)
            )
            Text(
                text       = "Unlock with Biometrics",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Locked Screen", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLockedScreen() {
    QuantumMessengerTheme {
        LockedScreenContent(onUnlockClick = {})
    }
}
