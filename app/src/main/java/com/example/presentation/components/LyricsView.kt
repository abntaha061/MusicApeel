package com.example.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
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
    val scope = rememberCoroutineScope()
    var showTranslation by remember { mutableStateOf(false) }

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
                    text = "كلمات الموسيقى غير متوفرة حالياً",
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

                    val targetScale = if (isActive) 1.12f else 1.0f
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
                        
                        // English translation overlay with custom slide-in animation
                        if (showTranslation && !line.translation.isNullOrEmpty()) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 10 }),
                                exit = fadeOut(tween(400)) + slideOutVertically()
                            ) {
                                Text(
                                    text = line.translation,
                                    color = if (isActive) Color(0xFF81D4FA) else Color.White.copy(alpha = 0.5f),
                                    fontSize = if (isActive) 15.sp else 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Translation toggle FAB floating bottom-right corner 🌐
        if (lines.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.BottomStart // RTL makes bottom start equivalent to bottom right
            ) {
                FilledIconButton(
                    onClick = { showTranslation = !showTranslation },
                    modifier = Modifier.size(54.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showTranslation) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "ترجمة الكلمات",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
