package com.aurum.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Parsed synced LRC lyric line.
 */
data class SyncedLyricLine(
    val timestampMs: Long,
    val text: String
)

/**
 * Helper to parse .lrc string formatted lyrics.
 */
fun parseLrcLyrics(lrcText: String): List<SyncedLyricLine> {
    if (lrcText.isBlank()) return emptyList()
    val lines = mutableListOf<SyncedLyricLine>()
    val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    lrcText.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val msStr = match.groupValues[3]
            val ms = (msStr.toLongOrNull() ?: 0L) * if (msStr.length == 2) 10 else 1
            val timestamp = min * 60 * 1000 + sec * 1000 + ms
            val content = match.groupValues[4].trim()
            if (content.isNotEmpty()) {
                lines.add(SyncedLyricLine(timestamp, content))
            }
        }
    }
    return lines.sortedBy { it.timestampMs }
}

/**
 * Real-Time Synchronized Lyrics Sheet Component.
 */
@Composable
fun SyncedLyricsView(
    positionMs: Long,
    lyricsList: List<SyncedLyricLine>,
    modifier: Modifier = Modifier,
) {
    if (lyricsList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No synchronized lyrics found for this track",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val activeIndex = remember(positionMs, lyricsList) {
        val idx = lyricsList.indexOfLast { it.timestampMs <= positionMs }
        if (idx == -1) 0 else idx
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyricsList.indices) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyricsList) { index, lyric ->
            val isActive = index == activeIndex
            val alpha = if (isActive) 1.0f else 0.45f
            val style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge

            Text(
                text = lyric.text,
                style = style,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SyncedLyricsViewPreview() {
    val sampleLyrics = listOf(
        SyncedLyricLine(timestampMs = 0L, text = "Into the midnight horizon"),
        SyncedLyricLine(timestampMs = 10000L, text = "Stars align in brilliant gold"),
        SyncedLyricLine(timestampMs = 20000L, text = "Soundscapes dancing through the night")
    )
    com.aurum.ui.theme.AurumTheme {
        SyncedLyricsView(lyricsList = sampleLyrics, positionMs = 10000L)
    }
}
