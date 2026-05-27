package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AuroraBackground(
    dominantColors: List<Color>,
    modifier: Modifier = Modifier
) {
    // Ensure we always have at least 3 custom colors (fallbacks if palette under-extracts)
    val color1 = dominantColors.getOrElse(0) { Color(0xFF8E24AA) } // Deep purple
    val color2 = dominantColors.getOrElse(1) { Color(0xFF1E88E5) } // Blue
    val color3 = dominantColors.getOrElse(2) { Color(0xFFD81B60) } // Pink

    // Smoothly transition colors when song changes
    val animatedColor1 by animateColorAsState(targetValue = color1, animationSpec = tween(1200))
    val animatedColor2 by animateColorAsState(targetValue = color2, animationSpec = tween(1200))
    val animatedColor3 by animateColorAsState(targetValue = color3, animationSpec = tween(1200))

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Animated positions for the blobs to create a drifting nebula feel
    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1y"
    )

    val blob2X by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2x"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2y"
    )

    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3x"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = EaseInOutElastic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3y"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw deep pure black luxury background base
        drawRect(Color(0xFF030303))

        // Draw Blob 1 (Dynamic Color 1)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedColor1.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(size.width * blob1X, size.height * blob1Y),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * blob1X, size.height * blob1Y),
            blendMode = BlendMode.Screen
        )

        // Draw Blob 2 (Dynamic Color 2)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedColor2.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * blob2X, size.height * blob2Y),
                radius = size.width * 0.80f
            ),
            radius = size.width * 0.80f,
            center = Offset(size.width * blob2X, size.height * blob2Y),
            blendMode = BlendMode.Screen
        )

        // Draw Blob 3 (Dynamic Color 3)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(animatedColor3.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * blob3X, size.height * blob3Y),
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * blob3X, size.height * blob3Y),
            blendMode = BlendMode.Screen
        )

        // Final super dark scrim for elegant content readability
        drawRect(Color.Black.copy(alpha = 0.55f))
    }
}
