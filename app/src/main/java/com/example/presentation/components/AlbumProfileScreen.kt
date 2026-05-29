package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
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

@Composable
fun AlbumProfileScreen(
    albumName: String,
    allSongs: List<SongEntity>,
    fontFamily: FontFamily,
    onBack: () -> Unit,
    onPlaySongList: (List<SongEntity>, Int) -> Unit,
    onAddToNext: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumSongs = remember(allSongs, albumName) {
        allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
    }
    val firstSong = albumSongs.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2836),
                        Color(0xFF0F1216),
                        Color(0xFF07080A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }

                Text(
                    text = "تفاصيل الألبوم",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(40.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Album Banner item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AlbumArtImage(
                            songId = firstSong?.id ?: 5L,
                            filePath = "",
                            modifier = Modifier.size(150.dp),
                            cornerRadius = 16.dp,
                            iconSize = 48.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = albumName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = firstSong?.artist ?: "غير معروف",
                            color = Color(0xFF4FC3F7),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "يحتوي على ${albumSongs.size} أغنية",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (albumSongs.isNotEmpty()) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 1.dp,
                                modifier = Modifier
                                    .clickable { onPlaySongList(albumSongs, 0) }
                                    .widthIn(min = 160.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "تشغيل الألبوم",
                                        tint = Color(0xFF4FC3F7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تشغيل الكل",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "قائمة أغاني الألبوم",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                if (albumSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أغاني في هذا الألبوم",
                                color = Color.White.copy(0.4f),
                                fontSize = 14.sp,
                                fontFamily = fontFamily
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = albumSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongRowComponent(
                            song = song,
                            fontFamily = fontFamily,
                            onClick = {
                                onPlaySongList(albumSongs, index)
                            },
                            onAddToNext = {
                                onAddToNext(song)
                            },
                            onViewArtist = {}
                        )
                    }
                }
            }
        }
    }
}
