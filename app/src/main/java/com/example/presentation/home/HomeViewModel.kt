package com.example.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LibraryStats
import com.example.data.db.SongDatabase
import com.example.data.db.SongEntity
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = SongDatabase.getDatabase(application).songDao()
    private val scanner = MediaScanner(application, songDao)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ALPHABETICAL_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    val allSongs: StateFlow<List<SongEntity>> = combine(
        songDao.getAllSongs(),
        _sortOrder
    ) { songs, order ->
        when (order) {
            SortOrder.ALPHABETICAL_ASC -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SortOrder.ALPHABETICAL_DESC -> songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            SortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { if (it.dateAdded == 0L) it.id else it.dateAdded }
            SortOrder.DATE_ADDED_ASC -> songs.sortedBy { if (it.dateAdded == 0L) it.id else it.dateAdded }
        }
    }.flowOn(Dispatchers.IO)
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

    val recentlyAdded: StateFlow<List<SongEntity>> = songDao.getRecentlyAdded()
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

    val libraryStats: StateFlow<LibraryStats> = allSongs.map { songs ->
        val uniqueArtists = songs.flatMap { splitArtists(it.artist) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val uniqueAlbums = songs.map { it.album.trim() }
            .filter { it.isNotEmpty() && !it.equals("ألبوم غير معروف", ignoreCase = true) && !it.equals("Unknown Album", ignoreCase = true) }
            .distinct()
        val totalDuration = songs.sumOf { it.duration }
        val totalListening = songs.sumOf { it.playCount * it.duration }
        LibraryStats(
            totalSongs = songs.size,
            totalDurationMs = totalDuration,
            totalArtists = uniqueArtists.size,
            totalAlbums = uniqueAlbums.size,
            totalListeningTimeMs = totalListening
        )
    }.flowOn(Dispatchers.IO)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats(0, 0, 0, 0, 0))

    fun checkAndStartLibrarySync() {
        viewModelScope.launch {
            _isSyncing.value = true
            scanner.scanAndPopulate(force = false)
            _isSyncing.value = false
        }
    }

    fun syncLibrary(force: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            scanner.scanAndPopulate(force = force)
            _isSyncing.value = false
        }
    }

    companion object {
        fun splitArtists(artistString: String): List<String> {
            return artistString.split(Regex("[,;&و]\\s*|\\s+and\\s+|\\s*\\+\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}
