package com.aurum.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoSurfaceView(
    thumbnailUri: String?,
    modifier: Modifier = Modifier,
)
