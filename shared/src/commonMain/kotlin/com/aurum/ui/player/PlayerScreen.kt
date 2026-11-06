package com.aurum.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurum.data.model.Song
import com.aurum.player.PlayerState
import com.aurum.player.RepeatMode
import com.aurum.ui.common.TopographicContourPlaceholder
import com.aurum.ui.library.formatDuration
import com.aurum.ui.theme.AppFps
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

val PauseIcon: ImageVector
    get() {
        if (pauseIconCache != null) {
            return pauseIconCache!!
        }
        pauseIconCache = ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(6f, 19f)
                close()
                moveTo(14f, 5f)
                lineTo(18f, 5f)
                lineTo(18f, 19f)
                lineTo(14f, 19f)
                close()
            }
        }.build()
        return pauseIconCache!!
    }

private var pauseIconCache: ImageVector? = null

val ShuffleIcon: ImageVector
    get() {
        if (shuffleIconCache != null) return shuffleIconCache!!
        shuffleIconCache = ImageVector.Builder(
            name = "Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10.59f, 9.17f)
                lineTo(5.41f, 4f)
                lineTo(4f, 5.41f)
                lineTo(9.17f, 10.59f)
                lineTo(10.59f, 9.17f)
                moveTo(14.50f, 4f)
                lineTo(16.86f, 6.36f)
                lineTo(4f, 19.22f)
                lineTo(5.41f, 20.63f)
                lineTo(18.27f, 7.77f)
                lineTo(20.50f, 10f)
                lineTo(20.50f, 4f)
                lineTo(14.50f, 4f)
                moveTo(14.83f, 13.41f)
                lineTo(13.41f, 14.83f)
                lineTo(16.86f, 18.28f)
                lineTo(14.50f, 20.64f)
                lineTo(20.50f, 20.64f)
                lineTo(20.50f, 14.64f)
                lineTo(18.14f, 17.00f)
                lineTo(14.83f, 13.41f)
                close()
            }
        }.build()
        return shuffleIconCache!!
    }
private var shuffleIconCache: ImageVector? = null

val RepeatIcon: ImageVector
    get() {
        if (repeatIconCache != null) return repeatIconCache!!
        repeatIconCache = ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                close()
            }
        }.build()
        return repeatIconCache!!
    }
private var repeatIconCache: ImageVector? = null

val RepeatOneIcon: ImageVector
    get() {
        if (repeatOneIconCache != null) return repeatOneIconCache!!
        repeatOneIconCache = ImageVector.Builder(
            name = "RepeatOne",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                moveTo(11f, 10f)
                horizontalLineTo(13f)
                verticalLineTo(16f)
                horizontalLineTo(11f)
                verticalLineTo(10f)
                close()
            }
        }.build()
        return repeatOneIconCache!!
    }
