package com.aurum.ui.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Artist
import com.aurum.ui.common.TopographicContourPlaceholder

@Composable
fun DesktopArtistsGridView(
    artists: List<Artist>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            text = "Artists",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${artists.size} artists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(artists, key = { it.id }) { artist ->
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
                        .clickable { onArtistClick(artist.name) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                        ) {
                            AurumAsyncImage(
                                model = artist.albumArtUri,
                                contentDescription = artist.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun DesktopArtistsGridViewPreview() {
    val sampleArtists = listOf(
        Artist(id = "1", name = "Aurum Soundscape", trackCount = 12),
        Artist(id = "2", name = "Nebula Dream", trackCount = 8)
    )
    com.aurum.ui.theme.AurumTheme {
        DesktopArtistsGridView(
            artists = sampleArtists,
            onArtistClick = {}
        )
    }
}
