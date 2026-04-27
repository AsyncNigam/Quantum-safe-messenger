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
 * Animated Mesh Gradient Background — Autumn Dusk / Evening Glow
 *
 * Three slow-moving warm blobs over a deep charcoal base.
 * The effect reads like light through amber glass at golden hour —
 * perceptible warmth, not a neon rave.
 *
 * Performance: Canvas + infinite-transition floats only, no bitmaps.
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "meshBg")

    // Three independent slow phases — deliberately unhurried
    val phase1 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing)
        ),
        label = "phase1"
    )
    val phase2 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19_000, easing = LinearEasing)
        ),
        label = "phase2"
    )
    val phase3 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing)
        ),
        label = "phase3"
    )

    Box(modifier = modifier.background(QuantumColors.Background)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // ── Blob 1 — Burnished amber, upper-right (the "sun remnant") ──
            val b1x = w * (0.68f + 0.18f * sin(phase1.toDouble()).toFloat())
            val b1y = h * (0.20f + 0.14f * cos(phase1.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC87840).copy(alpha = 0.38f),
                        Color(0xFF985820).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(b1x, b1y),
                    radius = w * 0.58f
                ),
                center = Offset(b1x, b1y),
                radius = w * 0.58f
            )

            // ── Blob 2 — Dusty rose-mauve, lower-left (dusk shadow) ─────────
            val b2x = w * (0.22f + 0.16f * cos(phase2.toDouble()).toFloat())
            val b2y = h * (0.72f + 0.14f * sin(phase2.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9B6878).copy(alpha = 0.22f),
                        Color(0xFF7A5060).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(b2x, b2y),
                    radius = w * 0.52f
                ),
                center = Offset(b2x, b2y),
                radius = w * 0.52f
            )

            // ── Blob 3 — Warm gold haze, mid-screen (the lingering warmth) ──
            val b3x = w * (0.48f + 0.12f * sin((phase3 + 0.8f).toDouble()).toFloat())
            val b3y = h * (0.42f + 0.10f * cos((phase3 * 0.6f).toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD4984A).copy(alpha = 0.10f),
                        Color(0xFF985820).copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(b3x, b3y),
                    radius = w * 0.42f
                ),
                center = Offset(b3x, b3y),
                radius = w * 0.42f
            )

            // ── Vignette — draws focus inward, keeps text readable ──────────
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.85f
                )
            )

            // ── Subtle top-to-bottom gradient seal ──────────────────────────
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.35f)
                    )
                )
            )
        }
    }
}

/**
 * Alias kept for any callers that reference the old name.
 */
@Composable
fun AdvancedAnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) = AnimatedMeshGradientBackground(modifier = modifier, isDarkTheme = isDarkTheme)

