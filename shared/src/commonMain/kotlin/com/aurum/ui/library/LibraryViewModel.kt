package com.aurum.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurum.data.model.Album
import com.aurum.data.model.Artist
import com.aurum.data.model.Playlist
import com.aurum.data.model.Song
import com.aurum.data.repository.MusicRepository
import com.aurum.data.repository.toSong
import com.aurum.domain.SongSortBy
import com.aurum.domain.SortOrder
import com.aurum.domain.filterByQuery
import com.aurum.domain.sortSongs
import com.aurum.player.MusicPlayer
import com.aurum.scanner.MusicScanner
import com.aurum.scanner.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(
        val allSongs: List<Song>,
        val filteredSongs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val playlists: List<Playlist> = emptyList(),
        val searchQuery: String = "",
        val sortBy: SongSortBy = SongSortBy.TITLE,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val musicPlayer: MusicPlayer,
    private val musicScanner: MusicScanner? = null,
) : ViewModel() {

    val scanProgress: StateFlow<ScanProgress> = musicScanner?.scanProgress ?: MutableStateFlow(ScanProgress()).asStateFlow()

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _appleMusicResults = MutableStateFlow<List<com.aurum.data.repository.AppleMusicSearchResult>>(emptyList())
    val appleMusicResults: StateFlow<List<com.aurum.data.repository.AppleMusicSearchResult>> = _appleMusicResults.asStateFlow()

    private val _isSearchingAppleMusic = MutableStateFlow(false)
    val isSearchingAppleMusic: StateFlow<Boolean> = _isSearchingAppleMusic.asStateFlow()

    private val _artistDiscography = MutableStateFlow<List<com.aurum.data.repository.AppleMusicSearchResult>>(emptyList())
    val artistDiscography: StateFlow<List<com.aurum.data.repository.AppleMusicSearchResult>> = _artistDiscography.asStateFlow()

    private val _isLoadingDiscography = MutableStateFlow(false)
    val isLoadingDiscography: StateFlow<Boolean> = _isLoadingDiscography.asStateFlow()

    private val _editingTargetSong = MutableStateFlow<Song?>(null)
    val editingTargetSong: StateFlow<Song?> = _editingTargetSong.asStateFlow()

    private val _editingTargetAlbum = MutableStateFlow<Album?>(null)
    val editingTargetAlbum: StateFlow<Album?> = _editingTargetAlbum.asStateFlow()

    private val _editingTargetArtist = MutableStateFlow<Artist?>(null)
    val editingTargetArtist: StateFlow<Artist?> = _editingTargetArtist.asStateFlow()

    private val _deezerSearchResults = MutableStateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>>(emptyList())
    val deezerSearchResults: StateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>> = _deezerSearchResults.asStateFlow()

    private val _deezerAlbumResults = MutableStateFlow<List<com.aurum.data.repository.DeezerAlbumSearchResult>>(emptyList())
    val deezerAlbumResults: StateFlow<List<com.aurum.data.repository.DeezerAlbumSearchResult>> = _deezerAlbumResults.asStateFlow()

    private val _deezerArtistResults = MutableStateFlow<List<com.aurum.data.repository.DeezerArtistSearchResult>>(emptyList())
    val deezerArtistResults: StateFlow<List<com.aurum.data.repository.DeezerArtistSearchResult>> = _deezerArtistResults.asStateFlow()

    private val _deezerPlaylistResults = MutableStateFlow<List<com.aurum.data.repository.DeezerPlaylistSearchResult>>(emptyList())
    val deezerPlaylistResults: StateFlow<List<com.aurum.data.repository.DeezerPlaylistSearchResult>> = _deezerPlaylistResults.asStateFlow()

    private val _deezerFlowTracks = MutableStateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>>(emptyList())
    val deezerFlowTracks: StateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>> = _deezerFlowTracks.asStateFlow()

    private val _userSavedTracks = MutableStateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>>(emptyList())
    val userSavedTracks: StateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>> = _userSavedTracks.asStateFlow()

    private val _userSavedAlbums = MutableStateFlow<List<com.aurum.data.repository.DeezerAlbumSearchResult>>(emptyList())
    val userSavedAlbums: StateFlow<List<com.aurum.data.repository.DeezerAlbumSearchResult>> = _userSavedAlbums.asStateFlow()

    private val _userSavedArtists = MutableStateFlow<List<com.aurum.data.repository.DeezerArtistSearchResult>>(emptyList())
    val userSavedArtists: StateFlow<List<com.aurum.data.repository.DeezerArtistSearchResult>> = _userSavedArtists.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<com.aurum.data.repository.DeezerPlaylistSearchResult>>(emptyList())
    val userPlaylists: StateFlow<List<com.aurum.data.repository.DeezerPlaylistSearchResult>> = _userPlaylists.asStateFlow()

    private val _importedLinkTracks = MutableStateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>>(emptyList())
    val importedLinkTracks: StateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>> = _importedLinkTracks.asStateFlow()

    private val _deezerSubTab = MutableStateFlow(DeezerSubTab.FLOW)
    val deezerSubTab: StateFlow<DeezerSubTab> = _deezerSubTab.asStateFlow()

    private val _isDeezerHubLoading = MutableStateFlow(false)
    val isDeezerHubLoading: StateFlow<Boolean> = _isDeezerHubLoading.asStateFlow()

    private val _activeDeezerCollectionTitle = MutableStateFlow<String?>(null)
    val activeDeezerCollectionTitle: StateFlow<String?> = _activeDeezerCollectionTitle.asStateFlow()

    private val _activeDeezerCollectionTracks = MutableStateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>>(emptyList())
    val activeDeezerCollectionTracks: StateFlow<List<com.aurum.data.repository.DeezerTrackSearchResult>> = _activeDeezerCollectionTracks.asStateFlow()

    private val _isDeezerCollectionLoading = MutableStateFlow(false)
    val isDeezerCollectionLoading: StateFlow<Boolean> = _isDeezerCollectionLoading.asStateFlow()

    fun setDeezerSubTab(tab: DeezerSubTab) {
        _deezerSubTab.value = tab
    }

    fun openDeezerPlaylist(playlistId: String, title: String) {
        viewModelScope.launch {
            _activeDeezerCollectionTitle.value = title
            _isDeezerCollectionLoading.value = true
            try {
                val tracks = com.aurum.data.repository.DeezerMusicProvider.getDeezerPlaylistTracks(playlistId)
                _activeDeezerCollectionTracks.value = tracks
            } catch (_: Exception) {
                _activeDeezerCollectionTracks.value = emptyList()
            } finally {
                _isDeezerCollectionLoading.value = false
            }
        }
    }

    fun openDeezerAlbum(albumId: String, title: String) {
        viewModelScope.launch {
            _activeDeezerCollectionTitle.value = title
            _isDeezerCollectionLoading.value = true
            try {
                val cleanId = albumId.trim()
                val tracks = if (cleanId.isNotBlank() && cleanId.all { it.isDigit() }) {
                    com.aurum.data.repository.DeezerMusicProvider.getDeezerAlbumTracks(cleanId)
                } else {
                    val albums = com.aurum.data.repository.DeezerMusicProvider.searchDeezerAlbums(title)
                    val firstId = albums.firstOrNull()?.id
                    if (!firstId.isNullOrBlank()) {
                        com.aurum.data.repository.DeezerMusicProvider.getDeezerAlbumTracks(firstId)
                    } else emptyList()
                }
                _activeDeezerCollectionTracks.value = tracks
            } catch (_: Exception) {
                _activeDeezerCollectionTracks.value = emptyList()
            } finally {
                _isDeezerCollectionLoading.value = false
            }
        }
    }

    fun openDeezerArtist(artistId: String, name: String) {
        viewModelScope.launch {
            _activeDeezerCollectionTitle.value = name
            _isDeezerCollectionLoading.value = true
            try {
                val tracks = com.aurum.data.repository.DeezerMusicProvider.getDeezerAlbumTracks(artistId)
                _activeDeezerCollectionTracks.value = tracks
            } catch (_: Exception) {
                _activeDeezerCollectionTracks.value = emptyList()
            } finally {
                _isDeezerCollectionLoading.value = false
            }
        }
    }

    fun closeDeezerCollection() {
        _activeDeezerCollectionTitle.value = null
        _activeDeezerCollectionTracks.value = emptyList()
        _isDeezerCollectionLoading.value = false
    }

    private val _isSearchingDeezer = MutableStateFlow(false)
    val isSearchingDeezer: StateFlow<Boolean> = _isSearchingDeezer.asStateFlow()

    private val _downloadingTrackId = MutableStateFlow<String?>(null)
    val downloadingTrackId: StateFlow<String?> = _downloadingTrackId.asStateFlow()

    private val _deezerArlStatus = MutableStateFlow<com.aurum.data.repository.DeezerArlStatus?>(null)
    val deezerArlStatus: StateFlow<com.aurum.data.repository.DeezerArlStatus?> = _deezerArlStatus.asStateFlow()

    fun validateDeezerArl(arlToken: String) {
        viewModelScope.launch {
            val status = com.aurum.data.repository.DeezerMusicProvider.validateArlToken(arlToken)
            _deezerArlStatus.value = status
        }
    }

    fun searchDeezer(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _deezerSearchResults.value = emptyList()
            _deezerAlbumResults.value = emptyList()
            _deezerArtistResults.value = emptyList()
            _deezerPlaylistResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingDeezer.value = true
            try {
                val rawResults = com.aurum.data.repository.DeezerMusicProvider.searchDeezerTracks(q)
                _deezerSearchResults.value = rawResults

                val albums = com.aurum.data.repository.DeezerMusicProvider.searchDeezerAlbums(q)
                _deezerAlbumResults.value = albums

                val artists = com.aurum.data.repository.DeezerMusicProvider.searchDeezerArtists(q)
                _deezerArtistResults.value = artists

                val playlists = com.aurum.data.repository.DeezerMusicProvider.searchDeezerPlaylists(q)
                _deezerPlaylistResults.value = playlists

                _isSearchingDeezer.value = false

                // Asynchronously enrich artwork with Apple Music/iTunes 1000x1000 HD fallback in background
                launch(Dispatchers.Default) {
                    val enriched = rawResults.map { track ->
                        val hdArt = com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(track.title, track.artist, track.album)
                            ?: com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(track.artist, track.album)
                        if (!hdArt.isNullOrBlank()) {
                            track.copy(artworkUrl = hdArt)
                        } else {
                            track
                        }
                    }
                    _deezerSearchResults.value = enriched
                }
            } catch (_: Exception) {
                _deezerSearchResults.value = emptyList()
                _isSearchingDeezer.value = false
            }
        }
    }

    fun loadAllDeezerUserData(arlToken: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _isDeezerHubLoading.value = true
            try {
                val flowDeferred = async { com.aurum.data.repository.DeezerMusicProvider.getDeezerFlowTracks(arlToken) }
                val savedTracksDeferred = async { com.aurum.data.repository.DeezerMusicProvider.getUserSavedTracks(arlToken) }
                val playlistsDeferred = async { com.aurum.data.repository.DeezerMusicProvider.getUserPlaylists(arlToken) }
                val savedAlbumsDeferred = async { com.aurum.data.repository.DeezerMusicProvider.getUserSavedAlbums(arlToken) }
                val savedArtistsDeferred = async { com.aurum.data.repository.DeezerMusicProvider.getUserSavedArtists(arlToken) }

                _deezerFlowTracks.value = flowDeferred.await()
                _userSavedTracks.value = savedTracksDeferred.await()
                _userPlaylists.value = playlistsDeferred.await()
                _userSavedAlbums.value = savedAlbumsDeferred.await()
                _userSavedArtists.value = savedArtistsDeferred.await()
            } catch (_: Exception) {
            } finally {
                _isDeezerHubLoading.value = false
            }
        }
    }

    fun loadDeezerFlow(arlToken: String) {
        viewModelScope.launch {
            val tracks = com.aurum.data.repository.DeezerMusicProvider.getDeezerFlowTracks(arlToken)
            _deezerFlowTracks.value = tracks
        }
    }

    fun loadUserSavedTracks(arlToken: String) {
        viewModelScope.launch {
            val tracks = com.aurum.data.repository.DeezerMusicProvider.getUserSavedTracks(arlToken)
            _userSavedTracks.value = tracks
        }
    }

    fun importDeezerLink(url: String, arlToken: String? = null, onLoaded: (List<com.aurum.data.repository.DeezerTrackSearchResult>) -> Unit = {}) {
        viewModelScope.launch {
            val linkResult = com.aurum.data.repository.DeezerMusicProvider.parseDeezerLink(url) ?: return@launch
            val tracks = when (linkResult.kind) {
                com.aurum.data.repository.DeezerLinkKind.TRACK -> {
                    val streamUrl = com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(linkResult.id, arlToken)
                    listOf(
                        com.aurum.data.repository.DeezerTrackSearchResult(
                            id = linkResult.id,
                            title = "Deezer Track ${linkResult.id}",
                            artist = "Deezer",
                            album = "Deezer",
                            durationSeconds = 180L,
                            previewStreamUrl = streamUrl ?: "",
                            artworkUrl = ""
                        )
                    )
                }
                com.aurum.data.repository.DeezerLinkKind.ALBUM -> {
                    com.aurum.data.repository.DeezerMusicProvider.getDeezerAlbumTracks(linkResult.id)
                }
                com.aurum.data.repository.DeezerLinkKind.PLAYLIST -> {
                    com.aurum.data.repository.DeezerMusicProvider.getDeezerPlaylistTracks(linkResult.id)
                }
                else -> emptyList()
            }
            if (tracks.isNotEmpty()) {
                _importedLinkTracks.value = tracks
                onLoaded(tracks)
            }
        }
    }

    fun playDeezerTrack(
        track: com.aurum.data.repository.DeezerTrackSearchResult,
        playlist: List<com.aurum.data.repository.DeezerTrackSearchResult>,
        arlToken: String? = null,
        onReady: (Song, List<Song>) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Fast-resolve full stream URL or local cache file for the clicked track (~80ms)
                val cachedFile = com.aurum.data.repository.DeezerMusicProvider.prepareLocalStreamCacheFile(
                    track = track,
                    arlToken = arlToken
                )
                val playableUri = cachedFile?.ifBlank { null }
                    ?: com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(track.id, arlToken)
                    ?: track.previewStreamUrl

                val fullSong = track.toSong(overrideStreamUrl = playableUri)
                val initialPlaylist = playlist.map { item ->
                    if (item.id == track.id) fullSong else item.toSong()
                }

                // 2. Start playback ONCE with full song on Main thread (no re-preparing active playing track)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    playSong(fullSong, initialPlaylist)
                    onReady(fullSong, initialPlaylist)
                }

                // 3. Asynchronously resolve HD artwork and remaining queue tracks in background
                launch(Dispatchers.IO) {
                    val appleHdArt = com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(track.title, track.artist, track.album)
                        ?: com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(track.artist, track.album)
                    val hdArt = appleHdArt?.ifBlank { null } ?: track.artworkUrl.ifBlank { null }

                    val songWithArt = fullSong.copy(albumArtUri = hdArt)

                    val resolvedPlaylist = playlist.map { item ->
                        if (item.id == track.id) {
                            songWithArt
                        } else {
                            val itemCache = com.aurum.data.repository.DeezerMusicProvider.prepareLocalStreamCacheFile(
                                track = item,
                                arlToken = arlToken
                            )
                            val itemUri = itemCache?.ifBlank { null }
                                ?: com.aurum.data.repository.DeezerMusicProvider.resolveFullTrackStreamUrl(item.id, arlToken)
                                ?: item.previewStreamUrl
                            item.toSong(overrideStreamUrl = itemUri)
                        }
                    }

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        musicPlayer.updateQueue(resolvedPlaylist)
                    }
                }
            } catch (_: Exception) {
                val fallbackSong = track.toSong()
                val fallbackPlaylist = playlist.map { it.toSong() }
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    playSong(fallbackSong, fallbackPlaylist)
                    onReady(fallbackSong, fallbackPlaylist)
                }
            }
        }
    }

    fun downloadDeezerTrackFile(
        track: com.aurum.data.repository.DeezerTrackSearchResult,
        downloadDir: String? = null,
        arlToken: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _downloadingTrackId.value = track.id
            try {
                // Ensure 1000x1000 HD artwork is fetched from Apple Music API and cached for download
                val appleHdArt = com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(track.title, track.artist, track.album)
                    ?: com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(track.artist, track.album)
                val targetTrack = if (!appleHdArt.isNullOrBlank()) track.copy(artworkUrl = appleHdArt) else track

                if (!appleHdArt.isNullOrBlank()) {
                    com.aurum.data.repository.OnlineArtworkFetcher.downloadAndCacheArtwork(
                        artist = targetTrack.artist,
                        album = targetTrack.album,
                        artworkUrl = appleHdArt
                    )
                }

                val filePath = com.aurum.data.repository.DeezerMusicProvider.downloadTrackAudioFile(
                    track = targetTrack,
                    downloadDir = downloadDir,
                    arlToken = arlToken
                )
                if (!filePath.isNullOrBlank()) {
                    rescan()
                    onComplete(true, filePath)
                } else {
                    onComplete(false, null)
                }
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _downloadingTrackId.value = null
            }
        }
    }

    fun setEditingTargetSong(song: Song?) {
        _editingTargetSong.value = song
        if (song != null) {
            _editingTargetAlbum.value = null
            _editingTargetArtist.value = null
            searchAppleMusic(song.title.ifBlank { song.artist }, "song")
        }
    }

    fun setEditingTargetAlbum(album: Album?) {
        _editingTargetAlbum.value = album
        if (album != null) {
            _editingTargetSong.value = null
            _editingTargetArtist.value = null
            searchAppleMusic(album.title.ifBlank { album.artist }, "album")
        }
    }

    fun setEditingTargetArtist(artist: Artist?) {
        _editingTargetArtist.value = artist
        if (artist != null) {
            _editingTargetSong.value = null
            _editingTargetAlbum.value = null
            searchAppleMusic(artist.name, "artist")
        }
    }

    fun clearEditingTargets() {
        _editingTargetSong.value = null
        _editingTargetAlbum.value = null
        _editingTargetArtist.value = null
    }

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                var songs = musicRepository.getSongs()

                // Merge user-created duplicate Big Ole Album entries directly into Big Ole Album, Vol. 1
                val hasSourceBigOle = songs.any { it.album.equals("Big Ole Album", ignoreCase = true) }
                val hasTargetBigOle = songs.any { it.album.contains("Big Ole Album", ignoreCase = true) && it.album.contains("Vol", ignoreCase = true) }
                if (hasSourceBigOle && hasTargetBigOle) {
                    musicRepository.mergeAlbums("Big Ole Album", "Big Ole Album, Vol. 1", "A Day to Remember", null)
                    songs = musicRepository.getSongs()
                }

                val albums = musicRepository.getAlbums()
                val artists = musicRepository.getArtists()
                val playlists = musicRepository.getPlaylists()

                val favoriteSongs = songs.filter { it.isFavorite }
                val smartPlaylists = mutableListOf(
                    Playlist(id = "smart_favorites", name = "★ Favorites", songIds = favoriteSongs.map { it.id }),
                    Playlist(id = "smart_recent", name = "🕒 Recently Added", songIds = songs.take(25).map { it.id })
                )
                smartPlaylists.addAll(playlists)

                val sortedAndFiltered = songs.sortSongs(SongSortBy.TITLE, SortOrder.ASCENDING)
                _uiState.value = LibraryUiState.Success(
                    allSongs = songs,
                    filteredSongs = sortedAndFiltered,
                    albums = albums,
                    artists = artists,
                    playlists = smartPlaylists,
                )
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load music library")
            }
        }
    }

    fun rescan() {
        viewModelScope.launch {
            try {
                musicRepository.rescan()
                loadLibrary()
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to rescan library")
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(song.id)
                loadLibrary()
            } catch (_: Exception) {
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            try {
                musicRepository.addSongToPlaylist(playlistId, songId)
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                musicRepository.deleteSong(song.id)
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun updateAlbumMetadata(oldAlbumTitle: String, newTitle: String, newArtist: String) {
        viewModelScope.launch {
            try {
                musicRepository.updateAlbumMetadata(oldAlbumTitle, newTitle, newArtist)
                loadLibrary()
            } catch (_: Exception) {
            }
        }
    }

    fun autoRepairAlbumMetadata(albumTitle: String) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val existingAlbum = if (currentState is LibraryUiState.Success) {
                    currentState.albums.find { it.title.equals(albumTitle, ignoreCase = true) }
                } else null
                val existingArt = existingAlbum?.albumArtUri?.takeIf { it.isNotBlank() }

                val cleanedTitle = albumTitle
                    .replace(Regex("""(?i)(yt1s\.com|x_media_|\.mp3|\.flac|\.m4a|720p|1080p|official|video|audio)"""), "")
                    .replace("_", " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                    .capitalizeWords()

                val finalTitle = if (cleanedTitle.isNotBlank()) cleanedTitle else "Restored Album"
                val cleanedArtist = existingAlbum?.artist?.takeIf { it.isNotBlank() && !it.contains("Unknown", ignoreCase = true) } ?: "Aurum Artist"

                if (existingArt != null) {
                    musicRepository.updateAlbumMetadata(albumTitle, finalTitle, cleanedArtist)
                } else {
                    val highResArtwork = "https://picsum.photos/seed/${kotlin.math.abs(finalTitle.hashCode())}/600/600"
                    musicRepository.updateAlbumMetadataAndArtwork(albumTitle, finalTitle, cleanedArtist, highResArtwork)
                }
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun removeDuplicateSongs(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is LibraryUiState.Success) {
                    val seen = mutableSetOf<String>()
                    val duplicates = mutableListOf<String>()

                    for (song in currentState.allSongs) {
                        val key = "${song.title.lowercase().trim()}_${song.artist.lowercase().trim()}"
                        if (!seen.add(key)) {
                            duplicates.add(song.id)
                        }
                    }

                    for (id in duplicates) {
                        musicRepository.deleteSong(id)
                    }

                    loadLibrary()
                    onResult(duplicates.size)
                }
            } catch (e: Exception) {
                onResult(0)
            }
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch {
            val currentState = uiState.value
            val allSongs = if (currentState is LibraryUiState.Success) currentState.allSongs else emptyList()
            val currentPlayerState = musicPlayer.state.value

            if (currentPlayerState.currentSong == null) {
                val playlist = if (currentPlayerState.shuffleEnabled) {
                    val remaining = allSongs.filter { it.id != song.id }.shuffled()
                    listOf(song) + remaining
                } else {
                    val index = allSongs.indexOfFirst { it.id == song.id }
                    if (index != -1) {
                        allSongs.drop(index) + allSongs.take(index)
                    } else {
                        allSongs
                    }
                }
                musicPlayer.play(playlist, 0)
            } else {
                musicPlayer.playNext(song)
            }
        }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch {
            val currentState = uiState.value
            val allSongs = if (currentState is LibraryUiState.Success) currentState.allSongs else emptyList()
            val currentPlayerState = musicPlayer.state.value

            if (currentPlayerState.currentSong == null) {
                val playlist = if (currentPlayerState.shuffleEnabled) {
                    val remaining = allSongs.filter { it.id != song.id }.shuffled()
                    listOf(song) + remaining
                } else {
                    val index = allSongs.indexOfFirst { it.id == song.id }
                    if (index != -1) {
                        allSongs.drop(index) + allSongs.take(index)
                    } else {
                        allSongs
                    }
                }
                musicPlayer.play(playlist, 0)
            } else {
                musicPlayer.addToQueue(song)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            if (state is LibraryUiState.Success) {
                val filtered = state.allSongs
                    .filterByQuery(query)
                    .sortSongs(state.sortBy, state.sortOrder)
                state.copy(searchQuery = query, filteredSongs = filtered)
            } else state
        }
    }

    fun onSortChanged(sortBy: SongSortBy, order: SortOrder) {
        _uiState.update { state ->
            if (state is LibraryUiState.Success) {
                val sorted = state.allSongs
                    .filterByQuery(state.searchQuery)
                    .sortSongs(sortBy, order)
                state.copy(sortBy = sortBy, sortOrder = order, filteredSongs = sorted)
            } else state
        }
    }

    fun playSong(song: Song, playlist: List<Song>) {
        viewModelScope.launch {
            if (song.id.startsWith("deezer_stream_") && (song.uri.isBlank() || song.uri.contains("cdns-preview") || song.uri.contains("preview"))) {
                val cleanId = song.id.removePrefix("deezer_stream_")
                val deezerTrack = com.aurum.data.repository.DeezerTrackSearchResult(
                    id = cleanId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationSeconds = song.duration / 1000L,
                    previewStreamUrl = song.uri,
                    artworkUrl = song.albumArtUri ?: ""
                )
                val deezerPlaylist = playlist.map { s ->
                    val cid = s.id.removePrefix("deezer_stream_")
                    com.aurum.data.repository.DeezerTrackSearchResult(
                        id = cid,
                        title = s.title,
                        artist = s.artist,
                        album = s.album,
                        durationSeconds = s.duration / 1000L,
                        previewStreamUrl = s.uri,
                        artworkUrl = s.albumArtUri ?: ""
                    )
                }
                val activeArl = com.aurum.data.repository.DeezerSessionManager.getSessionInfo()?.arl
                playDeezerTrack(deezerTrack, deezerPlaylist, activeArl)
                return@launch
            }

            val index = playlist.indexOfFirst { it.id == song.id }
            if (index != -1) {
                musicPlayer.play(playlist, index)
            } else {
                musicPlayer.play(song)
            }
        }
    }

    fun searchAppleMusic(query: String, type: String = "album") {
        viewModelScope.launch {
            if (query.isBlank()) {
                _appleMusicResults.value = emptyList()
                return@launch
            }
            _isSearchingAppleMusic.value = true
            try {
                val results = com.aurum.data.repository.OnlineArtworkFetcher.searchAppleMusic(query, type)
                _appleMusicResults.value = results
            } catch (_: Exception) {
                _appleMusicResults.value = emptyList()
            } finally {
                _isSearchingAppleMusic.value = false
            }
        }
    }

    fun fetchArtistDiscography(artistName: String) {
        viewModelScope.launch {
            if (artistName.isBlank()) {
                _artistDiscography.value = emptyList()
                return@launch
            }
            _isLoadingDiscography.value = true
            try {
                val results = com.aurum.data.repository.OnlineArtworkFetcher.fetchArtistDiscography(artistName)
                _artistDiscography.value = results
            } catch (_: Exception) {
                _artistDiscography.value = emptyList()
            } finally {
                _isLoadingDiscography.value = false
            }
        }
    }

    fun applyMetadataToSong(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUrl: String) {
        viewModelScope.launch {
            try {
                musicRepository.updateSongMetadataAndArtwork(songId, newTitle, newArtist, newAlbum, artworkUrl)
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun repairArtworkForAlbum(artistName: String, albumTitle: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val fetchedUrl = com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(artistName, albumTitle)
                    ?: com.aurum.data.repository.OnlineArtworkFetcher.fetchArtworkForTrack(albumTitle, artistName, albumTitle)

                if (!fetchedUrl.isNullOrBlank()) {
                    musicRepository.updateAlbumMetadataAndArtwork(albumTitle, albumTitle, artistName, fetchedUrl)
                    loadLibrary()
                    onResult?.invoke(true)
                } else {
                    onResult?.invoke(false)
                }
            } catch (e: Exception) {
                onResult?.invoke(false)
            }
        }
    }

    fun repairAllMissingArtwork(onProgress: (Int, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is LibraryUiState.Success) {
                    val missingAlbums = currentState.albums.filter { it.albumArtUri.isNullOrBlank() }
                    val total = missingAlbums.size
                    var count = 0

                    for (album in missingAlbums) {
                        count++
                        onProgress(count, total)
                        if (album.title.isNotBlank() && !album.title.contains("Unknown", ignoreCase = true)) {
                            val fetchedUrl = com.aurum.data.repository.OnlineArtworkFetcher.fetchAlbumArtworkUrl(album.artist, album.title)
                            if (!fetchedUrl.isNullOrBlank()) {
                                musicRepository.updateAlbumMetadataAndArtwork(album.title, album.title, album.artist, fetchedUrl)
                            }
                        }
                    }
                    loadLibrary()
                }
            } catch (_: Exception) {}
        }
    }

    fun applyMetadataToSongAndAlbum(songId: String, newTitle: String, newArtist: String, newAlbum: String, artworkUrl: String) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val song = if (currentState is LibraryUiState.Success) currentState.allSongs.find { it.id == songId } else null
                val oldAlbum = song?.album

                // 1. Update target song
                musicRepository.updateSongMetadataAndArtwork(
                    songId = songId,
                    newTitle = newTitle,
                    newArtist = newArtist,
                    newAlbum = newAlbum,
                    artworkUri = artworkUrl
                )

                // 2. Update sister songs in same album
                if (!oldAlbum.isNullOrBlank() && currentState is LibraryUiState.Success) {
                    val sisterSongs = currentState.allSongs.filter { it.album.equals(oldAlbum, ignoreCase = true) && it.id != songId }
                    for (sister in sisterSongs) {
                        musicRepository.updateSongMetadataAndArtwork(
                            songId = sister.id,
                            newTitle = sister.title,
                            newArtist = newArtist,
                            newAlbum = newAlbum,
                            artworkUri = artworkUrl
                        )
                    }
                }
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun applyMetadataToAlbum(oldAlbumTitle: String, newTitle: String, newArtist: String, artworkUrl: String) {
        viewModelScope.launch {
            try {
                musicRepository.updateAlbumMetadataAndArtwork(oldAlbumTitle, newTitle, newArtist, artworkUrl)
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun mergeAlbums(sourceAlbumTitle: String, targetAlbumTitle: String, targetArtist: String, artworkUrl: String? = null) {
        viewModelScope.launch {
            try {
                musicRepository.mergeAlbums(sourceAlbumTitle, targetAlbumTitle, targetArtist, artworkUrl)
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    fun applyMetadataToArtist(oldArtistName: String, newArtistName: String, artworkUrl: String) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is LibraryUiState.Success) {
                    val artistSongs = currentState.allSongs.filter { it.artist.equals(oldArtistName, ignoreCase = true) }
                    for (song in artistSongs) {
                        musicRepository.updateSongMetadataAndArtwork(
                            songId = song.id,
                            newTitle = song.title,
                            newArtist = newArtistName,
                            newAlbum = song.album,
                            artworkUri = artworkUrl
                        )
                    }
                }
                loadLibrary()
            } catch (_: Exception) {}
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
