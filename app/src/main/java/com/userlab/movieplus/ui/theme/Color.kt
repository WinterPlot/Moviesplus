package com.userlab.movieplus.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  UserLab design system  (v2)
//  Deep midnight canvas · electric violet brand · cyan highlight
// ─────────────────────────────────────────────────────────────

// Canvas & surfaces
val UlMidnight = Color(0xFF070A13)      // app background
val UlSurface = Color(0xFF10152A)       // cards
val UlSurfaceHigh = Color(0xFF1A2140)   // raised cards, inputs, chips
val UlOutline = Color(0xFF2A3358)       // hairline borders

// Brand
val UlViolet = Color(0xFF7C5CFF)        // primary
val UlVioletDeep = Color(0xFF5B3FE0)    // pressed / gradient start
val UlCyan = Color(0xFF22D3EE)          // secondary highlight
val UlAmber = Color(0xFFFFB547)         // ratings / warnings
val UlSuccess = Color(0xFF34D399)
val UlError = Color(0xFFFF5C7A)

// Text
val UlTextPrimary = Color(0xFFF4F6FF)
val UlTextSecondary = Color(0xFF9AA3C7)

// Brand gradients
val UlBrandGradient: Brush
    get() = Brush.linearGradient(listOf(UlVioletDeep, UlViolet, UlCyan))
val UlButtonGradient: Brush
    get() = Brush.horizontalGradient(listOf(UlVioletDeep, UlViolet))

// ── Legacy token names ──────────────────────────────────────
// Every existing screen reads these names, so pointing them at the
// new palette re-themes the whole app without touching each screen.
val ObsidianDark = UlMidnight
val ObsidianCard = UlSurface
val CinematicRed = UlViolet      // primary brand accent (kept name for compatibility)
val CrimsonRed = UlVioletDeep
val AccentAmber = UlAmber
val TextPrimary = UlTextPrimary
val TextSecondary = UlTextSecondary

// Legacy compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
