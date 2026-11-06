package com.aurum.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.player.PlayerState
import com.aurum.ui.theme.AppFps

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    visualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    visualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    appFps: AppFps = AppFps.FPS_60,
    progressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    onClick: () -> Unit,
    onOpenMetadataFixer: ((com.aurum.data.model.Song) -> Unit)? = null,
) {
    val song = playerState.currentSong ?: return

    val progress = if (playerState.duration > 0) {
        (playerState.position.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun MiniPlayerBarPreview() {
    val sampleSong = com.aurum.data.model.Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
    val sampleState = PlayerState(currentSong = sampleSong, isPlaying = true, position = 45000L, duration = 235000L)
    com.aurum.ui.theme.AurumTheme {
        MiniPlayerBar(playerState = sampleState, onClick = {})
    }
}
