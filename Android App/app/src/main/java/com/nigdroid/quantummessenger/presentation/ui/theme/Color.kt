package com.nigdroid.quantummessenger.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ─── DARK palette — Whiskey Barrel (matched to Image 2) ──────────────────────
// Image 2: Deep espresso #1A1008, warm copper/amber highlights, mocha surfaces
// Chats list bg is ~#1C1208, story rings are amber-orange, bubbles are #2A1A0E

private object Dark {
    // Burnished copper — the amber story rings & active accents in Image 2
    val Primary         = Color(0xFFBF7030)   // richer, darker amber than before
    val PrimaryLight    = Color(0xFFD4904A)
    val PrimaryDark     = Color(0xFF905020)

    // Muted dusty mauve — secondary accent
    val Accent          = Color(0xFF8C5A68)
    val AccentSoft      = Color(0xFFAA7888)

    // Sage teal (keep, used sparingly)
    val Teal            = Color(0xFF6A9882)
    val TealDark        = Color(0xFF4E7860)

    // KEY: Match Image 2 — deep espresso-black, nearly #100C06
    val Background      = Color(0xFF0E0A04)   // espresso black
    val Surface         = Color(0xFF1A1208)   // warm dark-brown card (matches Image 2 chat bg)
    val SurfaceElevated = Color(0xFF241A0C)   // slightly lighter mocha card
    val SurfaceInput    = Color(0xFF1E1610)   // input field surface

    // Glass overlays — warm amber-tinted glass (not cold white)
    val GlassWhite12       = Color(0x1EFFF5E8)   // warm amber-tinted glass
    val GlassWhite08       = Color(0x14FFF0DC)
    val GlassWhite20       = Color(0x33FFEDCC)
    val GlassBorder        = Color(0x28D4904A)   // warm amber border
    val GlassBorderBright  = Color(0x44D4904A)

    // Text — warm cream stack (Image 2 uses near-white #EEE0CC on dark bg)
    val TextPrimary     = Color(0xFFEEE4D4)
    val TextSecondary   = Color(0xFFAA9880)
    val TextTertiary    = Color(0xFF6E5E48)
    val TextDisabled    = Color(0xFF3A2E20)

    val Success         = Color(0xFF5EC898)
    val Warning         = Color(0xFFF0B84A)
    val Error           = Color(0xFFE05A60)

    // Chat bubbles — Image 2: own = warm dark amber, other = very dark mocha
    val BubbleOwn       = Color(0xFF3A2410)   // dark warm amber bubble (own)
    val BubbleOther     = Color(0xFF201508)   // near-black mocha (other)
}

// ─── LIGHT palette — Blushed Silk (matched to Image 1) ───────────────────────
// Image 1: Background is warm blush-peach ~#F2D8D0, cards are milky #FFF8F6,
// accent is dusty rose #C4607A, lavender is ~#9B7BB8, bottom bar is glass-white

private object Light {
    // Dusty rose — the "Connect" button & active icons in Image 1
    val Primary         = Color(0xFFC05878)   // deep dusty rose
    val PrimaryLight    = Color(0xFFD47890)
    val PrimaryDark     = Color(0xFF9A3858)

    // Soft lavender — the left-panel bleed in Image 1
    val Accent          = Color(0xFF8868AA)
    val AccentSoft      = Color(0xFFAA98CC)

    // Sage (consistent)
    val Teal            = Color(0xFF60A090)
    val TealDark        = Color(0xFF407870)

    // KEY: Match Image 1 — warm peach-salmon background ~#F0D4CC
    val Background      = Color(0xFFF8EAE6)   // warm blush-peach (matches Image 1 bg)
    val Surface         = Color(0xFFFFFAF8)   // milky white card (Image 1 cards)
    val SurfaceElevated = Color(0xFFF8EDE8)   // slightly tinted elevated surface
    val SurfaceInput    = Color(0xFFF4E4DE)   // warm peach input

    // Rose-tinted glass — strong enough to be visible on peach bg
    val GlassWhite12       = Color(0x28FFFFFF)
    val GlassWhite08       = Color(0x1CFFFFFF)
    val GlassWhite20       = Color(0x40FFFFFF)
    val GlassBorder        = Color(0x38FFFFFF)
    val GlassBorderBright  = Color(0x60FFFFFF)

    // Text — warm deep brown on blush (Image 1 uses #2A1018 on peach)
    val TextPrimary     = Color(0xFF281418)
    val TextSecondary   = Color(0xFF583040)
    val TextTertiary    = Color(0xFF886068)
    val TextDisabled    = Color(0xFFBCA0A8)

    val Success         = Color(0xFF2E9A68)
    val Warning         = Color(0xFFCA8818)
    val Error           = Color(0xFFB82850)

    // Bubbles — Image 1 aesthetic: own = warm blush, other = soft lavender-white
    val BubbleOwn       = Color(0xFFF4A8B8)   // warm rose bubble
    val BubbleOther     = Color(0xFFE8DCFF)   // soft lavender bubble
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