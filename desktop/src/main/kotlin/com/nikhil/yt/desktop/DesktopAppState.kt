package com.nikhil.yt.desktop

import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.AlbumItem
import com.nikhil.yt.innertube.models.ArtistItem
import com.nikhil.yt.innertube.models.BrowseEndpoint
import com.nikhil.yt.innertube.models.PlaylistItem
import com.nikhil.yt.innertube.models.SearchSuggestions
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.WatchEndpoint
import com.nikhil.yt.innertube.models.YTItem
import com.nikhil.yt.innertube.models.filterExplicit
import com.nikhil.yt.innertube.pages.AlbumPage
import com.nikhil.yt.innertube.pages.ArtistPage
import com.nikhil.yt.innertube.pages.BrowseResult
import com.nikhil.yt.innertube.pages.ChartsPage
import com.nikhil.yt.innertube.pages.ExplorePage
import com.nikhil.yt.innertube.pages.HomePage
import com.nikhil.yt.innertube.pages.MoodAndGenres
import com.nikhil.yt.innertube.pages.PlaylistPage
import com.nikhil.yt.innertube.pages.SearchSummary
import com.nikhil.yt.innertube.toHighResThumbnail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

sealed interface DesktopDestination {
    data object Home : DesktopDestination
    data object Explore : DesktopDestination
    data object Search : DesktopDestination
    data object Library : DesktopDestination
    data object Settings : DesktopDestination
    data class AlbumDetail(val browseId: String, val title: String) : DesktopDestination
    data class PlaylistDetail(val playlistId: String, val title: String) : DesktopDestination
    data class ArtistDetail(val browseId: String, val title: String) : DesktopDestination
    data class BrowseDetail(val browseId: String, val params: String?, val title: String) : DesktopDestination
}

enum class DesktopSearchFilter(val label: String, val filter: YouTube.SearchFilter?) {
    ALL("All", null),
    SONGS("Songs", YouTube.SearchFilter.FILTER_SONG),
    VIDEOS("Videos", YouTube.SearchFilter.FILTER_VIDEO),
    ALBUMS("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    ARTISTS("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    PLAYLISTS("Playlists", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST),
}

enum class LibraryTab(val label: String) {
    LIKED("Liked Songs"),
    PLAYLISTS("Playlists"),
    HISTORY("History"),
    STATS("Stats"),
}

data class DesktopUiState(
    val backStack: List<DesktopDestination> = listOf(DesktopDestination.Home),
    val persisted: DesktopPersistedState = DesktopPersistedState(),
    val artworkPalette: ArtworkPalette = ArtworkPalette(),
    val isPlayerExpanded: Boolean = false,
    val isPortableMiniPlayerOpen: Boolean = false,
    val isFullscreen: Boolean = false,
    val libraryTab: LibraryTab = LibraryTab.LIKED,
    val selectedLocalPlaylistId: String? = null,

    // Home
    val homeLoading: Boolean = false,
    val homePage: HomePage? = null,
    val selectedHomeChip: HomePage.Chip? = null,
    val homeError: String? = null,
    val forYouSuggestions: List<SongItem> = emptyList(),
    val quickPicks: List<SongItem> = emptyList(),
    val forgottenFavorites: List<SongItem> = emptyList(),

    // Explore
    val exploreLoading: Boolean = false,
    val explorePage: ExplorePage? = null,
    val chartsPage: ChartsPage? = null,
    val moodAndGenres: List<MoodAndGenres> = emptyList(),
    val exploreError: String? = null,

    // Search & Live Suggestions
    val searchQuery: String = "",
    val searchFocused: Boolean = false,
    val searchFilter: DesktopSearchFilter = DesktopSearchFilter.ALL,
    val searchSuggestions: SearchSuggestions? = null,
    val searchLoading: Boolean = false,
    val searchSummaries: List<SearchSummary> = emptyList(),
    val searchItems: List<YTItem> = emptyList(),
    val searchError: String? = null,

    // Detail screens
    val detailLoading: Boolean = false,
    val albumPage: AlbumPage? = null,
    val playlistPage: PlaylistPage? = null,
    val artistPage: ArtistPage? = null,
    val browseResult: BrowseResult? = null,
    val detailError: String? = null,

    // Queue & Lyrics
    val queue: List<SongItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val lyricsLoading: Boolean = false,
    val lyricsResult: LyricsResult? = null,
    val lyricsError: String? = null,
) {
    val currentDestination: DesktopDestination
        get() = backStack.lastOrNull() ?: DesktopDestination.Home

    val likedIds: Set<String>
        get() = persisted.likedSongs.map { it.id }.toSet()
}

class DesktopAppController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storage = DesktopStorage()

