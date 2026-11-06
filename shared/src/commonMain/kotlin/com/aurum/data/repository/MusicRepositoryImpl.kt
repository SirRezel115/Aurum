package com.aurum.data.repository

import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Playlist
import com.aurum.data.model.Song
import com.aurum.database.AurumDatabase
import com.aurum.database.CachedSong
import com.aurum.scanner.MusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MusicRepository implementation with SQLDelight metadata caching layer.
 */
class MusicRepositoryImpl(
    private val scanner: MusicScanner,
    private val database: AurumDatabase,
) : MusicRepository {

    private val dbQueries get() = database.aurumDatabaseQueries

    override suspend fun getSongs(): List<Song> {
        val cached = dbQueries.selectAllSongs().executeAsList()
        return if (cached.isNotEmpty()) {
            cached.map { it.toSong() }
        } else {
            val scanned = scanner.scanSongs()
            cacheSongs(scanned)
            scanned
        }
    }

    override suspend fun getAlbums(): List<Album> {
        val songs = getSongs()
        val albums = songs.groupBy { it.album }
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

        // Background resolve missing album artwork
        resolveMissingAlbumArtwork(albums)

        return albums
    }

    private fun resolveMissingAlbumArtwork(albums: List<Album>) {
        CoroutineScope(Dispatchers.Default).launch {
            for (album in albums) {
                if (album.albumArtUri.isNullOrBlank() && album.title.isNotBlank() && !album.title.contains("Unknown", ignoreCase = true)) {
                    try {
                        val fetchedUrl = OnlineArtworkFetcher.fetchAlbumArtworkUrl(album.artist, album.title)
                        if (!fetchedUrl.isNullOrBlank()) {
                            updateAlbumMetadataAndArtwork(album.title, album.title, album.artist, fetchedUrl)
                        }
                    } catch (_: Exception) {}
                    kotlinx.coroutines.delay(200L)
                }
            }
        }
    }

    override suspend fun getArtists(): List<Artist> {
        val songs = getSongs()
        val canonicalArtistMap = mutableMapOf<String, String>()

        return songs.groupBy { song ->
            val main = getMainArtist(song.artist)
            canonicalArtistMap.getOrPut(main.lowercase()) { main }
        }
            .map { (artistKey, artistSongs) ->
                val canonicalName = canonicalArtistMap[artistKey] ?: artistSongs.first().artist
                val firstWithArt = artistSongs.firstOrNull { !it.albumArtUri.isNullOrBlank() }
                Artist(
                    id = canonicalName.hashCode().toString(),
                    name = canonicalName,
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
        if (q.isEmpty()) return getSongs()
        return getSongs().filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    override suspend fun rescan() {
        dbQueries.deleteAllSongs()
        val scanned = scanner.scanSongs()
        cacheSongs(scanned)
    }

    override suspend fun toggleFavorite(songId: String): Boolean {
        val songs = getSongs()
        val song = songs.find { it.id == songId } ?: return false
        val newFav = if (song.isFavorite) 0L else 1L
        dbQueries.updateFavorite(newFav, songId)
        return true
    }

    override suspend fun updateAlbumMetadata(oldAlbumTitle: String, newTitle: String, newArtist: String) {
        withContext(Dispatchers.Default) {
            dbQueries.updateAlbumMetadata(newTitle, newArtist, oldAlbumTitle)
        }
    }

    override suspend fun updateAlbumMetadataAndArtwork(oldAlbumTitle: String, newTitle: String, newArtist: String, artworkUri: String) {
        withContext(Dispatchers.Default) {
            val finalArtUri = if (artworkUri.startsWith("http")) {
                OnlineArtworkFetcher.downloadAndCacheArtwork(newArtist, newTitle, artworkUri)
            } else artworkUri

            dbQueries.updateAlbumMetadataAndArtwork(newTitle, newArtist, finalArtUri, oldAlbumTitle)

            val normOld = oldAlbumTitle.lowercase().replace(Regex("""[^\w\s]"""), " ").replace(Regex("""\s+"""), " ").trim()
            val normNew = newTitle.lowercase().replace(Regex("""[^\w\s]"""), " ").replace(Regex("""\s+"""), " ").trim()

            val cachedSongs = dbQueries.selectAllSongs().executeAsList()
            val targetSongs = cachedSongs.filter { song ->
                val normSongAlbum = song.album.lowercase().replace(Regex("""[^\w\s]"""), " ").replace(Regex("""\s+"""), " ").trim()
                song.album.equals(oldAlbumTitle, ignoreCase = true) ||
                song.album.equals(newTitle, ignoreCase = true) ||
                normSongAlbum == normOld ||
                normSongAlbum == normNew ||
                (normOld.isNotBlank() && normSongAlbum.contains(normOld)) ||
                (normNew.isNotBlank() && normSongAlbum.contains(normNew))
            }

            for (dbSong in targetSongs) {
                dbQueries.insertSong(
                    id = dbSong.id,
                    title = dbSong.title,
                    artist = newArtist,
                    album = newTitle,
                    duration = dbSong.duration,
                    uri = dbSong.uri,
                    album_art_uri = finalArtUri,
                    date_added = dbSong.date_added,
                    is_favorite = dbSong.is_favorite
                )
            }
        }
    }

    override suspend fun updateSongMetadataAndArtwork(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUri: String) {
        withContext(Dispatchers.Default) {
            val cachedSongs = dbQueries.selectAllSongs().executeAsList()
            val dbSong = cachedSongs.find {
                it.id == songId ||
                it.id == songId.replace("song_", "") ||
                it.title.equals(songId, ignoreCase = true) ||
                it.uri == songId
            } ?: return@withContext

            val finalArtUri = if (artworkUri.startsWith("http")) {
                OnlineArtworkFetcher.downloadAndCacheArtwork(newArtist, newAlbum, artworkUri)
            } else artworkUri

            dbQueries.insertSong(
                id = dbSong.id,
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                duration = dbSong.duration,
                uri = dbSong.uri,
                album_art_uri = finalArtUri,
                date_added = dbSong.date_added,
                is_favorite = dbSong.is_favorite
            )
        }
    }

    override suspend fun mergeAlbums(sourceAlbumTitle: String, targetAlbumTitle: String, targetArtist: String, artworkUri: String?) {
        withContext(Dispatchers.Default) {
            val cachedSongs = dbQueries.selectAllSongs().executeAsList()
            val sourceSongs = cachedSongs.filter { it.album.equals(sourceAlbumTitle, ignoreCase = true) }
            val targetSongs = cachedSongs.filter { it.album.equals(targetAlbumTitle, ignoreCase = true) }

            val existingArtUri = artworkUri
                ?: targetSongs.mapNotNull { it.album_art_uri }.firstOrNull { !it.isNullOrBlank() }
                ?: sourceSongs.mapNotNull { it.album_art_uri }.firstOrNull { !it.isNullOrBlank() }
                ?: ""

            val finalArtUri = if (existingArtUri.startsWith("http")) {
                OnlineArtworkFetcher.downloadAndCacheArtwork(targetArtist, targetAlbumTitle, existingArtUri)
            } else existingArtUri

            for (dbSong in sourceSongs + targetSongs) {
                dbQueries.insertSong(
                    id = dbSong.id,
                    title = dbSong.title,
                    artist = targetArtist,
                    album = targetAlbumTitle,
                    duration = dbSong.duration,
                    uri = dbSong.uri,
                    album_art_uri = finalArtUri,
                    date_added = dbSong.date_added,
                    is_favorite = dbSong.is_favorite
                )
            }
        }
    }

    override suspend fun deleteSong(songId: String) {
        withContext(Dispatchers.Default) {
            dbQueries.deleteSong(songId)
        }
    }

    override suspend fun getPlaylists(): List<Playlist> {
        return dbQueries.selectAllPlaylists().executeAsList().map { dbPlaylist ->
            val songIds = dbQueries.selectSongsForPlaylist(dbPlaylist.id).executeAsList().map { it.id }
            Playlist(
                id = dbPlaylist.id,
                name = dbPlaylist.name,
                songIds = songIds,
                createdAt = dbPlaylist.created_at,
            )
        }
    }

    override suspend fun createPlaylist(name: String): Playlist {
        val id = name.hashCode().toString() + "_" + kotlin.time.Clock.System.now().toEpochMilliseconds()
        val playlist = Playlist(id = id, name = name)
        dbQueries.insertPlaylist(id = playlist.id, name = playlist.name, created_at = playlist.createdAt)
        return playlist
    }

    override suspend fun deletePlaylist(id: String): Boolean {
        dbQueries.deletePlaylist(id)
        return true
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Boolean {
        dbQueries.insertPlaylistSong(playlist_id = playlistId, song_id = songId)
        return true
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean {
        dbQueries.deletePlaylistSong(playlist_id = playlistId, song_id = songId)
        return true
    }

    private fun cacheSongs(songs: List<Song>) {
        dbQueries.transaction {
            songs.forEach { song ->
                dbQueries.insertSong(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    uri = song.uri,
                    album_art_uri = song.albumArtUri,
                    date_added = song.dateAdded,
                    is_favorite = if (song.isFavorite) 1L else 0L,
                )
            }
        }
    }

    private fun CachedSong.toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uri = uri,
        albumArtUri = album_art_uri,
        dateAdded = date_added,
        isFavorite = is_favorite == 1L,
    )
}
