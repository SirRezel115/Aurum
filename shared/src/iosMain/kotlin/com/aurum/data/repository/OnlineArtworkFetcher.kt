package com.aurum.data.repository

actual object OnlineArtworkFetcher {
    actual suspend fun fetchAlbumArtworkUrl(artist: String, album: String): String? = null
    actual suspend fun fetchArtworkForTrack(title: String, artist: String, album: String): String? = null
    actual suspend fun downloadImageData(imageUrl: String): ByteArray? = null
    actual suspend fun searchAppleMusic(query: String, entity: String): List<AppleMusicSearchResult> = emptyList()
    actual suspend fun fetchArtistDiscography(artistName: String): List<AppleMusicSearchResult> = emptyList()
    actual suspend fun downloadAndCacheArtwork(artist: String, album: String, artworkUrl: String): String = artworkUrl
}
