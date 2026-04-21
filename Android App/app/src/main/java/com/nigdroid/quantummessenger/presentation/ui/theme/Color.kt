package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color Palette for Quantum Messenger
 *
 * Designed for premium glassmorphism UI with
 * emphasis on accessibility and aesthetic appeal.
 */
object QuantumColors {
    // Primary Colors
    val PrimaryBlue = Color(0xFF6366FF)
    val PrimaryBlueLight = Color(0xFF818FFF)
    val PrimaryBlueDark = Color(0xFF4D5FFF)

    // Secondary Colors
    val SecondaryPurple = Color(0xFF8E6FA8)
    val SecondaryPurpleLight = Color(0xFFA98BC7)
    val SecondaryPurpleDark = Color(0xFF6F5387)

    // Accent Colors
    val AccentCyan = Color(0xFF00BCD4)
    val AccentCyanLight = Color(0xFF4DD0E1)
    val AccentCyanDark = Color(0xFF0097A7)

    // Neutral Colors - Light Theme
    val SurfaceLight = Color(0xFFF5F7FA)
    val SurfaceVariantLight = Color(0xFFEEF1F7)
    val BackgroundLight = Color(0xFFFAFBFC)
    val OnSurfaceLight = Color(0xFF1C1B1F)
    val OnSurfaceVariantLight = Color(0xFF49454E)

    // Neutral Colors - Dark Theme
    val SurfaceDark = Color(0xFF1C1B1F)
    val SurfaceVariantDark = Color(0xFF2B2A2F)
    val BackgroundDark = Color(0xFF0F1419)
    val OnSurfaceDark = Color(0xFFF5F5F5)
    val OnSurfaceVariantDark = Color(0xFFCAC7D0)

    // Status Colors
    val SuccessGreen = Color(0xFF4CAF50)
    val WarningOrange = Color(0xFFFFC107)
    val ErrorRed = Color(0xFFFF5252)
    val InfoBlue = Color(0xFF2196F3)

    // Glassmorphism Overlay Colors
    val GlassOverlayLight = Color(0x1AFFFFFF) // 10% white
    val GlassOverlayDark = Color(0x1A000000)  // 10% black
    val GlassBorderLight = Color(0x4DFFFFFF)  // 30% white border
    val GlassBorderDark = Color(0x4D808080)   // 30% gray border

    // Message Bubble Colors
    val MessageBubbleOwn = Color(0x4D6366FF)  // Primary blue, 30% alpha
    val MessageBubbleOther = Color(0x4DFFFFFF) // White, 30% alpha
}

/**
 * Semantic Color Role Definitions
 *
 * Used by Material3 ColorScheme for adaptive theming.
 */
object SemanticColors {
    // Message-related
    val MessageOwn = Color(0xFF6366FF)
    val MessageOther = Color(0xFFE5E5EA)
    val MessageTimestamp = Color(0xFF999999)

    // Interactive
    val ButtonBackground = Color(0xFF6366FF)
    val ButtonBackgroundDisabled = Color(0xFFCACAD1)
    val ButtonText = Color(0xFFFFFFFF)

    // Input
    val InputBackground = Color(0xFFF5F5F5)
    val InputBorder = Color(0xFFCCCCCC)
    val InputText = Color(0xFF333333)
    val InputPlaceholder = Color(0xFF999999)
}

