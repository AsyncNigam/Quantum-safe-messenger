package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism — Autumn Dusk flavour.
 *
 * Three-layer system:
 *  1. Warm tinted overlay — depth without neon
 *  2. Fine 1px gradient border — top-bright to bottom-gone
 *  3. Inner highlight — the frosted-glass "lip" at the top edge
 *
 * The amber tint in usePrimaryTint mode reads like candlelight through
 * smoked glass, not the generic purple glow of AI-generated UIs.
 */
fun Modifier.glassmorphism(
    blurRadius: Float = 20f,
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = true,
    overlayAlpha: Float = 0.10f,
    borderWidth: Dp = 1.dp,
    usePrimaryTint: Boolean = false
): Modifier = this.drawBehind {
    val cr = cornerRadius.dp.toPx()
    val bw = borderWidth.toPx()

    // ── 1. Frosted overlay ───────────────────────────────────────────────────
    val overlayBrush = if (usePrimaryTint) {
        // Warm amber radial — feels like candlelight warmth
        Brush.radialGradient(
            colors = listOf(
                QuantumColors.Primary.copy(alpha = overlayAlpha * 1.4f),
                QuantumColors.PrimaryDark.copy(alpha = overlayAlpha * 0.35f),
            ),
            center = Offset(size.width * 0.35f, size.height * 0.25f),
            radius = size.maxDimension * 0.75f
        )
    } else {
        // Neutral cool-warm: slightly warm white tint from top
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = overlayAlpha * 1.1f),
                Color.White.copy(alpha = overlayAlpha * 0.25f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width * 0.4f, size.height)
        )
    }
    drawRoundRect(
        brush        = overlayBrush,
        cornerRadius = CornerRadius(cr, cr),
        size         = size
    )

    // ── 2. Outer border — top-left bright → bottom-right gone ───────────────
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.06f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr, cr),
        size         = size,
        style        = Stroke(width = bw)
    )

    // ── 3. Inner "frosted lip" — top edge highlight ──────────────────────────
    val inset = bw * 2.5f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.09f),
                Color.Transparent
            ),
            startY = inset,
            endY   = inset + size.height * 0.28f
        ),
        topLeft      = Offset(inset, inset),
        size         = Size(size.width - inset * 2f, size.height * 0.30f),
        cornerRadius = CornerRadius(cr - inset, cr - inset)
    )
}

/**
 * Own message bubble — warm copper gradient with translucent top edge.
 * Reads as a lit amber stone, not a purple AI blob.
 */
fun Modifier.glassmorphismBubbleOwn(): Modifier = this.drawBehind {
    val cr = 18.dp.toPx()
    // Base fill: warm copper to dark copper
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                QuantumColors.Primary.copy(alpha = 0.60f),
                QuantumColors.PrimaryDark.copy(alpha = 0.75f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr, cr)
    )
    // Top-edge gleam
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.Transparent
            ),
            startY = 0f,
            endY   = size.height * 0.35f
        ),
        cornerRadius = CornerRadius(cr, cr)
    )
    // Outer border
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.14f),
        cornerRadius = CornerRadius(cr, cr),
        style        = Stroke(width = 1.dp.toPx())
    )
}

/**
 * Other-person bubble — cool dark glass, barely tinted.
 * Quiet contrast against the warm own-bubble.
 */
fun Modifier.glassmorphismBubbleOther(): Modifier = this.drawBehind {
    val cr = 18.dp.toPx()
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr, cr)
    )
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.10f),
        cornerRadius = CornerRadius(cr, cr),
        style        = Stroke(width = 0.8.dp.toPx())
    )
}