package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.lightColorScheme as defaultLightColorScheme
import androidx.compose.material3.darkColorScheme as defaultDarkColorScheme

/**
 * Light Theme ColorScheme
 *
 * Crafted for glassmorphism UI with premium aesthetics.
 */
private val LightColorScheme = lightColorScheme(
    primary = QuantumColors.PrimaryBlue,
    onPrimary = QuantumColors.SurfaceLight,
    primaryContainer = QuantumColors.PrimaryBlueLight,
    onPrimaryContainer = QuantumColors.OnSurfaceLight,
    secondary = QuantumColors.SecondaryPurple,
    onSecondary = QuantumColors.SurfaceLight,
    secondaryContainer = QuantumColors.SecondaryPurpleLight,
    onSecondaryContainer = QuantumColors.OnSurfaceLight,
    tertiary = QuantumColors.AccentCyan,
    onTertiary = QuantumColors.SurfaceLight,
    tertiaryContainer = QuantumColors.AccentCyanLight,
    onTertiaryContainer = QuantumColors.OnSurfaceLight,
    error = QuantumColors.ErrorRed,
    onError = QuantumColors.SurfaceLight,
    errorContainer = QuantumColors.ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = QuantumColors.ErrorRed,
    background = QuantumColors.BackgroundLight,
    onBackground = QuantumColors.OnSurfaceLight,
    surface = QuantumColors.SurfaceLight,
    onSurface = QuantumColors.OnSurfaceLight,
    surfaceVariant = QuantumColors.SurfaceVariantLight,
    onSurfaceVariant = QuantumColors.OnSurfaceVariantLight,
    outline = QuantumColors.OnSurfaceVariantLight.copy(alpha = 0.5f),
    outlineVariant = QuantumColors.OnSurfaceVariantLight.copy(alpha = 0.3f),
    scrim = QuantumColors.OnSurfaceLight.copy(alpha = 0.1f)
)

/**
 * Dark Theme ColorScheme
 *
 * Optimized for glassmorphism with deep color tones
 * and reduced eye strain at night.
 */
private val DarkColorScheme = darkColorScheme(
    primary = QuantumColors.PrimaryBlueLight,
    onPrimary = QuantumColors.BackgroundDark,
    primaryContainer = QuantumColors.PrimaryBlueDark,
    onPrimaryContainer = QuantumColors.PrimaryBlueLight,
    secondary = QuantumColors.SecondaryPurpleLight,
    onSecondary = QuantumColors.BackgroundDark,
    secondaryContainer = QuantumColors.SecondaryPurpleDark,
    onSecondaryContainer = QuantumColors.SecondaryPurpleLight,
    tertiary = QuantumColors.AccentCyanLight,
    onTertiary = QuantumColors.BackgroundDark,
    tertiaryContainer = QuantumColors.AccentCyanDark,
    onTertiaryContainer = QuantumColors.AccentCyanLight,
    error = QuantumColors.ErrorRed.copy(alpha = 0.9f),
    onError = QuantumColors.BackgroundDark,
    errorContainer = QuantumColors.ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = QuantumColors.ErrorRed.copy(alpha = 0.9f),
    background = QuantumColors.BackgroundDark,
    onBackground = QuantumColors.OnSurfaceDark,
    surface = QuantumColors.SurfaceDark,
    onSurface = QuantumColors.OnSurfaceDark,
    surfaceVariant = QuantumColors.SurfaceVariantDark,
    onSurfaceVariant = QuantumColors.OnSurfaceVariantDark,
    outline = QuantumColors.OnSurfaceVariantDark.copy(alpha = 0.5f),
    outlineVariant = QuantumColors.OnSurfaceVariantDark.copy(alpha = 0.3f),
    scrim = QuantumColors.BackgroundDark.copy(alpha = 0.1f)
)

/**
 * Main Theme Composable for Quantum Messenger
 *
 * Applies Material3 theming with custom glasses morphism styling.
 * Automatically adapts to system dark/light theme preference.
 *
 * Usage:
 *   QuantumMessengerTheme {
 *     // Your composable content
 *   }
 */
@Composable
fun QuantumMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuantumTypography,
        shapes = QuantumShapes,
        content = content
    )
}

/**
 * Custom Typography for Quantum Messenger
 *
 * Defines text styles for different UI components
 * with optimal readability and premium aesthetics.
 */
private val QuantumTypography = Typography()

/**
 * Custom Shapes for Quantum Messenger
 *
 * Uses rounded corners consistent with glassmorphism design.
 */
private val QuantumShapes = Shapes()

