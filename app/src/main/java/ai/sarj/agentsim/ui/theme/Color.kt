package ai.sarj.agentsim.ui.theme

import androidx.compose.ui.graphics.Color

// Light, premium sarj.ai palette. (Names kept stable for compatibility; SarjOnDark is now "ink".)
val SarjBg = Color(0xFFF6F7FB)        // warm off-white canvas
val SarjSurface = Color(0xFFFFFFFF)   // cards, sheets, nav
val SarjSurfaceHi = Color(0xFFEEF0F8) // received bubble, inset fields, chips
val SarjLine = Color(0xFFE3E6F0)      // hairline dividers

val SarjPrimary = Color(0xFF5B5BD6)   // indigo-violet
val SarjPrimaryHi = Color(0xFF7B6CF0) // gradient top
val SarjAccent = Color(0xFF12B886)    // mint

val SarjOnDark = Color(0xFF1A1C2A)    // primary text ("ink")
val SarjMuted = Color(0xFF6B7185)     // secondary text
val SarjFaint = Color(0xFF9AA0B4)     // tertiary

val Heart = Color(0xFFFF5A7A)
val Coin = Color(0xFFF4B740)
val Good = Color(0xFF12B886)
val Warn = Color(0xFFF59F00)
val Bad = Color(0xFFE8505B)
val Star = Color(0xFFFFC53D)

val InquiryColor = Color(0xFF4C6EF5)
val RequestColor = Color(0xFF12B886)
val ComplaintColor = Color(0xFFE8505B)

// Avatar fallback palette (personas carry their own accentColor)
val AvatarColors = listOf(
    Color(0xFF5B5BD6), Color(0xFF12B886), Color(0xFFF4845F),
    Color(0xFF4C6EF5), Color(0xFFE64980), Color(0xFFF59F00),
    Color(0xFF15AABF), Color(0xFF7048E8)
)
