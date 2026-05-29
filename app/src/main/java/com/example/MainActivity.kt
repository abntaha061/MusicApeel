package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.presentation.components.AlbumProfileScreen
import com.example.presentation.components.ArtistProfileScreen
import com.example.presentation.components.MiniPlayer
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.player.PlayerScreen
import com.example.presentation.player.PlayerViewModel
import com.example.service.MusicService

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private var musicService: MusicService? = null
    private var isBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            homeViewModel.syncLibrary(force = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start and bind playing Service
        Intent(this, MusicService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0E12)
                ) {
                    AppContent(
                        musicService = musicService,
                        isBound = isBound,
                        homeViewModel = homeViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            homeViewModel.checkAndStartLibrarySync()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4FC3F7),
            background = Color(0xFF0C0E12),
            surface = Color(0xFF141A23)
        ),
        content = content
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppContent(
    musicService: MusicService?,
    isBound: Boolean,
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel
) {
    val fontFamily = FontFamily.Default // Dynamic system fallback font for flawless build stability
    
    // View state mappings
    var activeTab by remember { mutableStateOf("home") } // "home", "artist_profile", "album_profile"
    var selectedArtistName by remember { mutableStateOf("") }
    var selectedAlbumName by remember { mutableStateOf("") }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Service streams collection
    val currentSong = musicService?.currentSong?.collectAsState()?.value
    val isPlaying = musicService?.isPlaying?.collectAsState()?.value ?: false
    val currentPosition = musicService?.currentPosition?.collectAsState()?.value ?: 0L

    // ViewModel collect streams
    val allSongs by homeViewModel.allSongs.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val stats by homeViewModel.libraryStats.collectAsState()
    val sortOrder by homeViewModel.sortOrder.collectAsState()
    val isSyncing by homeViewModel.isSyncing.collectAsState()

    val lyrics by playerViewModel.lyrics.collectAsState()
    val dominantColors by playerViewModel.dominantColors.collectAsState()

    // Side effect triggers when playing song changes
    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            playerViewModel.updateSong(currentSong)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Switcher Layout Screen Pane
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "home" -> {
                        HomeScreen(
                            allSongs = listOf(allSongs),
                            allSongsDirect = allSongs,
                            recentlyPlayed = recentlyPlayed,
                            stats = stats,
                            sortOrder = sortOrder,
                            isSyncing = isSyncing,
                            fontFamily = fontFamily,
                            onSetSortOrder = { homeViewModel.setSortOrder(it) },
                            onPlaySongList = { list, index ->
                                musicService?.playSongList(list, index)
                            },
                            onAddToNext = { song ->
                                musicService?.addToNext(song)
                            },
                            onViewArtist = { artist ->
                                selectedArtistName = artist
                                activeTab = "artist_profile"
                            },
                            onViewAlbum = { album ->
                                selectedAlbumName = album
                                activeTab = "album_profile"
                            },
                            onForceSync = { homeViewModel.syncLibrary(force = true) }
                        )
                    }
                    "artist_profile" -> {
                        ArtistProfileScreen(
                            artistName = selectedArtistName,
                            allSongs = allSongs,
                            fontFamily = fontFamily,
                            onBack = { activeTab = "home" },
                            onPlaySongList = { list, index ->
                                musicService?.playSongList(list, index)
                            },
                            onAddToNext = { song ->
                                musicService?.addToNext(song)
                            }
                        )
                    }
                    "album_profile" -> {
                        AlbumProfileScreen(
                            albumName = selectedAlbumName,
                            allSongs = allSongs,
                            fontFamily = fontFamily,
                            onBack = { activeTab = "home" },
                            onPlaySongList = { list, index ->
                                musicService?.playSongList(list, index)
                            },
                            onAddToNext = { song ->
                                musicService?.addToNext(song)
                            }
                        )
                    }
                }
            }

            // Bottom Mini Player Overlay spacer offset padding so list elements are not cut off
            if (currentSong != null) {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }

        // Floating Mini Player card (collapsible slide up view)
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val progress = if (currentSong.duration > 0) currentPosition.toFloat() / currentSong.duration else 0f
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    fontFamily = fontFamily,
                    onTogglePlay = { musicService?.togglePlayPause() },
                    onNext = { musicService?.next() },
                    onClick = { isPlayerExpanded = true }
                )
            }
        }

        // Animated Immersive Immersive Media Player full-screen overlay
        AnimatedVisibility(
            visible = isPlayerExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(450)) + fadeIn(animationSpec = tween(350)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(450)) + fadeOut(animationSpec = tween(350))
        ) {
            if (currentSong != null) {
                PlayerScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosition,
                    dominantColors = dominantColors,
                    lyrics = lyrics,
                    fontFamily = fontFamily,
                    onTogglePlay = { musicService?.togglePlayPause() },
                    onNext = { musicService?.next() },
                    onPrevious = { musicService?.previous() },
                    onSeekTo = { targetPosition -> musicService?.seekTo(targetPosition) },
                    onCollapse = { isPlayerExpanded = false }
                )
            }
        }
    }
}
