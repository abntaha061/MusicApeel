package com.example.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.AlbumArtImage
import com.example.data.db.ArtistStats
import com.example.data.db.SongEntity
import com.example.data.db.ArtistWithArt
import com.example.data.db.LibraryStats

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onSongSelected: (List<SongEntity>, Int) -> Unit,
    onArtistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by homeViewModel.allSongs.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val mostPlayed by homeViewModel.mostPlayed.collectAsState()
    val topArtists by homeViewModel.topArtists.collectAsState()
    val isSyncing by homeViewModel.isSyncing.collectAsState()
    val libraryStats by homeViewModel.libraryStats.collectAsState()
    val artistsForYou by homeViewModel.artistsForYou.collectAsState()
    val mostPlayedSong by homeViewModel.mostPlayedSong.collectAsState()

    val CairoBold = FontFamily.SansSerif // Fallback safe beautiful arabic typography

    Box(modifier = modifier.fillMaxSize()) {
        if (allSongs.isEmpty() && isSyncing) {
            // Full-screen beautiful loader during the initial setup/scan
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1E88E5),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "جاري مسح ملفاتك الصوتية...",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "نستخرج ميزات الأغاني وننظم مكتبتك التراثية بعناية هادئة",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontFamily = CairoBold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(top = 24.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App header title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "طرب",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = CairoBold
                            )
                            Text(
                                text = "عالمك الموسيقي الشخصي الكلاسيكي",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontFamily = CairoBold
                            )
                        }

                        IconButton(
                            onClick = { homeViewModel.syncLibrary(force = true) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "مزامنة المكتبة",
                                tint = if (isSyncing) Color(0xFF1E88E5) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 1. Library Stats Card
                item {
                    LibraryStatsCard(stats = libraryStats, fontFamily = CairoBold)
                }

                // 2. Most Played Song Card
                mostPlayedSong?.let { song ->
                    item {
                        MostPlayedCard(
                            song = song,
                            onClick = { onSongSelected(listOf(song), 0) },
                            fontFamily = CairoBold
                        )
                    }
                }

                // 3. Artists For You
                if (artistsForYou.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "فنانين من أجلك",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artistsForYou, key = { it.name }) { artist ->
                                    ArtistForYouCard(
                                        artist = artist,
                                        onClick = { onArtistSelected(artist.name) },
                                        fontFamily = CairoBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Recently Played
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "تم تشغيلها مؤخراً",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recentlyPlayed, key = { it.id }) { song ->
                                    PlaylistItemCard(
                                        song = song,
                                        fontFamily = CairoBold,
                                        onClick = {
                                            val index = recentlyPlayed.indexOf(song)
                                            onSongSelected(recentlyPlayed, index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Most Played list
                if (mostPlayed.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "الأكثر استماعاً",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(mostPlayed, key = { it.id }) { song ->
                                    PlaylistItemCard(
                                        song = song,
                                        fontFamily = CairoBold,
                                        onClick = {
                                            val index = mostPlayed.indexOf(song)
                                            onSongSelected(mostPlayed, index)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal subtle progress indicator for background scans when catalog is not empty
        if (isSyncing && allSongs.isNotEmpty()) {
            LinearProgressIndicator(
                color = Color(0xFF1E88E5),
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(3.dp)
            )
        }
    }
}

@Composable
fun PlaylistItemCard(
    song: SongEntity,
    fontFamily: FontFamily,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        // Frosted border premium cover with customized AlbumArtImage
        AlbumArtImage(
            songId = song.id,
            filePath = song.filePath,
            modifier = Modifier
                .size(120.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                ),
            cornerRadius = 14.dp,
            iconSize = 38.dp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCircleBlock(
    artist: ArtistStats,
    fontFamily: FontFamily,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Elegant micro vector placeholder representing professional dynamic background
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = artist.artist,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${artist.totalPlays} استماع",
            color = Color(0xFF1E88E5),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamily,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SongRowItem(
    song: SongEntity,
    index: Int,
    fontFamily: FontFamily,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            songId = song.id,
            filePath = song.filePath,
            modifier = Modifier.size(52.dp),
            cornerRadius = 8.dp,
            iconSize = 24.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Display small nice analytics count
        if (song.playCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color(0xFF1E88E5).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${song.playCount}",
                    color = Color(0xFF1E88E5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = formatDuration(song.duration),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = fontFamily
        )
    }
}

fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000)
    return String.format("%02d:%02d", min, sec)
}

@Composable
fun LibraryStatsCard(stats: LibraryStats, fontFamily: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.08f))
            .border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.End
    ) {
        // العنوان
        Text(
            "مكتبتك الموسيقية",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily
        )

        Spacer(Modifier.height(4.dp))

        // المدة الإجمالية
        Text(
            formatTotalDuration(stats.totalDurationMs),
            color = Color.White.copy(0.7f),
            fontSize = 14.sp,
            fontFamily = fontFamily
        )

        Spacer(Modifier.height(16.dp))

        // الإحصائيات في صف واحد مع فواصل
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = "${stats.totalSongs}", label = "أغنية", fontFamily = fontFamily)
            StatDivider()
            StatItem(value = "${stats.totalArtists}", label = "فنان", fontFamily = fontFamily)
            StatDivider()
            StatItem(value = "${stats.totalAlbums}", label = "ألبوم", fontFamily = fontFamily)
        }
    }
}

@Composable
fun StatItem(value: String, label: String, fontFamily: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily
        )
        Text(
            label,
            color = Color.White.copy(0.55f),
            fontSize = 12.sp,
            fontFamily = fontFamily
        )
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(32.dp)
            .background(Color.White.copy(0.2f))
    )
}

fun formatTotalDuration(ms: Long): String {
    val totalMinutes = ms / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

@Composable
fun MostPlayedCard(song: SongEntity, onClick: () -> Unit, fontFamily: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(0.08f))
            .border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "⭐ أكثر أغنية شغّلتها",
                color = Color.White.copy(0.6f),
                fontSize = 12.sp,
                fontFamily = fontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = Color.White.copy(0.6f),
                fontSize = 13.sp,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${song.playCount} مرة استماع",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        }

        // صورة الألبوم
        AlbumArtImage(
            songId = song.id,
            filePath = song.filePath,
            modifier = Modifier.size(70.dp),
            cornerRadius = 12.dp
        )
    }
}

@Composable
fun ArtistForYouCard(artist: ArtistWithArt, onClick: () -> Unit, fontFamily: FontFamily) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.1f))
        ) {
            AlbumArtImage(
                songId = artist.sampleFilePath.hashCode().toLong(),
                filePath = artist.sampleFilePath,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 50.dp
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${artist.songCount} أغنية",
            color = Color.White.copy(0.55f),
            fontSize = 11.sp,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