private var repeatOneIconCache: ImageVector? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    viewModel: PlayerViewModel,
    visualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    visualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    appFps: AppFps = AppFps.FPS_60,
    progressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    onOpenDrawer: () -> Unit,
    onOpenMetadataFixer: ((Song) -> Unit)? = null,
) {
    val rawSong = playerState.currentSong
    val song = rawSong ?: Song(
        id = "placeholder",
        title = "Mr.Music no here",
        artist = "Select a track from your library to experience Aurum audio.",
        album = "",
        duration = 0L,
        uri = "",
        albumArtUri = null
    )
    var controlsVisible by remember { mutableStateOf(true) }
    var transitionDirection by remember { mutableStateOf(1) } // 1 = Next (Right -> Left), -1 = Prev (Left -> Right)

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5.seconds)
            controlsVisible = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        containerColor = Color.Black,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!song.albumArtUri.isNullOrBlank()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 45.dp)
                        .graphicsLayer { alpha = 0.55f }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black
                                )
                            )
                        )
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        controlsVisible = true
                    }
            ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // Landscape Mode: Full-screen visualizer progress bar & controls (no album art)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Track Metadata Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        com.aurum.ui.common.RainbowText(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 2. Full-Width Visualizer Progress Bar
                    Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 8.dp)) {
                        WaveformProgressBar(
                            positionMs = playerState.position,
                            durationMs = playerState.duration,
                            onSeek = { viewModel.seekTo(it) },
                            isPlaying = playerState.isPlaying,
                            songId = song.id,
                            albumArtUri = song.albumArtUri,
                            albumArtColors = playerState.albumArtColors,
                            waveformAmplitudes = playerState.waveformAmplitudes,
                            fftBands = playerState.fftBands,
                            bassEnergy = playerState.bassEnergy,
                            visualizerStyle = visualizerStyle,
                            visualizerColorScheme = visualizerColorScheme,
                            appFps = appFps,
                            progressDotStyle = progressDotStyle,
                            height = 140.dp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(playerState.position),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatDuration(playerState.duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 3. Playback Controls & Toggles
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleShuffle() }) {
                                Icon(
                                    imageVector = ShuffleIcon,
                                    contentDescription = "Shuffle",
                                    tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = {
                                transitionDirection = -1
                                viewModel.previous()
                            }) {
                                com.aurum.ui.common.RainbowIcon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (com.aurum.ui.common.LocalVisualizerColorScheme.current == com.aurum.ui.player.VisualizerColorScheme.RAINBOW) com.aurum.ui.common.RainbowBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                    .clickable { viewModel.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) PauseIcon else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(onClick = {
                                transitionDirection = 1
                                viewModel.next()
                            }) {
                                com.aurum.ui.common.RainbowIcon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                                val repeatIcon = when (playerState.repeatMode) {
                                    RepeatMode.ONE -> RepeatOneIcon
                                    else -> RepeatIcon
                                }
                                val tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = "Repeat: ${playerState.repeatMode}",
                                    tint = tint
                                )
                            }
                        }
                    }
                }
            } else {
                    // Portrait Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Main Track Metadata Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            com.aurum.ui.common.RainbowText(
                                text = song.title,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${song.artist} • ${song.album}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Edge-to-Edge Album Art
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtView(
                                song = song,
                                direction = transitionDirection,
                                onTap = {},
                                onSwipeLeft = {
                                    transitionDirection = 1
                                    viewModel.next()
                                },
                                onSwipeRight = {
                                    transitionDirection = -1
                                    viewModel.previous()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }

                        // Progress Bar & Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = (-14).dp)
                        ) {
                            WaveformProgressBar(
                                positionMs = playerState.position,
                                durationMs = playerState.duration,
                                onSeek = { viewModel.seekTo(it) },
                                isPlaying = playerState.isPlaying,
                                songId = song.id,
                                albumArtUri = song.albumArtUri,
                                albumArtColors = playerState.albumArtColors,
                                waveformAmplitudes = playerState.waveformAmplitudes,
                                fftBands = playerState.fftBands,
                                bassEnergy = playerState.bassEnergy,
                                visualizerStyle = visualizerStyle,
                                visualizerColorScheme = visualizerColorScheme,
                                appFps = appFps,
                                progressDotStyle = progressDotStyle,
                                height = 120.dp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(playerState.position),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(playerState.duration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Playback Controls & Toggles
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    transitionDirection = -1
                                    viewModel.previous()
                                }) {
                                    com.aurum.ui.common.RainbowIcon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                val animatedPortraitFabBrush = com.aurum.ui.common.rememberAnimatedRainbowBrush()
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (com.aurum.ui.common.LocalVisualizerColorScheme.current == com.aurum.ui.player.VisualizerColorScheme.RAINBOW) animatedPortraitFabBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                        )
                                        .clickable { viewModel.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) PauseIcon else Icons.Default.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    transitionDirection = 1
                                    viewModel.next()
                                }) {
                                    com.aurum.ui.common.RainbowIcon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            // Speed, Shuffle & Repeat toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.toggleShuffle() }) {
                                    Icon(
                                        imageVector = ShuffleIcon,
                                        contentDescription = "Shuffle",
                                        tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.cyclePlaybackSpeed() },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${playerState.playbackSpeed}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                                    val repeatIcon = when (playerState.repeatMode) {
                                        RepeatMode.ONE -> RepeatOneIcon
                                        else -> RepeatIcon
                                    }
                                    val tint = if (playerState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    Icon(
                                        imageVector = repeatIcon,
                                        contentDescription = "Repeat: ${playerState.repeatMode}",
                                        tint = tint
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

@Composable
fun AlbumArtView(
    song: Song,
    direction: Int = 1,
    onTap: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures {
                    onTap()
                }
            }
            .pointerInput(density) {
                val swipeThresholdPx = with(density) { 40.dp.toPx() }
                var totalOffsetX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalOffsetX = 0f },
                    onDragEnd = {
                        if (totalOffsetX < -swipeThresholdPx) {
                            onSwipeLeft()
                        } else if (totalOffsetX > swipeThresholdPx) {
                            onSwipeRight()
                        }
                        totalOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalOffsetX += dragAmount
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = song.id to (song.albumArtUri ?: ""),
            transitionSpec = {
                if (direction > 0) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { (_, artUri) ->
            if (artUri.isBlank()) {
                TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PlayerScreenPreview() {
    val sampleSong = com.aurum.data.model.Song(id = "1", title = "Midnight Horizon", artist = "Aurum Soundscape", album = "Celestial Waves", duration = 235000L, uri = "")
    val sampleState = PlayerState(
        currentSong = sampleSong,
        isPlaying = true,
        position = 45000L,
        duration = 235000L
    )
    val fakePlayer = object : com.aurum.player.MusicPlayer {
        override val state: State<PlayerState> = mutableStateOf(sampleState)
        override suspend fun play(song: com.aurum.data.model.Song) {}
        override suspend fun play(songs: List<com.aurum.data.model.Song>, startIndex: Int) {}
        override suspend fun playNext(song: com.aurum.data.model.Song) {}
        override suspend fun playNext(songs: List<com.aurum.data.model.Song>) {}
        override suspend fun addToQueue(song: com.aurum.data.model.Song) {}
        override suspend fun addToQueue(songs: List<com.aurum.data.model.Song>) {}
        override suspend fun pause() {}
        override suspend fun resume() {}
        override suspend fun seekTo(positionMs: Long) {}
        override suspend fun next() {}
        override suspend fun previous() {}
        override suspend fun stop() {}
        override fun toggleShuffle() {}
        override fun cycleRepeatMode() {}
    }
    val previewViewModel = remember { PlayerViewModel(fakePlayer) }
    com.aurum.ui.theme.AurumTheme {
        PlayerScreen(playerState = sampleState, viewModel = previewViewModel, onOpenDrawer = {})
    }
}
