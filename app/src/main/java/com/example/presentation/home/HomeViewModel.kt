package com.example.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ArtistStats
import com.example.data.db.ArtistWithArt
import com.example.data.db.LibraryStats
import com.example.data.db.SongDao
import com.example.data.db.SongDatabase
import com.example.data.db.SongEntity
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao: SongDao = SongDatabase.getDatabase(application).songDao()
    private val mediaScanner = MediaScanner(application)

    private val isSyncInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val allSongs: StateFlow<List<SongEntity>> = songDao.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentlyPlayed: StateFlow<List<SongEntity>> = songDao.getRecentlyPlayed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mostPlayed: StateFlow<List<SongEntity>> = songDao.getMostPlayed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val topArtists: StateFlow<List<ArtistStats>> = songDao.getTopArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val libraryStats: StateFlow<LibraryStats> = allSongs.map {
        LibraryStats(
            totalSongs = songDao.getTotalSongs(),
            totalDurationMs = songDao.getTotalDuration(),
            totalArtists = songDao.getTotalArtists(),
            totalAlbums = songDao.getTotalAlbums()
        )
    }.flowOn(Dispatchers.IO)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats(0, 0, 0, 0))

    val artistsForYou: StateFlow<List<ArtistWithArt>> = allSongs.map {
        val allArtists = songDao.getAllArtistsWithSongs()
        allArtists.shuffled().take(5)
    }.flowOn(Dispatchers.IO)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSong: StateFlow<SongEntity?> = songDao.getMostPlayedSong()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var hasCheckedDatabase = false
    private val observers = mutableListOf<com.example.data.scanner.MusicFileObserver>()

    fun startWatchingMusicFolder() {
        if (observers.isNotEmpty()) return // Already watching
        listOf(
            "/storage/emulated/0/Music",
            "/sdcard/Music"
        ).forEach { path ->
            try {
                val dir = java.io.File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists() && dir.isDirectory) {
                    val observer = com.example.data.scanner.MusicFileObserver(path) { newFilePath ->
                        viewModelScope.launch(Dispatchers.IO) {
                            // Delay safely in the background thread to let file finish writing
                            kotlinx.coroutines.delay(2000)
                            addSingleSong(newFilePath)
                        }
                    }
                    observer.startWatching()
                    observers.add(observer)
                    android.util.Log.d("SCANNER", "Started watching $path")
                } else {
                    android.util.Log.w("SCANNER", "Path $path does not exist and could not be created")
                }
            } catch (e: Exception) {
                android.util.Log.e("SCANNER", "Failed to watch $path", e)
            }
        }
    }

    fun stopWatchingMusicFolder() {
        observers.forEach { it.stopWatching() }
        observers.clear()
        android.util.Log.d("SCANNER", "Stopped watching folder observers")
    }

    private suspend fun addSingleSong(filePath: String) {
        if (!filePath.endsWith(".mp3", ignoreCase = true)) return
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) return

            // Check if already exists in DB
            val exists = songDao.getSongByPath(filePath) != null
            if (exists) return

            val singleSong = mediaScanner.scanSingleFile(filePath)
            if (singleSong != null) {
                songDao.upsertSongs(listOf(singleSong))
                android.util.Log.d("SCANNER", "Successfully added auto-detected song: ${singleSong.title}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SCANNER", "Error adding auto-detected song: $filePath", e)
        }
    }

    fun checkAndStartLibrarySync() {
        if (hasCheckedDatabase) return
        hasCheckedDatabase = true
        syncLibrary(force = false)
        startWatchingMusicFolder()
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchingMusicFolder()
    }

    fun syncLibrary(force: Boolean = false) {
        if (isSyncInProgress.getAndSet(true)) {
            return // Already syncing. Skip duplicate scan to prevent CPU spikes and freezing.
        }
        _isSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbCount = songDao.getSongCount()
                val isFirstScanDone = getApplication<Application>()
                    .getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("first_scan_done", false)

                // Scan ONLY on:
                // 1. Initial installation when DB is empty OR first scan flag is false
                // 2. User manually triggers refresh (force == true)
                if (dbCount == 0 || !isFirstScanDone || force) {
                    val scanned = mediaScanner.scanDevice()
                    
                    // Retrieve existing library to preserve user stats (play counts & timestamps)
                    val existingSongs = songDao.getSongList()
                    val existingMap = existingSongs.associateBy { it.id }
                    
                    val mergedSongs = scanned.map { scannedSong ->
                        val existing = existingMap[scannedSong.id]
                        if (existing != null) {
                            scannedSong.copy(
                                playCount = existing.playCount,
                                lastPlayedTimestamp = existing.lastPlayedTimestamp
                            )
                        } else {
                            scannedSong
                        }
                    }
                    
                    val paths = mergedSongs.map { it.filePath }
                    songDao.removeDeletedSongs(paths)
                    songDao.upsertSongs(mergedSongs)
                    
                    // Mark first scan as successfully completed
                    getApplication<Application>()
                        .getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("first_scan_done", true)
                        .putLong("last_scan_time", System.currentTimeMillis())
                        .apply()
                } else {
                    // Under the "One-time Scan" architecture, automatic/incremental scans are completely skipped
                    // to prevent CPU hogging, heating, and lag. No disk I/O operations are made!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
                isSyncInProgress.set(false)
            }
        }
    }

    private fun saveLastScanTime(time: Long) {
        getApplication<Application>().getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_scan_time", time)
            .apply()
    }

    private fun getLastScanTime(): Long {
        return getApplication<Application>().getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)
            .getLong("last_scan_time", 0L)
    }
}
