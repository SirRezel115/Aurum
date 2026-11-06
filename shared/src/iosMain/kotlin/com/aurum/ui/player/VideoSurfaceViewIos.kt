package com.aurum.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import com.aurum.ui.common.TopographicContourPlaceholder

@Composable
actual fun VideoSurfaceView(
    thumbnailUri: String?,
    modifier: Modifier,
) {
    if (thumbnailUri.isNullOrBlank()) {
        TopographicContourPlaceholder(modifier = modifier)
    } else {
        AsyncImage(
            model = thumbnailUri,
            contentDescription = null,
            modifier = modifier
        )
    }
}
