package com.example.data.lyrics

import java.io.File
import android.util.Log

data class LrcLine(
    val timestamp: Long, // timestamp in ms
    val text: String,
    val translation: String? = null
)

object LrcParser {
    fun parseLrcFile(content: String): List<LrcLine> {
        if (content.isBlank()) return emptyList()
        // MATCH: [00:12.34] or [00:12.345] or [00:12:34] content
        val regex = Regex("""\[(\d{1,2}):(\d{2})[.:](\d{2,3})\](.*)""")
        return content.lines()
            .mapNotNull { line ->
                try {
                    regex.find(line.trim())?.let { match ->
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toLong()
                        val fractionStr = match.groupValues[3]
                        val fraction = fractionStr.toLong()
                        val text = match.groupValues[4].trim()
                        
                        // If the fraction is 2 digits (e.g. .34), it stands for centiseconds (multiply by 10)
                        // If 3 digits (e.g. .345), it's milliseconds
                        val msOffset = if (fractionStr.length == 2) fraction * 10 else fraction
                        val timestamp = (min * 60 + sec) * 1000 + msOffset
                        
                        // Remove translation support entirely to make lyrics occupy full space
                        if (text.isNotEmpty()) LrcLine(timestamp, text, null) else null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            .sortedBy { it.timestamp }
    }

    fun getCurrentLineIndex(lyrics: List<LrcLine>, currentPositionMs: Long): Int {
        var index = 0
        for (i in lyrics.indices) {
            if (lyrics[i].timestamp <= currentPositionMs) {
                index = i
            } else {
                break
            }
        }
        return index
    }

    fun findLrcForSong(songFilePath: String): String? {
        try {
            val songFile = File(songFilePath)
            val songDir = songFile.parentFile ?: return null
            if (!songDir.exists() || !songDir.isDirectory) return null
            val songNameNoExt = songFile.nameWithoutExtension.trim()

            // Strategy 1: Exact match (same name, .lrc / .LRC extension)
            val exactMatch = File(songDir, "$songNameNoExt.lrc")
            if (exactMatch.exists()) return exactMatch.absolutePath
            val exactMatchUpper = File(songDir, "$songNameNoExt.LRC")
            if (exactMatchUpper.exists()) return exactMatchUpper.absolutePath

            // Strategy 2: Case-insensitive match
            val caseInsensitive = songDir.listFiles()?.firstOrNull { file ->
                file.isFile &&
                file.extension.lowercase() == "lrc" &&
                file.nameWithoutExtension.trim().lowercase() == songNameNoExt.lowercase()
            }
            if (caseInsensitive != null) return caseInsensitive.absolutePath

            // Strategy 3: Normalized match (remove special chars, spaces)
            fun normalize(s: String) = s.lowercase()
                .replace(Regex("[^a-z0-9\\u0600-\\u06FF]"), "") // keep Arabic + alphanumeric
            
            val normalized = songDir.listFiles()?.firstOrNull { file ->
                file.isFile &&
                file.extension.lowercase() == "lrc" &&
                normalize(file.nameWithoutExtension) == normalize(songNameNoExt)
            }
            return normalized?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
