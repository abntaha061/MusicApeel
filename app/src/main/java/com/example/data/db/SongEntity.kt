package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String? = null,
    
    // Smart stats
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "last_played_timestamp") val lastPlayedTimestamp: Long = 0L,
    @ColumnInfo(name = "date_added", defaultValue = "0") val dateAdded: Long = 0L
)
