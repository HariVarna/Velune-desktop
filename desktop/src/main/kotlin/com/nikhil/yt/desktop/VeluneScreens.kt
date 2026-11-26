package com.nikhil.yt.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.filterExplicit
import com.nikhil.yt.innertube.pages.MoodAndGenres

@Composable
fun HomeScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val settings = uiState.persisted.settings
    val hideExplicit = settings.hideExplicit
    val cardWidth = settings.cardDensity.cardWidthDp.dp
    val activeSongId = playbackState.currentSong?.id

    when {
        uiState.homeLoading && uiState.homePage == null -> HomeShimmerSkeleton()
        uiState.homeError != null && uiState.homePage == null -> ErrorBox(uiState.homeError, onRetry = { controller.loadHome() })
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                // Home Category Chips (Relax, Energize, Workout, Focus, Commute...)
                val chips = uiState.homePage?.chips.orEmpty()
                if (settings.showHomeChips && chips.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(chips) { chip ->
                                val selected = uiState.selectedHomeChip?.title == chip.title
                                FilterChip(
                                    selected = selected,
                                    onClick = { controller.toggleHomeChip(chip) },
                                    label = { Text(chip.title) },
                                )
                            }
                        }
                    }
                }

                // Quick Picks (4-row horizontal grid like Android HomeScreen)
                if (settings.showQuickPicks && uiState.quickPicks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "Quick Picks",
                                actionLabel = "Play All",
                                onAction = { controller.playSongList(uiState.quickPicks, 0) },
                            )
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().height(264.dp),
                            ) {
                                items(uiState.quickPicks) { song ->
                                    Box(modifier = Modifier.width(310.dp)) {
                                        VeluneSongRow(
                                            item = song,
                                            isActive = song.id == activeSongId,
                                            isPlaying = playbackState.isPlaying,
                                            isLiked = song.id in uiState.likedIds,
                                            playlists = uiState.persisted.playlists,
                                            onClick = { controller.playSingleSong(song) },
                                            onToggleLike = { controller.toggleLike(song) },
                                            onPlayNext = { controller.playNextInQueue(song) },
                                            onAddToQueue = { controller.addToQueue(song) },
                                            onAddToPlaylist = { pid -> controller.addSongToPlaylist(pid, song) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // For You (Velune Suggestion Engine)
                if (settings.showMadeForYou && uiState.forYouSuggestions.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "Made For You",
                                actionLabel = "Play Mix",
                                onAction = { controller.playSongList(uiState.forYouSuggestions, 0) },
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(uiState.forYouSuggestions) { song ->
                                    VeluneGridCard(
                                        item = song,
                                        isActive = song.id == activeSongId,
                                        isPlaying = playbackState.isPlaying,
                                        cardWidth = cardWidth,
                                        onClick = { controller.playSingleSong(song) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Forgotten Favorites
                if (settings.showForgottenFavorites && uiState.forgottenFavorites.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(title = "Forgotten Favorites")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(uiState.forgottenFavorites) { song ->
                                    VeluneGridCard(
                                        item = song,
                                        isActive = song.id == activeSongId,
                                        isPlaying = playbackState.isPlaying,
                                        cardWidth = cardWidth,
                                        onClick = { controller.playSingleSong(song) },
                                    )
                                }
                            }
                        }
                    }
                }

                // YouTube Music Home Sections
                val sections = uiState.homePage?.sections.orEmpty()
                items(sections) { section ->
                    val filteredItems = section.items.filterExplicit(hideExplicit)
                    if (filteredItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = section.title,
                                actionLabel = if (section.endpoint != null) "See All" else null,
                                onAction = section.endpoint?.let { ep ->
                                    { controller.openBrowse(ep, section.title) }
                                },
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(filteredItems) { item ->
                                    VeluneGridCard(
                                        item = item,
                                        isActive = item.id == activeSongId,
                                        isPlaying = playbackState.isPlaying,
                                        cardWidth = cardWidth,
                                        onClick = { controller.openItem(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

                ExtendedFloatingActionButton(
                    onClick = controller::playRandomSong,
                    icon = { Icon(Icons.Default.Casino, contentDescription = "Random") },
                    text = { Text("Random") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                )
            }
        }
    }
}

@Composable
fun ExploreScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    when {
        uiState.exploreLoading && uiState.explorePage == null -> HomeShimmerSkeleton()
        uiState.exploreError != null && uiState.explorePage == null -> ErrorBox(uiState.exploreError, onRetry = controller::loadExplore)
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                val newReleases = uiState.explorePage?.newReleaseAlbums.orEmpty()
                if (newReleases.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(title = "New Release Albums")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(newReleases) { album ->
                                    VeluneGridCard(item = album, onClick = { controller.openItem(album) })
                                }
                            }
                        }
                    }
                }

                val chartSections = uiState.chartsPage?.sections.orEmpty()
                items(chartSections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = section.title)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(section.items) { item ->
                                VeluneGridCard(
                                    item = item,
                                    isActive = item.id == activeSongId,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { controller.openItem(item) },
                                )
                            }
                        }
                    }
                }

                val moodGroups = uiState.moodAndGenres
                items(moodGroups) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = group.title)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(group.items) { moodItem ->
                                MoodGenreTile(
                                    item = moodItem,
                                    onClick = { controller.openBrowse(moodItem.endpoint, moodItem.title) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodGenreTile(item: MoodAndGenres.Item, onClick: () -> Unit) {
    val stripeColor = Color(item.stripeColor)
    Row(
        modifier = Modifier
            .width(180.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(54.dp)
                .background(stripeColor),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

@Composable
fun SearchScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DesktopSearchFilter.entries) { filter ->
                FilterChip(
                    selected = uiState.searchFilter == filter,
                    onClick = { controller.selectSearchFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
        }

        when {
            uiState.searchLoading -> LoadingBox("Searching YouTube Music...")
            uiState.searchError != null -> ErrorBox(uiState.searchError, onRetry = { controller.performSearch() })
            uiState.searchSummaries.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    uiState.searchSummaries.forEach { summary ->
                        item {
                            Text(
                                text = summary.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(summary.items) { item ->
                            VeluneSongRow(
                                item = item,
                                isActive = item.id == activeSongId,
                                isPlaying = playbackState.isPlaying,
                                isLiked = item is SongItem && item.id in uiState.likedIds,
                                playlists = uiState.persisted.playlists,
                                onClick = { controller.openItem(item) },
                                onToggleLike = if (item is SongItem) ({ controller.toggleLike(item) }) else null,
                                onPlayNext = if (item is SongItem) ({ controller.playNextInQueue(item) }) else null,
                                onAddToQueue = if (item is SongItem) ({ controller.addToQueue(item) }) else null,
                                onAddToPlaylist = if (item is SongItem) ({ pid -> controller.addSongToPlaylist(pid, item) }) else null,
                            )
                        }
                    }
                }
            }
            uiState.searchItems.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.searchItems) { item ->
                        VeluneSongRow(
                            item = item,
                            isActive = item.id == activeSongId,
                            isPlaying = playbackState.isPlaying,
                            isLiked = item is SongItem && item.id in uiState.likedIds,
                            playlists = uiState.persisted.playlists,
                            onClick = { controller.openItem(item) },
                            onToggleLike = if (item is SongItem) ({ controller.toggleLike(item) }) else null,
                            onPlayNext = if (item is SongItem) ({ controller.playNextInQueue(item) }) else null,
                            onAddToQueue = if (item is SongItem) ({ controller.addToQueue(item) }) else null,
                            onAddToPlaylist = if (item is SongItem) ({ pid -> controller.addSongToPlaylist(pid, item) }) else null,
                        )
                    }
                }
            }
            else -> {
                // Show Search History & Live Suggestions when idle
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val suggestions = uiState.searchSuggestions
                    if (suggestions != null && (suggestions.queries.isNotEmpty() || suggestions.recommendedItems.isNotEmpty())) {
                        item {
                            Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(suggestions.queries) { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { controller.performSearch(q) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(q, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        items(suggestions.recommendedItems) { item ->
                            VeluneSongRow(
                                item = item,
                                isActive = item.id == activeSongId,
                                isPlaying = playbackState.isPlaying,
                                onClick = { controller.openItem(item) },
                            )
                        }
                    } else if (uiState.persisted.searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = controller::clearSearchHistory) {
                                    Text("Clear All")
                                }
                            }
                        }
                        items(uiState.persisted.searchHistory) { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { controller.performSearch(q) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(q, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { controller.removeSearchHistory(q) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                                Text("Search YouTube Music songs, albums, artists, or playlists", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    controller.createPlaylist(name)
                    showCreatePlaylistDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = uiState.libraryTab.ordinal) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.libraryTab == tab,
                    onClick = { controller.selectLibraryTab(tab) },
                    text = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                )
            }
        }

        when (uiState.libraryTab) {
            LibraryTab.LIKED -> {
                val songs = uiState.persisted.likedSongs.map { it.toSongItem() }
                if (songs.isEmpty()) {
                    EmptyMessage("No liked songs yet. Tap the heart icon on any track!")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                Button(onClick = { controller.playSongList(songs, 0) }) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play All (${songs.size})")
                                }
                                OutlinedButton(onClick = { controller.playSongList(songs, shuffle = true) }) {
                                    Icon(Icons.Default.Shuffle, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                        itemsIndexed(songs) { index, song ->
                            VeluneSongRow(
                                item = song,
                                index = index + 1,
                                isActive = song.id == activeSongId,
                                isPlaying = playbackState.isPlaying,
                                isLiked = true,
                                playlists = uiState.persisted.playlists,
                                onClick = { controller.playSongList(songs, index) },
                                onToggleLike = { controller.toggleLike(song) },
                                onPlayNext = { controller.playNextInQueue(song) },
                                onAddToQueue = { controller.addToQueue(song) },
                                onAddToPlaylist = { pid -> controller.addSongToPlaylist(pid, song) },
                            )
                        }
                    }
                }
            }
            LibraryTab.PLAYLISTS -> {
                val selectedPlaylist = uiState.persisted.playlists.find { it.id == uiState.selectedLocalPlaylistId }
                if (selectedPlaylist != null) {
                    val songs = selectedPlaylist.songs.map { it.toSongItem() }
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(selectedPlaylist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text("${songs.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { controller.playSongList(songs, 0) },
                                        enabled = songs.isNotEmpty(),
                                    ) { Text("Play All") }
                                    OutlinedButton(onClick = { controller.openLocalPlaylist(null) }) { Text("All Playlists") }
                                }
                            }
                        }
                        itemsIndexed(songs) { index, song ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    VeluneSongRow(
                                        item = song,
                                        index = index + 1,
                                        isActive = song.id == activeSongId,
                                        isPlaying = playbackState.isPlaying,
                                        isLiked = song.id in uiState.likedIds,
                                        onClick = { controller.playSongList(songs, index) },
                                        onToggleLike = { controller.toggleLike(song) },
                                    )
                                }
                                IconButton(onClick = { controller.removeSongFromPlaylist(selectedPlaylist.id, song.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove from playlist")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Button(onClick = { showCreatePlaylistDialog = true }) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("New Playlist")
                            }
                        }
                        items(uiState.persisted.playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable { controller.openLocalPlaylist(playlist.id) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                VeluneThumbnail(
                                    url = playlist.songs.firstOrNull()?.thumbnail,
                                    size = 52.dp,
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${playlist.songs.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { controller.deletePlaylist(playlist.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
                                }
                            }
                        }
                    }
                }
            }
            LibraryTab.HISTORY -> {
                val songs = uiState.persisted.history.map { it.toSongItem() }
                if (songs.isEmpty()) {
                    EmptyMessage("No listening history yet.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(songs) { index, song ->
                            VeluneSongRow(
                                item = song,
                                index = index + 1,
                                isActive = song.id == activeSongId,
                                isPlaying = playbackState.isPlaying,
                                isLiked = song.id in uiState.likedIds,
                                playlists = uiState.persisted.playlists,
                                onClick = { controller.playSongList(songs, index) },
                                onToggleLike = { controller.toggleLike(song) },
                                onPlayNext = { controller.playNextInQueue(song) },
                                onAddToQueue = { controller.addToQueue(song) },
                                onAddToPlaylist = { pid -> controller.addSongToPlaylist(pid, song) },
                            )
                        }
                    }
                }
            }
            LibraryTab.STATS -> {
                val stats = uiState.persisted.playStats.values.sortedByDescending { it.playCount }
                if (stats.isEmpty()) {
                    EmptyMessage("Play some songs to see your listening stats.")
                } else {
                    val totalPlays = stats.sumOf { it.playCount }
                    val totalMinutes = stats.sumOf { it.totalPlayTimeMs } / 60_000L
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                StatSummaryCard("Total Plays", totalPlays.toString(), Modifier.weight(1f))
                                StatSummaryCard("Listening Time", "${totalMinutes} min", Modifier.weight(1f))
                                StatSummaryCard("Unique Tracks", stats.size.toString(), Modifier.weight(1f))
                                StatSummaryCard("Liked Songs", uiState.persisted.likedSongs.size.toString(), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Most Played Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        itemsIndexed(stats.take(25)) { index, entry ->
                            val song = entry.song.toSongItem()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    VeluneSongRow(
                                        item = song,
                                        index = index + 1,
                                        isActive = song.id == activeSongId,
                                        isPlaying = playbackState.isPlaying,
                                        isLiked = song.id in uiState.likedIds,
                                        onClick = { controller.playSingleSong(song) },
                                        onToggleLike = { controller.toggleLike(song) },
                                    )
                                }
                                Text(
                                    text = "${entry.playCount} plays",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: DesktopUiState,
    controller: DesktopAppController,
) {
    val settings = uiState.persisted.settings

    LazyColumn(
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Velune Desktop Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Appearance
        item {
            SettingsCard("Appearance & Theme") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Switch between Dark and Light Material 3 themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { controller.updateSettings { it.copy(themeMode = mode) } },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VeluneThemeStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.themeStyle == style,
                                onClick = { controller.updateSettings { it.copy(themeStyle = style) } },
                                label = { Text(style.label) },
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Pure Black (OLED)",
                    subtitle = "Use true #000000 background in Dark mode",
                    checked = settings.pureBlack,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(pureBlack = v) } },
                )

                SettingsToggleRow(
                    title = "Dynamic Theme from Artwork",
                    subtitle = "Automatically adapt app accent color to the currently playing track",
                    checked = settings.dynamicThemeFromArtwork,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(dynamicThemeFromArtwork = v) } },
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    var showColorWheel by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Accent Color & Color Wheel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { showColorWheel = !showColorWheel }) {
                            Text(if (showColorWheel) "Hide Color Wheel" else "Open Color Wheel")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        VelunePresetColors.forEach { (name, color) ->
                            val argb = color.toArgb().toLong() and 0xFFFFFFFFL
                            val selected = !settings.dynamicThemeFromArtwork && (settings.themeColorArgb and 0xFFFFFFFFL) == argb
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        controller.updateSettings {
                                            it.copy(
                                                dynamicThemeFromArtwork = false,
                                                themeColorArgb = argb,
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    if (showColorWheel) {
                        AccentColorWheelPicker(
                            currentColor = Color(settings.themeColorArgb.toInt()),
                            onColorSelected = { picked ->
                                val argb = picked.toArgb().toLong() and 0xFFFFFFFFL
                                controller.updateSettings {
                                    it.copy(
                                        dynamicThemeFromArtwork = false,
                                        themeColorArgb = argb,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        // Player & Animations
        item {
            SettingsCard("Player & Animations") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Now Playing Background", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerBackgroundStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.playerBackgroundStyle == style,
                                onClick = { controller.updateSettings { it.copy(playerBackgroundStyle = style) } },
                                label = { Text(style.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seek Bar Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SliderStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.sliderStyle == style,
                                onClick = { controller.updateSettings { it.copy(sliderStyle = style) } },
                                label = { Text(style.label) },
                            )
                        }
                    }
                }
            }
        }

        // Desktop Layout Customization
        item {
            SettingsCard("Desktop Layout") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Navigation Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavigationLayoutStyle.entries.forEach { navStyle ->
                            FilterChip(
                                selected = settings.navigationLayoutStyle == navStyle,
                                onClick = { controller.updateSettings { it.copy(navigationLayoutStyle = navStyle) } },
                                label = { Text(navStyle.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Card Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardDensity.entries.forEach { density ->
                            FilterChip(
                                selected = settings.cardDensity == density,
                                onClick = { controller.updateSettings { it.copy(cardDensity = density) } },
                                label = { Text(density.label) },
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Show Home Category Chips",
                    subtitle = "Display Relax, Energize, Workout, and Focus mood chips on Home",
                    checked = settings.showHomeChips,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(showHomeChips = v) } },
                )
                SettingsToggleRow(
                    title = "Show Quick Picks",
                    subtitle = "Display the 4-row Quick Picks grid on Home",
                    checked = settings.showQuickPicks,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(showQuickPicks = v) } },
                )
                SettingsToggleRow(
                    title = "Show Made For You",
                    subtitle = "Display personalized For You mixes on Home",
                    checked = settings.showMadeForYou,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(showMadeForYou = v) } },
                )
                SettingsToggleRow(
                    title = "Show Forgotten Favorites",
                    subtitle = "Display tracks you loved in the past",
                    checked = settings.showForgottenFavorites,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(showForgottenFavorites = v) } },
                )
            }
        }

        // Portable & Customizable MiniPlayer
        item {
            SettingsCard("Portable & Customizable MiniPlayer") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MiniPlayer Bar Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniPlayerStyle.entries.forEach { mpStyle ->
                            FilterChip(
                                selected = settings.miniPlayerStyle == mpStyle,
                                onClick = { controller.updateSettings { it.copy(miniPlayerStyle = mpStyle) } },
                                label = { Text(mpStyle.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Portable MiniPlayer Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PortableMiniMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.portableMiniMode == mode,
                                onClick = { controller.updateSettings { it.copy(portableMiniMode = mode) } },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Portable MiniPlayer Size (Card Mode)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PortableMiniSize.entries.forEach { sz ->
                            FilterChip(
                                selected = settings.portableMiniSize == sz,
                                onClick = { controller.updateSettings { it.copy(portableMiniSize = sz) } },
                                label = { Text(sz.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Screen Corner / Anchor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(IslandAnchor.entries) { anchor ->
                            FilterChip(
                                selected = settings.islandAnchor == anchor,
                                onClick = { controller.updateSettings { it.copy(islandAnchor = anchor) } },
                                label = { Text(anchor.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MiniPlayer Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MiniPlayerThemeMode.entries) { mpTheme ->
                            FilterChip(
                                selected = settings.miniPlayerThemeMode == mpTheme,
                                onClick = { controller.updateSettings { it.copy(miniPlayerThemeMode = mpTheme) } },
                                label = { Text(mpTheme.label) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MiniPlayer Icon Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniPlayerIconStyle.entries.forEach { iconStyle ->
                            FilterChip(
                                selected = settings.miniPlayerIconStyle == iconStyle,
                                onClick = { controller.updateSettings { it.copy(miniPlayerIconStyle = iconStyle) } },
                                label = { Text(iconStyle.label) },
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Show Mini Lyrics in Portable MiniPlayer",
                    subtitle = "Display live synced lyrics inside the Floating Card or Mac Dynamic Island",
                    checked = settings.miniPlayerShowLyrics,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(miniPlayerShowLyrics = v) } },
                )
                SettingsToggleRow(
                    title = "Always On Top (Portable Mini Window)",
                    subtitle = "Keep the popped-out MiniPlayer floating above other apps",
                    checked = settings.miniPlayerAlwaysOnTop,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(miniPlayerAlwaysOnTop = v) } },
                )
                SettingsToggleRow(
                    title = "Show Waveform Equalizer in Portable Mini",
                    subtitle = "Display animated playing bars inside the floating MiniPlayer",
                    checked = settings.miniPlayerShowWaveform,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(miniPlayerShowWaveform = v) } },
                )
                Button(onClick = controller::togglePortableMiniPlayer) {
                    Text(if (uiState.isPortableMiniPlayerOpen) "Close Portable MiniPlayer" else "Launch Portable MiniPlayer")
                }
            }
        }

        // Content & Shortcuts
        item {
            SettingsCard("Content & Keyboard Shortcuts") {
                SettingsToggleRow(
                    title = "Hide Explicit Content",
                    subtitle = "Filter explicit songs and albums from Home, Search, and Suggestions",
                    checked = settings.hideExplicit,
                    onCheckedChange = { v -> controller.updateSettings { it.copy(hideExplicit = v) } },
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Shortcuts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("F11: Toggle Fullscreen  •  Esc: Exit Fullscreen / Close Now Playing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ctrl + + / Ctrl + -: Resize Window", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ctrl + Right / Left: Next / Previous Track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ctrl + Up / Down: Volume Up / Down  •  Ctrl + Space: Play / Pause", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ctrl + L: Now Playing & Lyrics  •  Ctrl + M: Portable MiniPlayer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AccentColorWheelPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    val hueColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red,
        )
    }

    var boxSizePx by remember { mutableStateOf(180f) }

    fun pickAt(offset: Offset) {
        val radius = boxSizePx / 2f
        if (radius <= 1f) return
        val dx = offset.x - radius
        val dy = offset.y - radius
        val dist = hypot(dx, dy).coerceAtMost(radius)
        val sat = (dist / radius).coerceIn(0f, 1f)
        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angleDeg < 0f) angleDeg += 360f
        val rgbInt = java.awt.Color.HSBtoRGB(angleDeg / 360f, sat, 0.96f)
        onColorSelected(Color(rgbInt))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(164.dp)
                .clip(CircleShape)
                .onSizeChanged { boxSizePx = min(it.width, it.height).toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> pickAt(offset) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        pickAt(change.position)
                    }
                },
        ) {
            drawCircle(brush = Brush.sweepGradient(hueColors))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            )
            Text(
                text = "Drag or click wheel\nto pick custom color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AlbumDetailScreen(
    title: String,
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    when {
        uiState.detailLoading -> LoadingBox("Loading album \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.albumPage != null -> {
            val page = uiState.albumPage
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.album.thumbnail,
                        title = page.album.title,
                        subtitle = page.album.artists?.joinToString { it.name }.orEmpty(),
                        onPlayAll = { controller.playSongList(page.songs, 0) },
                        onShuffle = { controller.playSongList(page.songs, shuffle = true) },
                    )
                    Spacer(Modifier.height(14.dp))
                }
                itemsIndexed(page.songs) { index, song ->
                    VeluneSongRow(
                        item = song,
                        index = index + 1,
                        isActive = song.id == activeSongId,
                        isPlaying = playbackState.isPlaying,
                        isLiked = song.id in uiState.likedIds,
                        playlists = uiState.persisted.playlists,
                        onClick = { controller.playSongList(page.songs, index) },
                        onToggleLike = { controller.toggleLike(song) },
                        onPlayNext = { controller.playNextInQueue(song) },
                        onAddToQueue = { controller.addToQueue(song) },
                        onAddToPlaylist = { pid -> controller.addSongToPlaylist(pid, song) },
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    title: String,
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    when {
        uiState.detailLoading -> LoadingBox("Loading playlist \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.playlistPage != null -> {
            val page = uiState.playlistPage
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.playlist.thumbnail,
                        title = page.playlist.title,
                        subtitle = page.playlist.author?.name ?: page.playlist.songCountText.orEmpty(),
                        onPlayAll = { controller.playSongList(page.songs, 0) },
                        onShuffle = { controller.playSongList(page.songs, shuffle = true) },
                    )
                    Spacer(Modifier.height(14.dp))
                }
                itemsIndexed(page.songs) { index, song ->
                    VeluneSongRow(
                        item = song,
                        index = index + 1,
                        isActive = song.id == activeSongId,
                        isPlaying = playbackState.isPlaying,
                        isLiked = song.id in uiState.likedIds,
                        playlists = uiState.persisted.playlists,
                        onClick = { controller.playSongList(page.songs, index) },
                        onToggleLike = { controller.toggleLike(song) },
                        onPlayNext = { controller.playNextInQueue(song) },
                        onAddToQueue = { controller.addToQueue(song) },
                        onAddToPlaylist = { pid -> controller.addSongToPlaylist(pid, song) },
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    title: String,
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    when {
        uiState.detailLoading -> LoadingBox("Loading artist \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.artistPage != null -> {
            val page = uiState.artistPage
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.artist.thumbnail,
                        title = page.artist.title,
                        subtitle = page.description.orEmpty(),
                        onPlayAll = null,
                        onShuffle = null,
                    )
                }
                items(page.sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader(title = section.title)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(section.items) { item ->
                                VeluneGridCard(
                                    item = item,
                                    isActive = item.id == activeSongId,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { controller.openItem(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseDetailScreen(
    title: String,
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val activeSongId = playbackState.currentSong?.id
    when {
        uiState.detailLoading -> LoadingBox("Loading \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.browseResult != null -> {
            val res = uiState.browseResult
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    Text(res.title ?: title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                items(res.items) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        section.title?.let { SectionHeader(title = it) }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(section.items) { item ->
                                VeluneGridCard(
                                    item = item,
                                    isActive = item.id == activeSongId,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { controller.openItem(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun DetailHeader(
    thumbnail: String?,
    title: String,
    subtitle: String,
    onPlayAll: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VeluneThumbnail(url = thumbnail, size = 152.dp, cornerRadius = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onPlayAll != null || onShuffle != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onPlayAll != null) {
                        Button(onClick = onPlayAll) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play All")
                        }
                    }
                    if (onShuffle != null) {
                        OutlinedButton(onClick = onShuffle) {
                            Icon(Icons.Default.Shuffle, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error)
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}
