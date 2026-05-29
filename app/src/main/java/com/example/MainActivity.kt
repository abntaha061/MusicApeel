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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.components.AlbumArtImage
import com.example.data.db.SongEntity
import com.example.presentation.components.MiniPlayer
import com.example.presentation.components.AuroraBackground
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.home.formatDuration
import com.example.presentation.player.PlayerScreen
import com.example.presentation.player.PlayerViewModel
import com.example.service.MusicService
import com.example.ui.theme.MyApplicationTheme
import com.example.presentation.components.SongRowComponent
import com.example.presentation.components.ArtistProfileScreen

class MainActivity : ComponentActivity() {

    private val homeViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private var musicService: MusicService? by mutableStateOf(null)
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            musicService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (results[storagePermission] == true) {
            homeViewModel.checkAndStartLibrarySync()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind the background Music Service
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        enableEdgeToEdge()

        // Handle permissions and trigger library sync exactly once on startup
        checkAndRequestPermissions()

        setContent {
            MyApplicationTheme {
                // FORCE layoutDirection = LayoutDirection.Rtl Arabic-first native design
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppContent(
                        musicService = musicService,
                        context = this
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }

        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionsToRequest = mutableListOf(storagePermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            homeViewModel.checkAndStartLibrarySync()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppContent(
    musicService: MusicService?,
    context: Context
) {
    val homeViewModel: HomeViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    val allSongs by homeViewModel.allSongs.collectAsState()
    val isSyncing by homeViewModel.isSyncing.collectAsState()

    // Observe service playback state flows safely and unconditionally
    val currentSongFlow = remember(musicService) { musicService?.currentSong ?: kotlinx.coroutines.flow.MutableStateFlow(null) }
    val currentSong by currentSongFlow.collectAsState()

    val isPlayingFlow = remember(musicService) { musicService?.isPlaying ?: kotlinx.coroutines.flow.MutableStateFlow(false) }
    val isPlaying by isPlayingFlow.collectAsState()

    val currentPositionFlow = remember(musicService) { musicService?.currentPosition ?: kotlinx.coroutines.flow.MutableStateFlow(0L) }
    val currentPosition by currentPositionFlow.collectAsState()

    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            playerViewModel.updateSong(currentSong)
        }
    }

    var selectedTab by remember { mutableStateOf("home") }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showAllSongsScreen by remember { mutableStateOf(false) }
    var selectedArtistForProfile by remember { mutableStateOf<String?>(null) }

    // Search view states
    var searchQuery by remember { mutableStateOf("") }

    // Unified Arabic safe typeface Font
    val CairoBold = FontFamily.SansSerif

    val activity = context as? android.app.Activity
    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (selectedArtistForProfile != null) {
            selectedArtistForProfile = null
        } else if (showAllSongsScreen) {
            showAllSongsScreen = false
        } else {
            when (selectedTab) {
                "search", "library" -> {
                    selectedTab = "home"
                }
                "home" -> {
                    if (backPressedOnce) {
                        activity?.finish()
                    } else {
                        backPressedOnce = true
                        android.widget.Toast.makeText(context, "اضغط مرة أخرى للخروج", android.widget.Toast.LENGTH_SHORT).show()
                        activity?.window?.decorView?.postDelayed({
                            backPressedOnce = false
                        }, 2000L)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isPlayerExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    // A. Persistent Mini Player floating overlay above Navigation Bar
                    if (currentSong != null) {
                        MiniPlayer(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition,
                            onPlayPauseClicked = { musicService?.togglePlayPause() },
                            onNextClicked = { musicService?.playNext() },
                            onPreviousClicked = { musicService?.playPrevious() },
                            onMiniPlayerClicked = { isPlayerExpanded = true }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // B. Translucent Glossy Bottom Navigation Bar
                    NavigationBar(
                        containerColor = Color(0xFF0C0C0D).copy(alpha = 0.94f),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .border(
                                width = 0.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                                ),
                                shape = androidx.compose.ui.graphics.RectangleShape
                            )
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == "home",
                            onClick = { selectedTab = "home" },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            label = { Text("الرئيسية", fontFamily = CairoBold, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1E88E5),
                                selectedTextColor = Color(0xFF1E88E5),
                                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                                indicatorColor = Color.White.copy(alpha = 0.06f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == "search",
                            onClick = { selectedTab = "search" },
                            icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            label = { Text("البحث", fontFamily = CairoBold, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1E88E5),
                                selectedTextColor = Color(0xFF1E88E5),
                                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                                indicatorColor = Color.White.copy(alpha = 0.06f)
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == "library",
                            onClick = { selectedTab = "library" },
                            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                            label = { Text("مكتبتي", fontFamily = CairoBold, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1E88E5),
                                selectedTextColor = Color(0xFF1E88E5),
                                unselectedIconColor = Color.White.copy(alpha = 0.45f),
                                unselectedTextColor = Color.White.copy(alpha = 0.45f),
                                indicatorColor = Color.White.copy(alpha = 0.06f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val dominantColors by playerViewModel.dominantColors.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPlayerExpanded) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            // Draw beautiful backgrounds behind routing screens
            if (currentSong != null) {
                AuroraBackground(
                    dominantColors = dominantColors,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF070708))
                )
            }

            // Screen router
            when (selectedTab) {
                "home" -> HomeScreen(
                    homeViewModel = homeViewModel,
                    onSongSelected = { songs, index ->
                        musicService?.playSongList(songs, index)
                    },
                    onArtistSelected = { artistName ->
                        selectedArtistForProfile = artistName
                    }
                )

                "search" -> {
                    // Fully implemented Local Search Page with Live Filtering
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "البحث الموسيقي",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن أغنية، فنان أو ألبوم...", color = Color.White.copy(alpha = 0.4f), fontFamily = CairoBold) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "مسح", tint = Color.White)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E88E5),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val filteredSongs = remember(allSongs, searchQuery) {
                            if (searchQuery.isBlank()) {
                                emptyList()
                            } else {
                                allSongs.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                            it.artist.contains(searchQuery, ignoreCase = true) ||
                                            it.album.contains(searchQuery, ignoreCase = true)
                                }
                            }
                        }

                        if (searchQuery.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "ابحث عن أي أغنية",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontFamily = CairoBold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (filteredSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد نتائج لـ \"$searchQuery\"",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontFamily = CairoBold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(
                                    items = filteredSongs,
                                    key = { _, song -> song.id }
                                ) { index, song ->
                                    SongRowComponent(
                                        song = song,
                                        fontFamily = CairoBold,
                                        onClick = {
                                            musicService?.playSongList(filteredSongs, index)
                                        },
                                        onAddToNext = {
                                            musicService?.addToNext(song)
                                        },
                                        onViewArtist = { artistName ->
                                            selectedArtistForProfile = artistName
                                            isPlayerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                "library" -> {
                    if (showAllSongsScreen) {
                        // All Songs Sub-screen with back button and high quality responsive list
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            // Header with beautiful back button and count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${allSongs.size} أغنية",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoBold
                                )

                                IconButton(
                                    onClick = { showAllSongsScreen = false },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.1f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = Color.White
                                    )
                                }
                            }

                            if (allSongs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "المكتبة فارغة",
                                        color = Color.White.copy(0.4f),
                                        fontFamily = CairoBold
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 160.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(
                                        items = allSongs,
                                        key = { _, song -> song.id }
                                    ) { index, song ->
                                        SongRowComponent(
                                            song = song,
                                            fontFamily = CairoBold,
                                            onClick = {
                                                musicService?.playSongList(allSongs, index)
                                            },
                                            onAddToNext = {
                                                musicService?.addToNext(song)
                                            },
                                            onViewArtist = { artistName ->
                                                selectedArtistForProfile = artistName
                                                isPlayerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Library Stats view (NO automatic list loaded)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                Text(
                                    text = "إحصائيات استماعي",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                                Text(
                                    text = "ترتيب وذكاء استخدام ذوقك الموسيقي",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontFamily = CairoBold
                                )
                            }

                            // ── بطاقة العداد — "585 أغنية" ──
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(0.10f))
                                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(18.dp))
                                        .clickable { showAllSongsScreen = true }
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronLeft,
                                            contentDescription = null,
                                            tint = Color.White.copy(0.5f)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${allSongs.size} أغنية",
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = CairoBold
                                            )
                                            Text(
                                                text = "اضغط لعرض المكتبة كاملة",
                                                color = Color.White.copy(0.5f),
                                                fontSize = 13.sp,
                                                fontFamily = CairoBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Summary Statistics Card (Row) representing total performance
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Rounded.Equalizer, null, tint = Color(0xFFD81B60), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val totalPlays = allSongs.sumOf { it.playCount }
                                        Text("$totalPlays مرة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = CairoBold)
                                        Text("إجمالي الاستماع", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = CairoBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // D. Artist Profile Screen Overlay
            if (selectedArtistForProfile != null) {
                ArtistProfileScreen(
                    artistName = selectedArtistForProfile!!,
                    allSongs = allSongs,
                    fontFamily = CairoBold,
                    onBack = { selectedArtistForProfile = null },
                    onSongSelected = { songs, index ->
                        musicService?.playSongList(songs, index)
                    },
                    onAddToNext = { song ->
                        musicService?.addToNext(song)
                    },
                    onViewArtist = { artistName ->
                        selectedArtistForProfile = artistName
                    }
                )
            }

            // C. Fullscreen Music Player animated modal slide up transition
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(450)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut()
            ) {
                PlayerScreen(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosition,
                    playerViewModel = playerViewModel,
                    onPlayPauseClicked = { musicService?.togglePlayPause() },
                    onNextClicked = { musicService?.playNext() },
                    onPreviousClicked = { musicService?.playPrevious() },
                    onSeek = { musicService?.seekTo(it) },
                    onCollapseClicked = { isPlayerExpanded = false },
                    musicService = musicService
                )
            }
        }
    }
}