    private val _uiState = MutableStateFlow(DesktopUiState(persisted = storage.load()))
    val uiState: StateFlow<DesktopUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var blurHideJob: Job? = null
    private var lyricsJob: Job? = null
    private var paletteJob: Job? = null

    val player = DesktopAudioPlayer(
        onTrackEnded = { handleTrackEnded() },
    )

    init {
        player.setVolume(_uiState.value.persisted.settings.volume)
        refreshLocalDerivedSections()
        scope.launch {
            if (YouTube.visitorData.isNullOrBlank()) {
                YouTube.visitorData().getOrNull()?.let { YouTube.visitorData = it }
            }
            loadHome()
            loadExplore()
        }
    }

    fun navigateTopLevel(destination: DesktopDestination) {
        _uiState.update {
            it.copy(
                backStack = listOf(destination),
                isPlayerExpanded = false,
                searchFocused = false,
            )
        }
        if (destination is DesktopDestination.Explore && _uiState.value.explorePage == null) {
            loadExplore()
        }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun togglePortableMiniPlayer() {
        _uiState.update { it.copy(isPortableMiniPlayerOpen = !it.isPortableMiniPlayerOpen) }
    }

    fun setPortableMiniPlayerOpen(open: Boolean) {
        _uiState.update { it.copy(isPortableMiniPlayerOpen = open) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun resizeWindow(deltaWidthDp: Int, deltaHeightDp: Int) {
        updateSettings { s ->
            s.copy(
                windowWidthDp = (s.windowWidthDp + deltaWidthDp).coerceIn(860, 2560),
                windowHeightDp = (s.windowHeightDp + deltaHeightDp).coerceIn(580, 1440),
            )
        }
    }

    fun selectLibraryTab(tab: LibraryTab) {
        _uiState.update { it.copy(libraryTab = tab, selectedLocalPlaylistId = null) }
    }

    fun openLocalPlaylist(playlistId: String?) {
        _uiState.update { it.copy(selectedLocalPlaylistId = playlistId) }
    }

    fun openItem(item: YTItem) {
        _uiState.update { it.copy(searchFocused = false) }
        when (item) {
            is SongItem -> playSingleSong(item)
            is AlbumItem -> openAlbum(item.browseId, item.title)
            is PlaylistItem -> openPlaylist(item.id, item.title)
            is ArtistItem -> openArtist(item.id, item.title)
        }
    }

    fun navigateBack() {
        _uiState.update { state ->
            when {
                state.isPlayerExpanded -> state.copy(isPlayerExpanded = false)
                state.selectedLocalPlaylistId != null -> state.copy(selectedLocalPlaylistId = null)
                state.backStack.size > 1 -> state.copy(backStack = state.backStack.dropLast(1))
                else -> state
            }
        }
    }

    fun loadHome(chip: HomePage.Chip? = _uiState.value.selectedHomeChip) {
        scope.launch {
            _uiState.update { it.copy(homeLoading = true, homeError = null, selectedHomeChip = chip) }
            val params = chip?.endpoint?.params
            YouTube.home(params = params)
                .onSuccess { page ->
                    _uiState.update { it.copy(homeLoading = false, homePage = page) }
                    refreshForYouSuggestions(page)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(homeLoading = false, homeError = err.message ?: "Failed to load Home")
                    }
                }
        }
    }

    fun toggleHomeChip(chip: HomePage.Chip) {
        val current = _uiState.value.selectedHomeChip
        if (current?.title == chip.title) {
            loadHome(null)
        } else {
            loadHome(chip)
        }
    }

    fun loadExplore() {
        scope.launch {
            _uiState.update { it.copy(exploreLoading = true, exploreError = null) }
            val explore = YouTube.explore().getOrNull()
            val charts = YouTube.getChartsPage().getOrNull()
            val moods = YouTube.moodAndGenres().getOrNull().orEmpty()
            if (explore == null && charts == null && moods.isEmpty()) {
                _uiState.update { it.copy(exploreLoading = false, exploreError = "Failed to load Explore") }
            } else {
                _uiState.update {
                    it.copy(
                        exploreLoading = false,
                        explorePage = explore,
                        chartsPage = charts,
                        moodAndGenres = moods,
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        suggestionJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchSuggestions = null) }
            return
        }
        suggestionJob = scope.launch {
            delay(250L)
            YouTube.searchSuggestions(query.trim())
                .onSuccess { suggestions ->
                    _uiState.update { it.copy(searchSuggestions = suggestions) }
                }
        }
    }

    fun setSearchFocused(focused: Boolean) {
        blurHideJob?.cancel()
        if (focused) {
            _uiState.update { it.copy(searchFocused = true) }
        } else {
            blurHideJob = scope.launch {
                delay(200L)
                _uiState.update { it.copy(searchFocused = false) }
            }
        }
    }

    fun selectSearchFilter(filter: DesktopSearchFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        if (_uiState.value.searchQuery.isNotBlank()) {
            performSearch()
        }
    }

    fun performSearch(overrideQuery: String? = null) {
        val query = (overrideQuery ?: _uiState.value.searchQuery).trim()
        if (query.isEmpty()) return

        addSearchHistory(query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchFocused = false,
                backStack = listOf(DesktopDestination.Search),
                isPlayerExpanded = false,
            )
        }

        val activeFilter = _uiState.value.searchFilter
        val hideExplicit = _uiState.value.persisted.settings.hideExplicit
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
                        val filtered = summaryPage.summaries.map { s ->
                            s.copy(items = s.items.filterExplicit(hideExplicit))
                        }.filter { it.items.isNotEmpty() }
                        _uiState.update {
                            it.copy(searchLoading = false, searchSummaries = filtered)
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
                                searchItems = result.items.filterExplicit(hideExplicit),
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
                isPlayerExpanded = false,
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                browseResult = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.album(browseId)
                .onSuccess { page -> _uiState.update { it.copy(detailLoading = false, albumPage = page) } }
                .onFailure { err ->
                    _uiState.update { it.copy(detailLoading = false, detailError = err.message ?: "Failed to load album") }
                }
        }
    }

    fun openPlaylist(playlistId: String, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.PlaylistDetail(playlistId, title),
                isPlayerExpanded = false,
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                browseResult = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.playlist(playlistId)
                .onSuccess { page -> _uiState.update { it.copy(detailLoading = false, playlistPage = page) } }
                .onFailure { err ->
                    _uiState.update { it.copy(detailLoading = false, detailError = err.message ?: "Failed to load playlist") }
                }
        }
    }

    fun openArtist(browseId: String, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.ArtistDetail(browseId, title),
                isPlayerExpanded = false,
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                browseResult = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.artist(browseId)
                .onSuccess { page -> _uiState.update { it.copy(detailLoading = false, artistPage = page) } }
                .onFailure { err ->
                    _uiState.update { it.copy(detailLoading = false, detailError = err.message ?: "Failed to load artist") }
                }
        }
    }

