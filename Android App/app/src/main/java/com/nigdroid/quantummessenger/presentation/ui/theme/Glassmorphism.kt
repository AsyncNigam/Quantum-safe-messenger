package com.nigdroid.quantummessenger.presentation.ui.theme

import android.os.Build
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
 * Production-grade glassmorphism Modifier.
 *
 * Renders:
 *  1. A semi-transparent radial gradient overlay (depth feeling)
 *  2. A crisp 1px rounded-rect border with gradient from bright-top to faint-bottom
 *  3. A subtle inner shadow line along the top edge for the frosted-glass "lip"
 *
 * The effect works on all API levels — no blur dependency needed.
 * True background blur requires the caller to use BlurMaskFilter on a Canvas
 * layer which is expensive; this approach gives the same premium look.
 */
fun Modifier.glassmorphism(
    blurRadius: Float = 20f,
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = true,
    overlayAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp,
    usePrimaryTint: Boolean = false
): Modifier = this.drawBehind {
    val cr = cornerRadius.dp.toPx()
    val bw = borderWidth.toPx()

    // ── 1.  Frosted overlay (radial gradient so centre is lighter) ──────────
    val overlayBrush = if (usePrimaryTint) {
        Brush.radialGradient(
            colors = listOf(
                QuantumColors.Primary.copy(alpha = overlayAlpha * 1.6f),
                QuantumColors.Primary.copy(alpha = overlayAlpha * 0.4f),
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.7f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = overlayAlpha),
                Color.White.copy(alpha = overlayAlpha * 0.3f),
            ),
            center = Offset(size.width / 2f, 0f),
            radius = size.maxDimension * 0.85f
        )
    }
    drawRoundRect(
        brush        = overlayBrush,
        cornerRadius = CornerRadius(cr, cr),
        size         = size
    )

    // ── 2.  Outer border — gradient top-bright → bottom-faint ──────────────
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),   // top-left: bright
            Color.White.copy(alpha = 0.08f),   // bottom-right: almost gone
        ),
        start = Offset(0f, 0f),
        end   = Offset(size.width, size.height)
    )
    drawRoundRect(
        brush        = borderBrush,
        cornerRadius = CornerRadius(cr, cr),
        size         = size,
        style        = Stroke(width = bw)
    )

    // ── 3.  Inner highlight — top edge "light catch" ────────────────────────
    val highlightPath = Path().apply {
        // Just the top arc of the rounded rect
        val inset = bw * 2f
        addRoundRect(
            RoundRect(
                left   = inset,
                top    = inset,
                right  = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius(cr - inset, cr - inset)
            )
        )
    }
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.07f),
        topLeft      = Offset(bw * 2, bw * 2),
        size         = Size(size.width - bw * 4, size.height / 3f),
        cornerRadius = CornerRadius(cr, cr)
    )
}

/**
 * Convenience — message-bubble glass with a subtle primary tint.
 */
fun Modifier.glassmorphismBubbleOwn(): Modifier = this.drawBehind {
    val cr = 18.dp.toPx()
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                QuantumColors.Primary.copy(alpha = 0.55f),
                QuantumColors.PrimaryDark.copy(alpha = 0.70f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr, cr)
    )
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.18f),
        cornerRadius = CornerRadius(cr, cr),
        style        = Stroke(width = 1.dp.toPx())
    )
}

fun Modifier.glassmorphismBubbleOther(): Modifier = this.drawBehind {
    val cr = 18.dp.toPx()
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.05f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(cr, cr)
    )
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.12f),
        cornerRadius = CornerRadius(cr, cr),
        style        = Stroke(width = 1.dp.toPx())
    )
}
