package com.aurum.player

import com.aurum.data.model.Song
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * iOS MusicPlayer (stub).
 * Implementation in Phase 4.
 */
class IosMusicPlayer : MusicPlayer {

    private val _state = mutableStateOf(PlayerState())
    override val state: State<PlayerState> = _state

    override suspend fun play(song: Song) {}
    override suspend fun play(songs: List<Song>, startIndex: Int) {}
    override suspend fun playNext(song: Song) {}
    override suspend fun playNext(songs: List<Song>) {}
    override suspend fun addToQueue(song: Song) {}
    override suspend fun addToQueue(songs: List<Song>) {}
    override suspend fun pause() {}
    override suspend fun resume() {}
    override suspend fun seekTo(positionMs: Long) {}
    override suspend fun next() {}
    override suspend fun previous() {}
    override suspend fun stop() {}
    override fun toggleShuffle() {}
    override fun cycleRepeatMode() {}
}
