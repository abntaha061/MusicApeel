package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ArtistStats(
    val artist: String,
    @ColumnInfo(name = "total_plays") val totalPlays: Int
)

data class ArtistWithArt(
    val name: String,
    val songCount: Int,
    @ColumnInfo(name = "sampleFilePath") val sampleFilePath: String
)

data class LibraryStats(
    val totalSongs: Int,
    val totalDurationMs: Long,
    val totalArtists: Int,
    val totalAlbums: Int,
    val totalListeningTimeMs: Long
)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("""
        SELECT artist as name, COUNT(*) as songCount, MIN(file_path) as sampleFilePath
        FROM songs
        GROUP BY artist
        ORDER BY RANDOM()
        LIMIT 10
    """)
    suspend fun getAllArtistsWithSongs(): List<ArtistWithArt>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getTotalSongs(): Int

    @Query("SELECT COALESCE(SUM(duration), 0) FROM songs")
    suspend fun getTotalDuration(): Long

    @Query("SELECT COUNT(DISTINCT artist) FROM songs")
    suspend fun getTotalArtists(): Int

    @Query("SELECT COUNT(DISTINCT album) FROM songs")
    suspend fun getTotalAlbums(): Int

    @Query("SELECT COALESCE(SUM(play_count * duration), 0) FROM songs")
    suspend fun getTotalListeningTime(): Long

    @Query("SELECT * FROM songs ORDER BY play_count DESC LIMIT 1")
    fun getMostPlayedSong(): Flow<SongEntity?>
    
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

    @Query("SELECT * FROM songs")
    suspend fun getSongList(): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT file_path FROM songs")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT * FROM songs WHERE file_path = :path LIMIT 1")
    suspend fun getSongByPath(path: String): SongEntity?

    @Query("DELETE FROM songs WHERE file_path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()
}