    fun openBrowse(endpoint: BrowseEndpoint, title: String) {
        _uiState.update {
            it.copy(
                backStack = it.backStack + DesktopDestination.BrowseDetail(endpoint.browseId, endpoint.params, title),
                isPlayerExpanded = false,
                detailLoading = true,
                albumPage = null,
                playlistPage = null,
                artistPage = null,
                browseResult = null,
                detailError = null,
            )
        }
        scope.launch {
            YouTube.browse(endpoint.browseId, endpoint.params)
                .onSuccess { res -> _uiState.update { it.copy(detailLoading = false, browseResult = res) } }
                .onFailure { err ->
                    _uiState.update { it.copy(detailLoading = false, detailError = err.message ?: "Failed to load page") }
                }
        }
    }

    fun playSingleSong(song: SongItem) {
        _uiState.update { it.copy(queue = listOf(song), currentQueueIndex = 0) }
        startPlayback(song)

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

    fun playSongList(songs: List<SongItem>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val effectiveList = if (shuffle) songs.shuffled() else songs
        val index = if (shuffle) 0 else startIndex.coerceIn(0, effectiveList.lastIndex)
        val selected = effectiveList[index]
        _uiState.update { it.copy(queue = effectiveList, currentQueueIndex = index) }
        startPlayback(selected)
    }

    fun jumpToQueueIndex(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        _uiState.update { it.copy(currentQueueIndex = index) }
        startPlayback(queue[index])
    }

    fun addToQueue(song: SongItem) {
        _uiState.update { it.copy(queue = it.queue + song) }
    }

    fun playNextInQueue(song: SongItem) {
        _uiState.update { state ->
            val insertAt = (state.currentQueueIndex + 1).coerceIn(0, state.queue.size)
            val mutable = state.queue.toMutableList()
            mutable.add(insertAt, song)
            state.copy(queue = mutable)
        }
    }

    fun playRandomSong() {
        val state = _uiState.value
        val pool = buildList {
            addAll(state.forYouSuggestions)
            addAll(state.quickPicks)
            addAll(state.persisted.likedSongs.map { it.toSongItem() })
            state.homePage?.sections?.forEach { section ->
                addAll(section.items.filterIsInstance<SongItem>())
            }
        }.distinctBy { it.id }

        if (pool.isNotEmpty()) {
            val shuffled = pool.shuffled()
            playSongList(shuffled, startIndex = 0)
        }
    }

    fun removeFromQueue(index: Int) {
        _uiState.update { state ->
            if (index !in state.queue.indices) return@update state
            val mutable = state.queue.toMutableList()
            mutable.removeAt(index)
            val newIndex = when {
                mutable.isEmpty() -> -1
                index < state.currentQueueIndex -> state.currentQueueIndex - 1
                index == state.currentQueueIndex -> state.currentQueueIndex.coerceAtMost(mutable.lastIndex)
                else -> state.currentQueueIndex
            }
            state.copy(queue = mutable, currentQueueIndex = newIndex)
        }
    }

    fun playNext() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        // Record skip if user skipped early
        val currentSong = player.state.value.currentSong
        if (currentSong != null && player.state.value.positionMs in 1000L..25_000L) {
            recordSkip(currentSong.id)
        }

        val nextIndex = if (state.persisted.settings.shuffleEnabled && state.queue.size > 1) {
            (state.queue.indices - state.currentQueueIndex).random()
        } else {
            state.currentQueueIndex + 1
        }

        if (nextIndex in state.queue.indices) {
            val nextSong = state.queue[nextIndex]
            _uiState.update { it.copy(currentQueueIndex = nextIndex) }
            startPlayback(nextSong)
        } else if (state.persisted.settings.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty()) {
            _uiState.update { it.copy(currentQueueIndex = 0) }
            startPlayback(state.queue.first())
        }
    }

