package com.aurum.scanner

import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Song
import com.aurum.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScanProgress(
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val currentItemName: String = "",
    val isScanning: Boolean = false
) {
    val fraction: Float
        get() = if (totalCount > 0) (scannedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Platform-agnostic music and media scanner interface.
 */
interface MusicScanner {

    val scanProgress: StateFlow<ScanProgress>
        get() = MutableStateFlow(ScanProgress()).asStateFlow()

    /**
     * Scans the device for music files and returns them.
     */
    suspend fun scanSongs(): List<Song>

    /**
     * Scans the device for video files and returns them.
     */
    suspend fun scanVideos(): List<VideoItem> = emptyList()

    /**
     * Scans the device for albums.
     */
    suspend fun scanAlbums(): List<Album>

    /**
     * Scans the device for artists.
     */
    suspend fun scanArtists(): List<Artist>

    /**
     * Searches songs by a query string.
     */
    suspend fun searchSongs(query: String): List<Song>
}
