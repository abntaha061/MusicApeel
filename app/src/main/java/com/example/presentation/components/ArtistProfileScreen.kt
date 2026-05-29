package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.presentation.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistProfileScreen(
    artistName: String,
    allSongs: List<SongEntity>,
    fontFamily: FontFamily,
    onBack: () -> Unit,
    onPlaySongList: (List<SongEntity>, Int) -> Unit,
    onAddToNext: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Filter tracks for the selected artist
    val artistSongs = remember(allSongs, artistName) {
        allSongs.filter { song ->
            HomeViewModel.splitArtists(song.artist).any { it.equals(artistName, ignoreCase = true) }
        }
    }

    // 2. Map dynamic aurora colors uniquely based on the selected artist
    val auroraColors = remember(artistName) {
        when {
            artistName.contains("أحمد جمال") || artistName.contains("Ahmed Gamal") -> {
                // Warm rose-shimmer and cosmic emeralds
                Triple(Color(0xFFEC407A), Color(0xFF26A69A), Color(0xFFFFA726))
            }
            artistName.contains("عمرو دياب") || artistName.contains("Amr Diab") -> {
                // Premium deep cyan and energetic violet leaks
                Triple(Color(0xFF00ACC1), Color(0xFF7E57C2), Color(0xFF1E88E5))
            }
            artistName.contains("فيروز") || artistName.contains("Fairouz") -> {
                // Galactic indigo and bright turquoise mist
                Triple(Color(0xFF3F51B5), Color(0xFF00BDB0), Color(0xFF90A4AE))
            }
            artistName.contains("أم كلثوم") || artistName.contains("Oum Kalthoum") -> {
                // Classic royal gold and midnight burgundy
                Triple(Color(0xFFD4AF37), Color(0xFF800020), Color(0xFF3E2723))
            }
            else -> {
                // Default high-contrast ambient violet
                Triple(Color(0xFF4FC3F7), Color(0xFF9575CD), Color(0xFFFFB74D))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 3. Render the animating dynamic aurora background behind the content
        AuroraBackground(
            primaryColor = auroraColors.first,
            secondaryColor = auroraColors.second,
            tertiaryColor = auroraColors.third,
            modifier = Modifier.fillMaxSize()
        )

        // 4. Actual Screen Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Action Row
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
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }

                Text(
                    text = "ملف الفنان",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(40.dp)) // Visual balance anchor
            }

            // Scrollable track list combined with headers
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Artist Visual Badge Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular visual profile placeholder or dynamic avatar
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(auroraColors.first.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                                .background(Color.White.copy(0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtImage(
                                songId = artistSongs.firstOrNull()?.id ?: 9L,
                                filePath = "",
                                modifier = Modifier.size(100.dp),
                                cornerRadius = 50.dp,
                                iconSize = 36.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = artistName,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${artistSongs.size} أغنية في المكتبة",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))

                        // Large Floating Quick Play Glass Action
                        if (artistSongs.isNotEmpty()) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 1.dp,
                                modifier = Modifier
                                    .clickable { onPlaySongList(artistSongs, 0) }
                                    .widthIn(min = 160.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "تشغيل الكل",
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

                // Tracks Heading Title
                item {
                    Text(
                        text = "الأغاني الشعبية للفنان",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }

                if (artistSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أغاني لهذا الفنان حالياً",
                                color = Color.White.copy(0.4f),
                                fontSize = 14.sp,
                                fontFamily = fontFamily
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = artistSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongRowComponent(
                            song = song,
                            fontFamily = fontFamily,
                            onClick = {
                                onPlaySongList(artistSongs, index)
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
