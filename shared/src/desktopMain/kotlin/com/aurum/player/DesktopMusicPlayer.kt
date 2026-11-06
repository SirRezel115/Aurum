package com.aurum.player

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.aurum.data.model.Song
import kotlinx.coroutines.*
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.net.URLDecoder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

class DesktopMusicPlayer : MusicPlayer {
    private val _state = mutableStateOf(PlayerState())
    override val state: State<PlayerState> = _state

    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex: Int = 0

    private var activeLine: SourceDataLine? = null
    private var jlayerPlayer: Player? = null
    private var playbackJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override suspend fun play(song: Song) {
        play(listOf(song), 0)
    }

    override suspend fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        currentPlaylist = songs
        currentIndex = startIndex.coerceIn(0, songs.size - 1)
        _state.value = _state.value.copy(queue = currentPlaylist)
        startSongPlayback(startPositionMs = 0L)
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        applyVolumeToLine(activeLine, clamped)
    }

    private fun applyVolumeToLine(line: SourceDataLine?, vol: Float) {
        if (line == null) return
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val clampedVol = vol.coerceIn(0.0001f, 1.0f)
                val min = gainControl.minimum
                val max = gainControl.maximum
                val dB = (Math.log10(clampedVol.toDouble()) * 20.0).toFloat().coerceIn(min, max)
                gainControl.value = dB
            }
        } catch (_: Exception) {}
    }

    private fun applyVolumeToPcmBuffer(buffer: ByteArray, bytesRead: Int, volume: Float) {
        if (volume >= 0.99f) return
        val vol = volume.coerceIn(0f, 1f)
        var i = 0
        while (i < bytesRead - 1) {
            val low = buffer[i].toInt() and 0xFF
            val high = buffer[i + 1].toInt()
            val sample = (high shl 8) or low
            val scaledSample = (sample * vol).toInt().coerceIn(-32768, 32767)
            buffer[i] = (scaledSample and 0xFF).toByte()
            buffer[i + 1] = ((scaledSample shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    private fun resolveFile(uriString: String): File {
        return try {
            if (uriString.startsWith("file:")) {
                val uri = URI(uriString)
                val f = File(uri)
                if (f.exists()) return f
            }
            val clean = uriString
                .removePrefix("file:///")
                .removePrefix("file://")
                .removePrefix("file:")
            val decoded = URLDecoder.decode(clean, "UTF-8")
            File(decoded)
        } catch (_: Exception) {
            val clean = uriString
                .removePrefix("file:///")
                .removePrefix("file://")
                .removePrefix("file:")
            File(clean)
        }
    }

    private fun startSongPlayback(startPositionMs: Long = 0L) {
        val song = currentPlaylist.getOrNull(currentIndex) ?: return

        // Instantly update UI state so buttons and display update with zero lag
        _state.value = _state.value.copy(
            currentSong = song,
            isPlaying = true,
            position = startPositionMs,
            duration = if (song.duration > 0) song.duration else 180_000L,
            queue = currentPlaylist,
            waveformAmplitudes = generateWigglyWaveformSamples(song.id, 160, 0f)
        )

        // Fetch / generate synced lyrics for the song
        playerScope.launch {
            val lyrics = com.aurum.data.repository.LyricsRepository.getLyricsForSong(song)
            if (_state.value.currentSong?.id == song.id) {
                _state.value = _state.value.copy(syncedLyrics = lyrics)
            }
        }

        playerScope.launch {
            // Stop audio lines first to instantly unblock any pending I/O read/write operations
            stopAudio()

            val prevJob = playbackJob
            playbackJob = null
            prevJob?.cancel()

            playbackJob = launch(Dispatchers.IO) {
                // Wait for previous playback job to finish closing off the main thread
                prevJob?.join()

                val file = resolveFile(song.uri)
                println("DesktopMusicPlayer: Attempting to play ${song.title} from file: ${file.absolutePath} (exists=${file.exists()})")

                if (!file.exists()) {
                    println("DesktopMusicPlayer: File does not exist: ${file.absolutePath}")
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                    return@launch
                }

                var playedSuccessfully = false

                // Method 1: Try Java Sound AudioSystem PCM Conversion
                try {
                    val rawStream = AudioSystem.getAudioInputStream(file)
                    val baseFormat = rawStream.format

                    val sampleRate = if (baseFormat.sampleRate > 0f) baseFormat.sampleRate else 44100f
                    val channels = if (baseFormat.channels > 0) baseFormat.channels else 2
                    val frameSize = channels * 2

                    val pcmFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sampleRate,
                        16,
                        channels,
                        frameSize,
                        sampleRate,
                        false
                    )

                    val pcmStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream)
                    val bytesPerSecond = (pcmFormat.sampleRate * pcmFormat.frameSize).toLong().coerceAtLeast(1L)

                    // Handle seek offset
                    var skippedBytes = 0L
                    val targetByteOffset = ((startPositionMs * bytesPerSecond) / 1000L)
                    if (targetByteOffset > 0) {
                        var totalSkipped = 0L
                        while (totalSkipped < targetByteOffset && isActive) {
                            val n = pcmStream.skip(targetByteOffset - totalSkipped)
                            if (n <= 0) break
                            totalSkipped += n
                        }
                        skippedBytes = totalSkipped
                    }

                    val line = AudioSystem.getSourceDataLine(pcmFormat)
                    line.open(pcmFormat)
                    line.start()
                    activeLine = line
                    applyVolumeToLine(line, _state.value.volume)

                    val totalBytes = (file.length() * 2).coerceAtLeast(1L)
                    val durMs = if (song.duration > 0) song.duration else ((totalBytes * 1000L) / bytesPerSecond).coerceAtLeast(180_000L)

                    withContext(Dispatchers.Main) {
                        if (_state.value.currentSong?.id == song.id) {
                            _state.value = _state.value.copy(duration = durMs)
                        }
                    }

                    val buffer = ByteArray(8192)
                    var writtenBytes = skippedBytes
                    var lastStateUpdateMs = System.currentTimeMillis()

                    try {
                        while (isActive && _state.value.isPlaying) {
                            val n = pcmStream.read(buffer, 0, buffer.size)
                            if (n < 0) {
                                playedSuccessfully = true
                                withContext(Dispatchers.Main) {
                                    handleSongFinished()
                                }
                                break
                            }

                            val curVol = _state.value.volume
                            applyVolumeToPcmBuffer(buffer, n, curVol)
                            line.write(buffer, 0, n)
                            writtenBytes += n
                            playedSuccessfully = true

                            val now = System.currentTimeMillis()
                            if (now - lastStateUpdateMs > 100) {
                                lastStateUpdateMs = now
                                val currentPosMs = ((writtenBytes * 1000L) / bytesPerSecond).coerceAtMost(durMs)
                                withContext(Dispatchers.Main) {
                                    if (_state.value.currentSong?.id == song.id && _state.value.isPlaying) {
                                        _state.value = _state.value.copy(position = currentPosMs)
                                    }
                                }
                            }
                        }
                    } finally {
                        try {
                            pcmStream.close()
                            rawStream.close()
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    println("DesktopMusicPlayer: AudioSystem failed for ${song.title}: ${e.message}")
                }

                // Method 2: Fallback to JLayer MP3 Player
                if (!playedSuccessfully && isActive) {
                    try {
                        println("DesktopMusicPlayer: Falling back to JLayer player for ${song.title}")
                        val fileStream = BufferedInputStream(file.inputStream())
                        val player = Player(fileStream)
                        jlayerPlayer = player

                        val durMs = if (song.duration > 0) song.duration else 180_000L
                        withContext(Dispatchers.Main) {
                            if (_state.value.currentSong?.id == song.id) {
                                _state.value = _state.value.copy(duration = durMs)
                            }
                        }

                        val startTime = System.currentTimeMillis() - startPositionMs
                        val playJob = launch(Dispatchers.IO) {
                            try {
                                player.play()
                            } catch (_: Exception) {}
                        }

                        while (isActive && _state.value.isPlaying && !player.isComplete) {
                            val elapsed = System.currentTimeMillis() - startTime
                            withContext(Dispatchers.Main) {
                                if (_state.value.currentSong?.id == song.id && _state.value.isPlaying) {
                                    _state.value = _state.value.copy(position = elapsed.coerceAtMost(durMs))
                                }
                            }
                            delay(100)
                        }

                        if (player.isComplete && isActive) {
                            withContext(Dispatchers.Main) {
                                handleSongFinished()
                            }
                        }

                        try {
                            player.close()
                        } catch (_: Exception) {}
                        playJob.cancel()
                    } catch (e: Exception) {
                        println("DesktopMusicPlayer: JLayer failed for ${song.title}: ${e.message}")
                    }
                }

                stopAudio()
            }
        }
    }

    private suspend fun handleSongFinished() {
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> startSongPlayback(0L)
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> {
                if (currentIndex < currentPlaylist.size - 1) {
                    next()
                } else {
                    _state.value = _state.value.copy(isPlaying = false, position = 0L)
                }
            }
        }
    }

    private fun stopAudio() {
        try {
            activeLine?.stop()
            activeLine?.flush()
            activeLine?.close()
        } catch (_: Exception) {}
        activeLine = null

        try {
            jlayerPlayer?.close()
        } catch (_: Exception) {}
        jlayerPlayer = null
    }

    override suspend fun playNext(song: Song) {
        val list = currentPlaylist.toMutableList()
        list.add((currentIndex + 1).coerceAtMost(list.size), song)
        currentPlaylist = list
        _state.value = _state.value.copy(queue = currentPlaylist)
    }

    override suspend fun playNext(songs: List<Song>) {
        val list = currentPlaylist.toMutableList()
        list.addAll((currentIndex + 1).coerceAtMost(list.size), songs)
        currentPlaylist = list
        _state.value = _state.value.copy(queue = currentPlaylist)
    }

    override suspend fun addToQueue(song: Song) {
        val list = currentPlaylist.toMutableList()
        list.add(song)
        currentPlaylist = list
        _state.value = _state.value.copy(queue = currentPlaylist)
    }

    override suspend fun addToQueue(songs: List<Song>) {
        val list = currentPlaylist.toMutableList()
        list.addAll(songs)
        currentPlaylist = list
        _state.value = _state.value.copy(queue = currentPlaylist)
    }

    override fun removeFromQueue(index: Int) {
        if (index in currentPlaylist.indices) {
            val list = currentPlaylist.toMutableList()
            list.removeAt(index)
            currentPlaylist = list
            if (currentIndex >= currentPlaylist.size) {
                currentIndex = (currentPlaylist.size - 1).coerceAtLeast(0)
            }
            _state.value = _state.value.copy(queue = currentPlaylist)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in currentPlaylist.indices && toIndex in currentPlaylist.indices) {
            val list = currentPlaylist.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            currentPlaylist = list
            _state.value = _state.value.copy(queue = currentPlaylist)
        }
    }

    override suspend fun pause() {
        try {
            activeLine?.stop()
            jlayerPlayer?.close()
        } catch (_: Exception) {}
        _state.value = _state.value.copy(isPlaying = false)
    }

    override suspend fun resume() {
        if (_state.value.currentSong != null) {
            if (activeLine != null) {
                activeLine?.start()
                _state.value = _state.value.copy(isPlaying = true)
            } else {
                startSongPlayback(startPositionMs = _state.value.position)
            }
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        if (_state.value.currentSong != null) {
            startSongPlayback(startPositionMs = positionMs)
        }
    }

    override suspend fun next() {
        if (currentPlaylist.isNotEmpty()) {
            if (_state.value.shuffleEnabled && currentPlaylist.size > 1) {
                var nextIdx = (0 until currentPlaylist.size).random()
                while (nextIdx == currentIndex) {
                    nextIdx = (0 until currentPlaylist.size).random()
                }
                currentIndex = nextIdx
            } else {
                currentIndex = (currentIndex + 1) % currentPlaylist.size
            }
            startSongPlayback(0L)
        }
    }

    override suspend fun previous() {
        if (currentPlaylist.isNotEmpty()) {
            currentIndex = if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
            startSongPlayback(0L)
        }
    }

    override suspend fun stop() {
        playerScope.launch {
            playbackJob?.cancelAndJoin()
            stopAudio()
            currentPlaylist = emptyList()
            _state.value = PlayerState()
        }
    }

    override fun toggleShuffle() {
        val newShuffle = !_state.value.shuffleEnabled
        _state.value = _state.value.copy(shuffleEnabled = newShuffle)
    }

    override fun cycleRepeatMode() {
        val nextMode: RepeatMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }
}
