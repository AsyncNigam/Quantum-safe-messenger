package com.nigdroid.quantummessenger.presentation.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Animated mesh gradient background using flowing color blobs.
 *
 * Creates a subtle, beautiful animated background that serves as the perfect
 * backdrop for the glassmorphism chat bubbles. The gradient smoothly animates
 * to create an organic, fluid appearance.
 *
 * Colors are based on the theme:
 * - Light theme: Soft blues, purples, and pinks
 * - Dark theme: Deep blues and purples with teal accents
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient")

    // Animation values for smooth color transitions
    val colorShift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_shift_1"
    )

    val colorShift2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_shift_2"
    )

    val colorShift3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_shift_3"
    )

    // Define color palette based on theme
    val isDarkTheme = MaterialTheme.colorScheme.surface == Color.Black

    val color1 = if (isDarkTheme) Color(0xFF1e3a8a) else Color(0xFFe0f2fe) // Dark blue / Light blue
    val color2 = if (isDarkTheme) Color(0xFF7c2d12) else Color(0xFFf0e7fe) // Dark red / Light purple
    val color3 = if (isDarkTheme) Color(0xFF164e63) else Color(0xFFffe4f0) // Dark teal / Light pink
    val color4 = if (isDarkTheme) Color(0xFF2d1b69) else Color(0xFFf3e8ff) // Dark purple / Light purple

    // Interpolate colors based on animation
    fun interpolateColor(color1: Color, color2: Color, progress: Float): Color {
        return Color(
            red = (color1.red * (1 - progress) + color2.red * progress).coerceIn(0f, 1f),
            green = (color1.green * (1 - progress) + color2.green * progress).coerceIn(0f, 1f),
            blue = (color1.blue * (1 - progress) + color2.blue * progress).coerceIn(0f, 1f),
            alpha = (color1.alpha * (1 - progress) + color2.alpha * progress).coerceIn(0f, 1f)
        )
    }

    val animColor1 = interpolateColor(color1, color4, colorShift1)
    val animColor2 = interpolateColor(color2, color1, colorShift2)
    val animColor3 = interpolateColor(color3, color2, colorShift3)
    val animColor4 = interpolateColor(color4, color3, colorShift1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(animColor1, animColor2, animColor3, animColor4),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                )
            )
            .graphicsLayer {
                translationX = colorShift1 * 20f - 10f
                translationY = colorShift2 * 15f - 7.5f
            }
    )
}

/**
 * Alternative animated background with diagonal moving gradient.
 * Simpler but still visually appealing.
 */
@Composable
fun AnimatedDiagonalGradientBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "diagonal_gradient")

    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_angle"
    )

    val isDarkTheme = MaterialTheme.colorScheme.surface == Color.Black

    val colors = if (isDarkTheme) {
        listOf(
            Color(0xFF0f172a),
            Color(0xFF1e3a8a),
            Color(0xFF7c3aed),
            Color(0xFF2d1b69)
        )
    } else {
        listOf(
            Color(0xFFe0f2fe),
            Color(0xFFf0e7fe),
            Color(0xFFf3e8ff),
            Color(0xFFffe4f0)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(
                        x = kotlin.math.cos(gradientAngle * kotlin.math.PI / 180f).toFloat() * 1000,
                        y = kotlin.math.sin(gradientAngle * kotlin.math.PI / 180f).toFloat() * 1000
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        x = -kotlin.math.cos(gradientAngle * kotlin.math.PI / 180f).toFloat() * 1000,
                        y = -kotlin.math.sin(gradientAngle * kotlin.math.PI / 180f).toFloat() * 1000
                    )
                )
            )
    )
}
