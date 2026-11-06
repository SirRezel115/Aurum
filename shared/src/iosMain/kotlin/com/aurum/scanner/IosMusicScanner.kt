package com.aurum.scanner

import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Song

/**
 * iOS MusicScanner (stub).
 * Implementation in Phase 3.
 */
class IosMusicScanner : MusicScanner {

    override suspend fun scanSongs(): List<Song> = emptyList()
    override suspend fun scanAlbums(): List<Album> = emptyList()
    override suspend fun scanArtists(): List<Artist> = emptyList()
    override suspend fun searchSongs(query: String): List<Song> = emptyList()
}