    private fun handleTrackEnded() {
        val state = _uiState.value
        if (state.persisted.settings.repeatMode == RepeatMode.ONE) {
            player.state.value.currentSong?.let { startPlayback(it) }
            return
        }
        playNext()
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
            startPlayback(prevSong)
        }
    }

    private fun startPlayback(song: SongItem) {
        player.play(song)
        recordPlay(song)
        loadLyricsForSong(song)
        extractPaletteForSong(song)
    }

    private fun extractPaletteForSong(song: SongItem) {
        paletteJob?.cancel()
        paletteJob = scope.launch {
            val palette = extractArtworkPalette(song.thumbnail.toHighResThumbnail())
            _uiState.update { it.copy(artworkPalette = palette) }
        }
    }

    fun loadLyricsForCurrentSong() {
        player.state.value.currentSong?.let { loadLyricsForSong(it) }
    }

    private fun loadLyricsForSong(song: SongItem) {
        lyricsJob?.cancel()
        lyricsJob = scope.launch {
            _uiState.update { it.copy(lyricsLoading = true, lyricsResult = null, lyricsError = null) }
            DesktopLyricsEngine.fetchLyrics(song)
                .onSuccess { result ->
                    _uiState.update { it.copy(lyricsLoading = false, lyricsResult = result) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(lyricsLoading = false, lyricsError = err.message ?: "Lyrics unavailable")
                    }
                }
        }
    }

    // --- Persistence & Settings mutations ---

    fun toggleLike(song: SongItem) {
        updatePersisted { persisted ->
            val exists = persisted.likedSongs.any { it.id == song.id }
            val updated = if (exists) {
                persisted.likedSongs.filterNot { it.id == song.id }
            } else {
                listOf(StoredSong.from(song)) + persisted.likedSongs
            }
            persisted.copy(likedSongs = updated)
        }
        refreshLocalDerivedSections()
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updatePersisted { it.copy(playlists = it.playlists + StoredPlaylist(name = trimmed)) }
    }

    fun deletePlaylist(playlistId: String) {
        updatePersisted { it.copy(playlists = it.playlists.filterNot { p -> p.id == playlistId }) }
        if (_uiState.value.selectedLocalPlaylistId == playlistId) {
            _uiState.update { it.copy(selectedLocalPlaylistId = null) }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: SongItem) {
        updatePersisted { persisted ->
            persisted.copy(
                playlists = persisted.playlists.map { pl ->
                    if (pl.id == playlistId && pl.songs.none { it.id == song.id }) {
                        pl.copy(songs = pl.songs + StoredSong.from(song))
                    } else {
                        pl
                    }
                }
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        updatePersisted { persisted ->
            persisted.copy(
                playlists = persisted.playlists.map { pl ->
                    if (pl.id == playlistId) {
                        pl.copy(songs = pl.songs.filterNot { it.id == songId })
                    } else {
                        pl
                    }
                }
            )
        }
    }

    fun removeSearchHistory(query: String) {
        updatePersisted { it.copy(searchHistory = it.searchHistory.filterNot { q -> q == query }) }
    }

    fun clearSearchHistory() {
        updatePersisted { it.copy(searchHistory = emptyList()) }
    }

    private fun addSearchHistory(query: String) {
        updatePersisted { persisted ->
            val updated = (listOf(query) + persisted.searchHistory.filterNot { it.equals(query, ignoreCase = true) }).take(20)
            persisted.copy(searchHistory = updated)
        }
    }

    fun updateSettings(transform: (DesktopSettings) -> DesktopSettings) {
        updatePersisted { it.copy(settings = transform(it.settings)) }
    }

    fun toggleShuffle() {
        updateSettings { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleRepeatMode() {
        updateSettings {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
        updateSettings { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    private fun recordPlay(song: SongItem) {
        val hour = LocalTime.now().hour
        updatePersisted { persisted ->
            val prev = persisted.playStats[song.id]
            val stored = StoredSong.from(song)
            val updatedStats = SongPlayStats(
                song = stored,
                playCount = (prev?.playCount ?: 0) + 1,
                totalPlayTimeMs = (prev?.totalPlayTimeMs ?: 0L) + ((song.duration ?: 180) * 1000L),
                skipCount = prev?.skipCount ?: 0,
                lastPlayedAtMs = System.currentTimeMillis(),
                morningPlays = (prev?.morningPlays ?: 0) + if (hour in 6..11) 1 else 0,
                afternoonPlays = (prev?.afternoonPlays ?: 0) + if (hour in 12..17) 1 else 0,
                eveningPlays = (prev?.eveningPlays ?: 0) + if (hour in 18..21) 1 else 0,
                nightPlays = (prev?.nightPlays ?: 0) + if (hour !in 6..21) 1 else 0,
            )
            val newHistory = (listOf(stored) + persisted.history.filterNot { it.id == song.id }).take(100)
            persisted.copy(
                playStats = persisted.playStats + (song.id to updatedStats),
                history = newHistory,
            )
        }
        refreshLocalDerivedSections()
    }

    private fun recordSkip(songId: String) {
        updatePersisted { persisted ->
            val prev = persisted.playStats[songId] ?: return@updatePersisted persisted
            persisted.copy(
                playStats = persisted.playStats + (songId to prev.copy(skipCount = prev.skipCount + 1))
            )
        }
    }

    private fun refreshLocalDerivedSections() {
        val persisted = _uiState.value.persisted
        val quickPicks = storage.getQuickPicks(persisted)
        val forgotten = storage.getForgottenFavorites(persisted)
        val localForYou = storage.scoreForYouSongs(persisted)
        _uiState.update {
            it.copy(
                quickPicks = quickPicks,
                forgottenFavorites = forgotten,
                forYouSuggestions = if (it.forYouSuggestions.isEmpty()) localForYou else it.forYouSuggestions,
            )
        }
    }

    private fun refreshForYouSuggestions(homePage: HomePage?) {
        scope.launch {
            val localScored = storage.scoreForYouSongs(_uiState.value.persisted)
            val seed = localScored.firstOrNull()
                ?: homePage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.firstOrNull()
            val related = if (seed != null) {
                YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.items.orEmpty()
            } else {
                emptyList()
            }
            val combined = (localScored.take(15) + related)
                .distinctBy { it.id }
                .filterExplicit(_uiState.value.persisted.settings.hideExplicit)
                .take(30)
            if (combined.isNotEmpty()) {
                _uiState.update { it.copy(forYouSuggestions = combined) }
            }
        }
    }

    private inline fun updatePersisted(crossinline block: (DesktopPersistedState) -> DesktopPersistedState) {
        var latest: DesktopPersistedState? = null
        _uiState.update { state ->
            val updated = block(state.persisted)
            latest = updated
            state.copy(persisted = updated)
        }
        latest?.let { toSave ->
            scope.launch { storage.save(toSave) }
        }
    }

    fun shutdown() {
        player.shutdown()
        scope.cancel()
    }
}
