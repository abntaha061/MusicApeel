package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
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
import com.example.data.lyrics.LyricLine
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    fontFamily: FontFamily,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Identify which lyric line is currently active
    val activeIndex = remember(lyrics, currentPositionMs) {
        val index = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (index == -1 && lyrics.isNotEmpty()) 0 else index
    }

    // 2. Perform smooth scroll to center the active lyric line
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            coroutineScope.launch {
                // Scroll with offset so it stays centered
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -150
                )
            }
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا توجد كلمات متزامنة لهذه الأغنية",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 15.sp,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeIndex
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color(0xFF4FC3F7) else Color.White,
                    animationSpec = tween(300),
                    label = "textColor"
                )
                val textAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.45f,
                    animationSpec = tween(300),
                    label = "textAlpha"
                )
                val scaleFactor by animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 1.0f,
                    animationSpec = tween(300),
                    label = "scale"
                )

                Text(
                    text = line.text,
                    color = textColor,
                    fontSize = (20 * scaleFactor).sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .alpha(textAlpha)
                        .clickable { onLineClick(line.timeMs) }
                )
            }
        }
    }
}
