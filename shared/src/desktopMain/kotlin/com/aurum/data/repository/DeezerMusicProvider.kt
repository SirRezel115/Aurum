package com.aurum.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

actual object DeezerMusicProvider {

    actual suspend fun searchDeezerTracks(query: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val results = mutableListOf<DeezerTrackSearchResult>()
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val urlString = "https://api.deezer.com/search?q=$encodedQuery"
            val url = URI(urlString).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val blocks = responseText.split("{\"id\":")

                for ((index, block) in blocks.withIndex()) {
                    if (index == 0) continue
                    val id = block.substringBefore(",").trim().replace("\"", "")
                    val title = extractJsonField(block, "\"title\":\"")
                    val durationStr = extractJsonField(block, "\"duration\":")
                    val duration = durationStr.toLongOrNull() ?: 0L
                    val previewUrl = extractJsonField(block, "\"preview\":\"").replace("\\/", "/")
                    val trackPosStr = extractJsonField(block, "\"track_position\":")
                    val trackPos = trackPosStr.toIntOrNull() ?: index

                    val artistName = extractJsonField(block, "\"name\":\"")
                    val albumTitle = extractJsonField(block, "\"title\":\"")
                    val rawCover = extractJsonField(block, "\"cover_xl\":\"").ifBlank {
                        extractJsonField(block, "\"cover_big\":\"").ifBlank {
                            extractJsonField(block, "\"cover_medium\":\"")
                        }
                    }.replace("\\/", "/")
                    val coverHd = rawCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")

                    if (title.isNotBlank() && id.isNotBlank()) {
                        results.add(
                            DeezerTrackSearchResult(
                                id = id,
                                title = title,
                                artist = artistName.ifBlank { "Unknown Artist" },
                                album = albumTitle.ifBlank { "Unknown Album" },
                                durationSeconds = duration,
                                previewStreamUrl = previewUrl,
                                artworkUrl = coverHd,
                                trackNumber = trackPos
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        results
    }

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
        message = "Desktop stub"
    )

    private fun extractJsonField(json: String, keyPattern: String): String {
        val at = json.indexOf(keyPattern)
        if (at == -1) return ""
        val start = at + keyPattern.length
        val endQuote = json.indexOf("\"", start)
        if (endQuote == -1) {
            val comma = json.indexOf(",", start)
            val brace = json.indexOf("}", start)
            val end = listOf(comma, brace).filter { it != -1 }.minOrNull() ?: json.length
            return json.substring(start, end).trim()
        }
        return json.substring(start, endQuote).trim()
    }
}
