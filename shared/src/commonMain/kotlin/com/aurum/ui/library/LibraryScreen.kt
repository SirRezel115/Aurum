package com.aurum.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Playlist
import com.aurum.data.model.Song
import com.aurum.domain.SongSortBy
import com.aurum.ui.common.TopographicContourPlaceholder

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${if (seconds < 10) "0" else ""}$seconds"
}

enum class LibraryTab(val displayName: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    DEEZER("The Stash")
}

enum class DeezerSubTab(val displayName: String) {
    FLOW("⚡ Flow"),
    SAVED_TRACKS("Saved Songs"),
    PLAYLISTS("My Playlists"),
    ALBUMS("Saved Albums"),
    ARTISTS("Saved Artists")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenMetadataFixerForSong: (Song) -> Unit = {},
    onOpenMetadataFixerForAlbum: (Album) -> Unit = {},
    onOpenMetadataFixerForArtist: (Artist) -> Unit = {},
    deezerArlToken: String = "",
    selectedFolder: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val glassBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColors = if (isDark) {
        listOf(Color(0xFF0D0E15), Color(0xFF141622), Color(0xFF090A0E))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F2), Color(0xFFF3EFE6))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { com.aurum.ui.common.RainbowText("Aurum Library", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (!isLandscape) {
                            IconButton(onClick = onOpenDrawer) {
                                com.aurum.ui.common.RainbowIcon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = bgColors))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                com.aurum.ui.common.ScanningProgressBanner(scanProgress = scanProgress)

                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is LibraryUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is LibraryUiState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))

                        var deezerSearchQuery by rememberSaveable { mutableStateOf("") }

                        // 1. Search Bar at the Top
                        OutlinedTextField(
                            value = if (selectedTab == LibraryTab.DEEZER) deezerSearchQuery else state.searchQuery,
                            onValueChange = { query ->
                                if (selectedTab == LibraryTab.DEEZER) {
                                    deezerSearchQuery = query
                                    viewModel.searchDeezer(query)
                                } else {
                                    viewModel.onSearchQueryChanged(query)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(glassBg),
                            placeholder = {
                                Text(
                                    if (selectedTab == LibraryTab.DEEZER) "Search The Stash..." else "Search library...",
                                    color = textSecondary
                                )
                            },
                            leadingIcon = { com.aurum.ui.common.RainbowIcon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                val currentQuery = if (selectedTab == LibraryTab.DEEZER) deezerSearchQuery else state.searchQuery
                                if (currentQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (selectedTab == LibraryTab.DEEZER) {
                                                deezerSearchQuery = ""
                                                viewModel.searchDeezer("")
                                            } else {
                                                viewModel.onSearchQueryChanged("")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = textSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Pill Filter Buttons (Songs, Albums, Artists, Playlists)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LibraryTab.entries.forEach { tab ->
                                val isSelected = (tab == selectedTab)
                                val isRainbow = com.aurum.ui.common.LocalVisualizerColorScheme.current == com.aurum.ui.player.VisualizerColorScheme.RAINBOW
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isRainbow) com.aurum.ui.common.RainbowBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                            } else androidx.compose.ui.graphics.SolidColor(glassBg)
                                        )
                                        .clickable { selectedTab = tab },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(50.dp),
                                    border = if (!isSelected) glassBorder else null
                                ) {
                                    Text(
                                        text = tab.displayName,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color.Black else textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Content List / Grid Sorted Alphabetically (A-Z)
                        when (selectedTab) {
                            LibraryTab.SONGS -> {
                                val sortedSongs = remember(state.filteredSongs) {
                                    state.filteredSongs.sortedBy { it.title.lowercase() }
                                }
                                if (sortedSongs.isEmpty()) {
                                    EmptyState("No songs found")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(sortedSongs, key = { "song_${it.id}" }) { song ->
                                            SongItemRow(
                                                song = song,
                                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                                onPlayNext = { viewModel.playNext(song) },
                                                onAddToQueue = { viewModel.addToQueue(song) },
                                                onEditMetadata = { onOpenMetadataFixerForSong(song) },
                                                onRepairArtwork = { viewModel.repairArtworkForAlbum(song.artist, song.album) },
                                                onClick = { onSongClick(song, sortedSongs) }
                                            )
                                        }
                                    }
                                }
                            }
                            LibraryTab.ALBUMS -> {
                                val sortedAlbums = remember(state.albums) {
                                    state.albums.sortedBy { it.title.lowercase() }
                                }
                                if (sortedAlbums.isEmpty()) {
                                    EmptyState("No albums found")
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(sortedAlbums, key = { "album_${it.id}" }) { album ->
                                            AlbumGridCard(
                                                album = album,
                                                onClick = { onAlbumClick(album.title) },
                                                onLongClick = { onOpenMetadataFixerForAlbum(album) },
                                                onRepairArtwork = { viewModel.repairArtworkForAlbum(album.artist, album.title) }
                                            )
                                        }
                                    }
                                }
                            }
                            LibraryTab.ARTISTS -> {
                                val sortedArtists = remember(state.artists) {
                                    state.artists.sortedBy { it.name.lowercase() }
                                }
                                if (sortedArtists.isEmpty()) {
                                    EmptyState("No artists found")
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(sortedArtists, key = { "artist_${it.id}" }) { artist ->
                                            ArtistGridCard(
                                                artist = artist,
                                                onClick = { onArtistClick(artist.name) },
                                                onLongClick = { onOpenMetadataFixerForArtist(artist) }
                                            )
                                        }
                                    }
                                }
                            }
                            LibraryTab.PLAYLISTS -> {
                                val sortedPlaylists = remember(state.playlists) {
                                    state.playlists.sortedBy { it.name.lowercase() }
                                }
                                if (sortedPlaylists.isEmpty()) {
                                    EmptyState("No playlists found")
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(sortedPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                                            PlaylistGridCard(playlist = playlist, onClick = {})
                                        }
                                    }
                                }
                            }
                            LibraryTab.DEEZER -> {
                                DeezerHubContent(
                                    viewModel = viewModel,
                                    deezerArlToken = deezerArlToken,
                                    selectedFolder = selectedFolder,
                                    deezerSearchQuery = deezerSearchQuery,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onArtistClick = onArtistClick,
                                    onSongClick = onSongClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SongItemRow(
    song: Song,
    trackNumber: Int? = null,
    showThumbnail: Boolean = true,
    onFavoriteClick: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onEditMetadata: (() -> Unit)? = null,
    onRepairArtwork: (() -> Unit)? = null,
    isGlass: Boolean = true,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBg = if (isGlass) (if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isGlass) (if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) else null
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { offset ->
                        with(density) {
                            menuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                        }
                        showMenu = true
                    }
                )
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = cardBg,
            shape = MaterialTheme.shapes.medium,
            border = cardBorder,
            tonalElevation = if (isGlass) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trackNumber != null) {
                    Text(
                        text = "$trackNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (showThumbnail) {
                    if (song.albumArtUri.isNullOrBlank()) {
                        TopographicContourPlaceholder(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    } else {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onFavoriteClick) {
                    if (song.isFavorite) {
                        com.aurum.ui.common.RainbowIcon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = textSecondary
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = menuOffset
        ) {
            DropdownMenuItem(
                text = { Text("Play Next") },
                onClick = {
                    showMenu = false
                    onPlayNext()
                }
            )
            DropdownMenuItem(
                text = { Text("Add to Queue") },
                onClick = {
                    showMenu = false
                    onAddToQueue()
                }
            )
            DropdownMenuItem(
                text = { Text("🎨 Fix & Fetch 1000x1000 Artwork") },
                onClick = {
                    showMenu = false
                    onRepairArtwork?.invoke()
                }
            )
            if (onEditMetadata != null) {
                DropdownMenuItem(
                    text = { Text("✏️ Edit Metadata & Search HD Artwork") },
                    onClick = {
                        showMenu = false
                        onEditMetadata()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGridCard(album: Album, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, onRepairArtwork: (() -> Unit)? = null) {
    val density = LocalDensity.current
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { offset ->
                        with(density) {
                            menuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                        }
                        showMenu = true
                    }
                )
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = cardBg,
            border = cardBorder,
            shape = RoundedCornerShape(16.dp)
        ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (album.albumArtUri.isNullOrBlank()) {
                    TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = album.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artist} • ${album.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                maxLines = 1
            )
        }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = menuOffset
        ) {
            DropdownMenuItem(
                text = { Text("🎨 Fix & Fetch 1000x1000 Artwork") },
                onClick = {
                    showMenu = false
                    onRepairArtwork?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text("✏️ Search Metadata Fixer") },
                onClick = {
                    showMenu = false
                    onLongClick?.invoke()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistGridCard(artist: Artist, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            ),
        color = cardBg,
        border = cardBorder,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(50.dp))
            ) {
                if (artist.albumArtUri.isNullOrBlank()) {
                    TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = artist.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PlaylistGridCard(playlist: Playlist, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = cardBg,
        border = cardBorder,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songIds.size} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeezerHubContent(
    viewModel: LibraryViewModel,
    deezerArlToken: String,
    selectedFolder: String?,
    deezerSearchQuery: String = "",
    glassBg: Color,
    glassBorder: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    onArtistClick: (String) -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit
) {
    val deezerSubTab by viewModel.deezerSubTab.collectAsState()
    val isHubLoading by viewModel.isDeezerHubLoading.collectAsState()
    val isSearchingDeezer by viewModel.isSearchingDeezer.collectAsState()
    val deezerSearchResults by viewModel.deezerSearchResults.collectAsState()
    val deezerAlbumResults by viewModel.deezerAlbumResults.collectAsState()
    val deezerArtistResults by viewModel.deezerArtistResults.collectAsState()
    val deezerPlaylistResults by viewModel.deezerPlaylistResults.collectAsState()

    val arlStatus by viewModel.deezerArlStatus.collectAsState()
    val flowTracks by viewModel.deezerFlowTracks.collectAsState()
    val savedTracks by viewModel.userSavedTracks.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val savedAlbums by viewModel.userSavedAlbums.collectAsState()
    val savedArtists by viewModel.userSavedArtists.collectAsState()
    val downloadingTrackId by viewModel.downloadingTrackId.collectAsState()

    val downloadedTrackIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(deezerArlToken) {
        if (arlStatus == null) {
            viewModel.validateDeezerArl(deezerArlToken)
            viewModel.loadAllDeezerUserData(deezerArlToken)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Account Banner + Hero Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = glassBg,
            border = glassBorder
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (arlStatus?.isValid == true) "🎶 ${arlStatus?.userName}" else "🎶 Deezer Guest Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary
                        )
                        Text(
                            text = if (arlStatus?.isValid == true) arlStatus?.message ?: "" else "Connect ARL in Settings for HiFi Lossless",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.loadDeezerFlow(deezerArlToken)
                            if (flowTracks.isNotEmpty()) {
                                viewModel.playDeezerTrack(flowTracks.first(), flowTracks, deezerArlToken) { song, list ->
                                    onSongClick(song, list)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⚡ Play Flow")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Sub-Category Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeezerSubTab.entries.forEach { subTab ->
                val isSelected = (subTab == deezerSubTab)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setDeezerSubTab(subTab) },
                    label = { Text(subTab.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Tab Content List or Search Results
        if (isHubLoading || isSearchingDeezer) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (deezerSearchQuery.isNotBlank()) {
            // Search Mode Content
            when (deezerSubTab) {
                DeezerSubTab.FLOW, DeezerSubTab.SAVED_TRACKS -> {
                    if (deezerSearchResults.isEmpty()) {
                        EmptyState("No Deezer tracks found for '$deezerSearchQuery'")
                    } else {
                        DeezerTrackList(
                            tracks = deezerSearchResults,
                            downloadingTrackId = downloadingTrackId,
                            downloadedTrackIds = downloadedTrackIds,
                            deezerArlToken = deezerArlToken,
                            selectedFolder = selectedFolder,
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            viewModel = viewModel,
                            onSongClick = onSongClick
                        )
                    }
                }
                DeezerSubTab.ALBUMS -> {
                    if (deezerAlbumResults.isEmpty()) {
                        EmptyState("No Deezer albums found for '$deezerSearchQuery'")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(deezerAlbumResults, key = { "dz_sr_alb_${it.id}" }) { album ->
                                DeezerCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    imageUrl = album.coverUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        viewModel.openDeezerAlbum(album.id, album.title)
                                    }
                                )
                            }
                        }
                    }
                }
                DeezerSubTab.ARTISTS -> {
                    if (deezerArtistResults.isEmpty()) {
                        EmptyState("No Deezer artists found for '$deezerSearchQuery'")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(deezerArtistResults, key = { "dz_sr_art_${it.id}" }) { artist ->
                                DeezerCard(
                                    title = artist.name,
                                    subtitle = "Artist",
                                    imageUrl = artist.pictureUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        onArtistClick(artist.name)
                                    }
                                )
                            }
                        }
                    }
                }
                DeezerSubTab.PLAYLISTS -> {
                    if (deezerPlaylistResults.isEmpty()) {
                        EmptyState("No Deezer playlists found for '$deezerSearchQuery'")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(deezerPlaylistResults, key = { "dz_sr_pl_${it.id}" }) { playlist ->
                                DeezerCard(
                                    title = playlist.title,
                                    subtitle = "${playlist.trackCount} tracks • ${playlist.ownerName}",
                                    imageUrl = playlist.coverUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        viewModel.openDeezerPlaylist(playlist.id, playlist.title)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Personal Collections Mode Content
            when (deezerSubTab) {
                DeezerSubTab.FLOW -> {
                    if (flowTracks.isEmpty()) {
                        EmptyState("No Flow tracks available. Tap 'Play Flow' above.")
                    } else {
                        DeezerTrackList(
                            tracks = flowTracks,
                            downloadingTrackId = downloadingTrackId,
                            downloadedTrackIds = downloadedTrackIds,
                            deezerArlToken = deezerArlToken,
                            selectedFolder = selectedFolder,
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            viewModel = viewModel,
                            onSongClick = onSongClick
                        )
                    }
                }
                DeezerSubTab.SAVED_TRACKS -> {
                    if (savedTracks.isEmpty()) {
                        EmptyState("No saved Deezer songs found.")
                    } else {
                        DeezerTrackList(
                            tracks = savedTracks,
                            downloadingTrackId = downloadingTrackId,
                            downloadedTrackIds = downloadedTrackIds,
                            deezerArlToken = deezerArlToken,
                            selectedFolder = selectedFolder,
                            glassBg = glassBg,
                            glassBorder = glassBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            viewModel = viewModel,
                            onSongClick = onSongClick
                        )
                    }
                }
                DeezerSubTab.PLAYLISTS -> {
                    if (userPlaylists.isEmpty()) {
                        EmptyState("No Deezer playlists found.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(userPlaylists, key = { "dz_pl_${it.id}" }) { playlist ->
                                DeezerCard(
                                    title = playlist.title,
                                    subtitle = "${playlist.trackCount} tracks • ${playlist.ownerName}",
                                    imageUrl = playlist.coverUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        viewModel.openDeezerPlaylist(playlist.id, playlist.title)
                                    }
                                )
                            }
                        }
                    }
                }
                DeezerSubTab.ALBUMS -> {
                    if (savedAlbums.isEmpty()) {
                        EmptyState("No saved Deezer albums found.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(savedAlbums, key = { "dz_alb_${it.id}" }) { album ->
                                DeezerCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    imageUrl = album.coverUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        viewModel.openDeezerAlbum(album.id, album.title)
                                    }
                                )
                            }
                        }
                    }
                }
                DeezerSubTab.ARTISTS -> {
                    if (savedArtists.isEmpty()) {
                        EmptyState("No saved Deezer artists found.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(savedArtists, key = { "dz_art_${it.id}" }) { artist ->
                                DeezerCard(
                                    title = artist.name,
                                    subtitle = "Artist",
                                    imageUrl = artist.pictureUrl,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        onArtistClick(artist.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeTitle by viewModel.activeDeezerCollectionTitle.collectAsState()
    val activeTracks by viewModel.activeDeezerCollectionTracks.collectAsState()
    val isCollectionLoading by viewModel.isDeezerCollectionLoading.collectAsState()

    if (activeTitle != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeDeezerCollection() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeTitle ?: "Collection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.closeDeezerCollection() }) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isCollectionLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (activeTracks.isEmpty()) {
                    EmptyState("No tracks found in this collection")
                } else {
                    DeezerTrackList(
                        tracks = activeTracks,
                        downloadingTrackId = downloadingTrackId,
                        downloadedTrackIds = downloadedTrackIds,
                        deezerArlToken = deezerArlToken,
                        selectedFolder = selectedFolder,
                        glassBg = glassBg,
                        glassBorder = glassBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        viewModel = viewModel,
                        onSongClick = onSongClick
                    )
                }
            }
        }
    }
}

@Composable
fun DeezerTrackList(
    tracks: List<com.aurum.data.repository.DeezerTrackSearchResult>,
    downloadingTrackId: String?,
    downloadedTrackIds: List<String>,
    deezerArlToken: String,
    selectedFolder: String?,
    glassBg: Color,
    glassBorder: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks, key = { "dz_tr_${it.id}" }) { track ->
            val isDownloading = downloadingTrackId == track.id
            val isDownloaded = downloadedTrackIds.contains(track.id)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.playDeezerTrack(track, tracks, deezerArlToken) { song, list ->
                            onSongClick(song, list)
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = glassBg,
                border = glassBorder
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
                        if (track.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = track.title, style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Text(text = "${track.artist} • ${track.album}", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }

                    IconButton(
                        onClick = {
                            viewModel.downloadDeezerTrackFile(track, selectedFolder, deezerArlToken)
                        }
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                        } else if (isDownloaded) {
                            Icon(Icons.Default.Check, contentDescription = "Downloaded", tint = Color(0xFF81C784))
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = textPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeezerCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    glassBg: Color,
    glassBorder: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = glassBg,
        border = glassBorder
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = textPrimary, maxLines = 1)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = textSecondary, maxLines = 1)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    LibraryScreen(viewModel = viewModel, onSongClick = onSongClick, onArtistClick = onArtistClick, onOpenDrawer = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    LibraryScreen(viewModel = viewModel, onSongClick = onSongClick, onAlbumClick = onAlbumClick, onOpenDrawer = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onGenreClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    LibraryScreen(viewModel = viewModel, onSongClick = onSongClick, onGenreClick = onGenreClick, onOpenDrawer = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
) {
    LibraryScreen(viewModel = viewModel, onSongClick = { _, _ -> }, onOpenDrawer = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortedLibraryScreen(
    title: String,
    sortBy: SongSortBy,
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
) {
    LibraryScreen(viewModel = viewModel, onSongClick = onSongClick, onOpenDrawer = onBackClick)
}
