package com.aurum.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Song
import com.aurum.player.PlayerState
import com.aurum.ui.common.TopographicContourPlaceholder

@Composable
fun DesktopQueueSidePanel(
    playerState: PlayerState,
    onSongSelect: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val panelBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surfaceContainerLow
    val cardBg = if (isDark) Color(0xFF242830) else MaterialTheme.colorScheme.surfaceContainerHighest
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isDark) Color(0xFFC4CBD4) else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxHeight().width(280.dp),
        color = panelBg,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUEUE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onClearQueue,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    }
                    TextButton(
                        onClick = onClearQueue,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Queue", tint = textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Now Playing Section
            val currentSong = playerState.currentSong
            if (currentSong != null) {
                Text(
                    text = "NOW PLAYING · From Local Music",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth().clickable { onSongSelect(currentSong) }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(4.dp))
                        ) {
                            AurumAsyncImage(
                                model = currentSong.albumArtUri,
                                contentDescription = currentSong.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Up Next Section
            Text(
                text = "UP NEXT",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val queue = playerState.queue
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(queue, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isCurrent = song.id == currentSong?.id
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCurrent) cardBg else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable { onSongSelect(song) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) accentColor else textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveFromQueue(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
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
fun DesktopQueueSidePanelPreview() {
    val sampleSong = Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
    val sampleQueue = listOf(
        sampleSong,
        Song(id = "2", title = "Starlight Echoes", artist = "Nebula Dream", album = "Cosmic Journey", duration = 198000L, uri = "")
    )
    val sampleState = PlayerState(
        currentSong = sampleSong,
        isPlaying = true,
        queue = sampleQueue
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopQueueSidePanel(
            playerState = sampleState,
            onSongSelect = {},
            onRemoveFromQueue = {},
            onClearQueue = {},
            onClose = {}
        )
    }
}
