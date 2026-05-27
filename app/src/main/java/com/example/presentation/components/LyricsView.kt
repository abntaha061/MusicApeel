package com.example.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val lines = remember(lrcContent) { LrcParser.parseLrcFile(lrcContent) }
    val activeLineIndex = remember(lines, currentPositionMs) {
        LrcParser.getCurrentLineIndex(lines, currentPositionMs)
    }

    val listState = rememberLazyListState()

    // Automatically center active lyric smoothly
    LaunchedEffect(activeLineIndex) {
        if (lines.isNotEmpty() && activeLineIndex in lines.indices) {
            // Animate scroll to active lyric line centered
            val centerOffset = 4 // Approx index offset to show active line in middle
            val scrollTo = if (activeLineIndex > centerOffset) activeLineIndex - 2 else 0
            listState.animateScrollToItem(scrollTo)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 24.dp,   // More breathing space from right edge
                    top = 180.dp,
                    bottom = 260.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End // Align item placeholders to the right
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index }
                ) { index, line ->
                    val isActive = index == activeLineIndex
                    val distance = abs(index - activeLineIndex)

                    // Smooth dynamic scaling of font size depending on active state
                    val fontSize by animateFloatAsState(
                        targetValue = when {
                            isActive -> 26f
                            distance == 1 -> 20f
                            else -> 17f
                        },
                        animationSpec = tween(300),
                        label = "fontSize"
                    )

                    // Smooth alpha transition based on closeness to current line
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            isActive -> 1.0f
                            distance == 1 -> 0.65f
                            distance == 2 -> 0.45f
                            else -> 0.30f
                        },
                        animationSpec = tween(300),
                        label = "alpha"
                    )

                    Text(
                        text = line.text,
                        fontSize = fontSize.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Right, // Right align text characters
                        lineHeight = (fontSize * 1.4f).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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
