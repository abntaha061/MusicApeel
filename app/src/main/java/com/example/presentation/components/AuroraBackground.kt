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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AuroraBackground(
    dominantColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    // Only 2 animated offsets instead of 8 to minimize calculation overhead
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing), // 20 seconds loop - extremely smooth and slow
            repeatMode = RepeatMode.Reverse
        ),
        label = "o1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing), // 25 seconds loop
            repeatMode = RepeatMode.Reverse
        ),
        label = "o2"
    )
    
    val color1 = dominantColors.getOrElse(0) { Color(0xFF1E88E5) }
    val color2 = dominantColors.getOrElse(1) { Color(0xFFD81B60) }
    val color3 = dominantColors.getOrElse(2) { Color(0xFF004D40) }

    // Smooth color transitions during song changes
    val animatedColor1 by animateColorAsState(targetValue = color1, animationSpec = tween(1500), label = "c1")
    val animatedColor2 by animateColorAsState(targetValue = color2, animationSpec = tween(1500), label = "c2")
    val animatedColor3 by animateColorAsState(targetValue = color3, animationSpec = tween(1500), label = "c3")

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Hardware accelerated GPU blur on Android S+ (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            renderEffect = BlurEffect(80f, 80f, TileMode.Clamp)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                }
        ) {
            // Underlay dark luxury canvas background
            drawRect(Color(0xFF030303))
            
            // Blob 1: Animates slowly horizontally near top
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor1.copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(size.width * offset1, size.height * 0.3f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * offset1, size.height * 0.3f),
                blendMode = BlendMode.Screen
            )
            
            // Blob 2: Animates slowly horizontally near bottom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor2.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * offset2, size.height * 0.7f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * offset2, size.height * 0.7f),
                blendMode = BlendMode.Screen
            )
            
            // Blob 3: Stationary stable center blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor3.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.60f
                ),
                radius = size.width * 0.60f,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                blendMode = BlendMode.Screen
            )
            
            // Dark elegant scrim for readability
            drawRect(Color.Black.copy(alpha = 0.40f))
        }
    }
}
