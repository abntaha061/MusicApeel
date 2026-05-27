package com.example.data.lyrics

import java.io.File

data class LrcLine(
    val timestamp: Long, // timestamp in ms
    val text: String,
    val translation: String? = null // Optional translation for the translation button 🌐
)

object LrcParser {
    fun parseLrcFile(content: String): List<LrcLine> {
        // MATCH: [00:12.34] or [00:12.345] or [00:12:34] content
        val regex = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})\](.*)""")
        return content.lines()
            .mapNotNull { line ->
                regex.find(line)?.let { match ->
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val fraction = match.groupValues[3].toLong()
                    val text = match.groupValues[4].trim()
                    
                    // If the fraction is 2 digits (e.g. .34), it stands for centiseconds (multiply by 10)
                    // If 3 digits (e.g. .345), it's milliseconds
                    val msOffset = if (match.groupValues[3].length == 2) fraction * 10 else fraction
                    val timestamp = (min * 60 + sec) * 1000 + msOffset
                    
                    // Simple logic to mock translation or separate with a specialized delimiter if existing
                    val actualText: String
                    val translationText: String?
                    if (text.contains(" | ")) {
                        val parts = text.split(" | ", limit = 2)
                        actualText = parts[0]
                        translationText = parts[1]
                    } else {
                        actualText = text
                        translationText = mockTranslationOf(text)
                    }
                    
                    if (actualText.isNotEmpty()) LrcLine(timestamp, actualText, translationText)
                    else null
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

    fun findLrcFile(songPath: String): File? {
        val base = File(songPath).nameWithoutExtension
        val dir = File(songPath).parentFile ?: return null
        return listOf("$base.lrc", "$base.LRC").firstNotNullOfOrNull {
            File(dir, it).takeIf { f -> f.exists() }
        }
    }

    // Helper to generate some high-quality translations for demonstration if not parsed
    private fun mockTranslationOf(arabicText: String): String {
        return when {
            arabicText.contains("حبيبي") -> "My love (Habibi)"
            arabicText.contains("يا غالي") -> "Oh precious one"
            arabicText.contains("ليلي") || arabicText.contains("الليل") -> "My night"
            arabicText.contains("عيون") || arabicText.contains("عيني") -> "My eyes"
            arabicText.contains("قلبي") || arabicText.contains("القلب") -> "My heart"
            arabicText.contains("روح") || arabicText.contains("روحي") -> "My soul"
            arabicText.contains("شوق") -> "Yearning / Longing"
            arabicText.contains("طرب") -> "Rapture / Musical ecstasy"
            else -> ""
        }
    }
}
