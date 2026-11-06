package com.aurum.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurum.data.model.Playlist
import com.aurum.data.model.Song
import com.aurum.ui.library.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopSongsTableView(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    playlists: List<Playlist> = emptyList(),
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (playlistId: String, songId: String) -> Unit = { _, _ -> },
    onDeleteSong: (Song) -> Unit = {},
    onEditMetadata: ((Song) -> Unit)? = null,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val textMuted = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val headerBg = if (isDark) Color(0xFF1A1C22) else MaterialTheme.colorScheme.surfaceContainerHigh
    val activeRowBg = if (isDark) Color(0xFF2A2E38) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    val hoverRowBg = if (isDark) Color(0xFF242830) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    val formattedTotalDuration = remember(totalDurationMs) {
        val totalHours = totalDurationMs / (1000 * 60 * 60)
        val totalMins = (totalDurationMs % (1000 * 60 * 60)) / (1000 * 60)
        "${totalHours}h ${totalMins}m"
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Golden Ring Logo Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF242830) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "LOCAL MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "${songs.size} songs • $formattedTotalDuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayAllClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFC4CBD4) else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying && currentSong != null) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pause / Play All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying && currentSong != null) "Pause" else "Play All", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShuffleAllClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Table Column Headers (#, Title, Artist, Album, Date added, Length, Actions)
        Surface(
            color = headerBg,
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.width(32.dp))
                Text("Title", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.weight(3.2f))
                Text("Artist", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.weight(2f))
                Text("Album", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.weight(2.3f))
                Text("Date added", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.weight(1.8f))
                Text("Length", style = MaterialTheme.typography.labelSmall, color = textMuted, modifier = Modifier.width(50.dp))
                Spacer(modifier = Modifier.width(64.dp))
            }
        }

        HorizontalDivider(color = dividerColor)

        // Table Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                val isCurrent = song.id == currentSong?.id
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                var menuExpanded by remember { mutableStateOf(false) }

                val formattedDate = remember(song.dateAdded) {
                    if (song.dateAdded > 0) {
                        dateFormat.format(Date(song.dateAdded * 1000L))
                    } else "Sep 26, 2026"
                }

                val rowColor = when {
                    isCurrent -> activeRowBg
                    isHovered -> hoverRowBg
                    else -> Color.Transparent
                }

                Surface(
                    color = rowColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverable(interactionSource)
                        .clickable { onSongClick(song, songs) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else textMuted,
                            modifier = Modifier.width(32.dp)
                        )

                        Row(
                            modifier = Modifier.weight(3.2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                            ) {
                                AurumAsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2f)
                        )

                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2.3f)
                        )

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.8f)
                        )

                        Text(
                            text = formatDuration(song.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            modifier = Modifier.width(50.dp)
                        )

                        // Action Buttons: Favorite Heart + More Options Menu
                        Row(
                            modifier = Modifier.width(64.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onFavoriteToggle(song) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Play Next") },
                                        leadingIcon = { Icon(Icons.Default.QueuePlayNext, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onPlayNext(song)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Add to Queue") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onAddToQueue(song)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (song.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                        leadingIcon = { Icon(if (song.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onFavoriteToggle(song)
                                        }
                                    )
                                    if (playlists.isNotEmpty()) {
                                        playlists.forEach { pl ->
                                            DropdownMenuItem(
                                                text = { Text("Add to ${pl.name}") },
                                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onAddToPlaylist(pl.id, song.id)
                                                }
                                            )
                                        }
                                    }
                                    if (onEditMetadata != null) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Metadata & Search HD Artwork") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onEditMetadata(song)
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete Song", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteSong(song)
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
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopSongsTableViewPreview() {
    val sampleSongs = listOf(
        Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = ""),
        Song(id = "2", title = "Starlight Echoes", artist = "Nebula Dream", album = "Cosmic Journey", duration = 198000L, uri = ""),
        Song(id = "3", title = "Golden Hour", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 210000L, uri = "")
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopSongsTableView(
            songs = sampleSongs,
            currentSong = sampleSongs[0],
            isPlaying = true,
            onSongClick = { _, _ -> },
            onPlayAllClick = {},
            onShuffleAllClick = {}
        )
    }
}
