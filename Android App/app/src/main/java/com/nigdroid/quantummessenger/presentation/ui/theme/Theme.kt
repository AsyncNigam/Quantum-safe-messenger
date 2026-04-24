package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Color Scheme ───────────────────────────────────────────────────────────

private val QuantumDarkColorScheme = darkColorScheme(
    primary              = QuantumColors.Primary,
    onPrimary            = QuantumColors.TextPrimary,
    primaryContainer     = QuantumColors.PrimaryDark,
    onPrimaryContainer   = QuantumColors.PrimaryLight,
    secondary            = QuantumColors.Accent,
    onSecondary          = QuantumColors.Background,
    secondaryContainer   = QuantumColors.AccentSoft.copy(alpha = 0.3f),
    onSecondaryContainer = QuantumColors.AccentSoft,
    tertiary             = QuantumColors.Teal,
    onTertiary           = QuantumColors.Background,
    tertiaryContainer    = QuantumColors.TealDark.copy(alpha = 0.3f),
    onTertiaryContainer  = QuantumColors.Teal,
    error                = QuantumColors.Error,
    onError              = QuantumColors.Background,
    errorContainer       = QuantumColors.Error.copy(alpha = 0.15f),
    onErrorContainer     = QuantumColors.Error,
    background           = QuantumColors.Background,
    onBackground         = QuantumColors.TextPrimary,
    surface              = QuantumColors.Surface,
    onSurface            = QuantumColors.TextPrimary,
    surfaceVariant       = QuantumColors.SurfaceElevated,
    onSurfaceVariant     = QuantumColors.TextSecondary,
    outline              = QuantumColors.GlassBorder,
    outlineVariant       = QuantumColors.GlassWhite08,
    scrim                = QuantumColors.Background.copy(alpha = 0.6f),
    inverseSurface       = QuantumColors.TextPrimary,
    inverseOnSurface     = QuantumColors.Background,
    inversePrimary       = QuantumColors.PrimaryDark,
)

// ─── Typography ─────────────────────────────────────────────────────────────

val QuantumTypography = Typography(
    // Large display — splash / hero text
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize   = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        color = QuantumColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 45.sp,
        lineHeight = 52.sp,
        color = QuantumColors.TextPrimary
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        color = QuantumColors.TextPrimary
    ),
    // Headlines — screen titles
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
        color = QuantumColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        lineHeight = 36.sp,
        color = QuantumColors.TextPrimary
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 32.sp,
        color = QuantumColors.TextPrimary
    ),
    // Titles — lists, cards
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
        color = QuantumColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = QuantumColors.TextPrimary
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = QuantumColors.TextPrimary
    ),
    // Body — message text, descriptions
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = QuantumColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = QuantumColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = QuantumColors.TextTertiary
    ),
    // Labels — timestamps, badges
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = QuantumColors.TextSecondary
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = QuantumColors.TextTertiary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = QuantumColors.TextTertiary
    ),
)

// ─── Shapes ─────────────────────────────────────────────────────────────────

val QuantumShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ─── Theme Composable ────────────────────────────────────────────────────────

@Composable
fun QuantumMessengerTheme(
    darkTheme: Boolean = true, // Always dark — matches design reference
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QuantumDarkColorScheme,
        typography  = QuantumTypography,
        shapes      = QuantumShapes,
        content     = content
    )
}
