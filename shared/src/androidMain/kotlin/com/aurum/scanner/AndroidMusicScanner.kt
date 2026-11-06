package com.aurum.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android MusicScanner
 */
class AndroidMusicScanner(
    private val context: Context,
) : MusicScanner {

    private val _scanProgress = MutableStateFlow(ScanProgress())
    override val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    @Suppress("DEPRECATION")
    override suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val albumArtCache = mutableMapOf<Long, String?>()
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val total = cursor.count
                var count = 0
                _scanProgress.value = ScanProgress(scannedCount = 0, totalCount = total, isScanning = true)

                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    count++
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    var title = cursor.getString(titleCol) ?: "Unknown"
                    var artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    var album = cursor.getString(albumCol) ?: "Unknown Album"
                    val filePath = cursor.getString(dataCol)

                    _scanProgress.value = ScanProgress(
                        scannedCount = count,
                        totalCount = total,
                        currentItemName = "$title - $artist",
                        isScanning = true
                    )

                    // Strip file extensions if accidentally attached to metadata fields
                    val extRegex = Regex("(?i)\\.(mp3|flac|m4a|wav|aac|ogg|wma|m4p|opus)$")
                    title = title.replace(extRegex, "").trim()
                    artist = artist.replace(extRegex, "").trim()
                    album = album.replace(extRegex, "").trim()

                    // Unique effective album key prevents albumId = 0 or shared albumId collisions
                    val uniqueAlbumKey = "$artist - $album".lowercase().trim().hashCode().toLong()

                    // Look up or compute artwork per-album
                    val albumArtPath = albumArtCache.getOrPut(uniqueAlbumKey) {
                        extractHdArtworkForAlbum(context, contentUri, filePath, title, artist, album, uniqueAlbumKey)
                    }

                    val finalAlbumArtUri = if (albumArtPath != null) {
                        if (albumArtPath.startsWith("http") || albumArtPath.startsWith("content://") || albumArtPath.startsWith("file://")) {
                            albumArtPath
                        } else {
                            Uri.fromFile(java.io.File(albumArtPath)).toString()
                        }
                    } else null

                    songs.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            duration = cursor.getLong(durationCol),
                            uri = contentUri.toString(),
                            albumArtUri = finalAlbumArtUri,
                            dateAdded = cursor.getLong(dateAddedCol),
                        )
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            _scanProgress.value = ScanProgress(isScanning = false)
        }

        songs
    }

    override suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val songs = scanSongs()
        songs.groupBy { it.album }
            .map { (albumTitle, albumSongs) ->
                val firstWithArt = albumSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }
                val first = albumSongs.first()
                Album(
                    id = albumTitle.hashCode().toString(),
                    title = albumTitle,
                    artist = first.artist,
                    albumArtUri = firstWithArt?.albumArtUri ?: first.albumArtUri,
                    trackCount = albumSongs.size,
                )
            }
    }

    override suspend fun scanArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val songs = scanSongs()
        songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                val firstWithArt = artistSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }
                Artist(
                    id = artistName.hashCode().toString(),
                    name = artistName,
                    albumArtUri = firstWithArt?.albumArtUri ?: artistSongs.firstOrNull()?.albumArtUri,
                    trackCount = artistSongs.size,
                )
            }
    }

    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext scanSongs()
        scanSongs().filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    private fun extractHdArtworkForAlbum(
        context: Context,
        contentUri: Uri,
        filePath: String?,
        title: String,
        artist: String,
        album: String,
        albumKey: Long
    ): String? {
        val cacheFile = File(context.cacheDir, "art_hd_v8_$albumKey.jpg")
        if (cacheFile.exists() && cacheFile.length() > 500) {
            return Uri.fromFile(cacheFile).toString()
        }

        // Tier 1: Apple Music API 1000x1000 HD Artwork -> Download and Force-Swap local disk cache
        try {
            val onlineArtUrl = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(6000L) {
                    if (album.isNotBlank() && !album.equals("Unknown Album", ignoreCase = true)) {
                        com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(artist, album)
                            ?: com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(title, artist, album)
                    } else {
                        com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(title, artist, album)
                    }
                }
            }
            if (!onlineArtUrl.isNullOrBlank()) {
                val imageBytes = kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeoutOrNull(6000L) {
                        com.aurum.data.repository.OnlineArtworkFetcher.downloadImageData(onlineArtUrl)
                    }
                }
                if (imageBytes != null && imageBytes.size > 500) {
                    cacheFile.writeBytes(imageBytes)
                    return Uri.fromFile(cacheFile).toString()
                }
            }
        } catch (_: Exception) {}

        // Tier 2: Extract embedded ID3 picture bytes directly from the audio file
        try {
            val retriever = android.media.MediaMetadataRetriever()
            if (contentUri.toString().startsWith("content://")) {
                context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }
            } else {
                retriever.setDataSource(context, contentUri)
            }
            val artBytes = retriever.embeddedPicture
            try { retriever.release() } catch (_: Exception) {}

            if (artBytes != null && artBytes.size > 500) {
                cacheFile.writeBytes(artBytes)
                return Uri.fromFile(cacheFile).toString()
            }
        } catch (_: Exception) {}

        // Tier 3: Search parent directory for album-specific cover files
        if (!filePath.isNullOrBlank()) {
            try {
                val parent = File(filePath).parentFile
                if (parent != null && parent.exists() && parent.isDirectory) {
                    val albumSanitized = album.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val files = parent.listFiles() ?: emptyArray()
                    for (file in files) {
                        if (file.isFile && file.length() > 500) {
                            val nameLower = file.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9]"), "")
                            if (albumSanitized.isNotBlank() && nameLower.contains(albumSanitized)) {
                                return Uri.fromFile(file).toString()
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Tier 4: MediaStore system thumbnail fallback
        try {
            var mediaStorePath: String? = null
            val albumProjection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
            context.contentResolver.query(
                ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumKey),
                albumProjection,
                null,
                null,
                null
            )?.use { albumCursor ->
                if (albumCursor.moveToFirst()) {
                    val artCol = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
                    if (artCol >= 0) {
                        mediaStorePath = albumCursor.getString(artCol)
                    }
                }
            }
            if (!mediaStorePath.isNullOrBlank()) {
                return mediaStorePath
            }
        } catch (_: Exception) {}

        return null
    }
}
