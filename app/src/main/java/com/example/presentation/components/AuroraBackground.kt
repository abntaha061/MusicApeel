package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AuroraBackground(
    primaryColor: Color = Color(0xFF4FC3F7),
    secondaryColor: Color = Color(0xFF9575CD),
    tertiaryColor: Color = Color(0xFFFFB74D),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val angleRad = Math.toRadians(phase1.toDouble())
            val cosVal = Math.cos(angleRad).toFloat()
            val sinVal = Math.sin(angleRad).toFloat()

            // Dynamic points for smooth ambient shifting
            val center1 = Offset(
                x = width * (0.3f + 0.12f * cosVal),
                y = height * (0.25f + 0.08f * sinVal)
            )
            val center2 = Offset(
                x = width * (0.75f - 0.12f * sinVal),
                y = height * (0.35f + 0.1f * cosVal)
            )

            // Background dynamic atmospheric radial layers
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                    center = center1,
                    radius = width * 1.1f
                ),
                center = center1,
                radius = width * 1.1f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.3f), Color.Transparent),
                    center = center2,
                    radius = width * 1.2f
                ),
                center = center2,
                radius = width * 1.2f
            )

            val center3 = Offset(width * 0.5f, height * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryColor.copy(alpha = 0.15f), Color.Transparent),
                    center = center3,
                    radius = width * 0.75f
                ),
                center = center3,
                radius = width * 0.75f
            )
        }
        
        // Double-scrim overlay - rich top scrim for status icon readable overlays, deep dark bottom scrim for track listing readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )
    }
}
