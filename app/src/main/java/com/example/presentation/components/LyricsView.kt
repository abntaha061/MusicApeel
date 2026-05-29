package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lyrics.LrcLine
import com.example.data.lyrics.LrcParser
import kotlin.math.abs

@Composable
fun LyricsView(
    lrcContent: String,
    currentPositionMs: Long,
    onLineClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val lines = remember(lrcContent) { LrcParser.parseLrcFile(lrcContent) }
        val currentLineIndex = remember(lines, currentPositionMs) {
            LrcParser.getCurrentLineIndex(lines, currentPositionMs)
        }

        val listState = rememberLazyListState()

        // Auto-scroll to current active line smoothly
        LaunchedEffect(currentLineIndex, lines) {
            if (currentLineIndex >= 0 && lines.isNotEmpty()) {
                listState.animateScrollToItem(
                    index = maxOf(0, currentLineIndex - 2),
                    scrollOffset = 0
                )
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد كلمات متاحة",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 16.dp,
                            top = 120.dp,
                            bottom = 220.dp
                        )
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { index, _ -> index }
                        ) { index, line ->
                            val distance = abs(index - currentLineIndex)
                            val isActive = distance == 0

                            // Performance optimization: only animate the alpha opacity of each lyric line
                            val alpha by animateFloatAsState(
                                targetValue = when (distance) {
                                    0 -> 1.0f
                                    1 -> 0.50f
                                    2 -> 0.28f
                                    3 -> 0.15f
                                    else -> 0.08f
                                },
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                label = "lyric_alpha_$index"
                            )

                            // Keep the structural dimensions static to avoid layout calculation bottleneck
                            val fontSize = when (distance) {
                                0 -> 25.sp
                                1 -> 20.sp
                                2 -> 17.sp
                                else -> 15.sp
                            }

                            val verticalPadding = if (isActive) 14.dp else 9.dp
                            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                            Text(
                                text = line.text,
                                color = Color.White.copy(alpha = alpha),
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                textAlign = TextAlign.Start, // RTL right-to-left layout
                                lineHeight = fontSize * 1.5f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = verticalPadding)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onLineClicked(line.timestamp)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Fade out top and bottom areas elegantly
fun Modifier.fadingEdge(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.15f to Color.Black,
                0.85f to Color.Black,
                1.0f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
