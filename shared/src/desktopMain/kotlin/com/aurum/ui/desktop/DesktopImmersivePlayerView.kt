package com.aurum.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.player.PlayerState
import com.aurum.player.RepeatMode
import com.aurum.ui.library.formatDuration
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.player.WaveformProgressBar
import com.aurum.ui.theme.AppFps

@Composable
fun DesktopImmersivePlayerView(
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
    onBackClick: () -> Unit,
    onOpenMetadataFixer: ((com.aurum.data.model.Song) -> Unit)? = null,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val song = playerState.currentSong

    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF081218))
    ) {
        // Ambient Background Image & Gradient Overlay
        if (song != null && !song.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(90.dp).opacity(0.3f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A141A).copy(alpha = 0.7f),
                            Color(0xFF060B0E).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Main 2-Column Content
        Row(
            modifier = Modifier.fillMaxSize().padding(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Large Artwork, Title & Controls
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Artist Header in Caps above cover
                Text(
                    text = (song?.artist ?: "CARPETMAN").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .sizeIn(maxWidth = 340.dp, maxHeight = 340.dp)
                        .aspectRatio(1f)
                        .shadow(28.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .then(
                            if (song != null && onOpenMetadataFixer != null) {
                                Modifier.clickable { onOpenMetadataFixer(song) }
                            } else Modifier
                        )
                ) {
                    AurumAsyncImage(
                        model = song?.albumArtUri,
                        contentDescription = song?.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Heart Outline Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song?.title ?: "Smoking Cherry",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (song != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) Color(0xFFC4CBD4) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = song?.artist ?: "Carpetman",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5CC5DD)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Waveform Bar
                Column(modifier = Modifier.widthIn(max = 340.dp)) {
                    WaveformProgressBar(
                        positionMs = playerState.position,
                        durationMs = playerState.duration,
                        onSeek = onSeekTo,
                        isPlaying = playerState.isPlaying,
                        songId = song?.id ?: "desktop_immersive",
                        albumArtUri = song?.albumArtUri,
                        albumArtColors = playerState.albumArtColors,
                        waveformAmplitudes = playerState.waveformAmplitudes,
                        fftBands = playerState.fftBands,
                        bassEnergy = playerState.bassEnergy,
                        visualizerStyle = visualizerStyle,
                        visualizerColorScheme = visualizerColorScheme,
                        appFps = appFps,
                        progressDotStyle = progressDotStyle,
                        height = 60.dp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(playerState.position), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text(formatDuration(playerState.duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.shuffleEnabled) Color(0xFFC4CBD4) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onCycleRepeatMode, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) Color(0xFFC4CBD4) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Right Column: Dynamic Real-Time Timed Synced Lyrics
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "♪  ♪  ♪",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val lyricsList = playerState.syncedLyrics
                if (lyricsList.isEmpty()) {
                    Text(
                        text = "Searching for lyrics...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    val activeIndex = remember(playerState.position, lyricsList) {
                        val idx = lyricsList.indexOfLast { it.timestampMs <= playerState.position }
                        if (idx == -1) 0 else idx
                    }

                    val listState = rememberLazyListState()

                    LaunchedEffect(activeIndex) {
                        if (activeIndex in lyricsList.indices) {
                            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(lyricsList) { index, lyric ->
                            val isActiveLine = index == activeIndex
                            Text(
                                text = lyric.text,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = if (isActiveLine) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActiveLine) Color(0xFFF0F4F8) else Color.White.copy(alpha = 0.3f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Pill on Bottom Right
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF162A34).copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Visualizer", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Lyrics", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Top Left Back Button & Top Right Fullscreen Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = {
                onToggleFullscreen()
                focusManager.clearFocus()
            }) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen (F11)",
                    tint = Color.White
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopImmersivePlayerViewPreview() {
    val sampleSong = com.aurum.data.model.Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
    val sampleState = PlayerState(
        currentSong = sampleSong,
        isPlaying = true,
        position = 78000L,
        duration = 235000L
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopImmersivePlayerView(
            playerState = sampleState,
            onPlayPauseToggle = {},
            onPrevious = {},
            onNext = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onCycleRepeatMode = {},
            onFavoriteToggle = {},
            onBackClick = {}
        )
    }
}

private fun Modifier.opacity(alpha: Float): Modifier = this.then(
    Modifier.background(Color.Black.copy(alpha = 1f - alpha))
)
