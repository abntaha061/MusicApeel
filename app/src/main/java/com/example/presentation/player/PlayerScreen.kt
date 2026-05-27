package com.example.presentation.player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SongEntity
import com.example.presentation.components.AuroraBackground
import com.example.presentation.components.GlassCard
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

    // Dynamic bitmap cover art to blur and project onto background for depth
    val bitmap = remember(currentSong.filePath) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(currentSong.filePath)
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
    ) {
        // A. Fullscreen smooth moving GPU-accelerated Aurora Background
        AuroraBackground(dominantColors = dominantColors, modifier = Modifier.fillMaxSize())

        // B. Large blurred low-opacity Album Art Background to give maximum depth
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                renderEffect = BlurEffect(60f, 60f, TileMode.Clamp)
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                        }
                        alpha = 0.22f
                    },
                contentScale = ContentScale.Crop
            )
        }

        // C. Super dark overlay scrim for legible text reading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // D. Inner content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Sleek Glass-designed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Glass Back Arrow Button
                GlassCard(
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 20.dp,
                    opacity = 0.15f
                ) {
                    IconButton(
                        onClick = onCollapseClicked,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "تصغير المشغل",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Centered song title and artist labels
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
                    Spacer(modifier = Modifier.height(2.dp))
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

                // Glass More Info Button
                GlassCard(
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 20.dp,
                    opacity = 0.15f
                ) {
                    IconButton(
                        onClick = { /* Menu placeholder */ },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "خيارات إضافية",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. Right-aligned Live Sync Lyrics View - occupies all dynamic remaining vertical space
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

            // 3. Glowing Control Bar Deck with Seekbar Progress Control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                val maxRange = currentSong.duration.toFloat().coerceAtLeast(1f)
                val sliderPosition = if (currentSong.duration > 0) {
                    currentPositionMs.toFloat().coerceIn(0f, maxRange)
                } else {
                    0f
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..maxRange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
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

            // 4. Playback Navigation Bar Buttons (Previous, Primary Play/Pause, Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, top = 16.dp, start = 24.dp, end = 24.dp),
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
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                IconButton(
                    onClick = onPlayPauseClicked,
                    modifier = Modifier.size(76.dp),
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
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
