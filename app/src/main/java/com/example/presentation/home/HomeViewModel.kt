package com.example.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ArtistStats
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

    init {
        syncLibrary()
    }

    fun syncLibrary(force: Boolean = false) {
        if (isSyncInProgress.getAndSet(true)) {
            return // Already syncing. Skip duplicate scan to prevent CPU spikes and freezing.
        }
        _isSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbCount = songDao.getSongCount()
                if (dbCount == 0 || force) {
                    // Full sync on first-run or on a manual refresh/force
                    val scanned = mediaScanner.scanDevice()
                    
                    // Retrieve existing library to preserve user interaction stats (play counts & timestamps)
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
                    saveLastScanTime(System.currentTimeMillis())
                } else {
                    // Quick and cheap incremental sync
                    performIncrementalSync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
                isSyncInProgress.set(false)
            }
        }
    }

    private suspend fun performIncrementalSync() {
        val lastScanTime = getLastScanTime()
        val now = System.currentTimeMillis()
        
        // Skip scan completely if 6 hours haven't elapsed yet
        if (now - lastScanTime < 6 * 60 * 60 * 1000) {
            return
        }
        
        val musicDir = java.io.File(com.example.data.scanner.MusicConfig.MUSIC_DIR)
        if (!musicDir.exists() || !musicDir.isDirectory) return
        
        val devicePaths = musicDir.listFiles { file ->
            file.isFile && file.extension.lowercase() == "mp3"
        }?.map { it.absolutePath }?.toSet() ?: emptySet()
        
        val dbPaths = songDao.getAllPaths().toSet()
        
        // 1. Instantly remove deleted path entities from DB
        val deletedPaths = dbPaths - devicePaths
        if (deletedPaths.isNotEmpty()) {
            songDao.deleteByPaths(deletedPaths.toList())
        }
        
        // 2. Scan and extract metadata for exclusively new files
        val newPaths = devicePaths - dbPaths
        if (newPaths.isNotEmpty()) {
            val newSongs = newPaths.mapNotNull { path ->
                mediaScanner.scanSingleFile(path)
            }
            if (newSongs.isNotEmpty()) {
                songDao.upsertSongs(newSongs)
            }
        }
        
        saveLastScanTime(now)
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
