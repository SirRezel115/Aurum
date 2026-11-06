package com.aurum.scanner

import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioSystem

class DesktopMusicScanner : MusicScanner {

    var customFolder: String? = null

    private val _scanProgress = MutableStateFlow(ScanProgress())
    override val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val supportedExtensions = setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "wma")

    override suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val musicDirs = mutableListOf<File>()
        val custom = customFolder
        if (!custom.isNullOrBlank()) {
            val customDir = File(custom)
            if (customDir.exists() && customDir.isDirectory) {
                musicDirs.add(customDir)
            }
        }
        if (musicDirs.isEmpty()) {
            val userHome = System.getProperty("user.home") ?: return@withContext emptyList()
            listOf(
                File(userHome, "Music"),
                File(userHome, "Downloads"),
            ).filterTo(musicDirs) { it.exists() && it.isDirectory }
        }

        val songs = mutableListOf<Song>()

        try {
            val targetFiles = mutableListOf<File>()
            for (dir in musicDirs) {
                dir.walkTopDown()
                    .maxDepth(6)
                    .filter { file -> file.isFile && file.extension.lowercase() in supportedExtensions }
                    .forEach { targetFiles.add(it) }
            }

            val total = targetFiles.size
            _scanProgress.value = ScanProgress(scannedCount = 0, totalCount = total, isScanning = true)

            targetFiles.forEachIndexed { index, file ->
                _scanProgress.value = ScanProgress(
                    scannedCount = index + 1,
                    totalCount = total,
                    currentItemName = file.name,
                    isScanning = true
                )

                val metadata = extractMetadata(file)
                val artworkUri = DesktopArtworkExtractor.extractArtworkUri(file)
                val nameWithoutExt = file.nameWithoutExtension
                val parts = nameWithoutExt.split(" - ", "-", "–")
                val fallbackTitle = if (parts.size >= 2) parts[1].trim() else nameWithoutExt.trim()
                val fallbackArtist = if (parts.size >= 2) parts[0].trim() else "Local Artist"
                val fallbackAlbum = file.parentFile?.name ?: "Local Album"
                val uri = file.toURI().toString()

                val title = metadata.title?.ifBlank { null } ?: fallbackTitle.ifBlank { nameWithoutExt }
                val artist = metadata.artist?.ifBlank { null } ?: fallbackArtist.ifBlank { "Local Artist" }
                val album = metadata.album?.ifBlank { null } ?: fallbackAlbum.ifBlank { "Local Album" }

                val finalArtworkUri = artworkUri ?: try {
                    kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(title, artist, album)
                    }
                } catch (_: Exception) {
                    null
                }

                songs.add(
                    Song(
                        id = uri.hashCode().toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        duration = metadata.durationMs,
                        uri = uri,
                        albumArtUri = finalArtworkUri,
                        dateAdded = file.lastModified() / 1000L,
                    )
                )
            }
        } finally {
            _scanProgress.value = ScanProgress(isScanning = false)
        }

        songs
    }

    private fun extractMetadata(file: File): AudioMetadata {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L

        try {
            val format = AudioSystem.getAudioFileFormat(file)
            val props = format.properties()
            if (props != null) {
                title = props["title"] as? String
                artist = (props["author"] as? String) ?: (props["artist"] as? String)
                album = props["album"] as? String
                val durationMicros = props["duration"] as? Long
                if (durationMicros != null && durationMicros > 0) {
                    durationMs = durationMicros / 1000L
                }
            }
        } catch (_: Exception) {}

        if (durationMs <= 0L) {
            try {
                val audioStream = AudioSystem.getAudioInputStream(file)
                val audioFormat = audioStream.format
                val frames = audioStream.frameLength
                if (frames > 0 && audioFormat.frameRate > 0) {
                    durationMs = ((frames / audioFormat.frameRate) * 1000).toLong()
                }
                audioStream.close()
            } catch (_: Exception) {}
        }

        if (durationMs <= 0L) {
            durationMs = 180_000L
        }

        return AudioMetadata(title, artist, album, durationMs)
    }

    private data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long
    )

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
            .sortedBy { it.artist.lowercase() }
    }

    override suspend fun scanArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val songs = scanSongs()
        songs.groupBy { getMainArtist(it.artist) }
            .map { (artistName, artistSongs) ->
                val firstWithArt = artistSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }
                Artist(
                    id = artistName.hashCode().toString(),
                    name = artistName,
                    albumArtUri = firstWithArt?.albumArtUri ?: artistSongs.firstOrNull()?.albumArtUri,
                    trackCount = artistSongs.size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun getMainArtist(artist: String): String {
        if (artist.isBlank()) return "Unknown Artist"
        val delimiters = arrayOf(
            " feat. ", " ft. ", " featuring ", " feat ", " ft ",
            " & ", ", ", " / ", " with ", " vs. ", " vs ", "(feat.", "(ft."
        )
        var main = artist
        for (delimiter in delimiters) {
            val index = main.indexOf(delimiter, ignoreCase = true)
            if (index != -1) {
                main = main.substring(0, index)
            }
        }
        return main.trim().ifBlank { artist.trim() }
    }

    override suspend fun searchSongs(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return scanSongs()
        return scanSongs().filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }
}
