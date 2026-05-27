package com.example.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.db.SongDao
import com.example.data.db.SongDatabase
import com.example.data.db.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MusicService : Service() {

    private val binder = MusicBinder()
    
    lateinit var player: ExoPlayer
        private set

    private lateinit var songDao: SongDao
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Currently playing playlist queue
    private val _playlist = MutableStateFlow<List<SongEntity>>(emptyList())
    val playlist: StateFlow<List<SongEntity>> = _playlist.asStateFlow()

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    // Listening Track Stats Logic
    private var songStartTime: Long = 0L
    private var currentSongId: Long = -1L
    private var currentSongDuration: Long = 0L
    private val MINIMUM_LISTEN_SECONDS = 30L

    private var positionTrackerJob: Job? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Database and Player
        songDao = SongDatabase.getDatabase(this).songDao()
        player = ExoPlayer.Builder(this).build()
        
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _isPlaying.value = player.isPlaying
                    startPositionTracker()
                } else if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionTracker()
                } else {
                    positionTrackerJob?.cancel()
                }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun playSongList(songs: List<SongEntity>, startIndex: Int) {
        if (songs.isEmpty()) return
        
        try {
            // Check list change, increment prior listening stats
            checkAndIncrementPlayCount()

            _playlist.value = songs
            
            player.clearMediaItems()
            songs.forEach { song ->
                val mediaUri = if (song.filePath.startsWith("assets:///")) {
                    UriUtil.getAssetUri(song.filePath)
                } else {
                    val file = java.io.File(song.filePath)
                    if (file.exists()) {
                        android.net.Uri.fromFile(file).toString()
                    } else {
                        song.filePath
                    }
                }
                player.addMediaItem(MediaItem.fromUri(mediaUri))
            }

            if (startIndex in songs.indices) {
                player.seekTo(startIndex, 0L)
                player.prepare()
                player.play()

                val selectedSong = songs[startIndex]
                _currentSong.value = selectedSong
                
                // Track listen stats initiation
                songStartTime = System.currentTimeMillis()
                currentSongId = selectedSong.id
                currentSongDuration = selectedSong.duration
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNext() {
        try {
            val list = _playlist.value
            val current = _currentSong.value
            if (list.isEmpty() || current == null) return

            val currentIndex = list.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex < list.size - 1) {
                // Check list change, increment prior stats
                checkAndIncrementPlayCount()
                
                val nextIndex = currentIndex + 1
                player.seekTo(nextIndex, 0L)
                
                val nextSong = list[nextIndex]
                _currentSong.value = nextSong
                
                songStartTime = System.currentTimeMillis()
                currentSongId = nextSong.id
                currentSongDuration = nextSong.duration
            } else if (list.isNotEmpty()) {
                // Loop back to start
                checkAndIncrementPlayCount()
                player.seekTo(0, 0L)
                val firstSong = list[0]
                _currentSong.value = firstSong
                songStartTime = System.currentTimeMillis()
                currentSongId = firstSong.id
                currentSongDuration = firstSong.duration
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPrevious() {
        try {
            val list = _playlist.value
            val current = _currentSong.value
            if (list.isEmpty() || current == null) return

            val currentIndex = list.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex > 0) {
                checkAndIncrementPlayCount()
                val prevIndex = currentIndex - 1
                player.seekTo(prevIndex, 0L)
                
                val prevSong = list[prevIndex]
                _currentSong.value = prevSong
                
                songStartTime = System.currentTimeMillis()
                currentSongId = prevSong.id
                currentSongDuration = prevSong.duration
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePlaybackEnded() {
        checkAndIncrementPlayCount()
        playNext()
    }

    private fun checkAndIncrementPlayCount() {
        if (currentSongId == -1L || songStartTime == 0L) return
        
        val listenedMs = System.currentTimeMillis() - songStartTime
        val listenedSeconds = listenedMs / 1000
        val songDurationSeconds = currentSongDuration / 1000
        val threshold20Percent = songDurationSeconds * 0.20

        // Only count if listened for >=30 seconds OR >=20% of song
        val qualifies = listenedSeconds >= MINIMUM_LISTEN_SECONDS ||
                (songDurationSeconds > 0 && listenedSeconds >= threshold20Percent)

        if (qualifies) {
            val songIdToIncrement = currentSongId
            serviceScope.launch(Dispatchers.IO) {
                songDao.incrementPlayCount(songIdToIncrement, System.currentTimeMillis())
            }
        }

        // Reset
        songStartTime = 0L
        currentSongId = -1L
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = serviceScope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                delay(250L) // update progress high frequency for lyrics smooth scrolling
            }
        }
    }

    override fun onDestroy() {
        checkAndIncrementPlayCount()
        positionTrackerJob?.cancel()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}

// Utility to translate custom assets:/// path into Android local Asset Uri
object UriUtil {
    fun getAssetUri(assetPath: String): String {
        val cleanPath = assetPath.removePrefix("assets:///").removePrefix("assets/")
        return "asset:///$cleanPath"
    }
}
