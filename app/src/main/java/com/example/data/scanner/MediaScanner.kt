package com.example.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

object MusicConfig {
    const val MUSIC_DIR = "/storage/emulated/0/Music"
}

class MediaScanner(private val context: Context) {
    
    suspend fun scanDevice(): List<SongEntity> = withContext(Dispatchers.IO) {
        val musicDir = File(MusicConfig.MUSIC_DIR)
        
        // Safety check
        if (!musicDir.exists() || !musicDir.isDirectory) {
            Log.e("SCANNER", "Music directory not found: ${MusicConfig.MUSIC_DIR}")
            return@withContext emptyList()
        }
        
        // Get all MP3 files ONLY from this directory (non-recursive)
        val mp3Files = musicDir.listFiles { file ->
            file.isFile && file.extension.lowercase() == "mp3"
        } ?: return@withContext emptyList()
        
        // Parallel batch scanning utilizing coroutines for major speed up!
        coroutineScope {
            mp3Files.toList()
                .chunked(15) // Process 15 files in parallel batches concurrently in pool
                .flatMap { batch ->
                    batch.map { file ->
                        async {
                            try { extractSongData(file) }
                            catch (e: Exception) { null }
                        }
                    }.awaitAll().filterNotNull()
                }
                .sortedBy { it.title }
        }
    }

    fun scanSingleFile(filePath: String): SongEntity? {
        return try {
            extractSongData(File(filePath))
        } catch (e: Exception) { null }
    }

    private fun extractSongData(file: File): SongEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            // Only add songs longer than 30 seconds
            if (duration < 30_000L) return null
            
            SongEntity(
                id = file.absolutePath.hashCode().toLong(),
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: file.nameWithoutExtension,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "فنان غير معروف",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?: "ألبوم غير معروف",
                duration = duration,
                filePath = file.absolutePath,
                albumArtUri = null // Will be loaded dynamically on-demand
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    companion object {
        fun getMockLyricsForSong(songId: Long): String {
            return when (songId) {
                10001L -> """
                    [00:00.00] (مقدمة موسيقية - نسم علينا الهوى)
                    [00:10.00] نسم علينا الهوى من مفرق الوادي
                    [00:20.00] يا هوى دخل الهوى خذني على بلادي
                    [00:30.00] نسم علينا الهوى من مفرق الوادي
                    [00:40.00] يا هوى دخل الهوى خذني على بلادي
                    [00:50.00] يا بتي طيار يا ورق يا رايح ع بلادي
                    [01:00.00] حبيبي غالي غاب ومن يوم ما غاب سهرني وبكاني
                    [01:15.00] نسم علينا الهوى من مفرق الوادي
                """.trimIndent()
                
                10002L -> """
                    [00:00.00] (روائع أم كلثوم - أنت عمري)
                    [00:15.00] رجعوني عينيك لأيامي اللي راحوا
                    [00:30.00] علموني اندم على الماضي وجراحه
                    [00:45.00] اللي شفته قبل ما تشوفك عينيا
                    [01:00.00] عمر ضايع يحسبوه إزاي عليا؟
                    [01:15.00] أنت عمري اللي ابتدى بنورك صباحه
                    [01:30.00] قد إيه من قبلك عمري ضاع من غير حب؟
                """.trimIndent()
                
                10003L -> """
                    [00:00.00] (العندليب - قارئة الفنجان)
                    [00:12.00] جلست.. والخوف في عينيها
                    [00:24.00] تتأمل فنجاني المقلوب
                    [00:36.00] قالت: يا ولدي.. لا تحزن
                    [00:48.00] فالحب عليك هو المكتوب
                    [01:00.00] قد مات شهيداً.. من يعشق
                    [01:12.00] من يسعى فوق جمر القلوب
                """.trimIndent()

                10004L -> """
                    [00:00.00] (عمرو دياب - حبيبي يا نور العين)
                    [00:10.00] حبيبي يا نور العين يا ساكن خيالي
                    [00:18.00] عاشق بقالي سنين ولا غيرك في بالي
                    [00:26.00] حبيبي حبيبي حبيبي يا نور العين
                    [00:34.00] أجمل عيون في الكون أنا شفتها عيونك
                    [00:42.00] الله عليك الله في قلبي ومصونك
                    [00:50.00] حبيبي يا نور العين يا ساكن خيالي
                """.trimIndent()

                10005L -> """
                    [00:00.00] (شيرين - على بالي)
                    [00:08.00] حبيبي على بالي ولا لحظة من بالي بتمشي
                    [00:18.00] وبفكر فيه.. وحاضناه في عيني ورمشي
                    [00:28.00] وعلى بالي.. حبيبي الغالي على بالي
                    [00:38.00] نسيت النوم وأيامي وجفوني ما بتنمشي
                    [00:48.00] حبيبي على بالي
                """.trimIndent()
                
                else -> ""
            }
        }
    }
}
