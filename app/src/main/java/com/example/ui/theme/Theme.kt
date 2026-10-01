package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyanSoft,
    onPrimaryContainer = NeonCyanBright,

    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = ElectricVioletSoft,
    onSecondaryContainer = ElectricVioletBright,

    tertiary = NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = NeonPinkSoft,
    onTertiaryContainer = Color(0xFFFDA4AF),

    background = StudioBackground,
    onBackground = TextPrimary,

    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,

    outline = StudioBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to stunning dark studio aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
