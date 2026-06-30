package com.nigdroid.quantummessenger.presentation.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.Preview
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val transition = rememberInfiniteTransition(label = "meshBg")

    val phase1 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label         = "phase1"
    )
    val phase2 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(19_000, easing = LinearEasing)),
        label         = "phase2"
    )
    val phase3 by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(25_000, easing = LinearEasing)),
        label         = "phase3"
    )

    Box(modifier = modifier.background(QuantumColors.Background)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            if (isDarkTheme) {
                // ── DARK: Volcanic Noir — ember glows & molten gold on obsidian ──

                // Blob 1 — Ember orange-red, upper-right (the volcanic glow)
                val b1x = w * (0.72f + 0.16f * sin(phase1.toDouble()).toFloat())
                val b1y = h * (0.18f + 0.14f * cos(phase1.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE85C3A).copy(alpha = 0.50f),   // ember core
                            Color(0xFFC44028).copy(alpha = 0.22f),   // deep crimson mid
                            Color(0xFF6A1A08).copy(alpha = 0.06f),   // dark lava edge
                            Color.Transparent
                        ),
                        center = Offset(b1x, b1y),
                        radius = w * 0.62f
                    ),
                    center = Offset(b1x, b1y),
                    radius = w * 0.62f
                )

                // Blob 2 — Deep crimson-maroon, lower-left (lava shadow)
                val b2x = w * (0.18f + 0.16f * cos(phase2.toDouble()).toFloat())
                val b2y = h * (0.74f + 0.13f * sin(phase2.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8A2010).copy(alpha = 0.38f),   // deep lava core
                            Color(0xFF501008).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(b2x, b2y),
                        radius = w * 0.55f
                    ),
                    center = Offset(b2x, b2y),
                    radius = w * 0.55f
                )

                // Blob 3 — Molten gold haze, center-left warmth
                val b3x = w * (0.38f + 0.14f * sin((phase3 + 0.8f).toDouble()).toFloat())
                val b3y = h * (0.45f + 0.12f * cos((phase3 * 0.6f).toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE8A848).copy(alpha = 0.22f),   // warm gold
                            Color(0xFFC88030).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(b3x, b3y),
                        radius = w * 0.46f
                    ),
                    center = Offset(b3x, b3y),
                    radius = w * 0.46f
                )

                // Blob 4 — Cool ash accent, top-left corner (depth contrast)
                val b4x = w * (0.08f + 0.08f * cos(phase2.toDouble()).toFloat())
                val b4y = h * (0.14f + 0.10f * sin(phase3.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF484848).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(b4x, b4y),
                        radius = w * 0.35f
                    ),
                    center = Offset(b4x, b4y),
                    radius = w * 0.35f
                )

                // Vignette — deep obsidian edges
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF050504).copy(alpha = 0.60f)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.80f
                    )
                )
                // Top & bottom bar darkening
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060604).copy(alpha = 0.40f),
                            Color.Transparent,
                            Color(0xFF050504).copy(alpha = 0.50f)
                        )
                    )
                )

            } else {
                // ── LIGHT: Blushed Silk — warm peach & lavender blobs ────────
                // Strongly saturated blobs on top of the peach background

                // Blob 1 — Warm rose-blush, upper-right (the soft sun glow)
                // FIXED: alpha raised from 0.55f → 0.72f, more saturated rose
                val b1x = w * (0.68f + 0.18f * sin(phase1.toDouble()).toFloat())
                val b1y = h * (0.16f + 0.14f * cos(phase1.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE890A8).copy(alpha = 0.72f),   // saturated blush core
                            Color(0xFFD06080).copy(alpha = 0.38f),   // deep rose mid
                            Color(0xFFC04870).copy(alpha = 0.10f),   // edge fade
                            Color.Transparent
                        ),
                        center = Offset(b1x, b1y),
                        radius = w * 0.62f
                    ),
                    center = Offset(b1x, b1y),
                    radius = w * 0.62f
                )

                // Blob 2 — Lavender-violet, lower-left (cool shadow)
                // FIXED: alpha raised from 0.40f → 0.60f
                val b2x = w * (0.16f + 0.16f * cos(phase2.toDouble()).toFloat())
                val b2y = h * (0.72f + 0.13f * sin(phase2.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB0A0D8).copy(alpha = 0.60f),   // vibrant lavender core
                            Color(0xFF9080C0).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(b2x, b2y),
                        radius = w * 0.56f
                    ),
                    center = Offset(b2x, b2y),
                    radius = w * 0.56f
                )

                // Blob 3 — Warm salmon-peach, center fill
                // FIXED: alpha raised from 0.30f → 0.50f
                val b3x = w * (0.44f + 0.14f * sin((phase3 + 1.0f).toDouble()).toFloat())
                val b3y = h * (0.42f + 0.12f * cos((phase3 * 0.65f).toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEEA898).copy(alpha = 0.50f),   // warm salmon-peach
                            Color(0xFFD88878).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(b3x, b3y),
                        radius = w * 0.46f
                    ),
                    center = Offset(b3x, b3y),
                    radius = w * 0.46f
                )

                // Blob 4 — Coral-pink top-left accent
                val b4x = w * (0.12f + 0.10f * sin(phase1.toDouble()).toFloat())
                val b4y = h * (0.10f + 0.08f * cos(phase2.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF0B8C0).copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(b4x, b4y),
                        radius = w * 0.38f
                    ),
                    center = Offset(b4x, b4y),
                    radius = w * 0.38f
                )

                // Soft card-readability overlay — keeps white cards crisp on peach
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF0EE).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h * 0.5f),
                        radius = w * 0.50f
                    )
                )
                // Soft top+bottom tint blends edges
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFD8D0).copy(alpha = 0.25f),
                            Color.Transparent,
                            Color(0xFFF0C8E0).copy(alpha = 0.30f)
                        )
                    )
                )
            }
        }
    }
}

/** Alias kept for any callers that reference the old name. */
@Composable
fun AdvancedAnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) = AnimatedMeshGradientBackground(modifier = modifier, isDarkTheme = isDarkTheme)