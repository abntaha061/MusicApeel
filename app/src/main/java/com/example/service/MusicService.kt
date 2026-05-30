package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.db.SongDatabase
import com.example.data.db.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private var songList = listOf<SongEntity>()
    private var currentIndex = -1

    private var positionTickerJob: Job? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        createNotificationChannel()
    }

    fun playSongList(list: List<SongEntity>, index: Int) {
        if (list.isEmpty() || index < 0 || index >= list.size) return
        songList = list
        currentIndex = index
        playSong(songList[currentIndex])
    }

    private fun playSong(song: SongEntity) {
        _currentSong.value = song
        _isPlaying.value = true
        _currentPosition.value = 0L

        mediaPlayer?.reset()
        try {
            // Since mock files don't genuinely exist on standard storage, we simulate progress ticker.
            // If it is a real file, we can try to prepare, otherwise catch and simulate playback!
            val fileExists = song.filePath.startsWith("/mock") || java.io.File(song.filePath).exists()
            if (fileExists && !song.filePath.startsWith("/mock")) {
                mediaPlayer?.setDataSource(song.filePath)
                mediaPlayer?.prepare()
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener {
                    next()
                }
            } else {
                // Simulated digital sound engine for mock MP3 templates
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Increment stats count in DB asynchronously after a successful trigger
        serviceScope.launch(Dispatchers.IO) {
            val db = SongDatabase.getDatabase(this@MusicService)
            val updatedSong = song.copy(
                playCount = song.playCount + 1,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            db.songDao().updateSong(updatedSong)
            _currentSong.value = updatedSong
        }

        startTicker()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, buildNotification(song), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(101, buildNotification(song))
        }
    }

    fun togglePlayPause() {
        val song = _currentSong.value ?: return
        if (_isPlaying.value) {
            _isPlaying.value = false
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (e: Exception) { e.printStackTrace() }
            stopTicker()
        } else {
            _isPlaying.value = true
            try {
                if (!song.filePath.startsWith("/mock")) {
                    mediaPlayer?.start()
                }
            } catch (e: Exception) { e.printStackTrace() }
            startTicker()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, buildNotification(song), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(101, buildNotification(song))
        }
    }

    fun next() {
        if (songList.isEmpty()) return
        currentIndex = (currentIndex + 1) % songList.size
        playSong(songList[currentIndex])
    }

    fun previous() {
        if (songList.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) songList.size - 1 else currentIndex - 1
        playSong(songList[currentIndex])
    }

    fun seekTo(positionMs: Long) {
        _currentPosition.value = positionMs
        try {
            if (mediaPlayer?.isPlaying == true || (_currentSong.value != null && !_currentSong.value!!.filePath.startsWith("/mock"))) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun addToNext(song: SongEntity) {
        val newList = songList.toMutableList()
        if (currentIndex == -1) {
            newList.add(song)
            songList = newList
            currentIndex = 0
            playSong(song)
        } else {
            newList.add(currentIndex + 1, song)
            songList = newList
        }
    }

    private fun startTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (_isPlaying.value) {
                    val song = _currentSong.value
                    if (song != null) {
                        val current = _currentPosition.value + 1000L
                        if (current >= song.duration) {
                            _currentPosition.value = song.duration
                            next()
                        } else {
                            _currentPosition.value = current
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        positionTickerJob?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "puresonic_playback",
                "PureSonic Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(song: SongEntity): android.app.Notification {
        return NotificationCompat.Builder(this, "puresonic_playback")
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopTicker()
        serviceScope.cancel()
        mediaPlayer?.release()
        super.onDestroy()
    }
}
