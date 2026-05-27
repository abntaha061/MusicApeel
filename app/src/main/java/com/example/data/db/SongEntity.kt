package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,           // MediaStore._ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,                 // milliseconds
    @ColumnInfo(name = "file_path") val filePath: String,             // Absolute path
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String?,         // content:// URI
    
    // Smart stats
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "last_played_timestamp") val lastPlayedTimestamp: Long = 0L // System.currentTimeMillis()
)
