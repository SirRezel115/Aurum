package com.aurum.player

import com.aurum.data.model.Song

/**
 * Playback state observed by the UI layer.
 */
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L, // ms
    val duration: Long = 0L, // ms
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimerMinutes: Int = 0,
    val crossfadeSeconds: Int = 0,
    val volume: Float = 1.0f,
    val queue: List<Song> = emptyList(),
    val lyrics: List<Pair<Long, String>> = emptyList(),
    val syncedLyrics: List<com.aurum.ui.player.SyncedLyricLine> = emptyList(),
    val waveformAmplitudes: List<Float> = emptyList(),
    val fftBands: List<Float> = emptyList(),
    val bassEnergy: Float = 0f,
    val albumArtColors: List<Int> = emptyList(),
)

/**
 * Repeat mode for the player.
 */
@Suppress("UNUSED_ENUM_ENTRY")
enum class RepeatMode {
    OFF, ONE, ALL
}

/**
 * Platform-agnostic music & media player interface.
 */
interface MusicPlayer {

    suspend fun play(song: Song)
    suspend fun play(songs: List<Song>, startIndex: Int)
    suspend fun playNext(song: Song)
    suspend fun playNext(songs: List<Song>)
    suspend fun addToQueue(song: Song)
    suspend fun addToQueue(songs: List<Song>)
    suspend fun pause()
    suspend fun resume()
    suspend fun seekTo(positionMs: Long)
    suspend fun next()
    suspend fun previous()
    suspend fun stop()
    fun toggleShuffle()
    fun cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) {}
    fun setPlaybackPitch(pitch: Float) {}
    fun setVolume(volume: Float) {}
    fun setCrossfadeSeconds(seconds: Int) {}
    fun startSleepTimer(minutes: Int) {}
    fun updateQueue(songs: List<Song>) {}
    fun removeFromQueue(index: Int) {}
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    fun getAudioSessionId(): Int = 0
    fun setEqualizerBandGain(band: Short, gain: Short) {}
    fun getEqualizerPresetNames(): List<String> = emptyList()
    fun setEqualizerPreset(preset: Short) {}
    fun openSystemEqualizer() {}

    val state: androidx.compose.runtime.State<PlayerState>
}
