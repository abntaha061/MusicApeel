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

    fun syncLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                // 1. Scan external storage/assets
                val scanned = mediaScanner.scanDevice()
                val paths = scanned.map { it.filePath }
                
                // 2. Remove deleted files
                songDao.removeDeletedSongs(paths)
                
                // 3. Sync library insert
                songDao.upsertSongs(scanned)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
