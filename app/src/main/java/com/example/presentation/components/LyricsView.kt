package com.example.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lyrics.LrcLine
import com.example.data.lyrics.LrcParser
import kotlinx.coroutines.launch

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

    // Synchronize auto scroll centering of the lyrics
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
                contentPadding = PaddingValues(top = 180.dp, bottom = 260.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lines) { index, line ->
                    val isActive = index == activeLineIndex
                    val distance = kotlin.math.abs(index - activeLineIndex)

                    // Compute smooth alpha/scale based on proximity to active line
                    val targetAlpha = when {
                        isActive -> 1.0f
                        distance == 1 -> 0.55f
                        distance == 2 -> 0.35f
                        else -> 0.15f
                    }

                    val displayFont = if (isActive) FontWeight.Bold else FontWeight.Medium
                    val fontSize = if (isActive) 26.sp else 19.sp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .alpha(targetAlpha)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onLineClicked(line.timestamp)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = line.text,
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                            fontSize = fontSize,
                            fontWeight = displayFont,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )
                    }
                }
            }
        }
    }
}
