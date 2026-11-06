package com.aurum.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aurum.player.MusicServiceHolder

@UnstableApi
@Composable
actual fun VideoSurfaceView(
    thumbnailUri: String?,
    modifier: Modifier,
) {
    val player = MusicServiceHolder.sharedPlayer
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
        update = { playerView ->
            playerView.player = player
        },
        modifier = modifier
    )
}
