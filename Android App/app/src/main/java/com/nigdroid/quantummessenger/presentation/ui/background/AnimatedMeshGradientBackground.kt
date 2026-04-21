package com.nigdroid.quantummessenger.presentation.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloat
import kotlin.math.sin

/**
 * Animated Mesh Gradient Background for Chat Screen
 *
 * Features:
 * - Smooth, continuously animating gradients
 * - Multiple color stops for rich visual depth
 * - Low performance impact using optimized rendering
 * - Adapts to light/dark theme
 * - Creates beautiful "blob" animations using sine waves
 *
 * Perfect for creating engaging, modern UIs without heavyweight
 * graphics libraries.
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshGradient")

    // Animate color positions and offsets
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "offsetX"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "offsetY"
    )

    // Create animated gradient colors with sine wave modulation
    val colors = if (isDarkTheme) {
        getDarkThemeColors(offsetX, offsetY)
    } else {
        getLightThemeColors(offsetX, offsetY)
    }

    // Radial gradient center follows animation
    val centerOffsetX = 0.5f + (sin(offsetX * Math.PI.toFloat()) * 0.3f)
    val centerOffsetY = 0.5f + (sin(offsetY * Math.PI.toFloat()) * 0.3f)

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f)
                )
            )
    )
}

/**
 * Provides animated gradient colors for light theme
 * Primary: Soft blue/cyan gradient with warm accents
 */
private fun getLightThemeColors(offsetX: Float, offsetY: Float): List<Color> {
    // Base colors with subtle animation
    val primaryBlue = Color(0x4D6366FF) // Primary blue with transparency
    val secondaryPurple = Color(0x4D8E6FA8) // Secondary purple
    val accentCyan = Color(0x4D00BCD4) // Accent cyan
    val lightBackground = Color(0xFFF5F7FA) // Light background

    // Apply sine wave modulation for smooth color transitions
    val modulationX = sin((offsetX * Math.PI).toFloat() * 2).toFloat() * 0.2f
    val modulationY = sin((offsetY * Math.PI).toFloat() * 2).toFloat() * 0.2f

    return listOf(
        lightBackground.copy(alpha = 0.85f + modulationX * 0.1f),
        primaryBlue.copy(alpha = 0.4f + modulationY * 0.1f),
        secondaryPurple.copy(alpha = 0.3f - modulationX * 0.1f),
        accentCyan.copy(alpha = 0.25f + modulationY * 0.15f),
        lightBackground.copy(alpha = 0.8f - modulationX * 0.1f)
    )
}

/**
 * Provides animated gradient colors for dark theme
 * Primary: Deep blue/purple gradient with vibrant accents
 */
private fun getDarkThemeColors(offsetX: Float, offsetY: Float): List<Color> {
    // Dark theme colors with more saturation
    val darkBackground = Color(0xFF0F1419)
    val deepBlue = Color(0x4D1F3A8F)
    val deepPurple = Color(0x4D370D52)
    val accentViolet = Color(0x4D6A4C93)
    val brightCyan = Color(0x4D00D9FF)

    // Apply sine wave modulation
    val modulationX = sin((offsetX * Math.PI).toFloat() * 2).toFloat() * 0.2f
    val modulationY = sin((offsetY * Math.PI).toFloat() * 2).toFloat() * 0.2f

    return listOf(
        darkBackground.copy(alpha = 0.95f + modulationX * 0.05f),
        deepBlue.copy(alpha = 0.5f + modulationY * 0.15f),
        deepPurple.copy(alpha = 0.4f - modulationX * 0.1f),
        brightCyan.copy(alpha = 0.3f + modulationY * 0.2f),
        accentViolet.copy(alpha = 0.25f + modulationX * 0.15f)
    )
}

/**
 * Simple Box composable for background composition.
 * Defined here to keep the background module self-contained.
 */
@Composable
private fun SimpleBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier) {
        content()
    }
}

/**
 * Enhanced Animated Mesh Gradient with Multiple Blob Animations
 *
 * This variant uses multiple animated offset points to create
 * a more complex, organic "blob" effect.
 */
@Composable
fun AdvancedAnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "advancedMeshGradient")

    // Multiple parallel animations for different color regions
    val blobOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing)
        ),
        label = "blobOffset1"
    )

    val blobOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing)
        ),
        label = "blobOffset2"
    )

    // Convert angles to positions with sine/cosine
    val offsetX = (sin((blobOffset1 * Math.PI / 180.0)).toFloat() + 1f) / 2f
    val offsetY = (sin(((blobOffset2 + 90f) * Math.PI / 180.0)).toFloat() + 1f) / 2f

    val colors = if (isDarkTheme) {
        getDarkThemeColors(offsetX, offsetY)
    } else {
        getLightThemeColors(offsetX, offsetY)
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors[1],
                        colors[2],
                        colors[3],
                        colors[4],
                        colors[0]
                    ),
                    center = Offset(offsetX, offsetY),
                    radius = 800f
                )
            )
    )
}

