package com.example.presentation.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SongEntity
import com.example.presentation.components.AuroraBackground
import com.example.presentation.components.LyricsView
import com.example.presentation.home.formatDuration

@Composable
fun PlayerScreen(
    currentSong: SongEntity?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    playerViewModel: PlayerViewModel,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onSeek: (Long) -> Unit,
    onCollapseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val lyrics by playerViewModel.lyrics.collectAsState()
    val dominantColors by playerViewModel.dominantColors.collectAsState()

    val CairoBold = FontFamily.SansSerif

    // Sync state update when track changes
    LaunchedEffect(currentSong) {
        playerViewModel.updateSong(currentSong)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Fullscreen animated Aurora background mesh gradient 🌌
        AuroraBackground(dominantColors = dominantColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 2. Beautiful compact Header block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapseClicked,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "تصغير المشغل",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.artist,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = CairoBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Invisible spacer matching left arrow size to balance alignment
                Box(modifier = Modifier.size(44.dp))
            }

            // 3. Huge centered Live Lyrics View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LyricsView(
                    lrcContent = lyrics,
                    currentPositionMs = currentPositionMs,
                    onLineClicked = onSeek
                )
            }

            // 4. Seekbar Progress Control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                val sliderPosition = if (currentSong.duration > 0) {
                    currentPositionMs.toFloat()
                } else {
                    0f
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(currentSong.duration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = CairoBold
                    )
                    Text(
                        text = formatDuration(currentSong.duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = CairoBold
                    )
                }
            }

            // 5. Minimal Playback controls panel (Exactly 3 high-contrast elements ONLY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousClicked,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "السابق",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp) // 36dp Skip
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                IconButton(
                    onClick = onPlayPauseClicked,
                    modifier = Modifier.size(76.dp), // Large 64dp active circle
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                IconButton(
                    onClick = onNextClicked,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "التالي",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp) // 36dp Skip
                    )
                }
            }
        }
    }
}
