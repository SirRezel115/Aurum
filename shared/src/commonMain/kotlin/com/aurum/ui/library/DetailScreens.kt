package com.aurum.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.aurum.data.repository.toSong
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import com.aurum.player.MusicPlayer
import com.aurum.player.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumTitle: String,
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
    onOpenMetadataFixerForSong: (Song) -> Unit = {},
    onOpenMetadataFixerForAlbum: (Album) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDeezerTracks by viewModel.activeDeezerCollectionTracks.collectAsState()
    val isCollectionLoading by viewModel.isDeezerCollectionLoading.collectAsState()

    val successState = (uiState as? LibraryUiState.Success)
    val album = successState?.albums?.find { it.title.equals(albumTitle, ignoreCase = true) }
    val localAlbumSongs = successState?.allSongs?.filter { it.album.equals(albumTitle, ignoreCase = true) } ?: emptyList()

    LaunchedEffect(albumTitle) {
        if (localAlbumSongs.isEmpty()) {
            viewModel.openDeezerAlbum(album?.id ?: "", albumTitle)
        }
    }

    val albumSongs = if (localAlbumSongs.isNotEmpty()) {
        localAlbumSongs
    } else {
        activeDeezerTracks.map { it.toSong() }
    }

    // Sophisticated, moody, elegant Apple Music background tone engine
    val themeColors = remember(albumTitle, album, albumSongs) {
        val artColors = album?.albumArtColors?.takeIf { it.isNotEmpty() }
        val dominantColor = if (!artColors.isNullOrEmpty()) Color(artColors.first()) else {
            val seed = albumTitle.hashCode()
            val hue = (kotlin.math.abs(seed) % 360).toFloat()
            Color.hsl(hue, 0.6f, 0.4f)
        }

        val luminance = 0.299f * dominantColor.red + 0.587f * dominantColor.green + 0.114f * dominantColor.blue
        val isLight = luminance > 0.55f

        val sophisticatedBg = if (isLight) {
            Color(
                red = (dominantColor.red * 0.2f + 0.8f).coerceIn(0f, 1f),
                green = (dominantColor.green * 0.2f + 0.8f).coerceIn(0f, 1f),
                blue = (dominantColor.blue * 0.2f + 0.75f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = (dominantColor.red * 0.28f).coerceIn(0.04f, 0.32f),
                green = (dominantColor.green * 0.28f).coerceIn(0.04f, 0.32f),
                blue = (dominantColor.blue * 0.28f).coerceIn(0.04f, 0.35f),
                alpha = 1f
            )
        }

        if (isLight) {
            Triple(sophisticatedBg, Color.Black, Color.DarkGray)
        } else {
            Triple(sophisticatedBg, Color.White, Color.White.copy(alpha = 0.7f))
        }
    }

    val backgroundColor = themeColors.first
    val textColor = themeColors.second
    val secondaryTextColor = themeColors.third
    val isLight = 0.299f * backgroundColor.red + 0.587f * backgroundColor.green + 0.114f * backgroundColor.blue > 0.55f
    val iconTint = if (isLight) Color.Black else Color.White
    val glassBg = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
    val glassBorder = if (isLight) BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    val playBtnBg = if (isLight) Color.Black else Color.White
    val playBtnFg = if (isLight) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share action */ }) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = glassBg,
                            border = glassBorder,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { /* More actions */ }) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = glassBg,
                            border = glassBorder,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = backgroundColor,
    ) { padding ->
        val artUri = album?.albumArtUri ?: albumSongs.firstOrNull()?.albumArtUri

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!artUri.isNullOrBlank()) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.5f }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black
                                )
                            )
                        )
                )
            }

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
                    if (isCollectionLoading && albumSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val totalDurationMs = remember(albumSongs) { albumSongs.sumOf { it.duration } }
                        val totalMins = totalDurationMs / (1000 * 60)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                // Prominent Album Cover Art - Left Clicking opens Art/Metadata Fixer!
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .shadow(16.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            if (album != null) {
                                                onOpenMetadataFixerForAlbum(album)
                                            } else if (albumSongs.isNotEmpty()) {
                                                onOpenMetadataFixerForSong(albumSongs.first())
                                            }
                                        }
                                ) {
                                    if (!artUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = artUri,
                                            contentDescription = albumTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        com.aurum.ui.common.TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = albumTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${album?.artist ?: albumSongs.firstOrNull()?.artist ?: "Artist"} • ${albumSongs.size} tracks${if (totalMins > 0) " • ${totalMins}m" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = secondaryTextColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pop • 2023 • Dolby Atmos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                // Metadata description snippet matching reference image style
                                Text(
                                    text = "For the most part, $albumTitle is a captivating musical journey exploring depth, rhythm, and emotion.  MORE",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                if (albumSongs.isNotEmpty()) {
                                    // Apple Music Action Buttons Row: Shuffle, Play, Add (+)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Shuffle Button
                                        Surface(
                                            onClick = {
                                                val shuffled = albumSongs.shuffled()
                                                if (shuffled.isNotEmpty()) {
                                                    onSongClick(shuffled.first(), shuffled)
                                                }
                                            },
                                            shape = RoundedCornerShape(23.dp),
                                            color = glassBg,
                                            border = glassBorder,
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Shuffle,
                                                    contentDescription = "Shuffle",
                                                    tint = iconTint,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Play Button (Pill)
                                        Button(
                                            onClick = { onSongClick(albumSongs.first(), albumSongs) },
                                            colors = ButtonDefaults.buttonColors(containerColor = playBtnBg),
                                            shape = RoundedCornerShape(23.dp),
                                            modifier = Modifier
                                                .height(46.dp)
                                                .weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = playBtnFg)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Play", style = MaterialTheme.typography.titleMedium, color = playBtnFg)
                                        }

                                        // Add (+) Button
                                        Surface(
                                            onClick = { /* Add to library/playlist */ },
                                            shape = RoundedCornerShape(23.dp),
                                            color = glassBg,
                                            border = glassBorder,
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    tint = iconTint,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        itemsIndexed(albumSongs, key = { _, song -> song.id }) { index, song ->
                            SongItemRow(
                                song = song,
                                trackNumber = index + 1,
                                showThumbnail = false,
                                isGlass = true,
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                onPlayNext = { viewModel.playNext(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onEditMetadata = { onOpenMetadataFixerForSong(song) },
                                onRepairArtwork = { viewModel.repairArtworkForAlbum(song.artist, song.album) },
                                onClick = { onSongClick(song, albumSongs) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenMetadataFixerForSong: (Song) -> Unit = {},
    onOpenMetadataFixerForAlbum: (Album) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { com.aurum.ui.common.RainbowText(artistName, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
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
                    val deezerSearchResults by viewModel.deezerSearchResults.collectAsState()
                    val deezerAlbumResults by viewModel.deezerAlbumResults.collectAsState()
                    var onlineArtistAlbums by remember(artistName) { mutableStateOf<List<Album>>(emptyList()) }

                    LaunchedEffect(artistName) {
                        viewModel.searchDeezer(artistName)
                        try {
                            val fetched = com.aurum.data.repository.DeezerMusicProvider.getDeezerArtistAlbums(artistName)
                            if (fetched.isNotEmpty()) {
                                onlineArtistAlbums = fetched.map {
                                    Album(id = it.id, title = it.title, artist = it.artist, albumArtUri = it.coverUrl, trackCount = it.trackCount)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    val artistSongs = remember(state.allSongs, artistName) {
                        state.allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                    }
                    val artistAlbums = remember(state.albums, artistName) {
                        state.albums.filter { it.artist.equals(artistName, ignoreCase = true) }
                    }

                    val onlineSongs = remember(deezerSearchResults, artistName) {
                        deezerSearchResults
                            .filter { it.artist.equals(artistName, ignoreCase = true) }
                            .ifEmpty { deezerSearchResults }
                            .map { it.toSong() }
                    }
                    val onlineAlbums = remember(deezerAlbumResults, onlineArtistAlbums, artistName) {
                        if (onlineArtistAlbums.isNotEmpty()) {
                            onlineArtistAlbums
                        } else {
                            deezerAlbumResults.map {
                                Album(id = it.id, title = it.title, artist = it.artist, albumArtUri = it.coverUrl, trackCount = it.trackCount)
                            }
                        }
                    }

                    val effectiveSongs = remember(artistSongs, onlineSongs) {
                        val set = mutableSetOf<String>()
                        val list = mutableListOf<Song>()
                        artistSongs.forEach {
                            val key = "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}"
                            if (set.add(key)) list.add(it)
                        }
                        onlineSongs.forEach {
                            val key = "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}"
                            if (set.add(key)) list.add(it)
                        }
                        list
                    }

                    val effectiveAlbums = remember(artistAlbums, onlineAlbums) {
                        val set = mutableSetOf<String>()
                        val list = mutableListOf<Album>()
                        artistAlbums.forEach {
                            val key = it.title.lowercase().trim()
                            if (set.add(key)) list.add(it)
                        }
                        onlineAlbums.forEach {
                            val key = it.title.lowercase().trim()
                            if (set.add(key)) list.add(it)
                        }
                        list
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(60.dp)),
                                    color = glassBg,
                                    border = glassBorder
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = artistName.take(1).uppercase(),
                                            style = MaterialTheme.typography.displayLarge,
                                            color = textPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${effectiveSongs.size} tracks • ${effectiveAlbums.size} albums",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (effectiveSongs.isNotEmpty()) {
                                    Button(
                                        onClick = { onSongClick(effectiveSongs.first(), effectiveSongs) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(23.dp),
                                        modifier = Modifier.height(46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play Songs", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                    }
                                }
                            }
                        }

                        if (effectiveAlbums.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textPrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(effectiveAlbums, key = { "album_${it.id}" }) { album ->
                                val albumSongs = effectiveSongs.filter { it.album == album.title }
                                AlbumItemRow(
                                    album = album,
                                    onPlayAlbum = {
                                        if (albumSongs.isNotEmpty()) {
                                            viewModel.playSong(albumSongs.first(), albumSongs)
                                        } else {
                                            onAlbumClick(album.title)
                                        }
                                    },
                                    onPlayNext = {
                                        albumSongs.forEach { viewModel.playNext(it) }
                                    },
                                    onAddToQueue = {
                                        albumSongs.forEach { viewModel.addToQueue(it) }
                                    },
                                    onEditMetadata = { onOpenMetadataFixerForAlbum(album) },
                                    onClick = { onAlbumClick(album.title) }
                                )
                            }
                        }

                        if (effectiveSongs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "All Songs",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textPrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(effectiveSongs, key = { "artist_song_${it.id}" }) { song ->
                                SongItemRow(
                                    song = song,
                                    isGlass = true,
                                    onFavoriteClick = { viewModel.toggleFavorite(song) },
                                    onPlayNext = { viewModel.playNext(song) },
                                    onAddToQueue = { viewModel.addToQueue(song) },
                                    onEditMetadata = { onOpenMetadataFixerForSong(song) },
                                    onClick = { onSongClick(song, effectiveSongs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItemRow(
    album: Album,
    onPlayAlbum: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onEditMetadata: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
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
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.title, style = MaterialTheme.typography.titleMedium, color = textPrimary)
                    Text("${album.trackCount} tracks", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = menuOffset
        ) {
            DropdownMenuItem(
                text = { Text("Play Album") },
                onClick = {
                    showMenu = false
                    onPlayAlbum()
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    genreName: String,
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onBackClick: () -> Unit,
    onOpenMetadataFixerForSong: (Song) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { com.aurum.ui.common.RainbowText(genreName, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
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
                    val genreSongs = state.allSongs.filter { song ->
                        val lower = song.title.lowercase() + " " + song.album.lowercase() + " " + song.artist.lowercase()
                        when (genreName) {
                            "Rock" -> lower.contains("rock")
                            "Pop" -> lower.contains("pop")
                            "Hip-Hop" -> lower.contains("hip") || lower.contains("rap")
                            "Jazz & Blues" -> lower.contains("jazz") || lower.contains("blues")
                            "Electronic" -> lower.contains("dance") || lower.contains("edm") || lower.contains("electro")
                            "Acoustic / Folk" -> lower.contains("acoustic") || lower.contains("folk")
                            "Indie / Alternative" -> lower.contains("indie") || lower.contains("alt")
                            else -> true
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    color = glassBg,
                                    border = glassBorder
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = genreName,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = textPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "${genreSongs.size} tracks in $genreName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (genreSongs.isNotEmpty()) {
                                    Button(
                                        onClick = { onSongClick(genreSongs.first(), genreSongs) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(23.dp),
                                        modifier = Modifier.height(46.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Play Genre", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                    }
                                }
                            }
                        }

                        items(genreSongs, key = { it.id }) { song ->
                            SongItemRow(
                                song = song,
                                isGlass = true,
                                onFavoriteClick = { viewModel.toggleFavorite(song) },
                                onPlayNext = { viewModel.playNext(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onEditMetadata = { onOpenMetadataFixerForSong(song) },
                                onClick = { onSongClick(song, genreSongs) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun AlbumDetailScreenPreview() {
    val fakeRepository = object : com.aurum.data.repository.MusicRepository {
        override suspend fun getSongs(): List<Song> = listOf(
            Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
        )
        override suspend fun getAlbums(): List<Album> = listOf(
            Album(id = "1", title = "Celestial Waves", artist = "Aurum Soundscape", trackCount = 12)
        )
        override suspend fun getArtists(): List<Artist> = emptyList()
        override suspend fun searchSongs(query: String): List<Song> = emptyList()
        override suspend fun rescan() {}
        override suspend fun toggleFavorite(songId: String): Boolean = false
        override suspend fun updateAlbumMetadata(oldAlbumTitle: String, newTitle: String, newArtist: String) {}
        override suspend fun updateAlbumMetadataAndArtwork(oldAlbumTitle: String, newTitle: String, newArtist: String, artworkUri: String) {}
        override suspend fun updateSongMetadataAndArtwork(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUri: String) {}
        override suspend fun mergeAlbums(sourceAlbumTitle: String, targetAlbumTitle: String, targetArtist: String, artworkUri: String?) {}
        override suspend fun deleteSong(songId: String) {}
        override suspend fun getPlaylists(): List<Playlist> = emptyList()
        override suspend fun createPlaylist(name: String): Playlist = Playlist("preview", name)
        override suspend fun deletePlaylist(id: String): Boolean = true
        override suspend fun addSongToPlaylist(playlistId: String, songId: String): Boolean = true
        override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean = true
    }
    val fakePlayer = object : MusicPlayer {
        override val state: State<PlayerState> = mutableStateOf(PlayerState())
        override suspend fun play(song: Song) {}
        override suspend fun play(songs: List<Song>, startIndex: Int) {}
        override suspend fun playNext(song: Song) {}
        override suspend fun playNext(songs: List<Song>) {}
        override suspend fun addToQueue(song: Song) {}
        override suspend fun addToQueue(songs: List<Song>) {}
        override suspend fun pause() {}
        override suspend fun resume() {}
        override suspend fun seekTo(positionMs: Long) {}
        override suspend fun next() {}
        override suspend fun previous() {}
        override suspend fun stop() {}
        override fun toggleShuffle() {}
        override fun cycleRepeatMode() {}
    }
    val previewViewModel = remember { LibraryViewModel(fakeRepository, fakePlayer) }
    com.aurum.ui.theme.AurumTheme {
        AlbumDetailScreen(albumTitle = "Celestial Waves", viewModel = previewViewModel, onSongClick = { _, _ -> }, onBackClick = {})
    }
}
