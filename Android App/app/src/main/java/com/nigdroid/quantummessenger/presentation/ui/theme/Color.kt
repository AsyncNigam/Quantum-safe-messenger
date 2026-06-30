package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ─── DARK palette — "Volcanic Noir" ──────────────────────────────────────────
// Pure obsidian black base. Ember orange-red (#E85C3A) as the signature accent.
// Pale platinum text. Think: raw power, volcanic energy, Tesla dark mode.
// No blue, no purple, no cyan — bold and aggressive.

private object Dark {
    // Ember — molten lava accent for buttons, toggles, active states
    val Primary         = Color(0xFFE85C3A)
    val PrimaryLight    = Color(0xFFF07850)
    val PrimaryDark     = Color(0xFFC44028)

    // Molten gold — warm secondary for highlights, badges
    val Accent          = Color(0xFFE8A848)
    val AccentSoft      = Color(0xFFF0C478)

    // Deep forest ash — success states
    val Teal            = Color(0xFF48B888)
    val TealDark        = Color(0xFF309868)

    // Pure obsidian — true black with zero color tint for OLED
    val Background      = Color(0xFF0A0A0C)   // near-true black
    val Surface         = Color(0xFF161618)   // charcoal card
    val SurfaceElevated = Color(0xFF1E1E22)   // elevated panels
    val SurfaceInput    = Color(0xFF1A1A1E)   // input field fill

    // Smoke-tinted glass overlays
    val GlassWhite12       = Color(0x1EFFFFFF)
    val GlassWhite08       = Color(0x14FFFFFF)
    val GlassWhite20       = Color(0x33FFFFFF)
    val GlassBorder        = Color(0x20E85C3A)   // ember-tinted border
    val GlassBorderBright  = Color(0x38E85C3A)

    // Text — pale platinum/silver stack
    val TextPrimary     = Color(0xFFEAE6E2)   // warm platinum white
    val TextSecondary   = Color(0xFF9A9490)   // muted stone
    val TextTertiary    = Color(0xFF5C5856)   // dark ash
    val TextDisabled    = Color(0xFF2E2C2A)

    val Success         = Color(0xFF48B888)
    val Warning         = Color(0xFFE8A848)   // molten gold
    val Error           = Color(0xFFE85C3A)   // ember (same as primary for cohesion)

    // Chat bubbles — volcanic theme
    val BubbleOwn       = Color(0xFF3A1810)   // deep ember charcoal (your msgs)
    val BubbleOther     = Color(0xFF1C1A18)   // dark volcanic ash (their msgs)
}

// ─── LIGHT palette — "Blushed Silk" (unchanged per user approval) ────────────

private object Light {
    val Primary         = Color(0xFFC05878)
    val PrimaryLight    = Color(0xFFD47890)
    val PrimaryDark     = Color(0xFF9A3858)

    val Accent          = Color(0xFF8868AA)
    val AccentSoft      = Color(0xFFAA98CC)

    val Teal            = Color(0xFF60A090)
    val TealDark        = Color(0xFF407870)

    val Background      = Color(0xFFF8EAE6)
    val Surface         = Color(0xFFFFFAF8)
    val SurfaceElevated = Color(0xFFF8EDE8)
    val SurfaceInput    = Color(0xFFF4E4DE)

    val GlassWhite12       = Color(0x28FFFFFF)
    val GlassWhite08       = Color(0x1CFFFFFF)
    val GlassWhite20       = Color(0x40FFFFFF)
    val GlassBorder        = Color(0x38FFFFFF)
    val GlassBorderBright  = Color(0x60FFFFFF)

    val TextPrimary     = Color(0xFF281418)
    val TextSecondary   = Color(0xFF583040)
    val TextTertiary    = Color(0xFF886068)
    val TextDisabled    = Color(0xFFBCA0A8)

    val Success         = Color(0xFF2E9A68)
    val Warning         = Color(0xFFCA8818)
    val Error           = Color(0xFFB82850)

    val BubbleOwn       = Color(0xFFF4A8B8)
    val BubbleOther     = Color(0xFFE8DCFF)
}

// ─── Unified semantic object — use this everywhere ───────────────────────────

object QuantumColors {

    @Volatile private var isDark: Boolean = true

    /** Called by QuantumMessengerTheme before content renders. */
    internal fun applyDark(dark: Boolean) { isDark = dark }

    val Primary         get() = if (isDark) Dark.Primary        else Light.Primary
    val PrimaryLight    get() = if (isDark) Dark.PrimaryLight   else Light.PrimaryLight
    val PrimaryDark     get() = if (isDark) Dark.PrimaryDark    else Light.PrimaryDark

    val Accent          get() = if (isDark) Dark.Accent         else Light.Accent
    val AccentSoft      get() = if (isDark) Dark.AccentSoft     else Light.AccentSoft

    val Teal            get() = if (isDark) Dark.Teal           else Light.Teal
    val TealDark        get() = if (isDark) Dark.TealDark       else Light.TealDark

    val Background      get() = if (isDark) Dark.Background     else Light.Background
    val Surface         get() = if (isDark) Dark.Surface        else Light.Surface
    val SurfaceElevated get() = if (isDark) Dark.SurfaceElevated else Light.SurfaceElevated
    val SurfaceInput    get() = if (isDark) Dark.SurfaceInput   else Light.SurfaceInput

    val GlassWhite12       get() = if (isDark) Dark.GlassWhite12      else Light.GlassWhite12
    val GlassWhite08       get() = if (isDark) Dark.GlassWhite08      else Light.GlassWhite08
    val GlassWhite20       get() = if (isDark) Dark.GlassWhite20      else Light.GlassWhite20
    val GlassBorder        get() = if (isDark) Dark.GlassBorder       else Light.GlassBorder
    val GlassBorderBright  get() = if (isDark) Dark.GlassBorderBright else Light.GlassBorderBright

    val TextPrimary     get() = if (isDark) Dark.TextPrimary    else Light.TextPrimary
    val TextSecondary   get() = if (isDark) Dark.TextSecondary  else Light.TextSecondary
    val TextTertiary    get() = if (isDark) Dark.TextTertiary   else Light.TextTertiary
    val TextDisabled    get() = if (isDark) Dark.TextDisabled   else Light.TextDisabled

    val Success         get() = if (isDark) Dark.Success        else Light.Success
    val Warning         get() = if (isDark) Dark.Warning        else Light.Warning
    val Error           get() = if (isDark) Dark.Error          else Light.Error

    val BubbleOwn       get() = if (isDark) Dark.BubbleOwn      else Light.BubbleOwn
    val BubbleOther     get() = if (isDark) Dark.BubbleOther    else Light.BubbleOther
}