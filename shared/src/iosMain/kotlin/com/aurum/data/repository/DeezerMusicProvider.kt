package com.aurum.data.repository

actual object DeezerMusicProvider {
    actual suspend fun searchDeezerTracks(query: String): List<DeezerTrackSearchResult> = emptyList()
    actual suspend fun searchDeezerAlbums(query: String): List<DeezerAlbumSearchResult> = emptyList()
    actual suspend fun getDeezerArtistAlbums(artistName: String): List<DeezerAlbumSearchResult> = emptyList()
    actual suspend fun searchDeezerArtists(query: String): List<DeezerArtistSearchResult> = emptyList()
    actual suspend fun searchDeezerPlaylists(query: String): List<DeezerPlaylistSearchResult> = emptyList()

    actual suspend fun getDeezerAlbumTracks(albumId: String): List<DeezerTrackSearchResult> = emptyList()
    actual suspend fun getDeezerPlaylistTracks(playlistId: String): List<DeezerTrackSearchResult> = emptyList()
    actual suspend fun getDeezerFlowTracks(arlToken: String?, configId: String): List<DeezerTrackSearchResult> = emptyList()

    actual suspend fun getUserSavedTracks(arlToken: String): List<DeezerTrackSearchResult> = emptyList()
    actual suspend fun getUserSavedAlbums(arlToken: String): List<DeezerAlbumSearchResult> = emptyList()
    actual suspend fun getUserSavedArtists(arlToken: String): List<DeezerArtistSearchResult> = emptyList()
    actual suspend fun getUserPlaylists(arlToken: String): List<DeezerPlaylistSearchResult> = emptyList()

    actual suspend fun parseDeezerLink(url: String): DeezerLinkResult? = null

    actual suspend fun resolveFullTrackStreamUrl(trackId: String, arlToken: String?): String? = null
    actual suspend fun prepareLocalStreamCacheFile(track: DeezerTrackSearchResult, arlToken: String?): String? = null
    actual suspend fun downloadTrackAudioFile(
        track: DeezerTrackSearchResult,
        downloadDir: String?,
        arlToken: String?
    ): String? = null

    actual suspend fun validateArlToken(arlToken: String): DeezerArlStatus = DeezerArlStatus(
        isValid = false,
        message = "iOS platform stub"
    )
}
