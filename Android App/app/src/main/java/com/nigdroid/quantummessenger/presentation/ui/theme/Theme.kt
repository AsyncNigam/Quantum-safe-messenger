package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ─── Dark color scheme — "Volcanic Noir" ─────────────────────────────────────

private val QuantumDarkColorScheme = darkColorScheme(
    primary              = Color(0xFFE85C3A),   // ember orange-red
    onPrimary            = Color(0xFFEAE6E2),
    primaryContainer     = Color(0xFFC44028),
    onPrimaryContainer   = Color(0xFFF07850),
    secondary            = Color(0xFFE8A848),   // molten gold
    onSecondary          = Color(0xFF0A0A0C),
    secondaryContainer   = Color(0xFFE8A848).copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFFF0C478),
    tertiary             = Color(0xFF48B888),   // ash green
    onTertiary           = Color(0xFF0A0A0C),
    tertiaryContainer    = Color(0xFF309868).copy(alpha = 0.15f),
    onTertiaryContainer  = Color(0xFF48B888),
    error                = Color(0xFFE85C3A),
    onError              = Color(0xFF0A0A0C),
    errorContainer       = Color(0xFFE85C3A).copy(alpha = 0.12f),
    onErrorContainer     = Color(0xFFE85C3A),
    background           = Color(0xFF0A0A0C),   // pure obsidian
    onBackground         = Color(0xFFEAE6E2),
    surface              = Color(0xFF161618),   // charcoal
    onSurface            = Color(0xFFEAE6E2),
    surfaceVariant       = Color(0xFF1E1E22),   // elevated
    onSurfaceVariant     = Color(0xFF9A9490),
    outline              = Color(0x20E85C3A),   // ember border
    outlineVariant       = Color(0x14E85C3A),
    scrim                = Color(0xFF0A0A0C).copy(alpha = 0.70f),
    inverseSurface       = Color(0xFFEAE6E2),
    inverseOnSurface     = Color(0xFF0A0A0C),
    inversePrimary       = Color(0xFFC44028),
)

// ─── Light color scheme — "Blushed Silk" (unchanged) ─────────────────────────

private val QuantumLightColorScheme = lightColorScheme(
    primary              = Color(0xFFC05878),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFD47890).copy(alpha = 0.28f),
    onPrimaryContainer   = Color(0xFF9A3858),
    secondary            = Color(0xFF8868AA),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFAA98CC).copy(alpha = 0.22f),
    onSecondaryContainer = Color(0xFF8868AA),
    tertiary             = Color(0xFF60A090),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFF407870).copy(alpha = 0.16f),
    onTertiaryContainer  = Color(0xFF407870),
    error                = Color(0xFFB82850),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFB82850).copy(alpha = 0.10f),
    onErrorContainer     = Color(0xFFB82850),
    background           = Color(0xFFF0D4CC),
    onBackground         = Color(0xFF281418),
    surface              = Color(0xFFFFFAF8),
    onSurface            = Color(0xFF281418),
    surfaceVariant       = Color(0xFFF8EDE8),
    onSurfaceVariant     = Color(0xFF583040),
    outline              = Color(0x30C05878),
    outlineVariant       = Color(0x18C05878),
    scrim                = Color(0xFF281418).copy(alpha = 0.38f),
    inverseSurface       = Color(0xFF281418),
    inverseOnSurface     = Color(0xFFF0D4CC),
    inversePrimary       = Color(0xFFD47890),
)

// ─── Typography ───────────────────────────────────────────────────────────────

val QuantumTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Black,     fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.05.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.05.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

// ─── Shapes ───────────────────────────────────────────────────────────────────

val QuantumShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun QuantumMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SideEffect { QuantumColors.applyDark(darkTheme) }

    MaterialTheme(
        colorScheme = if (darkTheme) QuantumDarkColorScheme else QuantumLightColorScheme,
        typography  = QuantumTypography,
        shapes      = QuantumShapes,
        content     = content
    )
}