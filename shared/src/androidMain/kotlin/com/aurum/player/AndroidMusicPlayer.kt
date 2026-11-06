package com.aurum.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aurum.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android MusicPlayer backed by Media3 ExoPlayer with AurumMusicService for robust background playback.
 */
@androidx.media3.common.util.UnstableApi
class AndroidMusicPlayer(
    private val context: Context,
) : MusicPlayer {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = mutableStateOf(PlayerState())
    override val state: State<PlayerState> = _state

    private var currentPlaylist: List<Song> = emptyList()
    private var originalPlaylist: List<Song> = emptyList()
    private var positionJob: Job? = null

    init {
        scope.launch {
            awaitPlayer()
        }
    }

    private val exoPlayer: ExoPlayer?
        get() = MusicServiceHolder.sharedPlayer

    private var currentPlayerInstance: ExoPlayer? = null

    private suspend fun awaitPlayer(): ExoPlayer = withContext(Dispatchers.Main) {
        var player = MusicServiceHolder.sharedPlayer
        if (player != null) {
            initPlayerWithInstance(player)
            return@withContext player
        }

        startForegroundService()

        if (player == null) {
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)

            val fallbackPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
            MusicServiceHolder.sharedPlayer = fallbackPlayer
            player = fallbackPlayer
        }

        initPlayerWithInstance(player)
        player
    }

    private fun initPlayerWithInstance(player: ExoPlayer) {
        if (currentPlayerInstance != player) {
            currentPlayerInstance = player
            checkAndInitAudioEffects(player)
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState { copy(isPlaying = isPlaying) }
                    try { context.sendBroadcast(Intent("com.aurum.aurum.UPDATE_WIDGET")) } catch (_: Exception) {}
                    if (isPlaying) {
                        checkAndInitAudioEffects(player)
                        startPositionPolling()
                    } else {
                        stopPositionPolling()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    checkAndInitAudioEffects(player)
                    val song = mediaItem?.mediaId?.let { id ->
                        currentPlaylist.find { it.id == id }
                    } ?: run {
                        val currentItemIndex = player.currentMediaItemIndex
                        if (currentItemIndex in currentPlaylist.indices) {
                            currentPlaylist[currentItemIndex]
                        } else null
                    } ?: mediaItem?.let { mi ->
                        Song(
                            id = mi.mediaId,
                            title = mi.mediaMetadata.title?.toString() ?: "Unknown",
                            artist = mi.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                            album = mi.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                            duration = player.duration.coerceAtLeast(0L),
                            uri = mi.localConfiguration?.uri?.toString() ?: "",
                            albumArtUri = mi.mediaMetadata.artworkUri?.toString()
                        )
                    }

                    if (song != null) {
                        updateState {
                            copy(
                                currentSong = song,
                                duration = player.duration.coerceAtLeast(0L),
                            )
                        }
                        try { context.sendBroadcast(Intent("com.aurum.aurum.UPDATE_WIDGET")) } catch (_: Exception) {}
                        extractWaveformForSong(song)
                        extractAlbumArtColors(song)

                        // Intercept 30-sec preview streams on transition and automatically upgrade to full decrypted stream
                        if (song.id.startsWith("deezer_stream_") && (song.uri.isBlank() || song.uri.contains("cdns-preview") || song.uri.contains("preview"))) {
                            scope.launch(Dispatchers.IO) {
                                val cleanId = song.id.removePrefix("deezer_stream_")
                                val searchItem = com.aurum.data.repository.DeezerTrackSearchResult(
                                    id = cleanId,
                                    title = song.title,
                                    artist = song.artist,
                                    album = song.album,
                                    durationSeconds = song.duration / 1000L,
                                    previewStreamUrl = song.uri,
                                    artworkUrl = song.albumArtUri ?: ""
                                )
                                val arlToken = com.aurum.data.repository.DeezerSessionManager.getSessionInfo()?.arl
                                val resolvedPath = com.aurum.data.repository.DeezerMusicProvider.prepareLocalStreamCacheFile(
                                    track = searchItem,
                                    arlToken = arlToken
                                ) ?: com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(cleanId, arlToken)

                                if (!resolvedPath.isNullOrBlank() && resolvedPath != song.uri) {
                                    val updatedSong = song.copy(uri = resolvedPath)
                                    withContext(Dispatchers.Main) {
                                        val p = currentPlayerInstance ?: return@withContext
                                        val currentIdx = p.currentMediaItemIndex
                                        if (currentIdx in currentPlaylist.indices && currentPlaylist[currentIdx].id == song.id) {
                                            currentPlaylist = currentPlaylist.toMutableList().apply {
                                                set(currentIdx, updatedSong)
                                            }
                                            updateState { copy(currentSong = updatedSong, queue = currentPlaylist) }
                                            Log.d("AndroidMusicPlayer", "Updated stream URL silently for active track ${updatedSong.title}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("AndroidMusicPlayer", "ExoPlayer playback error: ${error.message} (code=${error.errorCodeName})", error)

                    val currentSong = _state.value.currentSong
                    if (currentSong != null && currentSong.id.startsWith("deezer_stream_")) {
                        scope.launch(Dispatchers.IO) {
                            val cleanId = currentSong.id.removePrefix("deezer_stream_")
                            val arlToken = com.aurum.data.repository.DeezerSessionManager.getSessionInfo()?.arl
                            val freshUrl = com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(cleanId, arlToken)
                            if (!freshUrl.isNullOrBlank()) {
                                withContext(Dispatchers.Main) {
                                    val p = currentPlayerInstance ?: return@withContext
                                    val currentIdx = p.currentMediaItemIndex
                                    val formattedUri = Uri.parse(freshUrl)
                                    val newMediaItem = MediaItem.Builder()
                                        .setMediaId(currentSong.id)
                                        .setUri(formattedUri)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(currentSong.title)
                                                .setArtist(currentSong.artist)
                                                .setAlbumTitle(currentSong.album)
                                                .setArtworkUri(currentSong.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                                .build()
                                        )
                                        .build()
                                    p.replaceMediaItem(currentIdx, newMediaItem)
                                    p.prepare()
                                    p.play()
                                    Log.d("AndroidMusicPlayer", "Recovered from playback error for track ${currentSong.title}")
                                }
                            }
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateName = when (playbackState) {
                        Player.STATE_IDLE -> "STATE_IDLE"
                        Player.STATE_BUFFERING -> "STATE_BUFFERING"
                        Player.STATE_READY -> "STATE_READY"
                        Player.STATE_ENDED -> "STATE_ENDED"
                        else -> "STATE_$playbackState"
                    }
                    Log.d("AndroidMusicPlayer", "ExoPlayer playback state changed to $stateName, duration=${player.duration}")
                    if (playbackState == Player.STATE_READY) {
                        updateState { copy(duration = player.duration.coerceAtLeast(0L)) }
                    }
                }
            })

            val currentMediaItem = player.currentMediaItem
            if (currentMediaItem != null) {
                val songId = currentMediaItem.mediaId
                val meta = currentMediaItem.mediaMetadata
                val song = currentPlaylist.find { it.id == songId } ?: Song(
                    id = songId,
                    title = meta.title?.toString() ?: "Unknown",
                    artist = meta.artist?.toString() ?: "Unknown Artist",
                    album = meta.albumTitle?.toString() ?: "Unknown Album",
                    duration = player.duration.coerceAtLeast(0L),
                    uri = currentMediaItem.localConfiguration?.uri?.toString() ?: "",
                    albumArtUri = meta.artworkUri?.toString()
                )
                _state.value = _state.value.copy(
                    currentSong = song,
                    isPlaying = player.isPlaying,
                    position = player.currentPosition,
                    duration = player.duration.coerceAtLeast(0L)
                )
                if (player.isPlaying) {
                    startPositionPolling()
                }
                extractWaveformForSong(song)
                extractAlbumArtColors(song)
            }
        }
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, AurumMusicService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
        }
    }

    override suspend fun play(song: Song) {
        play(listOf(song), 0)
    }

    override suspend fun play(songs: List<Song>, startIndex: Int) = withContext(Dispatchers.Main) {
        if (songs.isEmpty()) return@withContext
        val player = awaitPlayer()

        player.pause()
        player.stop()
        player.clearMediaItems()

        originalPlaylist = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        val activeSongs = if (_state.value.shuffleEnabled) {
            val clickedSong = songs[validIndex]
            val remaining = songs.filter { it.id != clickedSong.id }.shuffled()
            listOf(clickedSong) + remaining
        } else {
            songs
        }
        currentPlaylist = activeSongs
        val actualIndex = if (_state.value.shuffleEnabled) 0 else validIndex

        val mediaItems = activeSongs.map { song ->
            val formattedUri = when {
                song.uri.isBlank() -> Uri.EMPTY
                song.uri.startsWith("/") -> Uri.fromFile(java.io.File(song.uri))
                else -> Uri.parse(song.uri)
            }
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(formattedUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build()
                )
                .build()
        }

        player.setMediaItems(mediaItems, actualIndex, 0L)
        player.prepare()
        player.play()

        updateState {
            copy(
                currentSong = activeSongs[actualIndex],
                isPlaying = true,
                position = 0L,
                queue = currentPlaylist
            )
        }
        extractWaveformForSong(activeSongs[actualIndex])
        extractAlbumArtColors(activeSongs[actualIndex])
    }

    override fun updateQueue(songs: List<Song>) {
        scope.launch(Dispatchers.Main) {
            currentPlaylist = songs
            updateState { copy(queue = songs) }

            val player = currentPlayerInstance ?: return@launch
            val currentIdx = player.currentMediaItemIndex

            for (i in songs.indices) {
                val song = songs[i]
                if (i < player.mediaItemCount && i != currentIdx) {
                    val existingItem = player.getMediaItemAt(i)
                    val existingUri = existingItem.localConfiguration?.uri?.toString() ?: ""

                    if (song.uri.isNotBlank() && song.uri != existingUri && !song.uri.contains("cdns-preview")) {
                        val formattedUri = if (song.uri.startsWith("/")) Uri.fromFile(java.io.File(song.uri)) else Uri.parse(song.uri)
                        val updatedMediaItem = MediaItem.Builder()
                            .setMediaId(song.id)
                            .setUri(formattedUri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .setAlbumTitle(song.album)
                                    .setArtworkUri(song.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                    .build()
                            )
                            .build()

                        player.replaceMediaItem(i, updatedMediaItem)
                        Log.d("AndroidMusicPlayer", "Replaced non-active queue item $i (${song.title}) with full stream $formattedUri")
                    }
                }
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                    .build()
            )
            .build()
    }

    override suspend fun playNext(song: Song) {
        playNext(listOf(song))
    }

    override suspend fun playNext(songs: List<Song>) = withContext(Dispatchers.Main) {
        if (songs.isEmpty()) return@withContext
        val player = awaitPlayer()
        val mediaItems = songs.map { it.toMediaItem() }
        val insertIndex = if (player.mediaItemCount == 0) {
            0
        } else {
            (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        }

        val updated = currentPlaylist.toMutableList()
        val pos = insertIndex.coerceIn(0, updated.size)
        updated.addAll(pos, songs)
        currentPlaylist = updated

        player.addMediaItems(insertIndex, mediaItems)
        updateState { copy(queue = currentPlaylist) }

        if (!player.isPlaying && player.mediaItemCount == songs.size) {
            player.prepare()
            player.play()
        }
    }

    override suspend fun addToQueue(song: Song) {
        addToQueue(listOf(song))
    }

    override suspend fun addToQueue(songs: List<Song>) = withContext(Dispatchers.Main) {
        if (songs.isEmpty()) return@withContext
        val player = awaitPlayer()
        val mediaItems = songs.map { it.toMediaItem() }
        val wasEmpty = player.mediaItemCount == 0

        currentPlaylist = currentPlaylist + songs
        player.addMediaItems(mediaItems)
        updateState { copy(queue = currentPlaylist) }

        if (!player.isPlaying && wasEmpty) {
            player.prepare()
            player.play()
        }
    }

    override suspend fun pause() = withContext(Dispatchers.Main) {
        exoPlayer?.pause()
        updateState { copy(isPlaying = false) }
    }

    override suspend fun resume() = withContext(Dispatchers.Main) {
        val player = awaitPlayer()
        player.play()
        updateState { copy(isPlaying = true) }
    }

    override suspend fun seekTo(positionMs: Long) = withContext(Dispatchers.Main) {
        exoPlayer?.seekTo(positionMs)
        updateState { copy(position = positionMs) }
    }

    override suspend fun next() = withContext(Dispatchers.Main) {
        val player = exoPlayer ?: return@withContext
        if (player.hasNextMediaItem()) {
            val nextIdx = player.currentMediaItemIndex + 1
            if (nextIdx in currentPlaylist.indices) {
                val nextSong = currentPlaylist[nextIdx]
                if (nextSong.id.startsWith("deezer_stream_") && (nextSong.uri.isBlank() || nextSong.uri.contains("preview"))) {
                    scope.launch(Dispatchers.IO) {
                        val cleanId = nextSong.id.removePrefix("deezer_stream_")
                        val arlToken = com.aurum.data.repository.DeezerSessionManager.getSessionInfo()?.arl
                        val fullUri = com.aurum.data.repository.DeezerMusicProvider.prepareLocalStreamCacheFile(
                            track = com.aurum.data.repository.DeezerTrackSearchResult(id = cleanId, title = nextSong.title, artist = nextSong.artist, album = nextSong.album, durationSeconds = nextSong.duration / 1000L, previewStreamUrl = nextSong.uri, artworkUrl = nextSong.albumArtUri ?: ""),
                            arlToken = arlToken
                        ) ?: com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(cleanId, arlToken)

                        if (!fullUri.isNullOrBlank()) {
                            val updated = nextSong.copy(uri = fullUri)
                            withContext(Dispatchers.Main) {
                                if (nextIdx in currentPlaylist.indices) {
                                    currentPlaylist = currentPlaylist.toMutableList().apply { set(nextIdx, updated) }
                                    updateState { copy(queue = currentPlaylist) }
                                    val formattedUri = if (fullUri.startsWith("/")) Uri.fromFile(java.io.File(fullUri)) else Uri.parse(fullUri)
                                    val newItem = MediaItem.Builder()
                                        .setMediaId(updated.id)
                                        .setUri(formattedUri)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(updated.title)
                                                .setArtist(updated.artist)
                                                .setAlbumTitle(updated.album)
                                                .setArtworkUri(updated.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                                .build()
                                        )
                                        .build()
                                    player.replaceMediaItem(nextIdx, newItem)
                                }
                            }
                        }
                    }
                }
            }
            player.seekToNextMediaItem()
        }
    }

    override suspend fun previous() = withContext(Dispatchers.Main) {
        val player = exoPlayer ?: return@withContext
        if (player.hasPreviousMediaItem()) {
            val prevIdx = player.currentMediaItemIndex - 1
            if (prevIdx in currentPlaylist.indices) {
                val prevSong = currentPlaylist[prevIdx]
                if (prevSong.id.startsWith("deezer_stream_") && (prevSong.uri.isBlank() || prevSong.uri.contains("preview"))) {
                    scope.launch(Dispatchers.IO) {
                        val cleanId = prevSong.id.removePrefix("deezer_stream_")
                        val arlToken = com.aurum.data.repository.DeezerSessionManager.getSessionInfo()?.arl
                        val fullUri = com.aurum.data.repository.DeezerMusicProvider.prepareLocalStreamCacheFile(
                            track = com.aurum.data.repository.DeezerTrackSearchResult(id = cleanId, title = prevSong.title, artist = prevSong.artist, album = prevSong.album, durationSeconds = prevSong.duration / 1000L, previewStreamUrl = prevSong.uri, artworkUrl = prevSong.albumArtUri ?: ""),
                            arlToken = arlToken
                        ) ?: com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(cleanId, arlToken)

                        if (!fullUri.isNullOrBlank()) {
                            val updated = prevSong.copy(uri = fullUri)
                            withContext(Dispatchers.Main) {
                                if (prevIdx in currentPlaylist.indices) {
                                    currentPlaylist = currentPlaylist.toMutableList().apply { set(prevIdx, updated) }
                                    updateState { copy(queue = currentPlaylist) }
                                    val formattedUri = if (fullUri.startsWith("/")) Uri.fromFile(java.io.File(fullUri)) else Uri.parse(fullUri)
                                    val newItem = MediaItem.Builder()
                                        .setMediaId(updated.id)
                                        .setUri(formattedUri)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(updated.title)
                                                .setArtist(updated.artist)
                                                .setAlbumTitle(updated.album)
                                                .setArtworkUri(updated.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                                .build()
                                        )
                                        .build()
                                    player.replaceMediaItem(prevIdx, newItem)
                                }
                            }
                        }
                    }
                }
            }
            player.seekToPreviousMediaItem()
        }
    }

    override suspend fun stop() = withContext(Dispatchers.Main) {
        stopPositionPolling()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        currentPlaylist = emptyList()
        originalPlaylist = emptyList()
        updateState { PlayerState() }
    }

    override fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.setPlaybackSpeed(speed)
        updateState { copy(playbackSpeed = speed) }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        updateState { copy(volume = clamped) }
    }

    override fun toggleShuffle() {
        val nextShuffleState = !_state.value.shuffleEnabled
        val player = exoPlayer ?: run {
            updateState { copy(shuffleEnabled = nextShuffleState) }
            return
        }
        val currentSong = _state.value.currentSong

        scope.launch(Dispatchers.Main) {
            if (nextShuffleState) {
                val baseList = if (originalPlaylist.isNotEmpty()) originalPlaylist else currentPlaylist
                if (baseList.isEmpty()) {
                    updateState { copy(shuffleEnabled = nextShuffleState) }
                    return@launch
                }
                val remaining = baseList.filter { it.id != currentSong?.id }.shuffled()
                val shuffled = if (currentSong != null) listOf(currentSong) + remaining else remaining
                currentPlaylist = shuffled

                val mediaItems = shuffled.map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id)
                        .setUri(song.uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle(song.album)
                                .setArtworkUri(song.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                .build()
                        )
                        .build()
                }
                player.setMediaItems(mediaItems, 0, player.currentPosition)
                player.prepare()
                player.play()
                updateState { copy(shuffleEnabled = true, queue = currentPlaylist) }
            } else {
                if (originalPlaylist.isNotEmpty()) {
                    currentPlaylist = originalPlaylist
                    val mediaItems = originalPlaylist.map { song ->
                        MediaItem.Builder()
                            .setMediaId(song.id)
                            .setUri(song.uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .setAlbumTitle(song.album)
                                    .setArtworkUri(song.albumArtUri?.takeIf { it.isNotBlank() }?.let(Uri::parse))
                                    .build()
                            )
                            .build()
                    }
                    val targetIndex = if (currentSong != null) originalPlaylist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0) else 0
                    player.setMediaItems(mediaItems, targetIndex, player.currentPosition)
                    player.prepare()
                    updateState { copy(shuffleEnabled = false, queue = currentPlaylist) }
                } else {
                    updateState { copy(shuffleEnabled = false) }
                }
            }
        }
    }

    private fun extractWaveformForSong(song: Song) {
        val immediateAmps = generateWigglyWaveformSamples(song.id, 180, 0f)
        updateState { copy(waveformAmplitudes = immediateAmps) }

        scope.launch(Dispatchers.IO) {
            try {
                val amps = AudioWaveformExtractor.getOrExtractAmplitudes(context, song)
                withContext(Dispatchers.Main) {
                    updateState { copy(waveformAmplitudes = amps) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun extractAlbumArtColors(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                var bitmap: android.graphics.Bitmap? = null
                val artUriStr = song.albumArtUri?.trim()

                if (!artUriStr.isNullOrBlank()) {
                    try {
                        when {
                            artUriStr.startsWith("http://") || artUriStr.startsWith("https://") -> {
                                val conn = java.net.URI(artUriStr).toURL().openConnection() as java.net.HttpURLConnection
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                                if (conn.responseCode == 200) {
                                    conn.inputStream.use { stream ->
                                        bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                    }
                                }
                            }
                            artUriStr.startsWith("content://") -> {
                                val uri = Uri.parse(artUriStr)
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                }
                            }
                            artUriStr.startsWith("file://") -> {
                                val cleanPath = artUriStr.removePrefix("file://")
                                bitmap = android.graphics.BitmapFactory.decodeFile(cleanPath)
                            }
                            artUriStr.startsWith("/") -> {
                                bitmap = android.graphics.BitmapFactory.decodeFile(artUriStr)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("AndroidMusicPlayer", "Failed to load album art bitmap from URI $artUriStr: ${e.message}")
                    }
                }

                if (bitmap == null) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        if (song.uri.startsWith("content://")) {
                            context.contentResolver.openFileDescriptor(Uri.parse(song.uri), "r")?.use { pfd ->
                                retriever.setDataSource(pfd.fileDescriptor)
                            }
                        } else if (song.uri.startsWith("/")) {
                            retriever.setDataSource(song.uri)
                        }
                        val embeddedArt = retriever.embeddedPicture
                        bitmap = embeddedArt?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                        retriever.release()
                    } catch (_: Exception) {}
                }

                if (bitmap != null) {
                    val bmp = android.graphics.Bitmap.createScaledBitmap(bitmap!!, 32, 32, false)
                    val colors = mutableListOf<Int>()
                    val samplePoints = listOf(
                        Pair(8, 8), Pair(24, 8), Pair(16, 16), Pair(8, 24), Pair(24, 24), Pair(16, 8), Pair(16, 24)
                    )
                    for ((x, y) in samplePoints) {
                        val pixel = bmp.getPixel(x, y)
                        if (!colors.contains(pixel)) {
                            colors.add(pixel)
                        }
                    }
                    bmp.recycle()

                    if (colors.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            updateState { copy(albumArtColors = colors) }
                            Log.d("AndroidMusicPlayer", "Successfully extracted ${colors.size} album art colors for track '${song.title}'")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidMusicPlayer", "Error extracting album art colors for track ${song.id}: ${e.message}")
            }
        }
    }

    override fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        val exoRepeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        exoPlayer?.repeatMode = exoRepeatMode
        updateState { copy(repeatMode = nextMode) }
    }

    private fun startPositionPolling() {
        stopPositionPolling()
        var tickCount = 0
        positionJob = scope.launch {
            while (isActive) {
                val currentPos = exoPlayer?.currentPosition ?: 0L
                val dur = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                updateState {
                    copy(
                        position = currentPos,
                        duration = if (dur > 0) dur else duration,
                    )
                }
                tickCount++
                if (tickCount >= 5) { // every 1 second
                    tickCount = 0
                    try {
                        context.sendBroadcast(Intent("com.aurum.aurum.UPDATE_WIDGET"))
                    } catch (_: Exception) {}
                }
                delay(200.milliseconds)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var realtimeVisualizer: android.media.audiofx.Visualizer? = null
    private val smoothedFftBands = FloatArray(64) { 0.1f }

    private fun initEqualizer(audioSessionId: Int) {
        if (audioSessionId != 0) {
            try {
                equalizer?.release()
                equalizer = android.media.audiofx.Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            } catch (_: Exception) {}
        }
    }

    private fun initRealtimeVisualizer(audioSessionId: Int) {
        if (audioSessionId != 0 && realtimeVisualizer == null) {
            try {
                realtimeVisualizer = android.media.audiofx.Visualizer(audioSessionId).apply {
                    captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                    setDataCaptureListener(object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(vis: android.media.audiofx.Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            if (waveform != null && _state.value.isPlaying) {
                                val pcmAmps = FloatArray(64)
                                val step = (waveform.size / 64).coerceAtLeast(1)
                                for (i in 0 until 64) {
                                    val idx = (i * step).coerceIn(waveform.indices)
                                    val raw = (waveform[idx].toInt() and 0xFF) - 128
                                    pcmAmps[i] = (kotlin.math.abs(raw) / 128f).coerceIn(0.05f, 1f)
                                }
                                updateState { copy(waveformAmplitudes = pcmAmps.toList()) }
                            }
                        }

                        override fun onFftDataCapture(vis: android.media.audiofx.Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && _state.value.isPlaying && fft.size >= 128) {
                                val bandCount = 64
                                val bands = FloatArray(bandCount)
                                val binsPerBand = (fft.size / 2 / bandCount).coerceAtLeast(1)

                                for (i in 0 until bandCount) {
                                    var sumSq = 0f
                                    for (j in 0 until binsPerBand) {
                                        val idx = (i * binsPerBand + j) * 2
                                        if (idx + 1 < fft.size) {
                                            val r = fft[idx].toFloat()
                                            val im = fft[idx + 1].toFloat()
                                            sumSq += r * r + im * im
                                        }
                                    }
                                    val rawMag = kotlin.math.sqrt(sumSq / binsPerBand) / 128f
                                    val prev = smoothedFftBands[i]
                                    val rate = if (rawMag > prev) 0.45f else 0.15f
                                    smoothedFftBands[i] = (prev + (rawMag - prev) * rate).coerceIn(0.02f, 1.0f)
                                    bands[i] = smoothedFftBands[i]
                                }

                                val bassAvg = (smoothedFftBands[0] + smoothedFftBands[1] + smoothedFftBands[2] + smoothedFftBands[3]) / 4f
                                updateState {
                                    copy(
                                        fftBands = bands.toList(),
                                        bassEnergy = bassAvg.coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                    }, android.media.audiofx.Visualizer.getMaxCaptureRate(), true, true)
                    enabled = true
                }
            } catch (_: Exception) {}
        }
    }

    private fun checkAndInitAudioEffects(player: ExoPlayer) {
        try {
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                if (equalizer == null) initEqualizer(sessionId)
                if (realtimeVisualizer == null) initRealtimeVisualizer(sessionId)
            }
        } catch (e: Exception) {
            Log.w("AndroidMusicPlayer", "Failed to init audio effects: ${e.message}")
        }
    }

    override fun getAudioSessionId(): Int {
        val sessionId = exoPlayer?.audioSessionId ?: 0
        if (sessionId != 0) {
            if (equalizer == null) initEqualizer(sessionId)
            if (realtimeVisualizer == null) initRealtimeVisualizer(sessionId)
        }
        return sessionId
    }

    override fun setEqualizerBandGain(band: Short, gain: Short) {
        try {
            equalizer?.setBandLevel(band, gain)
        } catch (_: Exception) {}
    }

    override fun getEqualizerPresetNames(): List<String> {
        return try {
            val eq = equalizer ?: return emptyList()
            val presets = mutableListOf<String>()
            for (i in 0 until eq.numberOfPresets) {
                presets.add(eq.getPresetName(i.toShort()))
            }
            presets
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun setEqualizerPreset(preset: Short) {
        try {
            equalizer?.usePreset(preset)
        } catch (_: Exception) {}
    }

    override fun openSystemEqualizer() {
        try {
            val sessionId = getAudioSessionId()
            val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    override fun setPlaybackPitch(pitch: Float) {
        try {
            val currentSpeed = exoPlayer?.playbackParameters?.speed ?: 1.0f
            exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(currentSpeed, pitch)
            updateState { copy(playbackPitch = pitch) }
        } catch (_: Exception) {}
    }

    override fun setCrossfadeSeconds(seconds: Int) {
        updateState { copy(crossfadeSeconds = seconds) }
    }

    private var sleepTimerJob: Job? = null

    override fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        updateState { copy(sleepTimerMinutes = minutes) }
        if (minutes <= 0) return

        sleepTimerJob = scope.launch {
            delay(minutes * 60 * 1000L)
            // Gentle Audio Fade-out over 5 seconds before pausing
            val initialVol = exoPlayer?.volume ?: 1.0f
            for (step in 10 downTo 0) {
                exoPlayer?.volume = initialVol * (step / 10f)
                delay(500)
            }
            pause()
            exoPlayer?.volume = initialVol
            updateState { copy(sleepTimerMinutes = 0) }
        }
    }

    override fun removeFromQueue(index: Int) {
        try {
            if (index in currentPlaylist.indices) {
                val updated = currentPlaylist.toMutableList()
                updated.removeAt(index)
                currentPlaylist = updated
                exoPlayer?.removeMediaItem(index)
                updateState { copy(queue = currentPlaylist) }
            }
        } catch (_: Exception) {}
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        try {
            if (fromIndex in currentPlaylist.indices && toIndex in currentPlaylist.indices) {
                val updated = currentPlaylist.toMutableList()
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
                currentPlaylist = updated
                exoPlayer?.moveMediaItem(fromIndex, toIndex)
                updateState { copy(queue = currentPlaylist) }
            }
        } catch (_: Exception) {}
    }

    private inline fun updateState(block: PlayerState.() -> PlayerState) {
        _state.value = _state.value.block()
        try {
            val intent = Intent("com.aurum.aurum.UPDATE_WIDGET")
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}
