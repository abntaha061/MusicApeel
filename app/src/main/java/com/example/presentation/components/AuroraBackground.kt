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
            dominantColors + listOf(Color(0xFF004D40))
        } else if (dominantColors.size == 1) {
            dominantColors + listOf(Color(0xFF6A1B9A), Color(0xFF004D40))
        } else {
            listOf(
                Color(0xFF1565C0), // Rich Blue
                Color(0xFF6A1B9A), // Cosmic Purple
                Color(0xFF00695C)  // Emerald Teal
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Blob 1 - moves horizontally and vertically speed cycle 5000ms
    val blob1X by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1y"
    )

    // Blob 2 - moves opposite of blob 1 speed cycle 5000ms
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = -0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2x"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2y"
    )

    // Blob 3 - moves diagonally speed cycle 5000ms
    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3x"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3y"
    )

    val color1 = safeColors[0]
    val color2 = safeColors[1]
    val color3 = safeColors[2]

    // Smooth color transitions during song changes
    val animatedColor1 by animateColorAsState(targetValue = color1, animationSpec = tween(1200), label = "c1")
    val animatedColor2 by animateColorAsState(targetValue = color2, animationSpec = tween(1500), label = "c2")
    val animatedColor3 by animateColorAsState(targetValue = color3, animationSpec = tween(1800), label = "c3")

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Hardware accelerated GPU blur on Android S+ (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            renderEffect = BlurEffect(120f, 120f, TileMode.Clamp)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                }
        ) {
            // Dark base luxury background underlay
            drawRect(Color(0xFF060606))

            // Blob 1 drawing - Elongated Oval, covering a huge area
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to animatedColor1.copy(alpha = 0.85f),
                        0.3f to animatedColor1.copy(alpha = 0.55f),
                        0.6f to animatedColor1.copy(alpha = 0.25f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * blob1X, size.height * blob1Y),
                    radius = size.width * 0.9f
                ),
                topLeft = Offset(
                    size.width * blob1X - size.width * 0.9f,
                    size.height * blob1Y - size.height * 0.45f
                ),
                size = Size(size.width * 1.8f, size.height * 0.9f)
            )

            // Blob 2 drawing
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to animatedColor2.copy(alpha = 0.80f),
                        0.3f to animatedColor2.copy(alpha = 0.50f),
                        0.6f to animatedColor2.copy(alpha = 0.20f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * blob2X, size.height * blob2Y),
                    radius = size.width * 0.85f
                ),
                topLeft = Offset(
                    size.width * blob2X - size.width * 0.85f,
                    size.height * blob2Y - size.height * 0.50f
                ),
                size = Size(size.width * 1.7f, size.height * 1.0f)
            )

            // Blob 3 drawing
            drawOval(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to animatedColor3.copy(alpha = 0.70f),
                        0.4f to animatedColor3.copy(alpha = 0.35f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * blob3X, size.height * blob3Y),
                    radius = size.width * 0.70f
                ),
                topLeft = Offset(
                    size.width * blob3X - size.width * 0.70f,
                    size.height * blob3Y - size.height * 0.40f
                ),
                size = Size(size.width * 1.4f, size.height * 0.80f)
            )

            // Center subtle ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.06f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )

            // Scrim to maintain content readability (text contrast)
            drawRect(Color.Black.copy(alpha = 0.20f))
        }
    }
}
