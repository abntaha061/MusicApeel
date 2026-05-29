package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun AlbumArtImage(
    songId: Long,
    filePath: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val model = remember(filePath) {
        ImageRequest.Builder(context)
            .data(File(filePath))
            .size(120, 120) // 120x120 is perfect downscaled thumbnail size
            .memoryCacheKey(filePath)
            .diskCacheKey(filePath)
            .crossfade(150)
            .build()
    }

    var isError by remember(filePath) { mutableStateOf(false) }
    var isLoading by remember(filePath) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(iconSize)
                )
            }
        } else {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    isLoading = state is coil.compose.AsyncImagePainter.State.Loading
                    isError = state is coil.compose.AsyncImagePainter.State.Error
                }
            )
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }
    }
}
