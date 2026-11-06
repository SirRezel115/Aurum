package com.aurum.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Song
import com.aurum.ui.common.TopographicContourPlaceholder

/**
 * Compact Neutral Frosted Glass Bottom Queue Card (no blue tint, preserves full screen).
 */
@Composable
fun QueueBottomSheet(
    visible: Boolean,
    queue: List<Song>,
    currentSong: Song?,
    onRemoveFromQueue: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val containerBg = if (isDark) Color(0xF0141418) else Color(0xFBF8F8F6)
    val glassBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val glassBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
    val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF636366)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                color = containerBg,
                border = glassBorder,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(textSecondary.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.aurum.ui.common.RainbowText(
                            text = "Playback Queue (${queue.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = glassBg,
                                border = glassBorder,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (queue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Queue is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(queue, key = { idx, song -> "${song.id}_$idx" }) { index, song ->
                                val isCurrent = song.id == currentSong?.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) (if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)) else glassBg,
                                    border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else glassBorder,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Reorder",
                                            tint = textSecondary,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            if (!song.albumArtUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = song.albumArtUri,
                                                    contentDescription = song.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else textPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = song.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveFromQueue(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun QueueBottomSheetPreview() {
    val sampleSongs = listOf(
        Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = ""),
        Song(id = "2", title = "Starlight Echoes", artist = "Nebula Dream", album = "Cosmic Journey", duration = 198000L, uri = "")
    )
    com.aurum.ui.theme.AurumTheme {
        QueueBottomSheet(
            visible = true,
            queue = sampleSongs,
            currentSong = sampleSongs[0],
            onRemoveFromQueue = {},
            onDismiss = {}
        )
    }
}
