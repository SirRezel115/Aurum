package com.aurum.ui.player

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurum.player.MusicPlayer
import com.aurum.player.PlayerState
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicPlayer: MusicPlayer,
) : ViewModel() {

    val playerState: State<PlayerState>
        get() = musicPlayer.state

    fun togglePlayPause() {
        viewModelScope.launch {
            if (playerState.value.isPlaying) {
                musicPlayer.pause()
            } else {
                musicPlayer.resume()
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            musicPlayer.stop()
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            musicPlayer.seekTo(positionMs)
        }
    }

    fun next() {
        viewModelScope.launch {
            musicPlayer.next()
        }
    }

    fun playNext(song: com.aurum.data.model.Song) {
        viewModelScope.launch {
            musicPlayer.playNext(song)
        }
    }

    fun addToQueue(song: com.aurum.data.model.Song) {
        viewModelScope.launch {
            musicPlayer.addToQueue(song)
        }
    }

    fun previous() {
        viewModelScope.launch {
            musicPlayer.previous()
        }
    }

    fun toggleShuffle() {
        musicPlayer.toggleShuffle()
    }

    fun cycleRepeatMode() {
        musicPlayer.cycleRepeatMode()
    }

    fun cyclePlaybackSpeed() {
        val currentSpeed = playerState.value.playbackSpeed
        val nextSpeed = when {
            currentSpeed < 0.9f -> 1.0f
            currentSpeed < 1.1f -> 1.25f
            currentSpeed < 1.3f -> 1.5f
            currentSpeed < 1.6f -> 2.0f
            else -> 0.5f
        }
        musicPlayer.setPlaybackSpeed(nextSpeed)
    }

    fun setPitch(pitch: Float) {
        musicPlayer.setPlaybackPitch(pitch)
    }

    fun setVolume(volume: Float) {
        musicPlayer.setVolume(volume)
    }

    fun startSleepTimer(minutes: Int) {
        musicPlayer.startSleepTimer(minutes)
    }

    fun removeFromQueue(index: Int) {
        musicPlayer.removeFromQueue(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicPlayer.moveQueueItem(fromIndex, toIndex)
    }

    fun openSystemEqualizer() {
        musicPlayer.openSystemEqualizer()
    }
}
