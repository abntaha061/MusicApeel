package com.example.presentation.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AuroraBackground(
    dominantColors: List<Color>,
    modifier: Modifier = Modifier
) {
    // Default colors if extracted list is empty or insufficient
    val safeColors = remember(dominantColors) {
        if (dominantColors.size >= 3) {
            dominantColors
        } else if (dominantColors.size == 2) {
            dominantColors + listOf(Color(0xFF018786))
        } else if (dominantColors.size == 1) {
            dominantColors + listOf(Color(0xFF03DAC6), Color(0xFF018786))
        } else {
            listOf(
                Color(0xFF6200EE), // Vibrant Purple
                Color(0xFF03DAC6), // Teal
                Color(0xFF018786)  // Muted green-teal
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Slow, fluid animation
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_offset"
    )

    val color1 = safeColors[0]
    val color2 = safeColors[1]
    val color3 = safeColors[2]

    // Smooth color transitions during song changes
    val animatedColor1 by animateColorAsState(targetValue = color1, animationSpec = tween(1200), label = "c1")
    val animatedColor2 by animateColorAsState(targetValue = color2, animationSpec = tween(1500), label = "c2")
    val animatedColor3 by animateColorAsState(targetValue = color3, animationSpec = tween(1800), label = "c3")

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Dark base luxury background underlay
        drawRect(Color.Black)

        // Spot 1 - Top-Right - Strong Alpha for vivid color
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedColor1.copy(alpha = 0.75f),
                    animatedColor1.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = Offset(
                    x = width * (0.75f + offset * 0.2f),
                    y = height * (0.15f + offset * 0.1f)
                ),
                radius = width * 0.85f
            ),
            radius = width * 0.85f,
            center = Offset(
                x = width * (0.75f + offset * 0.2f),
                y = height * (0.15f + offset * 0.1f)
                )
        )

        // Spot 2 - Bottom-Left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedColor2.copy(alpha = 0.65f),
                    animatedColor2.copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = Offset(
                    x = width * (0.20f - offset * 0.15f),
                    y = height * (0.75f - offset * 0.1f)
                ),
                radius = width * 0.80f
            ),
            radius = width * 0.80f,
            center = Offset(
                x = width * (0.20f - offset * 0.15f),
                y = height * (0.75f - offset * 0.1f)
            )
        )

        // Spot 3 - Center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedColor3.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(
                    x = width * 0.5f,
                    y = height * (0.5f + offset * 0.15f)
                ),
                radius = width * 0.60f
            ),
            radius = width * 0.60f,
            center = Offset(
                x = width * 0.5f,
                y = height * (0.5f + offset * 0.15f)
            )
        )

        // Light scrim to ensure text / lyrics are perfectly readable
        drawRect(Color.Black.copy(alpha = 0.30f))
    }
}
