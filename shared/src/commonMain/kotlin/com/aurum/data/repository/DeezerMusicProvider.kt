package com.aurum.data.repository

import com.aurum.data.model.Song

data class DeezerTrackSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Long,
    val previewStreamUrl: String,
    val artworkUrl: String,
    val trackNumber: Int = 0,
)

data class DeezerAlbumSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val trackCount: Int = 0,
    val releaseDate: String = ""
)

data class DeezerArtistSearchResult(
    val id: String,
    val name: String,
    val pictureUrl: String,
    val fanCount: Long = 0L
)

data class DeezerPlaylistSearchResult(
    val id: String,
    val title: String,
    val ownerName: String,
    val coverUrl: String,
    val trackCount: Int = 0
)

enum class DeezerLinkKind {
    TRACK, ALBUM, ARTIST, PLAYLIST
}

data class DeezerLinkResult(
    val kind: DeezerLinkKind,
    val id: String
)

fun DeezerTrackSearchResult.toSong(
    overrideStreamUrl: String? = null,
    overrideArtworkUrl: String? = null
): Song {
    val streamUrl = overrideStreamUrl?.ifBlank { null }
        ?: previewStreamUrl.ifBlank { "" }
    val artUrl = overrideArtworkUrl?.ifBlank { null }
        ?: artworkUrl.ifBlank { null }

    return Song(
        id = "deezer_stream_$id",
        title = title,
        artist = artist,
        album = album,
        duration = durationSeconds * 1000L,
        uri = streamUrl,
        albumArtUri = artUrl,
        dateAdded = 0L
    )
}

data class DeezerArlStatus(
    val isValid: Boolean,
    val userName: String? = null,
    val canStreamHq: Boolean = false,
    val maxQuality: DeezerAudioQuality = DeezerAudioQuality.MP3_320,
    val message: String
)

expect object DeezerMusicProvider {
    suspend fun searchDeezerTracks(query: String): List<DeezerTrackSearchResult>
    suspend fun searchDeezerAlbums(query: String): List<DeezerAlbumSearchResult>
    suspend fun getDeezerArtistAlbums(artistName: String): List<DeezerAlbumSearchResult>
    suspend fun searchDeezerArtists(query: String): List<DeezerArtistSearchResult>
    suspend fun searchDeezerPlaylists(query: String): List<DeezerPlaylistSearchResult>
    suspend fun getDeezerAlbumTracks(albumId: String): List<DeezerTrackSearchResult>
    suspend fun getDeezerPlaylistTracks(playlistId: String): List<DeezerTrackSearchResult>
    suspend fun getDeezerFlowTracks(arlToken: String?, configId: String = "default"): List<DeezerTrackSearchResult>
    suspend fun getUserSavedTracks(arlToken: String): List<DeezerTrackSearchResult>
    suspend fun getUserSavedAlbums(arlToken: String): List<DeezerAlbumSearchResult>
    suspend fun getUserSavedArtists(arlToken: String): List<DeezerArtistSearchResult>
    suspend fun getUserPlaylists(arlToken: String): List<DeezerPlaylistSearchResult>
    suspend fun parseDeezerLink(url: String): DeezerLinkResult?
    suspend fun resolveFullTrackStreamUrl(trackId: String, arlToken: String?): String?
    suspend fun prepareLocalStreamCacheFile(track: DeezerTrackSearchResult, arlToken: String? = null): String?
    suspend fun downloadTrackAudioFile(
        track: DeezerTrackSearchResult,
        downloadDir: String?,
        arlToken: String? = null
    ): String?
    suspend fun validateArlToken(arlToken: String): DeezerArlStatus
}
