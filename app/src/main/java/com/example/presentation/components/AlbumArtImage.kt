package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AlbumArtImage(
    songId: Long,
    filePath: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 24.dp
) {
    val index = (songId % 7).toInt()
    val gradients = listOf(
        listOf(Color(0xFF3F51B5), Color(0xFF1A237E)),
        listOf(Color(0xFFE91E63), Color(0xFF880E4F)),
        listOf(Color(0xFF009688), Color(0xFF004D40)),
        listOf(Color(0xFFFF9800), Color(0xFFE65100)),
        listOf(Color(0xFF9C27B0), Color(0xFF4A148C)),
        listOf(Color(0xFF03A9F4), Color(0xFF0D47A1)),
        listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
    )
    val colors = gradients[index]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "ملصق الأغنية",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
    }
}
