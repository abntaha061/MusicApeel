package com.example.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.db.LibraryStats
import com.example.data.db.SongEntity
import com.example.presentation.components.AlbumArtImage
import com.example.presentation.components.GlassCard
import com.example.presentation.components.SongRowComponent
import com.example.presentation.components.formatDuration

// Data structure models for search classification
data class ArtistSearchItem(
    val name: String,
    val songCount: Int,
    val sampleSongId: Long,
    val sampleFilePath: String
)

data class AlbumSearchItem(
    val name: String,
    val artist: String,
    val songCount: Int,
    val sampleSongId: Long,
    val sampleFilePath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    allSongs: List<List<SongEntity>>, // List wrapped inside container to represent full list trigger or direct list parameter
    allSongsDirect: List<SongEntity>,
    recentlyPlayed: List<SongEntity>,
    stats: LibraryStats,
    sortOrder: SortOrder,
    isSyncing: Boolean,
    fontFamily: FontFamily,
    onSetSortOrder: (SortOrder) -> Unit,
    onPlaySongList: (List<SongEntity>, Int) -> Unit,
    onAddToNext: (SongEntity) -> Unit,
    onViewArtist: (String) -> Unit,
    onViewAlbum: (String) -> Unit,
    onForceSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // Fuzzy and direct search classification matching
    val matchedArtists = remember(allSongsDirect, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            allSongsDirect.flatMap { HomeViewModel.splitArtists(it.artist) }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .filter { it.contains(searchQuery, ignoreCase = true) }
                .map { name ->
                    val artistSongs = allSongsDirect.filter { s ->
                        HomeViewModel.splitArtists(s.artist).any { it.equals(name, ignoreCase = true) }
                    }
                    val firstSong = artistSongs.firstOrNull()
                    ArtistSearchItem(
                        name = name,
                        songCount = artistSongs.size,
                        sampleSongId = firstSong?.id ?: 0L,
                        sampleFilePath = firstSong?.filePath ?: ""
                    )
                }
        }
    }

    val matchedAlbums = remember(allSongsDirect, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            allSongsDirect.map { it.album.trim() }
                .filter { it.isNotEmpty() && !it.equals("ألبوم غير معروف", ignoreCase = true) && !it.equals("Unknown Album", ignoreCase = true) }
                .distinct()
                .filter { it.contains(searchQuery, ignoreCase = true) }
                .map { albumName ->
                    val albumSongs = allSongsDirect.filter { it.album.equals(albumName, ignoreCase = true) }
                    val firstSong = albumSongs.firstOrNull()
                    AlbumSearchItem(
                        name = albumName,
                        artist = firstSong?.artist ?: "غير معروف",
                        songCount = albumSongs.size,
                        sampleSongId = firstSong?.id ?: 0L,
                        sampleFilePath = firstSong?.filePath ?: ""
                    )
                }
        }
    }

    val filteredSongs = remember(allSongsDirect, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            allSongsDirect.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141A23),
                        Color(0xFF0C0E12),
                        Color(0xFF050608)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Screen Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بيور سونيك",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = fontFamily
                )

                IconButton(
                    onClick = onForceSync,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(40.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color(0xFF4FC3F7),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "تحديث المكتبة",
                            tint = Color.White
                        )
                    }
                }
            }

            // Search Bar Input Field Design
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن أغنية، فنان أو ألبوم...", color = Color.White.copy(alpha = 0.45f), fontFamily = fontFamily) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "بحث", tint = Color.LightGray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Rounded.Clear, contentDescription = "مسح", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area (Conditional: Searching state vs Standard Home layout)
            if (searchQuery.isNotBlank()) {
                // RENDER SEARCH CLASSIFICATIONS
                if (matchedArtists.isEmpty() && matchedAlbums.isEmpty() && filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة لبحثك",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp,
                            fontFamily = fontFamily
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Matched Artists (Circular Cards)
                        if (matchedArtists.isNotEmpty()) {
                            item {
                                Text(
                                    text = "من نتائج الفنانين",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(matchedArtists) { item ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { onViewArtist(item.name) }
                                                .padding(4.dp)
                                                .width(90.dp)
                                        ) {
                                            AlbumArtImage(
                                                songId = item.sampleSongId,
                                                filePath = item.sampleFilePath,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape),
                                                cornerRadius = 36.dp,
                                                iconSize = 22.dp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                fontFamily = fontFamily
                                            )
                                            Text(
                                                text = "${item.songCount} أغنية",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = fontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Matched Albums
                        if (matchedAlbums.isNotEmpty()) {
                            item {
                                Text(
                                    text = "من نتائج الألبومات",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(matchedAlbums) { item ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { onViewAlbum(item.name) }
                                                .padding(4.dp)
                                                .width(100.dp)
                                        ) {
                                            AlbumArtImage(
                                                songId = item.sampleSongId,
                                                filePath = item.sampleFilePath,
                                                modifier = Modifier.size(80.dp),
                                                cornerRadius = 12.dp,
                                                iconSize = 24.dp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                fontFamily = fontFamily
                                            )
                                            Text(
                                                text = item.artist,
                                                color = Color(0xFF4FC3F7),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                fontFamily = fontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Matched Songs list
                        if (filteredSongs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "جميع الأغاني المطابقة",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            itemsIndexed(filteredSongs) { index, song ->
                                SongRowComponent(
                                    song = song,
                                    fontFamily = fontFamily,
                                    onClick = { onPlaySongList(filteredSongs, index) },
                                    onAddToNext = { onAddToNext(song) },
                                    onViewArtist = { onViewArtist(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                // NORMAL PORTRAIT LIBRARY SCREEN
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Standard Stats Row Card
                    item {
                        GlassCard(
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatItem(value = "${stats.totalSongs}", label = "أغنية", fontFamily = fontFamily)
                                StatItem(value = "${stats.totalArtists}", label = "فنان", fontFamily = fontFamily)
                                StatItem(value = "${stats.totalAlbums}", label = "ألبوم", fontFamily = fontFamily)
                                StatItem(
                                    value = formatStatsDuration(stats.totalDurationMs),
                                    label = "إجمالي الوقت",
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }

                    // Recently played horizontal list (optional / preview)
                    if (recentlyPlayed.isNotEmpty()) {
                        item {
                            Text(
                                text = "أحدث الأغاني تقديماً",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = fontFamily,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(recentlyPlayed) { index, song ->
                                    Column(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clickable { onPlaySongList(recentlyPlayed, index) }
                                    ) {
                                        AlbumArtImage(
                                            songId = song.id,
                                            filePath = song.filePath,
                                            modifier = Modifier.size(96.dp),
                                            cornerRadius = 14.dp,
                                            iconSize = 30.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = fontFamily
                                        )
                                        Text(
                                            text = song.artist,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = fontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // General Library Heading with Count + Sort triggers
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجلد الأغاني (${allDirectCount(allSongsDirect)} أغنية)",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = fontFamily
                            )

                            // Clean Sort Trigger Button Icon
                            IconButton(
                                onClick = { showSortBottomSheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sort,
                                    contentDescription = "ترتيب الأغاني",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Song List rows
                    if (allSongsDirect.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد أغاني، اضغط على أعلى لتحديث المكتبة",
                                    color = Color.White.copy(0.4f),
                                    fontSize = 13.sp,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(allSongsDirect) { index, song ->
                            SongRowComponent(
                                song = song,
                                fontFamily = fontFamily,
                                onClick = { onPlaySongList(allSongsDirect, index) },
                                onAddToNext = { onAddToNext(song) },
                                onViewArtist = { onViewArtist(it) }
                            )
                        }
                    }
                }
            }
        }

        // Custom Dark Modal Bottom Sheet for reactive sorting criteria selection
        if (showSortBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortBottomSheet = false },
                containerColor = Color(0xFF141414),
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = "ترتيب الأغاني المتاح",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    SortOrder.values().forEach { order ->
                        val isSelected = sortOrder == order
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(0.08f) else Color.Transparent)
                                .clickable {
                                    onSetSortOrder(order)
                                    showSortBottomSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.displayName,
                                color = if (isSelected) Color(0xFF4FC3F7) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = fontFamily
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4FC3F7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, fontFamily: FontFamily) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = fontFamily
        )
    }
}

fun formatStatsDuration(ms: Long): String {
    val hrs = ms / 3600000
    val mins = (ms / 60000) % 60
    return if (hrs > 0) "${hrs}س ${mins}د" else "${mins}د"
}

fun allDirectCount(list: List<SongEntity>): Int {
    return list.size
}
