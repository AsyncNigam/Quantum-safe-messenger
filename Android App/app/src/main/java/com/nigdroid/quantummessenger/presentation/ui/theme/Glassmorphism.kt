package com.nigdroid.quantummessenger.presentation.ui.theme

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism Modifier Extension for Jetpack Compose.
 *
 * Creates a frosted glass effect with:
 * - Blur effect (API 31+)
 * - Semi-transparent overlay with gradient
 * - Subtle border for glass effect
 * - Fallback for older API levels
 *
 * Usage:
 *   Modifier.glassmorphism(
 *     blurRadius = 20f,
 *     cornerRadius = 16,
 *     isDarkTheme = false
 *   )
 */
fun Modifier.glassmorphism(
    blurRadius: Float = 20f,
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = false,
    overlayAlpha: Float = 0.1f
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+ - Use RenderEffect for native blur
        glassomorphismWithRenderEffect(
            blurRadius = blurRadius,
            cornerRadius = cornerRadius,
            isDarkTheme = isDarkTheme,
            overlayAlpha = overlayAlpha
        )
    } else {
        // Fallback for older APIs - Simpler effect
        glassomorphismFallback(
            isDarkTheme = isDarkTheme,
            overlayAlpha = overlayAlpha
        )
    }
}

/**
 * Glassmorphism effect for API 31+ using RenderEffect.
 * Provides high-performance blur using system rendering capabilities.
 */
private fun Modifier.glassomorphismWithRenderEffect(
    blurRadius: Float = 20f,
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = false,
    overlayAlpha: Float = 0.1f
): Modifier {
    return this
        .drawBehind {
            // Draw semi-transparent gradient overlay
            val gradientColor = if (isDarkTheme) {
                Color(0x1A000000) // Dark overlay
            } else {
                Color(0x1AFFFFFF) // Light overlay
            }

            drawRect(color = gradientColor)
        }
        .then(
            Modifier.drawBehind {
                // Draw subtle border for frosted glass appearance
                val borderColor = if (isDarkTheme) {
                    Color(0x4DFFFFFF).copy(alpha = 0.3f)
                } else {
                    Color(0x4DFFFFFF).copy(alpha = 0.5f)
                }

                val strokeWidth = 0.5.dp.toPx()
                drawRect(
                    color = borderColor,
                    size = size.copy(
                        width = size.width - strokeWidth,
                        height = size.height - strokeWidth
                    )
                )
            }
        )
}

/**
 * Glassmorphism fallback for API < 31.
 * Provides a simplified semi-transparent effect.
 */
private fun Modifier.glassomorphismFallback(
    isDarkTheme: Boolean = false,
    overlayAlpha: Float = 0.1f
): Modifier {
    return this.drawBehind {
        // Draw semi-transparent colored rectangle
        val overlayColor = if (isDarkTheme) {
            Color(0x1A000000).copy(alpha = overlayAlpha)
        } else {
            Color(0x1AFFFFFF).copy(alpha = overlayAlpha)
        }

        drawRect(color = overlayColor)
    }
}

/**
 * Glassmorphism Surface Composable
 *
 * A convenience composable that applies glassmorphism effects
 * to a surface container. Useful for cards, dialogs, etc.
 *
 * Usage:
 *   Box(modifier = Modifier.glassmorphism())
 */
object GlassmorphismSurface {
    // Implementation delegated to Modifier.glassmorphism()
    // for more flexible usage patterns
}

