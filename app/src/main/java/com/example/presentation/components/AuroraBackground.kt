package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Composable
fun AuroraBackground(
    dominantColors: List<Color>,
    modifier: Modifier = Modifier
) {
    // Ensure extracted colors are not empty, transparent, or black
    val safeColors = remember(dominantColors) {
        val filtered = dominantColors.filter { 
            it != Color.Transparent && 
            it != Color.Black && 
            it != Color(0xFF0F172A) && 
            it != Color(0xFF1E293B) && 
            it != Color(0xFF334155) 
        }
        if (filtered.size >= 2) {
            filtered
        } else {
            listOf(
                Color(0xFF8B4513), // Warm Brown (Default)
                Color(0xFF2F4F4F), // Dark Slate Green
                Color(0xFF4B0082)  // Indigo Purple
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val b1x by infiniteTransition.animateFloat(
        initialValue = 0.0f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1x"
    )
    val b2x by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(8500, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2x"
    )
    val b3y by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3y"
    )

    // Ensure safe color alphas
    val c1 = safeColors[0].copy(alpha = 1f)
    val c2 = safeColors[1].copy(alpha = 1f)
    val c3 = safeColors.getOrElse(2) { safeColors[0] }.copy(alpha = 1f)

    // Smooth transition between track changes
    val animatedC1 by animateColorAsState(targetValue = c1, animationSpec = tween(1200), label = "anim_c1")
    val animatedC2 by animateColorAsState(targetValue = c2, animationSpec = tween(1500), label = "anim_c2")
    val animatedC3 by animateColorAsState(targetValue = c3, animationSpec = tween(1800), label = "anim_c3")

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val minDim = size.minDimension

            // Solid high end luxury dark slate backdrop underlay
            drawRect(color = Color(0xFF070708))

            // Blob 1 — Swirling left/right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedC1.copy(alpha = 0.70f),
                        animatedC1.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(width * b1x, height * 0.25f),
                    radius = minDim * 0.9f
                ),
                radius = minDim * 0.9f,
                center = Offset(width * b1x, height * 0.25f)
            )

            // Blob 2 — Swirling complementary opposite
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedC2.copy(alpha = 0.65f),
                        animatedC2.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(width * b2x, height * 0.65f),
                    radius = minDim * 0.85f
                ),
                radius = minDim * 0.85f,
                center = Offset(width * b2x, height * 0.65f)
            )

            // Blob 3 — Moving vertically in central axis
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedC3.copy(alpha = 0.55f),
                        animatedC3.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * b3y),
                    radius = minDim * 0.75f
                ),
                radius = minDim * 0.75f,
                center = Offset(width * 0.5f, height * b3y)
            )

            // Dark scrim overlay to secure typography legibility and readability contrast
            drawRect(color = Color.Black.copy(alpha = 0.16f))
        }
    }
}
