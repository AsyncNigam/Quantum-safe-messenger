package com.nigdroid.quantummessenger.presentation.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun VaultCompromisedScreen(
    onWipeAndReRegister: () -> Unit,
    isWiping: Boolean = false
) {
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

            // ── Centre warning ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WarningOrb()

                Text(
                    text       = "Security Alert",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = QuantumColors.Error,
                    textAlign  = TextAlign.Center
                )

                // ── Explanation glass card ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(cornerRadius = 16, overlayAlpha = 0.10f)
                        .background(
                            color = QuantumColors.Error.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text       = "Biometric Data Changed",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = QuantumColors.Warning
                        )
                        Text(
                            text  = "Your device biometric enrollment has changed since " +
                                    "your identity was created. This could mean:\n\n" +
                                    "• A new fingerprint was enrolled\n" +
                                    "• Device PIN/Pattern was changed\n" +
                                    "• Someone else gained device access\n\n" +
                                    "Under the Zero-Trust protocol, all local " +
                                    "encrypted data must be securely wiped to prevent " +
                                    "unauthorized access to your messages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QuantumColors.TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // ── Wipe button ────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .glassmorphism(cornerRadius = 20, overlayAlpha = 0.12f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    QuantumColors.Error,
                                    QuantumColors.Error.copy(alpha = 0.80f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Button(
                        onClick  = onWipeAndReRegister,
                        modifier = Modifier.fillMaxSize(),
                        shape    = RoundedCornerShape(20.dp),
                        enabled  = !isWiping,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor   = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (isWiping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color    = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text       = "Wiping Vault…",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text       = "Secure Wipe & Re-register",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        }
                    }
                }

                Text(
                    text      = "This will delete all local messages\nand generate a new anonymous identity.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = QuantumColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning Orb
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WarningOrb() {
    val infinite = rememberInfiniteTransition(label = "warningOrb")
    val pulse by infinite.animateFloat(
        initialValue  = 0.90f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "warnPulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue  = 0.20f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "warnGlow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuantumColors.Error.copy(alpha = glowAlpha),
                            QuantumColors.Warning.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Glass core
        Box(
            modifier = Modifier
                .size(100.dp)
                .glassmorphism(cornerRadius = 50, overlayAlpha = 0.18f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            QuantumColors.Error.copy(alpha = 0.60f),
                            QuantumColors.Warning.copy(alpha = 0.40f)
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
                imageVector        = Icons.Default.Warning,
                contentDescription = "Security Warning",
                tint               = Color.White,
                modifier           = Modifier.size(48.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Vault Compromised")
@Composable
private fun PreviewVaultCompromised() {
    QuantumMessengerTheme {
        VaultCompromisedScreen(onWipeAndReRegister = {}, isWiping = false)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Vault Wiping")
@Composable
private fun PreviewVaultWiping() {
    QuantumMessengerTheme {
        VaultCompromisedScreen(onWipeAndReRegister = {}, isWiping = true)
    }
}
