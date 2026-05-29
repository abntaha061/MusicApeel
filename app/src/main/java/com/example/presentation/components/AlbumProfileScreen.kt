package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
fun AlbumProfileScreen(
    albumName: String,
    allSongs: List<SongEntity>,
    fontFamily: FontFamily,
    onBack: () -> Unit,
    onSongSelected: (List<SongEntity>, Int) -> Unit,
    onAddToNext: (SongEntity) -> Unit,
    onViewArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumSongs = remember(albumName, allSongs) {
        allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
    }

    val firstSong = albumSongs.firstOrNull()
    val mainArtist = firstSong?.artist ?: "غير معروف"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }

            Text(
                text = "صفحة الألبوم",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(40.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Header Item
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (firstSong != null) {
                        AlbumArtImage(
                            songId = firstSong.id,
                            filePath = firstSong.filePath,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            cornerRadius = 20.dp,
                            iconSize = 48.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = albumName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .clickable { onViewArtist(mainArtist) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = mainArtist,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "يحتوي على ${albumSongs.size} أغنية",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = fontFamily
                    )
                }
            }

            item {
                Text(
                    text = "جميع أغاني الألبوم",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (albumSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد أغاني في هذا الألبوم",
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = fontFamily
                        )
                    }
                }
            } else {
                itemsIndexed(albumSongs, key = { _, s -> s.id }) { index, s ->
                    SongRowComponent(
                        song = s,
                        fontFamily = fontFamily,
                        onClick = { onSongSelected(albumSongs, index) },
                        onAddToNext = { onAddToNext(s) },
                        onViewArtist = { onViewArtist(it) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}
