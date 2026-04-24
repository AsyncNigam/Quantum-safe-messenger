package com.nigdroid.quantummessenger.presentation.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Mesh Gradient Background — Quantum Messenger
 *
 * Three layered animated "blobs" of colour painted on a deep-black canvas,
 * producing the premium iOS-style aurora/mesh gradient look.
 *
 * Performance: uses Canvas + infinite-transition floats only — no bitmaps.
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true  // kept for API compat, always dark now
) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshBg")

    // Three independent phase animations at different speeds
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing)
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13_000, easing = LinearEasing)
        ),
        label = "phase2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17_000, easing = LinearEasing)
        ),
        label = "phase3"
    )

    Box(
        modifier = modifier
            .background(QuantumColors.Background)   // true black base
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Blob 1 — Primary violet, top-right area
            val blob1X = w * (0.6f + 0.25f * sin(phase1.toDouble()).toFloat())
            val blob1Y = h * (0.25f + 0.20f * cos(phase1.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        QuantumColors.Primary.copy(alpha = 0.55f),
                        QuantumColors.PrimaryDark.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(blob1X, blob1Y),
                    radius = w * 0.65f
                ),
                center = Offset(blob1X, blob1Y),
                radius = w * 0.65f
            )

            // Blob 2 — Magenta/accent, bottom-left area
            val blob2X = w * (0.25f + 0.20f * cos(phase2.toDouble()).toFloat())
            val blob2Y = h * (0.70f + 0.18f * sin(phase2.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        QuantumColors.Accent.copy(alpha = 0.28f),
                        QuantumColors.AccentSoft.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(blob2X, blob2Y),
                    radius = w * 0.55f
                ),
                center = Offset(blob2X, blob2Y),
                radius = w * 0.55f
            )

            // Blob 3 — Cyan/teal, centre
            val blob3X = w * (0.50f + 0.15f * sin((phase3 + 1f).toDouble()).toFloat())
            val blob3Y = h * (0.45f + 0.15f * cos((phase3 * 0.7f).toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        QuantumColors.Teal.copy(alpha = 0.15f),
                        QuantumColors.TealDark.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(blob3X, blob3Y),
                    radius = w * 0.45f
                ),
                center = Offset(blob3X, blob3Y),
                radius = w * 0.45f
            )

            // ── Noise overlay (fine grain via small radial dots pattern) ────
            // Light scanline-like vignette from top — keeps text readable
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.40f)
                    )
                )
            )
        }
    }
}

/**
 * Advanced variant — alias kept for any callers that reference the old name.
 */
@Composable
fun AdvancedAnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) = AnimatedMeshGradientBackground(modifier = modifier, isDarkTheme = isDarkTheme)
