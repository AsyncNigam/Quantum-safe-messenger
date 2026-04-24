package com.nigdroid.quantummessenger.presentation.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Alternative animated diagonal gradient background.
 * Kept for any callers that reference AnimatedDiagonalGradientBackground.
 */
@Composable
fun AnimatedDiagonalGradientBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "diagonal_gradient")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_angle"
    )
    val colors = listOf(
        Color(0xFF08070E),
        Color(0xFF1F1535),
        Color(0xFF3A2070),
        Color(0xFF1A0F2E)
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start  = Offset(
                        cos(gradientAngle * Math.PI / 180f).toFloat() * 1000,
                        sin(gradientAngle * Math.PI / 180f).toFloat() * 1000
                    ),
                    end    = Offset(
                        -cos(gradientAngle * Math.PI / 180f).toFloat() * 1000,
                        -sin(gradientAngle * Math.PI / 180f).toFloat() * 1000
                    )
                )
            )
    )
}
