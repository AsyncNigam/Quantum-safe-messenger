package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Quantum Messenger — Autumn Dusk / Evening Glow Palette
 *
 * Direction: warm charcoal base, copper-amber primary, dusty mauve accent,
 * sage teal for live states. Feels handcrafted, not AI-generated.
 * Glassmorphism reads as frosted window at golden hour.
 */
object QuantumColors {

    // ── Core Brand ──────────────────────────────────────────────
    /** Burnished copper-amber — primary identity */
    val Primary        = Color(0xFFC87840)
    val PrimaryLight   = Color(0xFFE09860)
    val PrimaryDark    = Color(0xFF985820)

    /** Dusty mauve — secondary accent, CTAs */
    val Accent         = Color(0xFF9B6878)
    val AccentSoft     = Color(0xFFC49AAA)

    /** Sage teal — online indicators, delivery ticks */
    val Teal           = Color(0xFF7AAA94)
    val TealDark       = Color(0xFF5A8870)

    // ── Backgrounds ─────────────────────────────────────────────
    /** Deep warm charcoal — true base */
    val Background     = Color(0xFF0A0806)
    /** Card / modal surface */
    val Surface        = Color(0xFF130F0A)
    /** Slightly elevated — bottom sheets, popovers */
    val SurfaceElevated = Color(0xFF1D1610)
    /** Input fields */
    val SurfaceInput   = Color(0xFF1A1410)

    // ── Glass Overlays ──────────────────────────────────────────
    val GlassWhite12      = Color(0x1EFFFFFF)
    val GlassWhite08      = Color(0x14FFFFFF)
    val GlassWhite20      = Color(0x33FFFFFF)
    val GlassBorder       = Color(0x22FFFFFF)
    val GlassBorderBright = Color(0x3AFFFFFF)
    /** Warm amber tint overlay — used on active/selected glass */
    val GlassAmber06      = Color(0x10C87840)

    // ── Text ────────────────────────────────────────────────────
    val TextPrimary    = Color(0xFFEEE8DC)   // warm white
    val TextSecondary  = Color(0xFFACA098)   // warm grey
    val TextTertiary   = Color(0xFF706660)   // muted warm
    val TextDisabled   = Color(0xFF3D3530)

    // ── Status ──────────────────────────────────────────────────
    val Success        = Color(0xFF6EDDA8)   // soft mint
    val Warning        = Color(0xFFF0C060)   // warm amber
    val Error          = Color(0xFFE86870)   // warm red

    // ── Message Bubbles ─────────────────────────────────────────
    /** Own — deep copper warmth */
    val BubbleOwn      = Color(0xFF5A3C22)
    /** Other — cool-neutral dark glass */
    val BubbleOther    = Color(0xFF1A1410)

    // ── Legacy aliases — kept so build doesn't break ────────────
    val PrimaryBlue            = Primary
    val PrimaryBlueLight       = PrimaryLight
    val PrimaryBlueDark        = PrimaryDark
    val SecondaryPurple        = Accent
    val SecondaryPurpleLight   = AccentSoft
    val SecondaryPurpleDark    = Color(0xFF7A5060)
    val AccentCyan             = Teal
    val AccentCyanLight        = Color(0xFF9ACABB)
    val AccentCyanDark         = TealDark
    val SurfaceLight           = Color(0xFFF5F0EA)
    val SurfaceVariantLight    = Color(0xFFEDE8E0)
    val BackgroundLight        = Color(0xFFFAF7F2)
    val OnSurfaceLight         = Color(0xFF1C1810)
    val OnSurfaceVariantLight  = Color(0xFF49454A)
    val SurfaceDark            = Surface
    val SurfaceVariantDark     = SurfaceElevated
    val BackgroundDark         = Background
    val OnSurfaceDark          = TextPrimary
    val OnSurfaceVariantDark   = TextSecondary
    val SuccessGreen           = Success
    val WarningOrange          = Warning
    val ErrorRed               = Error
    val InfoBlue               = Color(0xFF5A8AAA)
    val GlassOverlayLight      = GlassWhite12
    val GlassOverlayDark       = GlassWhite08
    val GlassBorderLight       = GlassBorder
    val GlassBorderDark        = GlassBorder
    val MessageBubbleOwn       = BubbleOwn
    val MessageBubbleOther     = BubbleOther
}

object SemanticColors {
    val MessageOwn               = QuantumColors.Primary
    val MessageOther             = QuantumColors.BubbleOther
    val MessageTimestamp         = QuantumColors.TextTertiary
    val ButtonBackground         = QuantumColors.Primary
    val ButtonBackgroundDisabled = QuantumColors.TextDisabled
    val ButtonText               = QuantumColors.TextPrimary
    val InputBackground          = QuantumColors.SurfaceInput
    val InputBorder              = QuantumColors.GlassBorder
    val InputText                = QuantumColors.TextPrimary
    val InputPlaceholder         = QuantumColors.TextTertiary
}