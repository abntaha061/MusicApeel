package com.example.data.lyrics

import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    fun parse(file: File): List<LyricLine> {
        if (!file.exists()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        try {
            file.forEachLine { line ->
                val match = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2})](.*)").find(line)
                if (match != null) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].toLong() * 10
                    val text = match.groupValues[4].trim()
                    val totalMs = min * 60000 + sec * 1000 + ms
                    lines.add(LyricLine(totalMs, text))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lines.sortedBy { it.timeMs }
    }

    fun cleanTextForSearch(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF]"), "") // keep Arabic + alphanumeric
    }
}
