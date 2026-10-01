package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StudioBackground

@Composable
fun GlowingBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "studio_glow")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val shiftX by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shiftX"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Top-right glowing cyan orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = pulse1),
                        NeonCyan.copy(alpha = pulse1 * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.85f + shiftX, canvasHeight * 0.15f),
                    radius = canvasWidth * 0.7f
                )
            )

            // Middle-left glowing violet orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = pulse2),
                        ElectricViolet.copy(alpha = pulse2 * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.15f - shiftX, canvasHeight * 0.45f),
                    radius = canvasWidth * 0.8f
                )
            )

            // Bottom-right glowing magenta orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = pulse1 * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.75f, canvasHeight * 0.85f),
                    radius = canvasWidth * 0.65f
                )
            )
        }

        content()
    }
}
