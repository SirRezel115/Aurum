package com.aurum.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

actual object OnlineArtworkFetcher {

    actual suspend fun downloadAndCacheArtwork(artist: String, album: String, artworkUrl: String): String = withContext(Dispatchers.IO) {
        if (artworkUrl.isBlank() || !artworkUrl.startsWith("http")) return@withContext artworkUrl
        try {
            val albumKey = "$artist - $album".lowercase().trim().hashCode().toLong()
            val userHome = System.getProperty("user.home") ?: "."
            val cacheDir = java.io.File(userHome, ".aurum/cache").apply { mkdirs() }
            val cacheFile = java.io.File(cacheDir, "art_hd_v8_$albumKey.jpg")

            val bytes = downloadImageData(artworkUrl)
            if (bytes != null && bytes.size > 500) {
                cacheFile.writeBytes(bytes)
                return@withContext cacheFile.toURI().toString()
            }
        } catch (_: Exception) {}
        artworkUrl
    }

    actual suspend fun fetchAlbumArtworkUrl(artist: String, album: String): String? {
        return fetchArtworkForTrack(title = "", artist = artist, album = album)
    }

    actual suspend fun fetchArtworkForTrack(title: String, artist: String, album: String): String? = withContext(Dispatchers.IO) {
        val cleanArtist = sanitize(artist)
        val cleanAlbum = sanitize(album)
        val cleanTitle = sanitize(title)

        // Generate prioritized query attempts
        val queryCandidates = mutableListOf<String>()

        if (cleanArtist.isNotBlank() && cleanAlbum.isNotBlank()) {
            queryCandidates.add("$cleanArtist $cleanAlbum")
        }
        if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            queryCandidates.add("$cleanTitle $cleanArtist")
        }
        if (cleanTitle.isNotBlank()) {
            queryCandidates.add(cleanTitle)
        }
        if (cleanAlbum.isNotBlank()) {
            queryCandidates.add(cleanAlbum)
        }

        for (query in queryCandidates.distinct()) {
            val artUrl = queryItunesAppleApi(query)
            if (!artUrl.isNullOrBlank()) {
                return@withContext artUrl
            }
        }

        null
    }

    actual suspend fun downloadImageData(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null
        try {
            val url = URI(imageUrl).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > 500) return@withContext bytes
            }
        } catch (_: Exception) {}
        null
    }

    actual suspend fun fetchArtistDiscography(artistName: String): List<AppleMusicSearchResult> = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext emptyList()
        val encodedArtist = URLEncoder.encode(artistName.trim(), "UTF-8")
        val results = fetchItunesApiSearchResults(encodedArtist, entityParam = "album", requestedEntity = "album", attributeParam = "artistTerm")
        val cleanArtist = artistName.lowercase().trim()
        val filtered = results.filter { it.artist.lowercase().contains(cleanArtist) || cleanArtist.contains(it.artist.lowercase()) }
        if (filtered.isNotEmpty()) {
            filtered.distinctBy { "${it.title.lowercase()}_${it.album.lowercase()}" }
        } else {
            results.distinctBy { "${it.title.lowercase()}_${it.album.lowercase()}" }
        }
    }

    actual suspend fun searchAppleMusic(query: String, entity: String): List<AppleMusicSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val encodedFull = URLEncoder.encode(q, "UTF-8")

        if (entity.lowercase() == "artist") {
            val deezerArtistPhotos = fetchDeezerArtistPhotos(encodedFull)
            if (deezerArtistPhotos.isNotEmpty()) return@withContext deezerArtistPhotos
        }

        val delimiters = listOf(" by ", " - ", " feat ", " ft ", " featuring ")
        var searchTitle = q
        var searchArtist: String? = null

        for (delim in delimiters) {
            if (q.contains(delim, ignoreCase = true)) {
                val parts = q.split(Regex("(?i)$delim"), limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    searchTitle = parts[0].trim()
                    searchArtist = parts[1].trim()
                    break
                }
            }
        }

        val (entityParam, attributeParam) = when (entity.lowercase()) {
            "song" -> "song" to "songTerm"
            "artist" -> "album" to "artistTerm"
            else -> "album" to "albumTerm"
        }

        val encodedTitle = URLEncoder.encode(searchTitle, "UTF-8")

        val candidates = mutableListOf<AppleMusicSearchResult>()
        candidates.addAll(fetchItunesApiSearchResults(encodedTitle, entityParam, entity, attributeParam = attributeParam))
        if (entity.lowercase() == "album") {
            candidates.addAll(fetchItunesApiSearchResults(encodedTitle, entityParam = "song", requestedEntity = "album", attributeParam = null))
        }
        if (searchArtist != null) {
            val encodedArtist = URLEncoder.encode(searchArtist, "UTF-8")
            candidates.addAll(fetchItunesApiSearchResults(encodedArtist, entityParam, entity, attributeParam = "artistTerm"))
        }
        candidates.addAll(fetchItunesApiSearchResults(encodedFull, entityParam, entity, attributeParam = null))

        val normTitle = normalizeForMatching(searchTitle)
        val normArtist = searchArtist?.let { normalizeForMatching(it) }

        val filtered = candidates.filter { item ->
            val normItemTitle = normalizeForMatching(item.title)
            val normItemAlbum = normalizeForMatching(item.album)
            val normItemArtist = normalizeForMatching(item.artist)

            val titleMatch = normItemTitle.contains(normTitle) || normItemAlbum.contains(normTitle) || normTitle.contains(normItemTitle) || normTitle.contains(normItemAlbum)
            val artistMatch = normArtist == null || normItemArtist.contains(normArtist) || normArtist.contains(normItemArtist)
            titleMatch && artistMatch
        }

        if (filtered.isNotEmpty()) {
            filtered.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}_${it.album.lowercase()}" }
        } else {
            candidates.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}_${it.album.lowercase()}" }
        }
    }

    private fun normalizeForMatching(text: String): String {
        return text.lowercase()
            .replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun fetchItunesApiSearchResults(
        encodedQuery: String,
        entityParam: String,
        requestedEntity: String,
        attributeParam: String?
    ): List<AppleMusicSearchResult> {
        val results = mutableListOf<AppleMusicSearchResult>()
        try {
            val attributeSuffix = if (!attributeParam.isNullOrBlank()) "&attribute=$attributeParam" else ""
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=$entityParam&limit=50$attributeSuffix"
            val url = URI(urlString).toURL()

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val blocks = extractJsonObjects(json)

                for ((index, block) in blocks.withIndex()) {
                    val artUrlMarker = listOf("\"artworkUrl100\":\"", "\"artworkUrl60\":\"").find { block.contains(it) }
                    val rawArtUrl = if (artUrlMarker != null) {
                        val start = block.indexOf(artUrlMarker) + artUrlMarker.length
                        val end = block.indexOf("\"", start)
                        if (end != -1) block.substring(start, end) else ""
                    } else ""

                    val trackTitle = extractJsonField(block, "\"trackName\":\"")
                    val albumName = extractJsonField(block, "\"collectionName\":\"")
                    val artistName = extractJsonField(block, "\"artistName\":\"")

                    val displayTitle = when (requestedEntity.lowercase()) {
                        "song" -> trackTitle.ifBlank { albumName }.ifBlank { artistName }
                        "artist" -> artistName.ifBlank { albumName }.ifBlank { trackTitle }
                        else -> albumName.ifBlank { trackTitle }.ifBlank { artistName }
                    }

                    if (displayTitle.isNotBlank() || artistName.isNotBlank()) {
                        val hdArtUrl = rawArtUrl.replace("100x100bb", "1000x1000bb").replace("60x60bb", "1000x1000bb")
                        results.add(
                            AppleMusicSearchResult(
                                id = "${index}_${displayTitle.hashCode()}",
                                title = displayTitle,
                                artist = artistName.ifBlank { "Apple Music Artist" },
                                album = albumName.ifBlank { displayTitle },
                                artworkUrl = hdArtUrl,
                                type = requestedEntity
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun fetchDeezerArtistPhotos(encodedQuery: String): List<AppleMusicSearchResult> {
        val results = mutableListOf<AppleMusicSearchResult>()
        try {
            val urlString = "https://api.deezer.com/search/artist?q=$encodedQuery&limit=10"
            val url = URI(urlString).toURL()

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val blocks = json.split("{\"id\":")

                for ((index, block) in blocks.withIndex()) {
                    val artistName = extractJsonField(block, "\"name\":\"")
                    val pictureXlMarker = listOf("\"picture_xl\":\"", "\"picture_big\":\"").find { block.contains(it) }
                    val pictureXl = if (pictureXlMarker != null) {
                        val start = block.indexOf(pictureXlMarker) + pictureXlMarker.length
                        val end = block.indexOf("\"", start)
                        if (end != -1) block.substring(start, end).replace("\\/", "/") else ""
                    } else ""

                    if (artistName.isNotBlank() && pictureXl.isNotBlank()) {
                        results.add(
                            AppleMusicSearchResult(
                                id = "deezer_artist_${index}_${artistName.hashCode()}",
                                title = artistName,
                                artist = artistName,
                                album = "Artist Photo",
                                artworkUrl = pictureXl,
                                type = "artist"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun extractJsonObjects(json: String): List<String> {
        val resultBlocks = mutableListOf<String>()
        var pos = 0
        while (pos < json.length) {
            val wrapperIdx = json.indexOf("\"wrapperType\"", pos)
            val kindIdx = json.indexOf("\"kind\"", pos)
            val artistIdx = json.indexOf("\"artistId\"", pos)
            val collectionIdx = json.indexOf("\"collectionName\"", pos)

            val candidateIndices = listOf(wrapperIdx, kindIdx, artistIdx, collectionIdx).filter { it != -1 }
            if (candidateIndices.isEmpty()) break
            val start = candidateIndices.minOrNull() ?: break

            val objStart = json.lastIndexOf('{', start)
            if (objStart == -1) {
                pos = start + 10
                continue
            }

            var depth = 0
            var end = -1
            for (i in objStart until json.length) {
                if (json[i] == '{') depth++
                else if (json[i] == '}') {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }

            if (end != -1 && end > objStart) {
                resultBlocks.add(json.substring(objStart, end + 1))
                pos = end + 1
            } else {
                pos = start + 10
            }
        }
        return resultBlocks
    }

    private fun extractJsonField(jsonBlock: String, fieldMarker: String): String {
        val idx = jsonBlock.indexOf(fieldMarker)
        if (idx == -1) return ""
        val start = idx + fieldMarker.length
        val end = jsonBlock.indexOf("\"", start)
        return if (end != -1) jsonBlock.substring(start, end) else ""
    }

    private fun sanitize(input: String): String {
        val lower = input.trim()
        val ignoreList = listOf("local artist", "local album", "unknown artist", "unknown album", "downloads", "music", "unknown")
        if (lower.lowercase() in ignoreList) return ""
        return lower
    }

    private fun queryItunesAppleApi(term: String): String? {
        if (term.isBlank()) return null
        try {
            val encodedQuery = URLEncoder.encode(term, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&limit=3"
            val url = URI(urlString).toURL()

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }

                // Extract artworkUrl100 or artworkUrl60
                val markers = listOf("\"artworkUrl100\":\"", "\"artworkUrl60\":\"")
                for (marker in markers) {
                    val idx = json.indexOf(marker)
                    if (idx != -1) {
                        val start = idx + marker.length
                        val end = json.indexOf("\"", start)
                        if (end != -1) {
                            val rawUrl = json.substring(start, end)
                            val artUrl = rawUrl
                                .replace("100x100bb", "1000x1000bb")
                                .replace("60x60bb", "1000x1000bb")
                            println("OnlineArtworkFetcher: Apple Music API result for '$term' -> $artUrl")
                            return artUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
