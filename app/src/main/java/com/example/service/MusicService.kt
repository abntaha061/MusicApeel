package com.example.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.example.data.db.SongDao
import com.example.data.db.SongDatabase
import com.example.data.db.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MusicService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "tarab_music_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
    }

    private val binder = MusicBinder()
    
    lateinit var player: ExoPlayer
        private set

    private lateinit var songDao: SongDao
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // MediaSession fields
    private var mediaSession: MediaSession? = null

    // Tracking notification caching
    private var lastLoadedSongId: Long = -1L
    private var cachedAlbumArt: android.graphics.Bitmap? = null

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

    // Broadcast receiver for status bar controls
    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                ACTION_PLAY -> {
                    if (!player.isPlaying) {
                        togglePlayPause()
                    }
                }
                ACTION_PAUSE -> {
                    if (player.isPlaying) {
                        togglePlayPause()
                    }
                }
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrevious()
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Database and Player
        songDao = SongDatabase.getDatabase(this).songDao()
        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
        
        // Initialize Media3 MediaSession
        mediaSession = MediaSession.Builder(this, player).build()

        createNotificationChannel()

        // Register Broadcast Receiver for Notification commands
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }

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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                val list = _playlist.value
                if (index in list.indices) {
                    val nextSong = list[index]
                    if (_currentSong.value?.id != nextSong.id) {
                        checkAndIncrementPlayCount()
                        _currentSong.value = nextSong
                        songStartTime = System.currentTimeMillis()
                        currentSongId = nextSong.id
                        currentSongDuration = nextSong.duration
                    }
                }
            }
        })

        // Core dynamic observer to update system media notification controls reactively
        serviceScope.launch {
            combine(_currentSong, _isPlaying) { song, playing ->
                song to playing
            }.collect { (song, playing) ->
                if (song != null) {
                    updateNotification(song, playing)
                }
            }
        }

        // Live persistence of the last played song to SharedPreferences
        serviceScope.launch {
            _currentSong.collect { song ->
                if (song != null) {
                    val prefs = getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong("last_song_id", song.id)
                        .putString("last_song_path", song.filePath)
                        .apply()
                }
            }
        }

        // Restore last played song from DB on startup
        serviceScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
            val lastSongId = prefs.getLong("last_song_id", -1L)
            val lastSongPath = prefs.getString("last_song_path", null)
            if (lastSongId != -1L && lastSongPath != null) {
                val lastSong = songDao.getSongById(lastSongId)
                if (lastSong != null) {
                    withContext(Dispatchers.Main) {
                        if (_currentSong.value == null) {
                            _currentSong.value = lastSong
                        }
                    }
                }
            }
        }
    }

    // Modern multi-role binding support for Media3 controllers and internal app connections
    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == "androidx.media3.session.MediaSessionService") {
            return super.onBind(intent)
        }
        return binder
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createMediaItem(song: SongEntity, mediaUri: String): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun playSongList(songs: List<SongEntity>, startIndex: Int) {
        if (songs.isEmpty()) return
        
        try {
            // Check list change, increment prior listening stats
            checkAndIncrementPlayCount()
            _playlist.value = songs

            serviceScope.launch {
                try {
                    // Background processing of Uri and MediaItem mapping
                    val mediaItems = withContext(Dispatchers.IO) {
                        songs.map { song ->
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
                            createMediaItem(song, mediaUri)
                        }
                    }

                    // Apply on Main thread in a single bulk operation
                    player.clearMediaItems()
                    player.setMediaItems(mediaItems)

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
                } catch (coroutineEx: Exception) {
                    android.util.Log.e("MusicService", "Error in playSongList coroutine", coroutineEx)
                }
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
                if (player.mediaItemCount == 0) {
                    val current = _currentSong.value
                    if (current != null) {
                        playSongList(listOf(current), 0)
                        return
                    }
                }
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

    fun addToNext(song: SongEntity) {
        try {
            val list = _playlist.value.toMutableList()
            val current = _currentSong.value

            if (list.isEmpty() || current == null) {
                playSongList(listOf(song), 0)
                return
            }

            val currentIndex = list.indexOfFirst { it.id == current.id }
            if (currentIndex != -1) {
                list.add(currentIndex + 1, song)
                _playlist.value = list

                serviceScope.launch {
                    val mediaUri = withContext(Dispatchers.IO) {
                        if (song.filePath.startsWith("assets:///")) {
                            UriUtil.getAssetUri(song.filePath)
                        } else {
                            val file = java.io.File(song.filePath)
                            if (file.exists()) {
                                android.net.Uri.fromFile(file).toString()
                            } else {
                                song.filePath
                            }
                        }
                    }
                    val mediaItem = createMediaItem(song, mediaUri)
                    withContext(Dispatchers.Main) {
                        player.addMediaItem(currentIndex + 1, mediaItem)
                    }
                }
            } else {
                list.add(song)
                _playlist.value = list

                serviceScope.launch {
                    val mediaUri = withContext(Dispatchers.IO) {
                        if (song.filePath.startsWith("assets:///")) {
                            UriUtil.getAssetUri(song.filePath)
                        } else {
                            val file = java.io.File(song.filePath)
                            if (file.exists()) {
                                android.net.Uri.fromFile(file).toString()
                            } else {
                                song.filePath
                            }
                        }
                    }
                    val mediaItem = createMediaItem(song, mediaUri)
                    withContext(Dispatchers.Main) {
                        player.addMediaItem(mediaItem)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sleep Timer Support
    private var sleepTimerJob: Job? = null
    private val _sleepTimeRemaining = MutableStateFlow(0L) // Remaining time in ms
    val sleepTimeRemaining: StateFlow<Long> = _sleepTimeRemaining.asStateFlow()

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemaining.value = 0L
            return
        }
        val durationMs = minutes * 60 * 1000L
        val startTime = System.currentTimeMillis()
        _sleepTimeRemaining.value = durationMs
        
        sleepTimerJob = serviceScope.launch {
            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val remaining = durationMs - elapsedTime
                if (remaining <= 0) {
                    _sleepTimeRemaining.value = 0L
                    if (player.isPlaying) {
                        togglePlayPause()
                    }
                    break
                }
                _sleepTimeRemaining.value = remaining
                delay(1000L) // Tick every second
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = 0L
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "طرب — مشغل الموسيقى",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم في تشغيل الموسيقى"
                setShowBadge(false)
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(song: SongEntity, isPlaying: Boolean) {
        val session = mediaSession ?: return
        serviceScope.launch(Dispatchers.Main) {
            val artwork = withContext(Dispatchers.IO) {
                if (lastLoadedSongId == song.id) {
                    cachedAlbumArt
                } else {
                    lastLoadedSongId = song.id
                    cachedAlbumArt = loadAlbumArt(song.filePath)
                    cachedAlbumArt
                }
            }
            if (mediaSession != null) {
                showNotification(song, isPlaying, artwork)
            }
        }
    }

    private fun showNotification(song: SongEntity, isPlaying: Boolean, artwork: android.graphics.Bitmap?) {
        val session = mediaSession ?: return
        val openAppIntent = android.app.PendingIntent.getActivity(
            this, 0,
            android.content.Intent(this, com.example.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val pkg = packageName
        // build actions PendingIntents
        val prevIntent = android.app.PendingIntent.getBroadcast(
            this, ACTION_PREVIOUS.hashCode(),
            android.content.Intent(ACTION_PREVIOUS).apply { setPackage(pkg) },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = if (isPlaying) {
            android.app.PendingIntent.getBroadcast(
                this, ACTION_PAUSE.hashCode(),
                android.content.Intent(ACTION_PAUSE).apply { setPackage(pkg) },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getBroadcast(
                this, ACTION_PLAY.hashCode(),
                android.content.Intent(ACTION_PLAY).apply { setPackage(pkg) },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }
        val nextIntent = android.app.PendingIntent.getBroadcast(
            this, ACTION_NEXT.hashCode(),
            android.content.Intent(ACTION_NEXT).apply { setPackage(pkg) },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = androidx.core.app.NotificationCompat.Action(
            com.example.R.drawable.ic_skip_previous, "السابق", prevIntent
        )
        val playPauseAction = androidx.core.app.NotificationCompat.Action(
            if (isPlaying) com.example.R.drawable.ic_pause else com.example.R.drawable.ic_play,
            if (isPlaying) "إيقاف مؤقت" else "تشغيل",
            playPauseIntent
        )
        val nextAction = androidx.core.app.NotificationCompat.Action(
            com.example.R.drawable.ic_skip_next, "التالي", nextIntent
        )

        val defaultArt = android.graphics.BitmapFactory.decodeResource(
            resources,
            com.example.R.drawable.ic_music_note
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText("طرب")
            .setSmallIcon(com.example.R.drawable.ic_music_note)
            .setLargeIcon(artwork ?: defaultArt)
            .setContentIntent(openAppIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun loadAlbumArt(filePath: String): android.graphics.Bitmap? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes?.let { 
                android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) 
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        checkAndIncrementPlayCount()
        positionTrackerJob?.cancel()
        
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mediaSession?.run {
            release()
            mediaSession = null
        }
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
