package com.aurum.ui.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurum.data.model.Album
import kotlinx.coroutines.delay

@Composable
fun DesktopAlbumsGridView(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var albumCardSize by remember { mutableStateOf(160.dp) }
    var isZoomVisible by remember { mutableStateOf(false) }
    val zoomToolbarInteractionSource = remember { MutableInteractionSource() }
    val isZoomToolbarHovered by zoomToolbarInteractionSource.collectIsHoveredAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(albumCardSize) {
        if (albumCardSize.value != 160f || isZoomVisible) {
            isZoomVisible = true
            delay(2500)
            if (!isZoomToolbarHovered) {
                isZoomVisible = false
            }
        }
    }

    LaunchedEffect(isZoomToolbarHovered) {
        if (isZoomToolbarHovered) {
            isZoomVisible = true
        } else if (isZoomVisible) {
            delay(2500)
            isZoomVisible = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.Equals, Key.NumPadAdd -> {
                            albumCardSize = (albumCardSize.value + 15f).coerceAtMost(320f).dp
                            true
                        }
                        Key.Minus, Key.NumPadSubtract -> {
                            albumCardSize = (albumCardSize.value - 15f).coerceAtLeast(100f).dp
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            if (event.keyboardModifiers.isCtrlPressed) {
                                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (scrollDelta < 0f) {
                                    albumCardSize = (albumCardSize.value + 15f).coerceAtMost(320f).dp
                                } else if (scrollDelta > 0f) {
                                    albumCardSize = (albumCardSize.value - 15f).coerceAtLeast(100f).dp
                                }
                            }
                        }
                    }
                }
            }
            .padding(24.dp)
    ) {
        // Header with Zoom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${albums.size} albums • Ctrl + Scroll to zoom",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Zoom Controls Pill Toolbar - Fades out after 2.5s
            AnimatedVisibility(
                visible = isZoomVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(500))
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 2.dp,
                    modifier = Modifier.hoverable(zoomToolbarInteractionSource)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { albumCardSize = (albumCardSize.value - 15f).coerceAtLeast(100f).dp },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${albumCardSize.value.toInt()} px",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { albumCardSize = (albumCardSize.value + 15f).coerceAtMost(320f).dp },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }

                        if (albumCardSize.value.toInt() != 160) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { albumCardSize = 160.dp },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = albumCardSize),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums, key = { it.id }) { album ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHovered) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverable(interactionSource)
                        .clickable { onAlbumClick(album.title) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AurumAsyncImage(
                                model = album.albumArtUri,
                                contentDescription = album.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Album · ${album.artist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopAlbumsGridViewPreview() {
    val sampleAlbums = listOf(
        Album(id = "1", title = "Celestial Waves", artist = "Aurum Soundscape", trackCount = 12, albumArtUri = ""),
        Album(id = "2", title = "Cosmic Journey", artist = "Nebula Dream", trackCount = 8, albumArtUri = "")
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopAlbumsGridView(
            albums = sampleAlbums,
            onAlbumClick = {}
        )
    }
}
