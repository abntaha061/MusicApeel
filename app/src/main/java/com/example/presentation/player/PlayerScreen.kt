package com.example.presentation.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SongEntity
import com.example.data.lyrics.LyricLine
import com.example.presentation.components.AlbumArtImage
import com.example.presentation.components.GlassCard
import com.example.presentation.components.LyricsView
import com.example.presentation.components.formatDuration

@Composable
fun PlayerScreen(
    song: SongEntity,
    isPlaying: Boolean,
    currentPositionMs: Long,
    dominantColors: List<Color>,
    lyrics: List<LyricLine>,
    fontFamily: FontFamily,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Controls whether we are currently showing AlbumArt mode or Lyrics mode
    var showLyricsMode by remember { mutableStateOf(false) }

    // Dynamic reactive background gradients representing album visual leaks
    val primaryThemeColor = dominantColors.firstOrNull() ?: Color(0xFF4FC3F7)
    val secondaryColor = dominantColors.getOrNull(1) ?: Color(0xFF141414)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryThemeColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.8f),
                        Color(0xFF0F0F0F)
                    )
                )
            )
    ) {
        // Vertical content alignment
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Action Hub
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "تصغير المشغل",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "مشغل الأغنية",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = song.album,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                IconButton(
                    onClick = { showLyricsMode = !showLyricsMode },
                    modifier = Modifier
                        .background(if (showLyricsMode) Color(0xFF4FC3F7).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "عرض الكلمات",
                        tint = if (showLyricsMode) Color(0xFF4FC3F7) else Color.White
                    )
                }
            }

            // Central View Area: Artwork mode vs. Synder Lymph-lines mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showLyricsMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "centerView"
                ) { lyricsMode ->
                    if (lyricsMode) {
                        LyricsView(
                            lyrics = lyrics,
                            currentPositionMs = currentPositionMs,
                            fontFamily = fontFamily,
                            onLineClick = { targetTime -> onSeekTo(targetTime) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Immersive square shadow Card layout for artwork
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 1.6.dp,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AlbumArtImage(
                                        songId = song.id,
                                        filePath = song.filePath,
                                        modifier = Modifier.fillMaxSize(0.9f),
                                        cornerRadius = 16.dp,
                                        iconSize = 56.dp
                                    )
                                }
                            }

                            // Meta titles
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = song.artist,
                                color = Color(0xFF4FC3F7),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Player seek indicators and controls (always displayed at the bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek timeline sliders
                val progress = if (song.duration > 0) currentPositionMs.toFloat() / song.duration else 0f
                Slider(
                    value = progress,
                    onValueChange = { percent ->
                        val targetMs = (percent * song.duration).toLong()
                        onSeekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4FC3F7),
                        activeTrackColor = Color(0xFF4FC3F7),
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timer labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = fontFamily
                    )
                    Text(
                        text = formatDuration(song.duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = fontFamily
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Navigation Control Circle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "السابق",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "تشغيل وقوف مؤقت",
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "التالي",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
