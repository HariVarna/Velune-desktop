package com.nikhil.yt.desktop

import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.AlbumItem
import com.nikhil.yt.innertube.models.ArtistItem
import com.nikhil.yt.innertube.models.PlaylistItem
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.WatchEndpoint
import com.nikhil.yt.innertube.models.YTItem
import com.nikhil.yt.innertube.pages.AlbumPage
import com.nikhil.yt.innertube.pages.ArtistPage
import com.nikhil.yt.innertube.pages.HomePage
import com.nikhil.yt.innertube.pages.PlaylistPage
import com.nikhil.yt.innertube.pages.SearchSummary
import com.nikhil.yt.lrclib.LrcLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DesktopDestination {
    data object Home : DesktopDestination
    data object Search : DesktopDestination
    data object Queue : DesktopDestination
    data object Lyrics : DesktopDestination
    data class AlbumDetail(val browseId: String, val title: String) : DesktopDestination
    data class PlaylistDetail(val playlistId: String, val title: String) : DesktopDestination
    data class ArtistDetail(val browseId: String, val title: String) : DesktopDestination
}

enum class DesktopSearchFilter(val label: String, val filter: YouTube.SearchFilter?) {
    ALL("All", null),
    SONGS("Songs", YouTube.SearchFilter.FILTER_SONG),
    ALBUMS("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    ARTISTS("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    PLAYLISTS("Playlists", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST),
}

data class DesktopUiState(
    val backStack: List<DesktopDestination> = listOf(DesktopDestination.Home),
    val homeLoading: Boolean = false,
    val homePage: HomePage? = null,
    val homeError: String? = null,
    val searchQuery: String = "",
    val searchFilter: DesktopSearchFilter = DesktopSearchFilter.ALL,
    val searchLoading: Boolean = false,
    val searchSummaries: List<SearchSummary> = emptyList(),
    val searchItems: List<YTItem> = emptyList(),
    val searchError: String? = null,
    val detailLoading: Boolean = false,
    val albumPage: AlbumPage? = null,
    val playlistPage: PlaylistPage? = null,
    val artistPage: ArtistPage? = null,
    val detailError: String? = null,
    val queue: List<SongItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val lyricsLoading: Boolean = false,
    val lyricsText: String? = null,
    val lyricsError: String? = null,
) {
    val currentDestination: DesktopDestination
        get() = backStack.lastOrNull() ?: DesktopDestination.Home
}

class DesktopAppController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(DesktopUiState())
    val uiState: StateFlow<DesktopUiState> = _uiState.asStateFlow()

    val player = DesktopAudioPlayer(
        onTrackEnded = { playNext() },
    )

    init {
        scope.launch {
            if (YouTube.visitorData.isNullOrBlank()) {
                YouTube.visitorData().getOrNull()?.let { YouTube.visitorData = it }
            }
            loadHome()
        }
    }

    fun navigateTopLevel(destination: DesktopDestination) {
        _uiState.update { it.copy(backStack = listOf(destination)) }
        if (destination is DesktopDestination.Lyrics) {
            loadLyricsForCurrentSong()
        }
    }

    fun openItem(item: YTItem) {
        when (item) {
            is SongItem -> playSingleSong(item)
            is AlbumItem -> openAlbum(item.browseId, item.title)
            is PlaylistItem -> openPlaylist(item.id, item.title)
            is ArtistItem -> openArtist(item.id, item.title)
        }
    }

    fun navigateBack() {
        _uiState.update { state ->
            if (state.backStack.size > 1) {
                state.copy(backStack = state.backStack.dropLast(1))
            } else {
                state
            }
        }
    }

