package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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

        // Auto-scroll to current active line
        LaunchedEffect(currentLineIndex, lines) {
            if (currentLineIndex >= 0 && lines.isNotEmpty()) {
                listState.animateScrollToItem(
                    index = maxOf(0, currentLineIndex - 3),
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
                            start = 24.dp,  // RTL: right margin padding
                            end = 16.dp,    // RTL: left margin padding
                            top = 120.dp,
                            bottom = 220.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { index, _ -> index }
                        ) { index, line ->
                            val distance = abs(index - currentLineIndex)
                            val isActive = distance == 0

                            val textColor by animateColorAsState(
                                targetValue = when (distance) {
                                    0 -> Color.White                       // Active: Pure White
                                    1 -> Color.White.copy(alpha = 0.45f)   // Near: Medium Gray
                                    2 -> Color.White.copy(alpha = 0.25f)   // Farther: Faded
                                    3 -> Color.White.copy(alpha = 0.15f)   // Even Farther: Very Faded
                                    else -> Color.White.copy(alpha = 0.08f) // Unfocused: Ghostly
                                },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "lyric_color_$index"
                            )

                            val fontSize by animateFloatAsState(
                                targetValue = when (distance) {
                                    0 -> 26f   // Active: 26sp
                                    1 -> 20f   // Near: 20sp
                                    2 -> 17f   // Far: 17sp
                                    else -> 15f // Others: 15sp
                                },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                label = "lyric_size_$index"
                            )

                            val verticalPadding by animateDpAsState(
                                targetValue = if (isActive) 14.dp else 9.dp,
                                animationSpec = tween(durationMillis = 350),
                                label = "lyric_padding_$index"
                            )

                            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = fontSize.sp,
                                fontWeight = fontWeight,
                                textAlign = TextAlign.Start, // RTL: Start = Right
                                lineHeight = (fontSize * 1.45f).sp,
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

// إخفاء الكلمات من الأعلى والأسفل بشكل انسيابي وبأداء عالي مستقر
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
