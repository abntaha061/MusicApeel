package com.example.data.db

data class LibraryStats(
    val totalSongs: Int,
    val totalDurationMs: Long,
    val totalArtists: Int,
    val totalAlbums: Int,
    val totalListeningTimeMs: Long
)
