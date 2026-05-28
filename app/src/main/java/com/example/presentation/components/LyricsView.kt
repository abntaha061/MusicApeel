package com.example.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
            val currentLine = lines.getOrNull(activeLineIndex)
            val previousLine = lines.getOrNull(activeLineIndex - 1)
            val nextLine = lines.getOrNull(activeLineIndex + 1)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // ── Previous line (25% opaque) ──
                AnimatedContent(
                    targetState = previousLine,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "prevLine"
                ) { prev ->
                    if (prev != null) {
                        Text(
                            text = prev.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.25f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onLineClicked(prev.timestamp)
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                // ── Active line (Pure white & Bold) ──
                AnimatedContent(
                    targetState = currentLine,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(500)) { it / 3 } + fadeIn(tween(500)))
                            .togetherWith(slideOutVertically(animationSpec = tween(400)) { -it / 3 } + fadeOut(tween(400)))
                    },
                    label = "activeLine"
                ) { curr ->
                    Text(
                        text = curr?.text ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                curr?.let { onLineClicked(it.timestamp) }
                            }
                    )
                }

                // ── Next line (20% opaque) ──
                AnimatedContent(
                    targetState = nextLine,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "nextLine"
                ) { next ->
                    if (next != null) {
                        Text(
                            text = next.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.20f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onLineClicked(next.timestamp)
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}
