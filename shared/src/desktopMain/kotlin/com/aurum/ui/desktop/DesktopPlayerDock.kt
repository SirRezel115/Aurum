package com.aurum.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurum.player.PlayerState
import com.aurum.player.RepeatMode
import com.aurum.ui.library.formatDuration
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.player.WaveformProgressBar
import com.aurum.ui.theme.AppFps

@Composable
fun DesktopPlayerDock(
    playerState: PlayerState,
    visualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    visualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    appFps: AppFps = AppFps.FPS_60,
    progressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    onPlayPauseToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onQueueToggle: () -> Unit,
    isQueueOpen: Boolean,
    onOpenNowPlaying: () -> Unit,
    onOpenMetadataFixer: ((com.aurum.data.model.Song) -> Unit)? = null,
    onVolumeChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val dockBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surfaceContainer
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val iconInactive = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val activeAccent = if (isDark) Color(0xFFC4CBD4) else MaterialTheme.colorScheme.primary

    var lastUnmutedVolume by remember { mutableStateOf(0.8f) }
    val currentVolume = playerState.volume
    val isMuted = currentVolume == 0f

    Surface(
        modifier = modifier.fillMaxWidth().height(72.dp),
        color = dockBg,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Song Info & Artwork
            val song = playerState.currentSong
            Row(
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .then(
                            if (song != null && onOpenMetadataFixer != null) {
                                Modifier.clickable { onOpenMetadataFixer(song) }
                            } else {
                                Modifier.clickable { onOpenNowPlaying() }
                            }
                        )
                ) {
                    AurumAsyncImage(
                        model = song?.albumArtUri,
                        contentDescription = song?.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onOpenNowPlaying() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song?.title ?: "No Song Playing",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onFavoriteToggle,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (song.isFavorite) activeAccent else iconInactive,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = song?.artist ?: "Select a song to play",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Center Column: Seek Bar & Controls
            Column(
                modifier = Modifier.weight(2.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.shuffleEnabled) activeAccent else iconInactive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCycleRepeatMode, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) activeAccent else iconInactive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Progress Waveform Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(playerState.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f).height(24.dp)) {
                        WaveformProgressBar(
                            positionMs = playerState.position,
                            durationMs = playerState.duration,
                            onSeek = onSeekTo,
                            isPlaying = playerState.isPlaying,
                            songId = song?.id ?: "desktop_dock",
                            albumArtUri = song?.albumArtUri,
                            albumArtColors = playerState.albumArtColors,
                            waveformAmplitudes = playerState.waveformAmplitudes,
                            fftBands = playerState.fftBands,
                            bassEnergy = playerState.bassEnergy,
                            visualizerStyle = visualizerStyle,
                            visualizerColorScheme = visualizerColorScheme,
                            appFps = appFps,
                            progressDotStyle = progressDotStyle,
                            height = 24.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDuration(playerState.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
            }

            // Right Column: Controls (Visualizer, Queue, Volume)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenNowPlaying, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Visualizer / Now Playing",
                        tint = iconInactive,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onQueueToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isQueueOpen) activeAccent else iconInactive,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (isMuted) {
                            val restoreVol = if (lastUnmutedVolume > 0f) lastUnmutedVolume else 0.8f
                            onVolumeChange(restoreVol)
                        } else {
                            if (currentVolume > 0f) lastUnmutedVolume = currentVolume
                            onVolumeChange(0f)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = iconInactive,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Slider(
                    value = currentVolume,
                    onValueChange = { newVol ->
                        if (newVol > 0f) lastUnmutedVolume = newVol
                        onVolumeChange(newVol)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = activeAccent,
                        activeTrackColor = activeAccent,
                        inactiveTrackColor = activeAccent.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopPlayerDockPreview() {
    val sampleSong = com.aurum.data.model.Song(
        id = "1",
        title = "Midnight Horizon",
        artist = "Aurum Soundscape",
        album = "Celestial Waves",
        duration = 235000L,
        uri = ""
    )
    val sampleState = PlayerState(
        currentSong = sampleSong,
        isPlaying = true,
        position = 78000L,
        duration = 235000L,
        volume = 0.8f
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopPlayerDock(
            playerState = sampleState,
            onPlayPauseToggle = {},
            onPrevious = {},
            onNext = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onCycleRepeatMode = {},
            onFavoriteToggle = {},
            onQueueToggle = {},
            isQueueOpen = false,
            onOpenNowPlaying = {}
        )
    }
}
