package com.aurum.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Song
import com.aurum.data.repository.AppleMusicSearchResult
import com.aurum.ui.common.RainbowText
import com.aurum.ui.common.TopographicContourPlaceholder
import com.aurum.ui.library.LibraryUiState
import com.aurum.ui.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.appleMusicResults.collectAsState()
    val isSearching by viewModel.isSearchingAppleMusic.collectAsState()
    val editingTargetSong by viewModel.editingTargetSong.collectAsState()
    val editingTargetAlbum by viewModel.editingTargetAlbum.collectAsState()
    val editingTargetArtist by viewModel.editingTargetArtist.collectAsState()

    val artistDiscography by viewModel.artistDiscography.collectAsState()
    val isLoadingDiscography by viewModel.isLoadingDiscography.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedEntityType by remember { mutableStateOf("album") } // "album", "song", "artist"

    var selectedResultForApply by remember { mutableStateOf<AppleMusicSearchResult?>(null) }
    var selectedArtistForDiscography by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(searchResults) {
        if (searchResults.isNotEmpty()) {
            val first = searchResults.first()
            selectedEntityType = first.type
            if (query.isBlank()) {
                query = if (first.type == "song") first.title else first.album.ifBlank { first.title }
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RainbowText("Metadata & Artwork Editor", style = MaterialTheme.typography.titleLarge) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = bgColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val activeTargetName = when {
                    editingTargetSong != null -> "Song: ${editingTargetSong!!.title}"
                    editingTargetAlbum != null -> "Album: ${editingTargetAlbum!!.title}"
                    editingTargetArtist != null -> "Artist: ${editingTargetArtist!!.name}"
                    else -> null
                }

                if (activeTargetName != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Target Media: $activeTargetName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Tap 'Apply' on any search result to update this target in 1 tap", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Search Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Search Apple Music API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        // Search Input
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search by album, track, or artist...", color = textSecondary) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Entity Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("album" to "Albums", "song" to "Songs", "artist" to "Artists").forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedEntityType == type,
                                    onClick = {
                                        selectedEntityType = type
                                        if (query.isNotBlank()) viewModel.searchAppleMusic(query, type)
                                    },
                                    label = { Text(label) },
                                    leadingIcon = if (selectedEntityType == type) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Search Action Button
                        Button(
                            onClick = { viewModel.searchAppleMusic(query, selectedEntityType) },
                            enabled = query.isNotBlank() && !isSearching,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching Apple Music...")
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search Apple Music API", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apple Music Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "${searchResults.size} matches",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Results List
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSearching) "Querying Apple Music..." else "Type a query and press Search to fetch 1000x1000 HD artwork",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults, key = { it.id }) { result ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = glassBg,
                                border = glassBorder,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(if (result.type == "artist") RoundedCornerShape(32.dp) else RoundedCornerShape(8.dp))
                                    ) {
                                        if (result.artworkUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = result.artworkUrl,
                                                contentDescription = result.artist,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = result.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (result.album.isNotBlank() && result.album != result.title) {
                                            Text(
                                                text = result.album,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val targetSong = editingTargetSong
                                    val targetAlbum = editingTargetAlbum
                                    val targetArtist = editingTargetArtist

                                    if (targetSong != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.applyMetadataToSong(
                                                        songId = targetSong.id,
                                                        newTitle = result.title,
                                                        newArtist = result.artist,
                                                        newAlbum = result.album,
                                                        artworkUrl = result.artworkUrl
                                                    )
                                                    snackbarMessage = "Applied artwork & metadata to '${targetSong.title}'!"
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Apply Song", style = MaterialTheme.typography.labelSmall)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.applyMetadataToSongAndAlbum(
                                                        songId = targetSong.id,
                                                        newTitle = result.title,
                                                        newArtist = result.artist,
                                                        newAlbum = result.album,
                                                        artworkUrl = result.artworkUrl
                                                    )
                                                    snackbarMessage = "Applied artwork & metadata to '${targetSong.title}' & Full Album '${result.album}'!"
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Apply Full Album", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } else if (targetAlbum != null) {
                                        Button(
                                            onClick = {
                                                viewModel.applyMetadataToAlbum(
                                                    oldAlbumTitle = targetAlbum.title,
                                                    newTitle = result.album.ifBlank { result.title },
                                                    newArtist = result.artist,
                                                    artworkUrl = result.artworkUrl
                                                )
                                                snackbarMessage = "Applied 1000x1000 artwork & metadata to album '${targetAlbum.title}'!"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Apply Album", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else if (targetArtist != null) {
                                        Button(
                                            onClick = {
                                                viewModel.applyMetadataToArtist(
                                                    oldArtistName = targetArtist.name,
                                                    newArtistName = result.artist.ifBlank { result.title },
                                                    artworkUrl = result.artworkUrl
                                                )
                                                snackbarMessage = "Applied 1000x1000 photo to artist '${targetArtist.name}'!"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Apply Artist Photo", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        Button(
                                            onClick = { selectedResultForApply = result },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Apply", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Artist Discography Sheet
    val discographyArtist = selectedArtistForDiscography
    if (discographyArtist != null) {
        ArtistDiscographySheet(
            artistName = discographyArtist,
            discography = artistDiscography,
            isLoading = isLoadingDiscography,
            onDismiss = { selectedArtistForDiscography = null },
            onApplyRelease = { release ->
                val targetSong = editingTargetSong
                val targetAlbum = editingTargetAlbum
                val targetArtist = editingTargetArtist

                if (targetSong != null) {
                    viewModel.applyMetadataToSong(
                        songId = targetSong.id,
                        newTitle = targetSong.title,
                        newArtist = release.artist,
                        newAlbum = release.album,
                        artworkUrl = release.artworkUrl
                    )
                    snackbarMessage = "Applied 1000x1000 artwork & metadata to '${targetSong.title}'!"
                } else if (targetAlbum != null) {
                    viewModel.applyMetadataToAlbum(
                        oldAlbumTitle = targetAlbum.title,
                        newTitle = release.album.ifBlank { targetAlbum.title },
                        newArtist = release.artist,
                        artworkUrl = release.artworkUrl
                    )
                    snackbarMessage = "Applied 1000x1000 artwork & metadata to album '${targetAlbum.title}'!"
                } else if (targetArtist != null) {
                    viewModel.applyMetadataToArtist(
                        oldArtistName = targetArtist.name,
                        newArtistName = release.artist,
                        artworkUrl = release.artworkUrl
                    )
                    snackbarMessage = "Applied 1000x1000 photo to artist '${targetArtist.name}'!"
                } else {
                    selectedResultForApply = release
                }
                selectedArtistForDiscography = null
            }
        )
    }

    // Apply to Local Library Target Selection Sheet
    val applyResult = selectedResultForApply
    if (applyResult != null && uiState is LibraryUiState.Success) {
        val successState = uiState as LibraryUiState.Success
        ApplyTargetSelectionSheet(
            result = applyResult,
            songs = successState.allSongs,
            albums = successState.albums,
            artists = successState.artists,
            onDismiss = { selectedResultForApply = null },
            onApplyToSong = { song ->
                viewModel.applyMetadataToSong(
                    songId = song.id,
                    newTitle = applyResult.title,
                    newArtist = applyResult.artist,
                    newAlbum = applyResult.album,
                    artworkUrl = applyResult.artworkUrl
                )
                snackbarMessage = "Applied 1000x1000 artwork & metadata to song '${song.title}'!"
                selectedResultForApply = null
            },
            onApplyToAlbum = { album ->
                viewModel.applyMetadataToAlbum(
                    oldAlbumTitle = album.title,
                    newTitle = applyResult.album.ifBlank { applyResult.title },
                    newArtist = applyResult.artist,
                    artworkUrl = applyResult.artworkUrl
                )
                snackbarMessage = "Applied 1000x1000 artwork & metadata to album '${album.title}'!"
                selectedResultForApply = null
            },
            onApplyToArtist = { artist ->
                viewModel.applyMetadataToArtist(
                    oldArtistName = artist.name,
                    newArtistName = applyResult.artist.ifBlank { applyResult.title },
                    artworkUrl = applyResult.artworkUrl
                )
                snackbarMessage = "Applied 1000x1000 artwork & metadata to artist '${artist.name}'!"
                selectedResultForApply = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistDiscographySheet(
    artistName: String,
    discography: List<AppleMusicSearchResult>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onApplyRelease: (AppleMusicSearchResult) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColors = if (isDark) {
        listOf(Color(0xFF0D0E15), Color(0xFF141622), Color(0xFF090A0E))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F2), Color(0xFFF3EFE6))
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = bgColors))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artistName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Apple Music Discography (${discography.size} releases)", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (discography.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No releases found for $artistName", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(discography, key = { "disco_${it.id}" }) { release ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                        if (release.artworkUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = release.artworkUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(release.album.ifBlank { release.title }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(release.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onApplyRelease(release) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Apply 1000x1000")
                                    }
                                }
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
private fun ApplyTargetSelectionSheet(
    result: AppleMusicSearchResult,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    onDismiss: () -> Unit,
    onApplyToSong: (Song) -> Unit,
    onApplyToAlbum: (Album) -> Unit,
    onApplyToArtist: (Artist) -> Unit
) {
    val initialTab = remember(result.type) {
        when (result.type.lowercase()) {
            "song" -> 1      // Local Songs
            "artist" -> 2    // Local Artists
            else -> 0        // Local Albums
        }
    }
    var activeTab by remember { mutableStateOf(initialTab) }

    val initialFilter = remember(result) {
        when (result.type.lowercase()) {
            "song" -> result.title.ifBlank { result.artist }
            "artist" -> result.artist.ifBlank { result.title }
            else -> result.album.ifBlank { result.title }
        }
    }
    var filterQuery by remember { mutableStateOf(initialFilter) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColors = if (isDark) {
        listOf(Color(0xFF0D0E15), Color(0xFF141622), Color(0xFF090A0E))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F2), Color(0xFFF3EFE6))
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = bgColors))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Select Local Target File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Pick item in your library to apply 1000x1000 HD artwork", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Apple Music Selected Result Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (result.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = result.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(result.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${result.artist} • ${result.album}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Tab Switcher with 3 tabs: Local Albums, Local Songs, Local Artists
                SecondaryTabRow(selectedTabIndex = activeTab) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Local Albums (${albums.size})") })
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Local Songs (${songs.size})") })
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Local Artists (${artists.size})") })
                }

                // Search/Filter Bar
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filter by title, artist, or file path...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterQuery.isNotBlank()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // List of local items
                when (activeTab) {
                    0 -> {
                        val filteredAlbums = albums.filter {
                            filterQuery.isBlank() || it.title.contains(filterQuery, ignoreCase = true) || it.artist.contains(filterQuery, ignoreCase = true)
                        }
                        if (filteredAlbums.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No matching local albums found", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredAlbums, key = { "album_${it.id}" }) { album ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(album.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${album.artist} • ${album.trackCount} tracks", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onApplyToAlbum(album) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Apply to This Album")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val filteredSongs = songs.filter {
                            filterQuery.isBlank() ||
                            it.title.contains(filterQuery, ignoreCase = true) ||
                            it.artist.contains(filterQuery, ignoreCase = true) ||
                            it.album.contains(filterQuery, ignoreCase = true) ||
                            it.uri.contains(filterQuery, ignoreCase = true)
                        }
                        if (filteredSongs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No matching local songs found", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredSongs, key = { "song_${it.id}" }) { song ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${song.artist} • ${song.album}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                if (song.uri.isNotBlank()) {
                                                    Text(
                                                        text = "File: ${song.uri}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onApplyToSong(song) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Apply to This File")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        val filteredArtists = artists.filter {
                            filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true)
                        }
                        if (filteredArtists.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No matching local artists found", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredArtists, key = { "artist_${it.id}" }) { artist ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(artist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${artist.trackCount} tracks", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { onApplyToArtist(artist) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Apply to This Artist")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
