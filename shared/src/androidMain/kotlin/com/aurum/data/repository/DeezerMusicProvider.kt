package com.aurum.data.repository

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

actual object DeezerMusicProvider {

    var applicationContext: android.content.Context? = null

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    private const val GATEWAY_URL = "https://www.deezer.com/ajax/gw-light.php"
    private const val PUBLIC_API_URL = "https://api.deezer.com"

    private val paceMutex = Mutex()
    private var lastRequestTimestamp = 0L
    private const val PACE_INTERVAL_MS = 110L

    private suspend fun enforcePacing() {
        paceMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestTimestamp
            if (diff < PACE_INTERVAL_MS) {
                delay(PACE_INTERVAL_MS - diff)
            }
            lastRequestTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun httpGetJson(urlStr: String): JSONObject? = withContext(Dispatchers.IO) {
        var retries = 0
        while (retries < 3) {
            enforcePacing()
            try {
                val url = URI(urlStr).toURL()
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")

                if (conn.responseCode == 429) {
                    retries++
                    delay(1000L * retries)
                    continue
                }

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = runCatching { JSONObject(text) }.getOrNull()
                    if (json != null && json.has("error")) {
                        val errObj = json.optJSONObject("error")
                        val errCode = errObj?.optInt("code", 0) ?: 0
                        if (errCode == 4 || errCode == 700) {
                            retries++
                            delay(1000L * retries)
                            continue
                        }
                    }
                    return@withContext json
                }
            } catch (e: Exception) {
                Log.w("DeezerMusicProvider", "GET error for $urlStr: ${e.message}")
            }
            break
        }
        null
    }

    private suspend fun gwPostJson(method: String, payload: JSONObject, arlToken: String?): JSONObject? = withContext(Dispatchers.IO) {
        val cleanToken = arlToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
        val session = DeezerSessionManager.ensureActiveSession(cleanToken)

        val apiToken = session?.checkForm ?: ""
        val sid = session?.sid ?: ""

        val urlString = "$GATEWAY_URL?method=$method&input=3&api_version=1.0&api_token=$apiToken"

        try {
            val url = URI(urlString).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", USER_AGENT)

            val cookieHeader = when {
                cleanToken.isNotBlank() && sid.isNotBlank() -> "arl=$cleanToken; sid=$sid"
                cleanToken.isNotBlank() -> "arl=$cleanToken"
                else -> ""
            }
            if (cookieHeader.isNotBlank()) {
                conn.setRequestProperty("Cookie", cookieHeader)
            }
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext runCatching { JSONObject(jsonText) }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e("DeezerMusicProvider", "GW POST error ($method): ${e.message}")
        }
        null
    }

    actual suspend fun searchDeezerTracks(query: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        val json = httpGetJson("$PUBLIC_API_URL/search?q=$encodedQuery&limit=50") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()

        parseTrackArray(dataArray)
    }

    actual suspend fun searchDeezerAlbums(query: String): List<DeezerAlbumSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        val json = httpGetJson("$PUBLIC_API_URL/search/album?q=$encodedQuery&limit=30") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerAlbumSearchResult>()
        for (i in 0 until dataArray.length()) {
            val albumObj = dataArray.optJSONObject(i) ?: continue
            val id = albumObj.optLong("id").toString()
            val title = albumObj.optString("title", "Unknown Album")
            val artistObj = albumObj.optJSONObject("artist")
            val artistName = artistObj?.optString("name", "Unknown Artist") ?: "Unknown Artist"
            val rawCover = albumObj.optString("cover_xl").ifBlank { albumObj.optString("cover_big") }
            val coverUrl = rawCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")
            val trackCount = albumObj.optInt("nb_tracks", 0)
            val releaseDate = albumObj.optString("release_date", "")

            if (id.isNotBlank()) {
                results.add(
                    DeezerAlbumSearchResult(
                        id = id,
                        title = title,
                        artist = artistName,
                        coverUrl = coverUrl,
                        trackCount = trackCount,
                        releaseDate = releaseDate
                    )
                )
            }
        }
        results
    }

    actual suspend fun getDeezerArtistAlbums(artistName: String): List<DeezerAlbumSearchResult> = withContext(Dispatchers.IO) {
        val q = artistName.trim()
        if (q.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        val results = mutableListOf<DeezerAlbumSearchResult>()

        // 1. Match exact artist ID from artist search
        val artistSearch = httpGetJson("$PUBLIC_API_URL/search/artist?q=$encodedQuery&limit=10")
        val artistArr = artistSearch?.optJSONArray("data")
        var bestArtistId: String? = null

        if (artistArr != null) {
            for (i in 0 until artistArr.length()) {
                val artObj = artistArr.optJSONObject(i) ?: continue
                val name = artObj.optString("name", "")
                val id = artObj.optLong("id", 0L).takeIf { it != 0L }?.toString()
                if (!id.isNullOrBlank()) {
                    if (name.equals(q, ignoreCase = true)) {
                        bestArtistId = id
                        break
                    } else if (bestArtistId == null) {
                        bestArtistId = id
                    }
                }
            }
        }

        // 2. Query official artist albums endpoint
        if (!bestArtistId.isNullOrBlank()) {
            val json = httpGetJson("$PUBLIC_API_URL/artist/$bestArtistId/albums?limit=100")
            val dataArray = json?.optJSONArray("data")
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val albumObj = dataArray.optJSONObject(i) ?: continue
                    val id = albumObj.optLong("id").toString()
                    val title = albumObj.optString("title", "Unknown Album")
                    val artistObj = albumObj.optJSONObject("artist")
                    val artName = artistObj?.optString("name")?.ifBlank { q } ?: q

                    var trackCount = albumObj.optInt("nb_tracks", 0)
                    if (trackCount <= 0 && id.isNotBlank()) {
                        val detail = httpGetJson("$PUBLIC_API_URL/album/$id")
                        trackCount = detail?.optInt("nb_tracks", 0) ?: 0
                    }

                    val itunesArt = OnlineArtworkFetcher.fetchAlbumArtworkUrl(artName, title)
                    val rawCover = albumObj.optString("cover_xl").ifBlank { albumObj.optString("cover_big") }
                    val coverUrl = itunesArt?.ifBlank { null }
                        ?: rawCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")
                    val releaseDate = albumObj.optString("release_date", "")

                    if (id.isNotBlank() && !results.any { it.id == id }) {
                        results.add(
                            DeezerAlbumSearchResult(
                                id = id,
                                title = title,
                                artist = artName,
                                coverUrl = coverUrl,
                                trackCount = trackCount,
                                releaseDate = releaseDate
                            )
                        )
                    }
                }
            }
        }

        // 3. Supplemental query by artist string to capture any remaining releases
        val searchJson = httpGetJson("$PUBLIC_API_URL/search/album?q=artist:\"$encodedQuery\"&limit=100")
        val searchArray = searchJson?.optJSONArray("data")
        if (searchArray != null) {
            for (i in 0 until searchArray.length()) {
                val albumObj = searchArray.optJSONObject(i) ?: continue
                val id = albumObj.optLong("id").toString()
                val title = albumObj.optString("title", "Unknown Album")
                val artistObj = albumObj.optJSONObject("artist")
                val artName = artistObj?.optString("name")?.ifBlank { q } ?: q
                var trackCount = albumObj.optInt("nb_tracks", 0)
                if (trackCount <= 0 && id.isNotBlank()) {
                    val detail = httpGetJson("$PUBLIC_API_URL/album/$id")
                    trackCount = detail?.optInt("nb_tracks", 0) ?: 0
                }

                val itunesArt = OnlineArtworkFetcher.fetchAlbumArtworkUrl(artName, title)
                val rawCover = albumObj.optString("cover_xl").ifBlank { albumObj.optString("cover_big") }
                val coverUrl = itunesArt?.ifBlank { null }
                    ?: rawCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")
                val releaseDate = albumObj.optString("release_date", "")

                if (id.isNotBlank() && !results.any { it.id == id }) {
                    results.add(
                        DeezerAlbumSearchResult(
                            id = id,
                            title = title,
                            artist = artName,
                            coverUrl = coverUrl,
                            trackCount = trackCount,
                            releaseDate = releaseDate
                        )
                    )
                }
            }
        }

        results
    }

    actual suspend fun searchDeezerArtists(query: String): List<DeezerArtistSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        val json = httpGetJson("$PUBLIC_API_URL/search/artist?q=$encodedQuery&limit=30") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerArtistSearchResult>()
        for (i in 0 until dataArray.length()) {
            val artistObj = dataArray.optJSONObject(i) ?: continue
            val id = artistObj.optLong("id").toString()
            val name = artistObj.optString("name", "Unknown Artist")
            val rawPicture = artistObj.optString("picture_xl").ifBlank { artistObj.optString("picture_big") }
            val pictureUrl = rawPicture.replace("250x250", "1000x1000").replace("500x500", "1000x1000")
            val fanCount = artistObj.optLong("nb_fan", 0L)

            if (id.isNotBlank()) {
                results.add(
                    DeezerArtistSearchResult(
                        id = id,
                        name = name,
                        pictureUrl = pictureUrl,
                        fanCount = fanCount
                    )
                )
            }
        }
        results
    }

    actual suspend fun searchDeezerPlaylists(query: String): List<DeezerPlaylistSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(q, "UTF-8")
        val json = httpGetJson("$PUBLIC_API_URL/search/playlist?q=$encodedQuery&limit=30") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerPlaylistSearchResult>()
        for (i in 0 until dataArray.length()) {
            val playlistObj = dataArray.optJSONObject(i) ?: continue
            val id = playlistObj.optLong("id").toString()
            val title = playlistObj.optString("title", "Unknown Playlist")
            val userObj = playlistObj.optJSONObject("user")
            val ownerName = userObj?.optString("name", "Deezer") ?: "Deezer"
            val rawCover = playlistObj.optString("picture_xl").ifBlank { playlistObj.optString("picture_big") }
            val coverUrl = rawCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")
            val trackCount = playlistObj.optInt("nb_tracks", 0)

            if (id.isNotBlank()) {
                results.add(
                    DeezerPlaylistSearchResult(
                        id = id,
                        title = title,
                        ownerName = ownerName,
                        coverUrl = coverUrl,
                        trackCount = trackCount
                    )
                )
            }
        }
        results
    }

    actual suspend fun getDeezerAlbumTracks(albumId: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val cleanId = albumId.trim()
        val albumInfo = httpGetJson("$PUBLIC_API_URL/album/$cleanId")
        val rawAlbumCover = albumInfo?.optString("cover_xl")?.takeIf { it.isNotBlank() && it != "null" }
            ?: albumInfo?.optString("cover_big")?.takeIf { it.isNotBlank() && it != "null" }
            ?: ""
        val albumCoverUrl = rawAlbumCover.replace("250x250", "1000x1000").replace("500x500", "1000x1000")

        val json = httpGetJson("$PUBLIC_API_URL/album/$cleanId/tracks?limit=300") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()
        val tracks = parseTrackArray(dataArray)

        if (albumCoverUrl.isNotBlank()) {
            tracks.map { track ->
                if (track.artworkUrl.isBlank()) track.copy(artworkUrl = albumCoverUrl) else track
            }
        } else {
            tracks
        }
    }

    actual suspend fun getDeezerPlaylistTracks(playlistId: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.trim()
        val session = DeezerSessionManager.getSessionInfo()
        val token = session?.arl ?: ""

        val payload = JSONObject().apply {
            put("playlist_id", cleanId)
            put("start", 0)
            put("nb", 1000)
            put("tags", true)
            put("header", true)
        }

        val gwRes = gwPostJson("deezer.pagePlaylist", payload, token)
        val results = gwRes?.optJSONObject("results")
        val gwSongs = results?.optJSONObject("SONGS")?.optJSONArray("data")
            ?: results?.optJSONArray("SONGS")

        if (gwSongs != null && gwSongs.length() > 0) {
            val tracks = parseTrackArray(gwSongs)
            if (tracks.isNotEmpty()) return@withContext tracks
        }

        val json = httpGetJson("$PUBLIC_API_URL/playlist/$cleanId/tracks?limit=500") ?: return@withContext emptyList()
        val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()
        parseTrackArray(dataArray)
    }

    actual suspend fun getDeezerFlowTracks(arlToken: String?, configId: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val cleanArl = arlToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
        val session = DeezerSessionManager.connect(cleanArl) ?: return@withContext emptyList()

        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("config_id", configId)
        }

        val response = gwPostJson("radio.getUserRadio", payload, cleanArl) ?: return@withContext emptyList()
        val resultsObj = response.optJSONObject("results") ?: return@withContext emptyList()
        val dataArr = resultsObj.optJSONArray("data") ?: return@withContext emptyList()

        parseTrackArray(dataArr)
    }

    actual suspend fun getUserSavedTracks(arlToken: String): List<DeezerTrackSearchResult> = withContext(Dispatchers.IO) {
        val session = DeezerSessionManager.connect(arlToken) ?: return@withContext emptyList()
        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("start", 0)
            put("nb", 2000)
        }
        val response = gwPostJson("favorite_song.getList", payload, arlToken) ?: return@withContext emptyList()
        val resultsObj = response.optJSONObject("results") ?: return@withContext emptyList()
        val dataArr = resultsObj.optJSONArray("data") ?: return@withContext emptyList()
        parseTrackArray(dataArr)
    }

    actual suspend fun getUserSavedAlbums(arlToken: String): List<DeezerAlbumSearchResult> = withContext(Dispatchers.IO) {
        val session = DeezerSessionManager.connect(arlToken) ?: return@withContext emptyList()
        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("tab", "albums")
            put("nb", 1000)
        }
        val response = gwPostJson("deezer.pageProfile", payload, arlToken) ?: return@withContext emptyList()
        val resultsObj = response.optJSONObject("results") ?: return@withContext emptyList()
        val tabObj = resultsObj.optJSONObject("TAB") ?: return@withContext emptyList()
        val albumsObj = tabObj.optJSONObject("albums") ?: return@withContext emptyList()
        val dataArr = albumsObj.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerAlbumSearchResult>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val id = item.optString("ALB_ID").ifBlank { item.optString("id") }
            val title = item.optString("ALB_TITLE").ifBlank { item.optString("title") }
            val artistName = item.optString("ART_NAME").ifBlank { item.optString("artist_name") }
            val coverMd5 = item.optString("ALB_PICTURE").ifBlank { item.optString("picture") }
            val coverUrl = if (coverMd5.isNotBlank()) {
                "https://cdn-images.dzcdn.net/images/cover/$coverMd5/1000x1000-000000-80-0-0.jpg"
            } else ""

            if (id.isNotBlank()) {
                results.add(
                    DeezerAlbumSearchResult(
                        id = id,
                        title = title,
                        artist = artistName,
                        coverUrl = coverUrl,
                        trackCount = item.optInt("NB_SONG", 0)
                    )
                )
            }
        }
        results
    }

    actual suspend fun getUserSavedArtists(arlToken: String): List<DeezerArtistSearchResult> = withContext(Dispatchers.IO) {
        val session = DeezerSessionManager.connect(arlToken) ?: return@withContext emptyList()
        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("tab", "artists")
            put("nb", 1000)
        }
        val response = gwPostJson("deezer.pageProfile", payload, arlToken) ?: return@withContext emptyList()
        val resultsObj = response.optJSONObject("results") ?: return@withContext emptyList()
        val tabObj = resultsObj.optJSONObject("TAB") ?: return@withContext emptyList()
        val artistsObj = tabObj.optJSONObject("artists") ?: return@withContext emptyList()
        val dataArr = artistsObj.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerArtistSearchResult>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val id = item.optString("ART_ID").ifBlank { item.optString("id") }
            val name = item.optString("ART_NAME").ifBlank { item.optString("name") }
            val pictureMd5 = item.optString("ART_PICTURE").ifBlank { item.optString("picture") }
            val pictureUrl = if (pictureMd5.isNotBlank()) {
                "https://cdn-images.dzcdn.net/images/artist/$pictureMd5/1000x1000-000000-80-0-0.jpg"
            } else ""

            if (id.isNotBlank()) {
                results.add(
                    DeezerArtistSearchResult(
                        id = id,
                        name = name,
                        pictureUrl = pictureUrl
                    )
                )
            }
        }
        results
    }

    actual suspend fun getUserPlaylists(arlToken: String): List<DeezerPlaylistSearchResult> = withContext(Dispatchers.IO) {
        val session = DeezerSessionManager.connect(arlToken) ?: return@withContext emptyList()
        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("tab", "playlists")
            put("nb", 1000)
        }
        val response = gwPostJson("deezer.pageProfile", payload, arlToken) ?: return@withContext emptyList()
        val resultsObj = response.optJSONObject("results") ?: return@withContext emptyList()
        val tabObj = resultsObj.optJSONObject("TAB") ?: return@withContext emptyList()
        val playlistsObj = tabObj.optJSONObject("playlists") ?: return@withContext emptyList()
        val dataArr = playlistsObj.optJSONArray("data") ?: return@withContext emptyList()

        val results = mutableListOf<DeezerPlaylistSearchResult>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val id = item.optString("PLAYLIST_ID").ifBlank { item.optString("id") }
            val title = item.optString("TITLE").ifBlank { item.optString("title") }
            val owner = item.optString("PARENT_USERNAME").ifBlank { item.optString("CREATOR_NAME") }
            val coverMd5 = item.optString("PLAYLIST_PICTURE").ifBlank { item.optString("picture") }
            val coverUrl = if (coverMd5.isNotBlank()) {
                "https://cdn-images.dzcdn.net/images/playlist/$coverMd5/1000x1000-000000-80-0-0.jpg"
            } else ""

            if (id.isNotBlank()) {
                results.add(
                    DeezerPlaylistSearchResult(
                        id = id,
                        title = title,
                        ownerName = owner.ifBlank { "Deezer User" },
                        coverUrl = coverUrl,
                        trackCount = item.optInt("NB_SONG", 0)
                    )
                )
            }
        }
        results
    }

    actual suspend fun parseDeezerLink(url: String): DeezerLinkResult? = withContext(Dispatchers.IO) {
        var currentUrl = url.trim()
        if (currentUrl.isBlank()) return@withContext null

        try {
            if (currentUrl.contains("dzr.page.link") || currentUrl.contains("deezer.page.link") || currentUrl.contains("link.deezer.com")) {
                var redirects = 0
                while (redirects < 5) {
                    val conn = URI(currentUrl).toURL().openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location")
                        if (!loc.isNullOrBlank()) {
                            currentUrl = loc
                            redirects++
                            continue
                        }
                    }
                    break
                }
            }

            val uri = URI(currentUrl)
            val path = uri.path ?: ""
            val segments = path.split("/").filter { it.isNotBlank() }

            var kind: DeezerLinkKind? = null
            var id: String? = null

            for (i in segments.indices) {
                val seg = segments[i].lowercase()
                if (seg in listOf("track", "album", "artist", "playlist") && i + 1 < segments.size) {
                    kind = when (seg) {
                        "track" -> DeezerLinkKind.TRACK
                        "album" -> DeezerLinkKind.ALBUM
                        "artist" -> DeezerLinkKind.ARTIST
                        else -> DeezerLinkKind.PLAYLIST
                    }
                    val targetId = segments[i + 1]
                    if (targetId.all { it.isDigit() }) {
                        id = targetId
                        break
                    }
                }
            }

            if (kind != null && !id.isNullOrBlank()) {
                return@withContext DeezerLinkResult(kind = kind, id = id)
            }
        } catch (e: Exception) {
            Log.e("DeezerMusicProvider", "Error parsing Deezer link $url: ${e.message}")
        }
        null
    }

    actual suspend fun resolveFullTrackStreamUrl(
        trackId: String,
        arlToken: String?
    ): String? = withContext(Dispatchers.IO) {
        val cleanTrackId = trackId.removePrefix("deezer_stream_").trim()
        val token = arlToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""

        val session = DeezerSessionManager.connect(token)
            ?: DeezerSessionManager.getSessionInfo()
            ?: DeezerSessionManager.connect("")

        val licenseToken = session?.licenseToken ?: ""

        val gatewayPayload = JSONObject().apply {
            put("SNG_ID", cleanTrackId)
        }

        val songData = gwPostJson("song.getData", gatewayPayload, token)
        val resultsObj = songData?.optJSONObject("results")

        val fallbackObj = resultsObj?.optJSONObject("FALLBACK")
        val dataObj = if (fallbackObj != null && !fallbackObj.isNull("SNG_ID")) fallbackObj else resultsObj

        val effectiveTrackId = dataObj?.optString("SNG_ID")?.ifBlank { cleanTrackId } ?: cleanTrackId
        val trackToken = dataObj?.optString("TRACK_TOKEN")

        if (!trackToken.isNullOrBlank()) {
            val formatsToTry = when (session?.maxQuality) {
                DeezerAudioQuality.FLAC -> listOf("FLAC", "MP3_320", "MP3_128")
                DeezerAudioQuality.MP3_320 -> listOf("MP3_320", "MP3_128")
                else -> listOf("MP3_128", "MP3_320")
            }

            for (formatName in formatsToTry) {
                try {
                    val mediaUrl = "https://media.deezer.com/v1/get_url"
                    val mediaConn = URI(mediaUrl).toURL().openConnection() as HttpURLConnection
                    mediaConn.connectTimeout = 8000
                    mediaConn.readTimeout = 8000
                    mediaConn.requestMethod = "POST"
                    mediaConn.setRequestProperty("User-Agent", USER_AGENT)
                    if (token.isNotBlank()) {
                        mediaConn.setRequestProperty("Cookie", "arl=$token")
                    } else if (!session?.sid.isNullOrBlank()) {
                        mediaConn.setRequestProperty("Cookie", "sid=${session.sid}")
                    }
                    mediaConn.setRequestProperty("Content-Type", "application/json")
                    mediaConn.doOutput = true

                    val effectiveLicenseToken = session?.licenseToken?.ifBlank { null }
                        ?: licenseToken.ifBlank { null }
                        ?: trackToken

                    val mediaPayload = JSONObject().apply {
                        put("license_token", effectiveLicenseToken)
                        put("track_tokens", JSONArray().apply { put(trackToken) })
                        put("media", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "FULL")
                                put("formats", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("cipher", "BF_CBC_STRIPE")
                                        put("format", formatName)
                                    })
                                })
                            })
                        })
                    }
                    mediaConn.outputStream.use { it.write(mediaPayload.toString().toByteArray(Charsets.UTF_8)) }

                    if (mediaConn.responseCode == 200) {
                        val mediaJsonText = mediaConn.inputStream.bufferedReader().use { it.readText() }
                        val mediaObj = runCatching { JSONObject(mediaJsonText) }.getOrNull()
                        val dataArr = mediaObj?.optJSONArray("data")
                        if (dataArr != null && dataArr.length() > 0) {
                            val firstMedia = dataArr.optJSONObject(0)
                            val mediaArr = firstMedia?.optJSONArray("media")
                            val firstSource = mediaArr?.optJSONObject(0)
                            val sourcesArr = firstSource?.optJSONArray("sources")
                            val directUrl = sourcesArr?.optJSONObject(0)?.optString("url")

                            if (!directUrl.isNullOrBlank()) {
                                Log.d("DeezerMusicProvider", "Resolved full authenticated stream URL ($formatName) for track $effectiveTrackId")
                                return@withContext directUrl
                            }
                        } else {
                            val errObj = mediaObj?.optJSONObject("error")
                            Log.w("DeezerMusicProvider", "Format $formatName rejected for track $effectiveTrackId: ${errObj?.optString("message")}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DeezerMusicProvider", "Error resolving $formatName stream URL: ${e.message}")
                }
            }
        }
        null
    }

    actual suspend fun prepareLocalStreamCacheFile(
        track: DeezerTrackSearchResult,
        arlToken: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val ctx = applicationContext
            val cacheDir = ctx?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val cleanId = track.id.removePrefix("deezer_stream_")
            val cacheFile = File(cacheDir, "stream_$cleanId.mp3")

            // Purge old corrupted/30s preview cache files smaller than 800KB
            if (cacheFile.exists()) {
                if (cacheFile.length() < 800_000) {
                    Log.d("DeezerMusicProvider", "Deleting old preview cache file (${cacheFile.length()} bytes) for track $cleanId")
                    cacheFile.delete()
                } else {
                    return@withContext cacheFile.absolutePath
                }
            }

            val tempFile = File(cacheDir, "temp_stream_$cleanId.tmp")
            if (tempFile.exists()) tempFile.delete()

            // Strictly resolve full stream URL (never fallback to 30-sec previewUrl for local stream cache)
            val streamUrl = resolveFullTrackStreamUrl(cleanId, arlToken)
            if (streamUrl.isNullOrBlank()) {
                Log.w("DeezerMusicProvider", "Could not resolve full stream URL for track $cleanId")
                return@withContext null
            }

            val success = downloadStreamToFile(streamUrl, tempFile, cleanId)

            if (success && tempFile.exists() && tempFile.length() > 500_000) {
                tempFile.renameTo(cacheFile)
                Log.d("DeezerMusicProvider", "Successfully downloaded full stream file (${cacheFile.length()} bytes) to ${cacheFile.absolutePath}")
                return@withContext cacheFile.absolutePath
            } else {
                if (tempFile.exists()) tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("DeezerMusicProvider", "Error preparing stream cache file for ${track.id}: ${e.message}")
        }
        null
    }

    actual suspend fun downloadTrackAudioFile(
        track: DeezerTrackSearchResult,
        downloadDir: String?,
        arlToken: String?
    ): String? = withContext(Dispatchers.IO) {
        val streamUrl = resolveFullTrackStreamUrl(track.id, arlToken)?.ifBlank { null }
        if (streamUrl.isNullOrBlank()) {
            Log.w("DeezerMusicProvider", "Could not resolve full stream URL for download of track ${track.id}")
            return@withContext null
        }

        try {
            val targetDir = resolveDownloadDirectory(downloadDir)

            val cleanArtist = sanitizeFilename(track.artist)
            val cleanAlbum = sanitizeFilename(track.album)
            val cleanTitle = sanitizeFilename(track.title)

            val albumFolder = File(targetDir, "$cleanArtist/$cleanAlbum")
            if (!albumFolder.exists()) albumFolder.mkdirs()

            val extension = if (streamUrl.contains("format=FLAC") || streamUrl.contains("FLAC")) ".flac" else ".mp3"
            val prefix = if (track.trackNumber > 0) "${track.trackNumber.toString().padStart(2, '0')} - " else ""
            val fileName = "$prefix$cleanTitle$extension"
            val targetFile = File(albumFolder, fileName)
            val tempFile = File(albumFolder, "$fileName.tmp")

            val success = downloadStreamToFile(streamUrl, tempFile, track.id)
            if (success && tempFile.exists() && tempFile.length() > 10_000) {
                tempFile.renameTo(targetFile)
                Log.d("DeezerMusicProvider", "Downloaded Deezer track to ${targetFile.absolutePath}")

                // Notify Android MediaStore so the file is immediately indexed into system library
                applicationContext?.let { ctx ->
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            ctx,
                            arrayOf(targetFile.absolutePath),
                            null,
                            null
                        )
                    } catch (_: Exception) {}
                }

                return@withContext targetFile.absolutePath
            } else {
                if (tempFile.exists()) tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("DeezerMusicProvider", "Failed downloading track '${track.title}': ${e.message}")
        }
        null
    }

    private fun resolveDownloadDirectory(downloadDir: String?): File {
        val ctx = applicationContext
        if (!downloadDir.isNullOrBlank()) {
            val clean = downloadDir.trim()
            if (clean.startsWith("/")) {
                val dir = File(clean)
                if (!dir.exists()) dir.mkdirs()
                if (dir.exists()) return dir
            } else if (clean.contains("primary:")) {
                val relativePath = clean.substringAfter("primary:").trim('/', ' ')
                val extStorage = Environment.getExternalStorageDirectory()
                val dir = File(extStorage, relativePath)
                if (!dir.exists()) dir.mkdirs()
                if (dir.exists()) return dir
            } else if (clean.startsWith("content://")) {
                val decoded = runCatching { java.net.URLDecoder.decode(clean, "UTF-8") }.getOrDefault(clean)
                if (decoded.contains("primary:")) {
                    val relativePath = decoded.substringAfter("primary:").substringBefore('"').substringBefore('?').trim('/', ' ')
                    val extStorage = Environment.getExternalStorageDirectory()
                    val dir = File(extStorage, relativePath)
                    if (!dir.exists()) dir.mkdirs()
                    if (dir.exists()) return dir
                }
            }
        }
        val defaultMusicDir = ctx?.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(Environment.getExternalStorageDirectory(), "Music/Aurum")
        if (!defaultMusicDir.exists()) defaultMusicDir.mkdirs()
        return defaultMusicDir
    }

    actual suspend fun validateArlToken(arlToken: String): DeezerArlStatus = withContext(Dispatchers.IO) {
        val session = DeezerSessionManager.connect(arlToken)
        if (session != null) {
            val message = "Connected as ${session.userName} (${session.maxQuality.displayName})"
            DeezerArlStatus(
                isValid = true,
                userName = session.userName,
                canStreamHq = session.maxQuality != DeezerAudioQuality.MP3_128,
                maxQuality = session.maxQuality,
                message = message
            )
        } else {
            DeezerArlStatus(
                isValid = false,
                userName = null,
                canStreamHq = false,
                maxQuality = DeezerAudioQuality.MP3_128,
                message = "Invalid or expired ARL token"
            )
        }
    }

    private fun parseTrackArray(dataArray: JSONArray): List<DeezerTrackSearchResult> {
        val results = mutableListOf<DeezerTrackSearchResult>()
        for (i in 0 until dataArray.length()) {
            val trackObj = dataArray.optJSONObject(i) ?: continue
            val id = trackObj.optLong("SNG_ID", 0L).takeIf { it != 0L }?.toString()
                ?: trackObj.optLong("id", 0L).takeIf { it != 0L }?.toString()
                ?: trackObj.optString("SNG_ID").ifBlank { trackObj.optString("id") }

            val title = trackObj.optString("SNG_TITLE").ifBlank { trackObj.optString("title", "Unknown Track") }
            val duration = trackObj.optLong("DURATION", 0L).takeIf { it != 0L } ?: trackObj.optLong("duration", 0L)
            val previewUrl = trackObj.optString("preview", "")
            val trackPosition = trackObj.optInt("TRACK_NUMBER", 0).takeIf { it != 0 } ?: trackObj.optInt("track_position", i + 1)

            val artistObj = trackObj.optJSONObject("artist")
            val artistName = artistObj?.optString("name")
                ?.ifBlank { trackObj.optString("ART_NAME").ifBlank { "Unknown Artist" } }
                ?: trackObj.optString("ART_NAME", "Unknown Artist")

            val albumObj = trackObj.optJSONObject("album")
            val albumTitle = albumObj?.optString("title")
                ?.ifBlank { trackObj.optString("ALB_TITLE").ifBlank { "Unknown Album" } }
                ?: trackObj.optString("ALB_TITLE", "Unknown Album")

            val rawArtUrl = albumObj?.optString("cover_xl")?.takeIf { it.isNotBlank() && it != "null" }
                ?: albumObj?.optString("cover_big")?.takeIf { it.isNotBlank() && it != "null" }
                ?: trackObj.optString("ALB_PICTURE").takeIf { it.isNotBlank() && it != "null" }
                ?.let { "https://cdn-images.dzcdn.net/images/cover/$it/1000x1000-000000-80-0-0.jpg" }
                ?: ""

            val artworkUrl = rawArtUrl.replace("250x250", "1000x1000").replace("500x500", "1000x1000")

            if (id.isNotBlank()) {
                results.add(
                    DeezerTrackSearchResult(
                        id = id,
                        title = title,
                        artist = artistName,
                        album = albumTitle,
                        durationSeconds = duration,
                        previewStreamUrl = previewUrl,
                        artworkUrl = artworkUrl,
                        trackNumber = trackPosition
                    )
                )
            }
        }
        return results
    }

    private fun downloadStreamToFile(streamUrl: String, targetFile: File, trackId: String): Boolean {
        try {
            var currentUrl = streamUrl
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                val isEncrypted = currentUrl.contains("dzcdn.net/media/") ||
                                  currentUrl.contains("cdnt-stream") ||
                                  currentUrl.contains("media.deezer.com") ||
                                  currentUrl.contains("e-cdns-proxy") ||
                                  currentUrl.contains("mobile/1")

                val url = URI(currentUrl).toURL()
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 20000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", USER_AGENT)

                val code = conn.responseCode
                if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        conn.disconnect()
                        continue
                    }
                }

                if (code == 200) {
                    conn.inputStream.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            if (isEncrypted) {
                                DeezerDecrypter.decryptTrackStream(input, output, trackId)
                            } else {
                                input.copyTo(output)
                            }
                        }
                    }
                    return targetFile.exists() && targetFile.length() > 10_000
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("DeezerMusicProvider", "Error downloading stream to file ${targetFile.name}: ${e.message}")
        }
        return false
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