    fun loadHome() {
        scope.launch {
            _uiState.update { it.copy(homeLoading = true, homeError = null) }
            YouTube.home()
                .onSuccess { page ->
                    _uiState.update { it.copy(homeLoading = false, homePage = page) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(homeLoading = false, homeError = err.message ?: "Failed to load Home")
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectSearchFilter(filter: DesktopSearchFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        if (_uiState.value.searchQuery.isNotBlank()) {
            performSearch()
        }
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return

        if (_uiState.value.currentDestination !is DesktopDestination.Search) {
            _uiState.update { it.copy(backStack = listOf(DesktopDestination.Search)) }
        }

        val activeFilter = _uiState.value.searchFilter
        scope.launch {
            _uiState.update {
                it.copy(
                    searchLoading = true,
                    searchError = null,
                    searchSummaries = emptyList(),
                    searchItems = emptyList(),
                )
            }
            if (activeFilter.filter == null) {
                YouTube.searchSummary(query)
                    .onSuccess { summaryPage ->
                        _uiState.update {
                            it.copy(
                                searchLoading = false,
                                searchSummaries = summaryPage.summaries,
                            )
                        }
                    }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(searchLoading = false, searchError = err.message ?: "Search failed")
                        }
                    }
            } else {
                YouTube.search(query, activeFilter.filter)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                searchLoading = false,
                                searchItems = result.items,
                            )
                        }
                    }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(searchLoading = false, searchError = err.message ?: "Search failed")
                        }
                    }
            }
        }
    }

    fun openAlbum(browseId: String, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.AlbumDetail(browseId, title),
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.album(browseId)
                .onSuccess { page ->
                    _uiState.update { it.copy(detailLoading = false, albumPage = page) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(detailLoading = false, detailError = err.message ?: "Failed to load album")
                    }
                }
        }
    }

    fun openPlaylist(playlistId: String, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.PlaylistDetail(playlistId, title),
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.playlist(playlistId)
                .onSuccess { page ->
                    _uiState.update { it.copy(detailLoading = false, playlistPage = page) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(detailLoading = false, detailError = err.message ?: "Failed to load playlist")
                    }
                }
        }
    }

    fun openArtist(browseId: String, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.ArtistDetail(browseId, title),
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.artist(browseId)
                .onSuccess { page ->
                    _uiState.update { it.copy(detailLoading = false, artistPage = page) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(detailLoading = false, detailError = err.message ?: "Failed to load artist")
                    }
                }
        }
    }

    fun playSingleSong(song: SongItem) {
        _uiState.update {
            it.copy(
                queue = listOf(song),
                currentQueueIndex = 0,
            )
        }
        player.play(song)
        loadLyricsForSong(song)

        // Populate Up Next radio tracks in background
        scope.launch {
            val nextResult = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
            val radioSongs = nextResult?.items?.filter { it.id != song.id }.orEmpty()
            if (radioSongs.isNotEmpty()) {
                _uiState.update { state ->
                    if (state.queue.firstOrNull()?.id == song.id && state.queue.size == 1) {
                        state.copy(queue = listOf(song) + radioSongs)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun playSongList(songs: List<SongItem>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(0, songs.lastIndex)
        val selected = songs[index]
        _uiState.update {
            it.copy(
                queue = songs,
                currentQueueIndex = index,
            )
        }
        player.play(selected)
        loadLyricsForSong(selected)
    }

    fun addToQueue(song: SongItem) {
        _uiState.update { state ->
            state.copy(queue = state.queue + song)
        }
    }

    fun playNext() {
        val state = _uiState.value
        val nextIndex = state.currentQueueIndex + 1
        if (nextIndex in state.queue.indices) {
            val nextSong = state.queue[nextIndex]
            _uiState.update { it.copy(currentQueueIndex = nextIndex) }
            player.play(nextSong)
            loadLyricsForSong(nextSong)
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        if (player.state.value.positionMs > 4000L) {
            player.seekTo(0L)
            return
        }
        val prevIndex = state.currentQueueIndex - 1
        if (prevIndex in state.queue.indices) {
            val prevSong = state.queue[prevIndex]
            _uiState.update { it.copy(currentQueueIndex = prevIndex) }
            player.play(prevSong)
            loadLyricsForSong(prevSong)
        }
    }

    fun loadLyricsForCurrentSong() {
        val song = player.state.value.currentSong ?: return
        loadLyricsForSong(song)
    }

    private fun loadLyricsForSong(song: SongItem) {
        scope.launch {
            _uiState.update { it.copy(lyricsLoading = true, lyricsText = null, lyricsError = null) }
            val artistName = song.artists.firstOrNull()?.name.orEmpty()
            val duration = song.duration ?: -1
            LrcLib.getLyrics(
                title = song.title,
                artist = artistName,
                duration = duration,
                album = song.album?.name,
            ).onSuccess { text ->
                _uiState.update { it.copy(lyricsLoading = false, lyricsText = text) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        lyricsLoading = false,
                        lyricsError = err.message ?: "Lyrics unavailable",
                    )
                }
            }
        }
    }

    fun shutdown() {
        player.shutdown()
        scope.cancel()
    }
}
