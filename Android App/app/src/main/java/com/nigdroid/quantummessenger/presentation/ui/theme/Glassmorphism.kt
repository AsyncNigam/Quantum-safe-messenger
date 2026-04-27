package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism — dual-mode
 *
 * DARK  → Autumn Dusk: amber-tinted frosted glass
 * LIGHT → Blush Petal: rose-tinted frosted glass, white card feel
 *
 * Three-layer system:
 *  1. Warm tinted overlay (depth without neon)
 *  2. Fine 1px gradient border (top-bright → bottom-gone)
 *  3. Inner frosted-glass "lip" at the top edge
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

    val isDark = QuantumColors.Background.red < 0.10f   // runtime dark detection

    // ── 1. Frosted overlay ───────────────────────────────────────────────────
    val overlayBrush = when {
        usePrimaryTint && isDark -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFC87840).copy(alpha = overlayAlpha * 1.4f),
                Color(0xFF985820).copy(alpha = overlayAlpha * 0.30f),
            ),
            center = Offset(size.width * 0.35f, size.height * 0.25f),
            radius = size.maxDimension * 0.75f
        )
        usePrimaryTint -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFBF6B8A).copy(alpha = overlayAlpha * 1.3f),
                Color(0xFF954E6A).copy(alpha = overlayAlpha * 0.25f),
            ),
            center = Offset(size.width * 0.35f, size.height * 0.25f),
            radius = size.maxDimension * 0.75f
        )
        isDark -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = overlayAlpha * 1.1f),
                Color.White.copy(alpha = overlayAlpha * 0.25f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width * 0.4f, size.height)
        )
        else -> Brush.linearGradient(
            // Light mode: warm white — reads as a lifted card
            colors = listOf(
                Color.White.copy(alpha = overlayAlpha * 1.6f),
                Color.White.copy(alpha = overlayAlpha * 0.60f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width * 0.3f, size.height)
        )
    }
    drawRoundRect(brush = overlayBrush, cornerRadius = CornerRadius(cr), size = size)

    // ── 2. Outer border ──────────────────────────────────────────────────────
    val borderAlphaTop = if (isDark) 0.28f else 0.55f
    val borderAlphaBot = if (isDark) 0.06f else 0.14f
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlphaTop),
                Color.White.copy(alpha = borderAlphaBot),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr),
        size         = size,
        style        = Stroke(width = bw)
    )

    // ── 3. Inner top-edge highlight ──────────────────────────────────────────
    val inset = bw * 2.5f
    val lipAlpha = if (isDark) 0.09f else 0.18f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = lipAlpha), Color.Transparent),
            startY = inset,
            endY   = inset + size.height * 0.28f
        ),
        topLeft      = Offset(inset, inset),
        size         = Size(size.width - inset * 2f, size.height * 0.30f),
        cornerRadius = CornerRadius(cr - inset)
    )
}

/**
 * Own message bubble — warm copper dark / soft blush light.
 */
fun Modifier.glassmorphismBubbleOwn(): Modifier = this.drawBehind {
    val cr   = 18.dp.toPx()
    val dark = QuantumColors.Background.red < 0.10f

    drawRoundRect(
        brush = if (dark) Brush.linearGradient(
            colors = listOf(
                Color(0xFFC87840).copy(alpha = 0.60f),
                Color(0xFF985820).copy(alpha = 0.75f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ) else Brush.linearGradient(
            colors = listOf(
                Color(0xFFE8A0B8).copy(alpha = 0.70f),
                Color(0xFFBF6B8A).copy(alpha = 0.55f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr)
    )
    // Top gleam
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = if (dark) 0.16f else 0.30f), Color.Transparent),
            startY = 0f, endY = size.height * 0.35f
        ),
        cornerRadius = CornerRadius(cr)
    )
    // Border
    drawRoundRect(
        color        = Color.White.copy(alpha = if (dark) 0.14f else 0.40f),
        cornerRadius = CornerRadius(cr),
        style        = Stroke(width = 1.dp.toPx())
    )
}

/**
 * Other-person bubble — cool dark glass / lavender-white light.
 */
fun Modifier.glassmorphismBubbleOther(): Modifier = this.drawBehind {
    val cr   = 18.dp.toPx()
    val dark = QuantumColors.Background.red < 0.10f

    drawRoundRect(
        brush = if (dark) Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)),
            start = Offset(0f, 0f), end = Offset(size.width, size.height)
        ) else Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEEAF4).copy(alpha = 0.85f),
                Color(0xFFF4F0F8).copy(alpha = 0.70f),
            ),
            start = Offset(0f, 0f), end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr)
    )
    drawRoundRect(
        color        = Color.White.copy(alpha = if (dark) 0.10f else 0.55f),
        cornerRadius = CornerRadius(cr),
        style        = Stroke(width = if (dark) 0.8.dp.toPx() else 1.dp.toPx())
    )
}