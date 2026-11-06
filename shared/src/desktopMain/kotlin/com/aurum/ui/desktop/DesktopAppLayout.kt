package com.aurum.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurum.ui.common.LocalVisualizerColorScheme
import com.aurum.ui.common.RainbowText
import com.aurum.ui.common.ScanningProgressBanner
import com.aurum.ui.library.*
import com.aurum.ui.player.PlayerViewModel
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.search.SearchScreen
import com.aurum.ui.settings.SettingsScreen
import com.aurum.ui.theme.AppFps

enum class DesktopNavSection(val title: String, val icon: ImageVector) {
    SEARCH("Search", Icons.Default.Search),
    SONGS("Songs", Icons.Default.MusicNote),
    ALBUMS("Albums", Icons.Default.Album),
    ARTISTS("Artists", Icons.Default.Person),
    GENRES("Genres", Icons.Default.Category),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun DesktopAppLayout(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    selectedVisualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    onVisualizerStyleChange: (VisualizerStyle) -> Unit = {},
    selectedVisualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    onVisualizerColorSchemeChange: (VisualizerColorScheme) -> Unit = {},
    selectedAppFps: AppFps = AppFps.FPS_60,
    onAppFpsChange: (AppFps) -> Unit = {},
    selectedProgressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    onProgressDotStyleChange: (ProgressDotStyle) -> Unit = {},
    stopMusicOnAppClose: Boolean = false,
    onStopMusicOnAppCloseChange: (Boolean) -> Unit = {},
    selectedFolder: String?,
    onChooseFolder: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val uiState by libraryViewModel.uiState.collectAsState()
    val scanProgress by libraryViewModel.scanProgress.collectAsState()
    val playerState = playerViewModel.playerState.value

    var currentSection by remember { mutableStateOf(DesktopNavSection.SONGS) }
    var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var selectedGenreName by remember { mutableStateOf<String?>(null) }
    var isMetadataEditorOpen by remember { mutableStateOf(false) }
    var isImmersivePlayerOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSidebarOpen by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalVisualizerColorScheme provides selectedVisualizerColorScheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Escape -> {
                                when {
                                    isMetadataEditorOpen -> {
                                        isMetadataEditorOpen = false
                                        true
                                    }
                                    isImmersivePlayerOpen -> {
                                        isImmersivePlayerOpen = false
                                        true
                                    }
                                    selectedAlbumTitle != null || selectedArtistName != null || selectedGenreName != null -> {
                                        selectedAlbumTitle = null
                                        selectedArtistName = null
                                        selectedGenreName = null
                                        true
                                    }
                                    isFullscreen -> {
                                        onToggleFullscreen()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            Key.Spacebar -> {
                                if (!isMetadataEditorOpen && currentSection != DesktopNavSection.SEARCH) {
                                    playerViewModel.togglePlayPause()
                                    focusManager.clearFocus()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            if (isImmersivePlayerOpen) {
                DesktopImmersivePlayerView(
                    playerState = playerState,
                    visualizerStyle = selectedVisualizerStyle,
                    visualizerColorScheme = selectedVisualizerColorScheme,
                    appFps = selectedAppFps,
                    progressDotStyle = selectedProgressDotStyle,
                    onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                    onPrevious = { playerViewModel.previous() },
                    onNext = { playerViewModel.next() },
                    onSeekTo = { pos -> playerViewModel.seekTo(pos) },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onCycleRepeatMode = { playerViewModel.cycleRepeatMode() },
                    onFavoriteToggle = {
                        val song = playerState.currentSong
                        if (song != null) libraryViewModel.toggleFavorite(song)
                    },
                    onBackClick = { isImmersivePlayerOpen = false },
                    onOpenMetadataFixer = { song ->
                        libraryViewModel.setEditingTargetSong(song)
                        isMetadataEditorOpen = true
                        isImmersivePlayerOpen = false
                    },
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Header Window Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isSidebarOpen = !isSidebarOpen },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                                        contentDescription = "Toggle Sidebar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (selectedAlbumTitle != null || selectedArtistName != null || selectedGenreName != null) {
                                            selectedAlbumTitle = null
                                            selectedArtistName = null
                                            selectedGenreName = null
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Right Action Controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        onToggleFullscreen()
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullscreen) "Exit Fullscreen (F11)" else "Fullscreen (F11)",
                                        tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { playerViewModel.openSystemEqualizer() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { currentSection = DesktopNavSection.SEARCH },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { isQueueOpen = !isQueueOpen },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = if (isQueueOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ScanningProgressBanner(scanProgress = scanProgress)

                    // Top Main Body Row (Left Nav Sidebar + Center Content + Right Queue Panel)
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Left Sidebar
                        if (isSidebarOpen) {
                            Surface(
                                modifier = Modifier.width(220.dp).fillMaxHeight(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp)
                                ) {
                                    RainbowText(
                                        text = "Aurum",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                                    )

                                    // Search Nav
                                    DesktopNavRow(
                                        title = DesktopNavSection.SEARCH.title,
                                        icon = DesktopNavSection.SEARCH.icon,
                                        isSelected = currentSection == DesktopNavSection.SEARCH,
                                        onClick = {
                                            currentSection = DesktopNavSection.SEARCH
                                            selectedAlbumTitle = null
                                            selectedArtistName = null
                                            selectedGenreName = null
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Your Library",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Local Music",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    listOf(
                                        DesktopNavSection.SONGS,
                                        DesktopNavSection.ALBUMS,
                                        DesktopNavSection.ARTISTS,
                                        DesktopNavSection.GENRES,
                                        DesktopNavSection.PLAYLISTS
                                    ).forEach { section ->
                                        DesktopNavRow(
                                            title = section.title,
                                            icon = section.icon,
                                            isSelected = currentSection == section && selectedAlbumTitle == null && selectedArtistName == null && selectedGenreName == null,
                                            onClick = {
                                                currentSection = section
                                                selectedAlbumTitle = null
                                                selectedArtistName = null
                                                selectedGenreName = null
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    DesktopNavRow(
                                        title = DesktopNavSection.SETTINGS.title,
                                        icon = DesktopNavSection.SETTINGS.icon,
                                        isSelected = currentSection == DesktopNavSection.SETTINGS,
                                        onClick = {
                                            currentSection = DesktopNavSection.SETTINGS
                                            selectedAlbumTitle = null
                                            selectedArtistName = null
                                            selectedGenreName = null
                                        }
                                    )
                                }
                            }
                        }

                        // Center Content Area
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when {
                                isMetadataEditorOpen -> {
                                com.aurum.ui.settings.MetadataEditorScreen(
                                    viewModel = libraryViewModel,
                                    onBackClick = { isMetadataEditorOpen = false }
                                )
                            }
                            selectedAlbumTitle != null -> {
                                    AlbumDetailScreen(
                                        albumTitle = selectedAlbumTitle!!,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) },
                                        onBackClick = { selectedAlbumTitle = null },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            isMetadataEditorOpen = true
                                        },
                                        onOpenMetadataFixerForAlbum = { album ->
                                            libraryViewModel.setEditingTargetAlbum(album)
                                            isMetadataEditorOpen = true
                                        }
                                    )
                                }
                                selectedArtistName != null -> {
                                    ArtistDetailScreen(
                                        artistName = selectedArtistName!!,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) },
                                        onAlbumClick = { title -> selectedAlbumTitle = title },
                                        onBackClick = { selectedArtistName = null },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            isMetadataEditorOpen = true
                                        },
                                        onOpenMetadataFixerForAlbum = { album ->
                                            libraryViewModel.setEditingTargetAlbum(album)
                                            isMetadataEditorOpen = true
                                        }
                                    )
                                }
                                selectedGenreName != null -> {
                                    GenreDetailScreen(
                                        genreName = selectedGenreName!!,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) },
                                        onBackClick = { selectedGenreName = null }
                                    )
                                }
                                else -> {
                                    when (val state = uiState) {
                                        is LibraryUiState.Loading -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        is LibraryUiState.Error -> {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(state.message, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        is LibraryUiState.Success -> {
                                            when (currentSection) {
                                                DesktopNavSection.SEARCH -> {
                                                    SearchScreen(
                                                        viewModel = libraryViewModel,
                                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) }
                                                    )
                                                }
                                                DesktopNavSection.SONGS -> {
                                                    DesktopSongsTableView(
                                                        songs = state.filteredSongs,
                                                        currentSong = playerState.currentSong,
                                                        isPlaying = playerState.isPlaying,
                                                        playlists = state.playlists,
                                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) },
                                                        onFavoriteToggle = { song -> libraryViewModel.toggleFavorite(song) },
                                                        onPlayNext = { song -> playerViewModel.playNext(song) },
                                                        onAddToQueue = { song -> playerViewModel.addToQueue(song) },
                                                        onAddToPlaylist = { playlistId, songId -> libraryViewModel.addSongToPlaylist(playlistId, songId) },
                                                        onDeleteSong = { song -> libraryViewModel.deleteSong(song) },
                                                        onEditMetadata = { song ->
                                                            isMetadataEditorOpen = true
                                                            libraryViewModel.searchAppleMusic(song.album.ifBlank { song.title })
                                                        },
                                                        onPlayAllClick = {
                                                            if (state.filteredSongs.isNotEmpty()) {
                                                                if (playerState.isPlaying && playerState.currentSong != null) {
                                                                    playerViewModel.togglePlayPause()
                                                                } else {
                                                                    libraryViewModel.playSong(state.filteredSongs.first(), state.filteredSongs)
                                                                }
                                                            }
                                                        },
                                                        onShuffleAllClick = {
                                                            if (state.filteredSongs.isNotEmpty()) {
                                                                libraryViewModel.playSong(state.filteredSongs.shuffled().first(), state.filteredSongs.shuffled())
                                                            }
                                                        }
                                                    )
                                                }
                                                DesktopNavSection.ALBUMS -> {
                                                    DesktopAlbumsGridView(
                                                        albums = state.albums,
                                                        onAlbumClick = { title -> selectedAlbumTitle = title }
                                                    )
                                                }
                                                DesktopNavSection.ARTISTS -> {
                                                    DesktopArtistsGridView(
                                                        artists = state.artists,
                                                        onArtistClick = { name -> selectedArtistName = name }
                                                    )
                                                }
                                                DesktopNavSection.GENRES -> {
                                                    GenresScreen(
                                                        viewModel = libraryViewModel,
                                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist) },
                                                        onGenreClick = { name -> selectedGenreName = name },
                                                        onBackClick = { currentSection = DesktopNavSection.SONGS }
                                                    )
                                                }
                                                DesktopNavSection.PLAYLISTS -> {
                                                    PlaylistsScreen(
                                                        viewModel = libraryViewModel,
                                                        onBackClick = { currentSection = DesktopNavSection.SONGS }
                                                    )
                                                }
                                                DesktopNavSection.SETTINGS -> {
                                                    SettingsScreen(
                                                        isDarkTheme = isDarkTheme,
                                                        onDarkThemeChange = onDarkThemeChange,
                                                        selectedVisualizerStyle = selectedVisualizerStyle,
                                                        onVisualizerStyleChange = onVisualizerStyleChange,
                                                        selectedVisualizerColorScheme = selectedVisualizerColorScheme,
                                                        onVisualizerColorSchemeChange = onVisualizerColorSchemeChange,
                                                        selectedAppFps = selectedAppFps,
                                                        onAppFpsChange = onAppFpsChange,
                                                        selectedProgressDotStyle = selectedProgressDotStyle,
                                                        onProgressDotStyleChange = onProgressDotStyleChange,
                                                        selectedFolder = selectedFolder,
                                                        onChooseFolder = onChooseFolder,
                                                        stopMusicOnAppClose = stopMusicOnAppClose,
                                                        onStopMusicOnAppCloseChange = onStopMusicOnAppCloseChange,
                                                        onOpenEqualizer = { playerViewModel.openSystemEqualizer() },
                                                        onOpenMetadataEditor = { isMetadataEditorOpen = true },
                                                        onRepairAllArtwork = { libraryViewModel.repairAllMissingArtwork { _, _ -> } },
                                                        onRemoveDuplicates = { libraryViewModel.removeDuplicateSongs { } },
                                                        onRescan = { libraryViewModel.rescan() },
                                                        onBackClick = { currentSection = DesktopNavSection.SONGS }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Queue Side Panel
                        if (isQueueOpen) {
                            DesktopQueueSidePanel(
                                playerState = playerState,
                                onSongSelect = { song -> libraryViewModel.playSong(song, playerState.queue) },
                                onRemoveFromQueue = { idx -> playerViewModel.removeFromQueue(idx) },
                                onClearQueue = { playerViewModel.next() },
                                onClose = { isQueueOpen = false }
                            )
                        }
                    }

                    // Persistent Bottom Player Dock
                    DesktopPlayerDock(
                        playerState = playerState,
                        visualizerStyle = selectedVisualizerStyle,
                        visualizerColorScheme = selectedVisualizerColorScheme,
                        appFps = selectedAppFps,
                        progressDotStyle = selectedProgressDotStyle,
                        onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                        onPrevious = { playerViewModel.previous() },
                        onNext = { playerViewModel.next() },
                        onSeekTo = { pos -> playerViewModel.seekTo(pos) },
                        onToggleShuffle = { playerViewModel.toggleShuffle() },
                        onCycleRepeatMode = { playerViewModel.cycleRepeatMode() },
                        onFavoriteToggle = {
                            val song = playerState.currentSong
                            if (song != null) libraryViewModel.toggleFavorite(song)
                        },
                        onQueueToggle = { isQueueOpen = !isQueueOpen },
                        isQueueOpen = isQueueOpen,
                        onOpenNowPlaying = { isImmersivePlayerOpen = true },
                        onOpenMetadataFixer = { song ->
                            libraryViewModel.setEditingTargetSong(song)
                            isMetadataEditorOpen = true
                        },
                        onVolumeChange = { vol -> playerViewModel.setVolume(vol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopNavRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val selectedBg = if (isDark) Color(0xFF2A2E38) else MaterialTheme.colorScheme.primaryContainer
    val selectedContent = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
    val unselectedContent = MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedContent else unselectedContent
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopAppLayoutPreview() {
    val fakeRepository = object : com.aurum.data.repository.MusicRepository {
        override suspend fun getSongs(): List<com.aurum.data.model.Song> = listOf(
            com.aurum.data.model.Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
        )
        override suspend fun getAlbums(): List<com.aurum.data.model.Album> = emptyList()
        override suspend fun getArtists(): List<com.aurum.data.model.Artist> = emptyList()
        override suspend fun searchSongs(query: String): List<com.aurum.data.model.Song> = emptyList()
        override suspend fun rescan() {}
        override suspend fun toggleFavorite(songId: String): Boolean = false
        override suspend fun updateAlbumMetadata(oldAlbumTitle: String, newTitle: String, newArtist: String) {}
        override suspend fun updateAlbumMetadataAndArtwork(oldAlbumTitle: String, newTitle: String, newArtist: String, artworkUri: String) {}
        override suspend fun updateSongMetadataAndArtwork(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUri: String) {}
        override suspend fun mergeAlbums(sourceAlbumTitle: String, targetAlbumTitle: String, targetArtist: String, artworkUri: String?) {}
        override suspend fun deleteSong(songId: String) {}
        override suspend fun getPlaylists(): List<com.aurum.data.model.Playlist> = emptyList()
        override suspend fun createPlaylist(name: String): com.aurum.data.model.Playlist = com.aurum.data.model.Playlist("preview", name)
        override suspend fun deletePlaylist(id: String): Boolean = true
        override suspend fun addSongToPlaylist(playlistId: String, songId: String): Boolean = true
        override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean = true
    }

    val fakePlayer = object : com.aurum.player.MusicPlayer {
        override val state: State<com.aurum.player.PlayerState> = mutableStateOf(com.aurum.player.PlayerState(
            currentSong = com.aurum.data.model.Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = ""),
            isPlaying = true,
            position = 45000L,
            duration = 235000L
        ))
        override suspend fun play(song: com.aurum.data.model.Song) {}
        override suspend fun play(songs: List<com.aurum.data.model.Song>, startIndex: Int) {}
        override suspend fun playNext(song: com.aurum.data.model.Song) {}
        override suspend fun playNext(songs: List<com.aurum.data.model.Song>) {}
        override suspend fun addToQueue(song: com.aurum.data.model.Song) {}
        override suspend fun addToQueue(songs: List<com.aurum.data.model.Song>) {}
        override suspend fun pause() {}
        override suspend fun resume() {}
        override suspend fun seekTo(positionMs: Long) {}
        override suspend fun next() {}
        override suspend fun previous() {}
        override suspend fun stop() {}
        override fun toggleShuffle() {}
        override fun cycleRepeatMode() {}
    }

    val libraryViewModel = remember { LibraryViewModel(fakeRepository, fakePlayer) }
    val playerViewModel = remember { PlayerViewModel(fakePlayer) }

    com.aurum.ui.theme.AurumTheme {
        DesktopAppLayout(
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            isDarkTheme = true,
            onDarkThemeChange = {},
            selectedFolder = null,
            onChooseFolder = {}
        )
    }
}
