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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.R
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LockedScreen(
    onUnlockClick: () -> Unit,
    errorMessage: String? = null
) {
    LockedScreenContent(onUnlockClick = onUnlockClick, errorMessage = errorMessage)
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LockedScreenContent(
    onUnlockClick: () -> Unit,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(16.dp))

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
                        text       = "Quantum Safe",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = QuantumColors.TextPrimary,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text      = "Authentication is required to access Quantum Safe app",
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = QuantumColors.TextSecondary,
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

            // ── Bottom section — unlock + error ───────────────────────────────
            Column(
                modifier            = Modifier.padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Error message (biometric failure / lockout) ────────────────
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(cornerRadius = 12, overlayAlpha = 0.08f)
                            .background(
                                color = QuantumColors.Error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text      = errorMessage,
                            style     = MaterialTheme.typography.bodySmall,
                            color     = QuantumColors.Error,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Biometric unlock button — large tap target
                UnlockButton(onClick = onUnlockClick)

                Text(
                    text  = "Tap to unlock with biometrics",
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
    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing)
        ),
        label = "particleAngle"
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

        // Orbiting particles
        Canvas(modifier = Modifier.size(180.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbRadius = size.width / 2f * 0.78f
            for (i in 0 until 6) {
                val angle = (particleAngle + i * 60f) * (PI / 180f).toFloat()
                val x = center.x + orbRadius * cos(angle)
                val y = center.y + orbRadius * sin(angle)
                val alpha = 0.35f + (i % 3) * 0.18f
                drawCircle(
                    color  = QuantumColors.Primary.copy(alpha = alpha),
                    radius = (2f + (i % 2)).dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

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
            Image(
                painter            = painterResource(id = R.drawable.qlogo),
                contentDescription = "App Logo",
                modifier           = Modifier.size(72.dp)
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
    // Pulsing glow behind the button
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(contentAlignment = Alignment.Center) {
        // Subtle glow behind button
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(68.dp)
                .scale(glowPulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuantumColors.Primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
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
                    text       = "Tap to Unlock",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Locked Screen", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLockedScreen() {
    QuantumMessengerTheme {
        LockedScreenContent(onUnlockClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Locked — With Error", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLockedScreenError() {
    QuantumMessengerTheme {
        LockedScreenContent(
            onUnlockClick = {},
            errorMessage  = "Biometric authentication failed — too many attempts. Try again in 30 seconds."
        )
    }
}
