package com.aurum.data.model

/**
 * Media type categorization.
 */
enum class MediaType {
    AUDIO, VIDEO
}

/**
 * Represents a song in the library.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val uri: String,
    val albumArtUri: String? = null,
    val dateAdded: Long = 0, // epoch seconds
    val isFavorite: Boolean = false,
    val isVideo: Boolean = false,
)

/**
 * Represents a video file in the library.
 */
data class VideoItem(
    val id: String,
    val title: String,
    val duration: Long, // ms
    val uri: String,
    val thumbnailUri: String? = null,
    val resolution: String = "HD",
    val dateAdded: Long = 0,
)

/**
 * Represents an album in the library.
 */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUri: String? = null,
    val trackCount: Int = 0,
    val albumArtColors: List<Int> = emptyList(),
)

/**
 * Represents an artist in the library.
 */
data class Artist(
    val id: String,
    val name: String,
    val albumArtUri: String? = null,
    val trackCount: Int = 0,
)

/**
 * Represents a playlist.
 */
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    @OptIn(kotlin.time.ExperimentalTime::class)
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
)
