package com.aurum.data.repository

data class AppleMusicSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val type: String
)

expect object OnlineArtworkFetcher {
    suspend fun fetchAlbumArtworkUrl(artist: String, album: String): String?
    suspend fun fetchArtworkForTrack(title: String, artist: String, album: String): String?
    suspend fun downloadImageData(imageUrl: String): ByteArray?
    suspend fun searchAppleMusic(query: String, entity: String = "album"): List<AppleMusicSearchResult>
    suspend fun fetchArtistDiscography(artistName: String): List<AppleMusicSearchResult>
    suspend fun downloadAndCacheArtwork(artist: String, album: String, artworkUrl: String): String
}
