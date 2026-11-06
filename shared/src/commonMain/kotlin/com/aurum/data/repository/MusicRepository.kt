package com.aurum.data.repository

import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Playlist
import com.aurum.data.model.Song

/**
 * Repository for accessing music and video media from the device.
 */
interface MusicRepository {

    /**
     * Returns the full list of songs detected on the device.
     */
    suspend fun getSongs(): List<Song>

    /**
     * Returns the full list of albums detected on the device.
     */
    suspend fun getAlbums(): List<Album>

    /**
     * Returns the full list of artists detected on the device.
     */
    suspend fun getArtists(): List<Artist>

    /**
     * Searches songs by query (matches title, artist, or album).
     */
    suspend fun searchSongs(query: String): List<Song>

    /**
     * Triggers a rescan of the device for music and video files.
     */
    suspend fun rescan()

    /**
     * Toggles favorite state for a song.
     */
    suspend fun toggleFavorite(songId: String): Boolean

    /**
     * Updates album title, artist, and artwork URI across all songs in the album.
     */
    suspend fun updateAlbumMetadata(oldAlbumTitle: String, newTitle: String, newArtist: String)
    suspend fun updateAlbumMetadataAndArtwork(oldAlbumTitle: String, newTitle: String, newArtist: String, artworkUri: String)

    /**
     * Updates individual song title, artist, album, and artwork URI.
     */
    suspend fun updateSongMetadataAndArtwork(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUri: String)

    /**
     * Merges all songs from source album into target album in the database.
     */
    suspend fun mergeAlbums(sourceAlbumTitle: String, targetAlbumTitle: String, targetArtist: String, artworkUri: String? = null)

    /**
     * Deletes a song by ID.
     */
    suspend fun deleteSong(songId: String)

    // -- Playlist operations --

    suspend fun getPlaylists(): List<Playlist>
    suspend fun createPlaylist(name: String): Playlist
    suspend fun deletePlaylist(id: String): Boolean
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Boolean
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Boolean
}
