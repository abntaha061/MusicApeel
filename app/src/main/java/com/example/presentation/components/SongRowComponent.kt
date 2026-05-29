package com.example.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SongEntity
import com.example.presentation.home.formatDuration

@Composable
fun SongRowComponent(
    song: SongEntity,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    onAddToNext: () -> Unit,
    onViewArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            songId = song.id,
            filePath = song.filePath,
            modifier = Modifier.size(48.dp),
            cornerRadius = 8.dp,
            iconSize = 22.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val artists = splitArtists(song.artist)
                artists.forEachIndexed { i, artistName ->
                    Text(
                        text = artistName,
                        color = Color(0xFF4FC3F7),
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onViewArtist(artistName) }
                    )
                    if (i < artists.size - 1) {
                        Text(
                            text = " ، ",
                            color = Color(0xFF4FC3F7).copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = fontFamily
                        )
                    }
                }
            }
        }

        Text(
            text = formatDuration(song.duration),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontFamily = fontFamily
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "خيارات الأغنية",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E1E24))
            ) {
                DropdownMenuItem(
                    text = { Text("تشغيل الأغنية", color = Color.White, fontFamily = fontFamily) },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("إضافة للتالي في القائمة", color = Color.White, fontFamily = fontFamily) },
                    leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onAddToNext()
                    }
                )
                val artistsList = splitArtists(song.artist)
                if (artistsList.size <= 1) {
                    val targetArtist = artistsList.firstOrNull() ?: song.artist
                    DropdownMenuItem(
                        text = { Text("عرض صفحة الفنان", color = Color.White, fontFamily = fontFamily) },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onViewArtist(targetArtist)
                        }
                    )
                } else {
                    artistsList.forEach { artistName ->
                        DropdownMenuItem(
                            text = { Text("صفحة الفنان: $artistName", color = Color.White, fontFamily = fontFamily) },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White) },
                            onClick = {
                                showMenu = false
                                onViewArtist(artistName)
                            }
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("معلومات الأغنية", color = Color.White, fontFamily = fontFamily) },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        showInfoDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("مشاركة الأغنية", color = Color.White, fontFamily = fontFamily) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        shareSongText(context, song)
                    }
                )
            }
        }
    }

    if (showInfoDialog) {
        val file = java.io.File(song.filePath)
        val sizeText = if (file.exists()) {
            String.format("%.2f MB", file.length().toFloat() / (1024 * 1024))
        } else {
            "غير معروف"
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("إغلاق", color = Color(0xFF1E88E5), fontFamily = fontFamily)
                }
            },
            title = {
                Text("معلومات الملف", color = Color.White, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("العنوان:", song.title, fontFamily)
                    InfoRow("الفنان:", song.artist, fontFamily)
                    InfoRow("الألبوم:", song.album, fontFamily)
                    InfoRow("المدة:", formatDuration(song.duration), fontFamily)
                    InfoRow("الحجم:", sizeText, fontFamily)
                    InfoRow("المسار:", song.filePath, fontFamily)
                }
            },
            containerColor = Color(0xFF16161B)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, fontFamily: FontFamily) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = fontFamily)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontFamily = fontFamily, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private fun shareSongText(context: Context, song: SongEntity) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "أستمع الآن إلى: ${song.title} للفنان ${song.artist} عبر تطبيق PureSonic!")
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الأغنية"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun splitArtists(artistString: String): List<String> {
    if (artistString.isBlank()) return emptyList()
    return artistString.split(Regex("[,،&]"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

