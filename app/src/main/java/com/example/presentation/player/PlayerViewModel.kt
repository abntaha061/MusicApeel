package com.example.presentation.player

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.data.db.SongEntity
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _lyrics = MutableStateFlow("")
    val lyrics: StateFlow<String> = _lyrics.asStateFlow()

    private val _dominantColors = MutableStateFlow<List<Color>>(getDefaultColors())
    val dominantColors: StateFlow<List<Color>> = _dominantColors.asStateFlow()

    fun updateSong(song: SongEntity?) {
        if (song == null) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("LRC_DEBUG", "Song path: ${song.filePath}")
                val lrcPath = com.example.data.lyrics.LrcParser.findLrcForSong(song.filePath)
                Log.d("LRC_DEBUG", "LRC found: $lrcPath")
                
                val lyricsText = if (lrcPath != null) {
                    val file = java.io.File(lrcPath)
                    try {
                        file.readText(Charsets.UTF_8)
                    } catch (e: Exception) {
                        try {
                            file.readText(java.nio.charset.Charset.forName("windows-1256"))
                        } catch (e2: Exception) {
                            ""
                        }
                    }
                } else {
                    com.example.data.scanner.MediaScanner.getMockLyricsForSong(song.id)
                }
                
                Log.d("LRC_DEBUG", "Lrc content loaded length: ${lyricsText.length}")
                _lyrics.value = lyricsText
            } catch (e: Exception) {
                e.printStackTrace()
                _lyrics.value = ""
            }

            // 2. Extract dynamic palette from album art or fall back to artist-tailored vibes
            val bitmap = com.example.data.utils.AlbumArtLoader.loadBitmap(song.id, song.filePath)
            var extractedColors: List<Color>? = null
            
            if (bitmap != null) {
                try {
                    val palette = Palette.from(bitmap).generate()
                    val list = mutableListOf<Color>()
                    palette.vibrantSwatch?.let { list.add(Color(it.rgb)) }
                    palette.darkVibrantSwatch?.let { list.add(Color(it.rgb)) }
                    palette.mutedSwatch?.let { list.add(Color(it.rgb)) }
                    palette.darkMutedSwatch?.let { list.add(Color(it.rgb)) }
                    palette.lightVibrantSwatch?.let { list.add(Color(it.rgb)) }
                    
                    if (list.size >= 2) {
                        extractedColors = list
                    }
                } catch (e: Exception) {
                    Log.e("PALETTE", "Error extracting colors", e)
                }
            }
            
            _dominantColors.value = extractedColors ?: getArtistPalette(song.artist)
        }
    }

    private fun getArtistPalette(artist: String): List<Color> {
        return when (artist) {
            "فيروز" -> listOf(
                Color(0xFF0F2027), // Midnight sea blue
                Color(0xFF203A43), // Turquoise Teal
                Color(0xFF2C5364)  // Sea foam slate
            )
            "أم كلثوم" -> listOf(
                Color(0xFF4A0E17), // Deep burgundy
                Color(0xFF8B0000), // Royal ruby
                Color(0xFFD4AF37)  // Egyptian gold
            )
            "عبد الحليم" -> listOf(
                Color(0xFF2E0854), // Nocturnal violet
                Color(0xFF4B0082), // Dreamy indigo
                Color(0xFF1F4068)  // Nostalgic steel blue
            )
            "عمرو دياب" -> listOf(
                Color(0xFFE65100), // Sunset orange
                Color(0xFFBF360C), // Red ochre
                Color(0xFF0D47A1)  // Electric ocean cobalt
            )
            "شيرين" -> listOf(
                Color(0xFF7B1FA2), // Rose purple
                Color(0xFFC2185B), // Deep cotton candy pink
                Color(0xFF311B92)  // Celestial navy blue
            )
            else -> getDefaultColors()
        }
    }

    private fun getDefaultColors(): List<Color> {
        return listOf(
            Color(0xFF0F172A), // Dark slate
            Color(0xFF1E293B), // Navy blue gray
            Color(0xFF334155)  // Cool metal
        )
    }
}
