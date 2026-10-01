package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Navy Slate Dark Studio Theme
val StudioBackground = Color(0xFF090D16)
val StudioSurface = Color(0xFF111827)
val StudioCard = Color(0xFF1E293B)
val StudioCardGlow = Color(0xFF1E293B).copy(alpha = 0.85f)
val StudioBorder = Color(0xFF334155)

// Glowing Neon Accents
val NeonCyan = Color(0xFF06B6D4)
val NeonCyanBright = Color(0xFF22D3EE)
val NeonCyanSoft = Color(0x3306B6D4)

val ElectricViolet = Color(0xFF8B5CF6)
val ElectricVioletBright = Color(0xFFA78BFA)
val ElectricVioletSoft = Color(0x338B5CF6)

val NeonPink = Color(0xFFF43F5E)
val NeonPinkSoft = Color(0x33F43F5E)

val NeonEmerald = Color(0xFF10B981)
val NeonAmber = Color(0xFFF59E0B)

// Text tokens
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Glowing gradient brushes for studio lighting
val CyanVioletGradient = Brush.horizontalGradient(
    listOf(NeonCyan, ElectricViolet)
)

val HeroGlowGradient = Brush.radialGradient(
    colors = listOf(
        NeonCyan.copy(alpha = 0.35f),
        ElectricViolet.copy(alpha = 0.20f),
        Color.Transparent
    )
)

val CardGlowBorder = Brush.linearGradient(
    listOf(
        NeonCyan.copy(alpha = 0.6f),
        ElectricViolet.copy(alpha = 0.6f),
        NeonPink.copy(alpha = 0.3f)
    )
)
