package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ArtistStats(
    val artist: String,
    @ColumnInfo(name = "total_plays") val totalPlays: Int
)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE play_count > 0 ORDER BY play_count DESC LIMIT 20")
    fun getMostPlayed(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE last_played_timestamp > 0 ORDER BY last_played_timestamp DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<SongEntity>>
    
    @Query("""
        SELECT artist, SUM(play_count) as total_plays 
        FROM songs 
        GROUP BY artist 
        ORDER BY total_plays DESC 
        LIMIT 6
    """)
    fun getTopArtists(): Flow<List<ArtistStats>>
    
    @Query("""
        UPDATE songs 
        SET play_count = play_count + 1, 
            last_played_timestamp = :timestamp 
        WHERE id = :songId
    """)
    suspend fun incrementPlayCount(songId: Long, timestamp: Long)
    
    @Query("DELETE FROM songs WHERE file_path NOT IN (:existingPaths)")
    suspend fun removeDeletedSongs(existingPaths: List<String>)
    
    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT file_path FROM songs")
    suspend fun getAllPaths(): List<String>

    @Query("DELETE FROM songs WHERE file_path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)
}
