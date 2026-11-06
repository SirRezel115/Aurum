package com.aurum.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Song
import com.aurum.data.repository.DeezerTrackSearchResult
import com.aurum.ui.common.TopographicContourPlaceholder
import com.aurum.ui.library.LibraryUiState
import com.aurum.ui.library.LibraryViewModel
import com.aurum.ui.library.SongItemRow

enum class SearchProviderMode {
    LOCAL, DEEZER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: LibraryViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    selectedFolder: String? = null,
    deezerArlToken: String? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val deezerResults by viewModel.deezerSearchResults.collectAsState()
    val isSearchingDeezer by viewModel.isSearchingDeezer.collectAsState()
    val downloadingTrackId by viewModel.downloadingTrackId.collectAsState()

    var searchMode by remember { mutableStateOf(SearchProviderMode.LOCAL) }
    var deezerQuery by remember { mutableStateOf("") }
    val downloadedTrackIds = remember { mutableStateListOf<String>() }

    val keyboardController = LocalSoftwareKeyboardController.current

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
                title = { com.aurum.ui.common.RainbowText("Search", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        com.aurum.ui.common.RainbowIcon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Provider Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = searchMode == SearchProviderMode.LOCAL,
                        onClick = { searchMode = SearchProviderMode.LOCAL },
                        label = { Text("Local Library") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = searchMode == SearchProviderMode.DEEZER,
                        onClick = { searchMode = SearchProviderMode.DEEZER },
                        label = { Text("🎶 Deezer Music Files") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (searchMode == SearchProviderMode.LOCAL) {
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
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(glassBg),
                                placeholder = { Text("Search local tracks, artists, albums...", color = textSecondary) },
                                leadingIcon = { com.aurum.ui.common.RainbowIcon(Icons.Default.Search, contentDescription = null) },
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
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.filteredSongs, key = { it.id }) { song ->
                                    SongItemRow(
                                        song = song,
                                        isGlass = true,
                                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                                        onPlayNext = { viewModel.playNext(song) },
                                        onAddToQueue = { viewModel.addToQueue(song) },
                                        onClick = {
                                            keyboardController?.hide()
                                            onSongClick(song, state.filteredSongs)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Deezer Search Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = deezerQuery,
                            onValueChange = {
                                deezerQuery = it
                                if (!it.contains("deezer.com") && !it.contains("dzr.page.link")) {
                                    viewModel.searchDeezer(it)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(glassBg),
                            placeholder = { Text("Search Deezer or paste link...", color = textSecondary) },
                            leadingIcon = { com.aurum.ui.common.RainbowIcon(Icons.Default.Search, contentDescription = null) },
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
                        if (deezerQuery.contains("deezer.com") || deezerQuery.contains("dzr.page.link") || deezerQuery.contains("deezer://")) {
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.importDeezerLink(deezerQuery, deezerArlToken)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Import")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSearchingDeezer) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (deezerResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (deezerQuery.isBlank()) "Type to search Deezer music files" else "No Deezer music files found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deezerResults, key = { "deezer_${it.id}" }) { track ->
                                val isDownloading = downloadingTrackId == track.id
                                val isDownloaded = downloadedTrackIds.contains(track.id)

                                DeezerTrackItemCard(
                                    track = track,
                                    isDownloading = isDownloading,
                                    isDownloaded = isDownloaded,
                                    glassBg = glassBg,
                                    glassBorder = glassBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onItemClick = {
                                        keyboardController?.hide()
                                        viewModel.playDeezerTrack(
                                            track = track,
                                            playlist = deezerResults,
                                            arlToken = deezerArlToken,
                                            onReady = { song, playlist ->
                                                onSongClick(song, playlist)
                                            }
                                        )
                                    },
                                    onDownloadClick = {
                                        viewModel.downloadDeezerTrackFile(
                                            track = track,
                                            downloadDir = selectedFolder,
                                            arlToken = deezerArlToken
                                        ) { success, _ ->
                                            if (success) {
                                                downloadedTrackIds.add(track.id)
                                            }
                                        }
                                    }
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
private fun DeezerTrackItemCard(
    track: DeezerTrackSearchResult,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    glassBg: Color,
    glassBorder: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    onItemClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        color = glassBg,
        border = glassBorder
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (track.artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} · ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (isDownloaded) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Music File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
