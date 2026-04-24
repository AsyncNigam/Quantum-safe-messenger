package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color Palette for Quantum Messenger
 *
 * iPhone-inspired dark glass UI with rich purple/violet gradients.
 * Every color is intentional — production-grade palette.
 */
object QuantumColors {

    // ── Core Brand ──────────────────────────────────────────────
    /** Deep electric violet — primary identity color */
    val Primary        = Color(0xFF7C5CFC)
    val PrimaryLight   = Color(0xFF9D82FF)
    val PrimaryDark    = Color(0xFF5A3DD8)

    /** Neon magenta accent — call-to-action, highlights */
    val Accent         = Color(0xFFE040FB)
    val AccentSoft     = Color(0xFFCE93D8)

    /** Cyan-teal for online indicators, delivery ticks */
    val Teal           = Color(0xFF00E5FF)
    val TealDark       = Color(0xFF00ACC1)

    // ── Backgrounds ─────────────────────────────────────────────
    /** True black — base canvas for the app */
    val Background     = Color(0xFF08070E)
    /** Card / modal surface — deep navy */
    val Surface        = Color(0xFF110F1E)
    /** Elevated surface — slightly lighter */
    val SurfaceElevated = Color(0xFF1C1930)
    /** Input fields */
    val SurfaceInput   = Color(0xFF1A1826)

    // ── Glass Overlays ──────────────────────────────────────────
    val GlassWhite12   = Color(0x1FFFFFFF) // 12% white
    val GlassWhite08   = Color(0x14FFFFFF) // 8%  white
    val GlassWhite20   = Color(0x33FFFFFF) // 20% white
    val GlassBorder    = Color(0x26FFFFFF) // subtle border
    val GlassBorderBright = Color(0x40FFFFFF)

    // ── Text ────────────────────────────────────────────────────
    val TextPrimary    = Color(0xFFF0EEFF)
    val TextSecondary  = Color(0xFFB0AACC)
    val TextTertiary   = Color(0xFF6E697A)
    val TextDisabled   = Color(0xFF3D3956)

    // ── Status ──────────────────────────────────────────────────
    val Success        = Color(0xFF4DFFB4)
    val Warning        = Color(0xFFFFD740)
    val Error          = Color(0xFFFF6680)

    // ── Message Bubbles ─────────────────────────────────────────
    val BubbleOwn      = Color(0xFF4A3BA0)   // rich purple
    val BubbleOther    = Color(0xFF1E1C33)    // dark glass

    // ── Legacy aliases (kept so build doesn't break) ────────────
    val PrimaryBlue       = Primary
    val PrimaryBlueLight  = PrimaryLight
    val PrimaryBlueDark   = PrimaryDark
    val SecondaryPurple   = Color(0xFF8E6FA8)
    val SecondaryPurpleLight = Color(0xFFA98BC7)
    val SecondaryPurpleDark  = Color(0xFF6F5387)
    val AccentCyan        = Teal
    val AccentCyanLight   = Color(0xFF4DD0E1)
    val AccentCyanDark    = TealDark
    val SurfaceLight      = Color(0xFFF5F7FA)
    val SurfaceVariantLight = Color(0xFFEEF1F7)
    val BackgroundLight   = Color(0xFFFAFBFC)
    val OnSurfaceLight    = Color(0xFF1C1B1F)
    val OnSurfaceVariantLight = Color(0xFF49454E)
    val SurfaceDark       = Surface
    val SurfaceVariantDark = SurfaceElevated
    val BackgroundDark    = Background
    val OnSurfaceDark     = TextPrimary
    val OnSurfaceVariantDark = TextSecondary
    val SuccessGreen      = Success
    val WarningOrange     = Warning
    val ErrorRed          = Error
    val InfoBlue          = Color(0xFF2196F3)
    val GlassOverlayLight = GlassWhite12
    val GlassOverlayDark  = GlassWhite08
    val GlassBorderLight  = GlassBorder
    val GlassBorderDark   = GlassBorder
    val MessageBubbleOwn  = BubbleOwn
    val MessageBubbleOther = BubbleOther
}

object SemanticColors {
    val MessageOwn          = QuantumColors.Primary
    val MessageOther        = QuantumColors.BubbleOther
    val MessageTimestamp    = QuantumColors.TextTertiary
    val ButtonBackground    = QuantumColors.Primary
    val ButtonBackgroundDisabled = QuantumColors.TextDisabled
    val ButtonText          = QuantumColors.TextPrimary
    val InputBackground     = QuantumColors.SurfaceInput
    val InputBorder         = QuantumColors.GlassBorder
    val InputText           = QuantumColors.TextPrimary
    val InputPlaceholder    = QuantumColors.TextTertiary
}
