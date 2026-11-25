package com.nikhil.yt.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import com.nikhil.yt.innertube.models.AlbumItem
import com.nikhil.yt.innertube.models.ArtistItem
import com.nikhil.yt.innertube.models.PlaylistItem
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.YTItem
import java.util.Locale

private val VeluneDarkScheme = darkColorScheme(
    primary = Color(0xFF7C8CFF),
    onPrimary = Color(0xFF0A0F2C),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0B0D14),
    surface = Color(0xFF131622),
    surfaceVariant = Color(0xFF1C2030),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF),
)

fun main() = application {
    val controller = remember { DesktopAppController() }

    DisposableEffect(Unit) {
        onDispose {
            controller.shutdown()
        }
    }

    Window(
        onCloseRequest = {
            controller.shutdown()
            exitApplication()
        },
        title = "Velune Desktop",
        state = rememberWindowState(width = 1220.dp, height = 800.dp),
    ) {
        MaterialTheme(colorScheme = VeluneDarkScheme) {
            VeluneDesktopApp(controller)
        }
    }
}

@Composable
private fun VeluneDesktopApp(controller: DesktopAppController) {
    val uiState by controller.uiState.collectAsState()
    val playbackState by controller.player.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight().width(84.dp),
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "VELUNE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    NavigationRailItem(
                        selected = uiState.currentDestination is DesktopDestination.Home,
                        onClick = { controller.navigateTopLevel(DesktopDestination.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                    )
                    NavigationRailItem(
                        selected = uiState.currentDestination is DesktopDestination.Search,
                        onClick = { controller.navigateTopLevel(DesktopDestination.Search) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                    )
                    NavigationRailItem(
                        selected = uiState.currentDestination is DesktopDestination.Queue,
                        onClick = { controller.navigateTopLevel(DesktopDestination.Queue) },
                        icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue") },
                        label = { Text("Queue") },
                    )
                    NavigationRailItem(
                        selected = uiState.currentDestination is DesktopDestination.Lyrics,
                        onClick = { controller.navigateTopLevel(DesktopDestination.Lyrics) },
                        icon = { Icon(Icons.Default.Lyrics, contentDescription = "Lyrics") },
                        label = { Text("Lyrics") },
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TopSearchHeader(
                        uiState = uiState,
                        onBack = controller::navigateBack,
                        onQueryChange = controller::updateSearchQuery,
                        onSearchSubmit = controller::performSearch,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val dest = uiState.currentDestination) {
                            DesktopDestination.Home -> HomeScreen(uiState, controller)
                            DesktopDestination.Search -> SearchScreen(uiState, controller)
                            DesktopDestination.Queue -> QueueScreen(uiState, playbackState, controller)
                            DesktopDestination.Lyrics -> LyricsScreen(uiState, playbackState, controller)
                            is DesktopDestination.AlbumDetail -> AlbumDetailScreen(dest.title, uiState, controller)
                            is DesktopDestination.PlaylistDetail -> PlaylistDetailScreen(dest.title, uiState, controller)
                            is DesktopDestination.ArtistDetail -> ArtistDetailScreen(dest.title, uiState, controller)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            PlayerBottomBar(
                playbackState = playbackState,
                onTogglePlayPause = controller.player::togglePlayPause,
                onPrevious = controller::playPrevious,
                onNext = controller::playNext,
                onSeek = controller.player::seekTo,
                onVolumeChange = controller.player::setVolume,
                onOpenLyrics = { controller.navigateTopLevel(DesktopDestination.Lyrics) },
                onOpenQueue = { controller.navigateTopLevel(DesktopDestination.Queue) },
            )
        }
    }
}

@Composable
private fun TopSearchHeader(
    uiState: DesktopUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.backStack.size > 1) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search songs, albums, artists, playlists...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onSearchSubmit) {
            Text("Search")
        }
    }
}

@Composable
private fun HomeScreen(uiState: DesktopUiState, controller: DesktopAppController) {
    when {
        uiState.homeLoading -> LoadingBox("Loading YouTube Music Home...")
        uiState.homeError != null -> ErrorBox(uiState.homeError, onRetry = controller::loadHome)
        uiState.homePage != null -> {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.homePage.sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(section.items) { item ->
                                MediaCard(item = item, onClick = { controller.openItem(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(uiState: DesktopUiState, controller: DesktopAppController) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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
            uiState.searchLoading -> LoadingBox("Searching...")
            uiState.searchError != null -> ErrorBox(uiState.searchError, onRetry = controller::performSearch)
            uiState.searchSummaries.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
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
                            MediaListRow(
                                item = item,
                                onClick = { controller.openItem(item) },
                                onAddToQueue = if (item is SongItem) {
                                    { controller.addToQueue(item) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
            uiState.searchItems.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.searchItems) { item ->
                        MediaListRow(
                            item = item,
                            onClick = { controller.openItem(item) },
                            onAddToQueue = if (item is SongItem) {
                                { controller.addToQueue(item) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Type a query above and press Enter or Search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(title: String, uiState: DesktopUiState, controller: DesktopAppController) {
    when {
        uiState.detailLoading -> LoadingBox("Loading album \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.albumPage != null -> {
            val page = uiState.albumPage
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.album.thumbnail,
                        title = page.album.title,
                        subtitle = page.album.artists?.joinToString { it.name }.orEmpty(),
                        onPlayAll = { controller.playSongList(page.songs, 0) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                itemsIndexed(page.songs) { index, song ->
                    MediaListRow(
                        item = song,
                        index = index + 1,
                        onClick = { controller.playSongList(page.songs, index) },
                        onAddToQueue = { controller.addToQueue(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailScreen(title: String, uiState: DesktopUiState, controller: DesktopAppController) {
    when {
        uiState.detailLoading -> LoadingBox("Loading playlist \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.playlistPage != null -> {
            val page = uiState.playlistPage
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.playlist.thumbnail,
                        title = page.playlist.title,
                        subtitle = page.playlist.author?.name ?: page.playlist.songCountText.orEmpty(),
                        onPlayAll = { controller.playSongList(page.songs, 0) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                itemsIndexed(page.songs) { index, song ->
                    MediaListRow(
                        item = song,
                        index = index + 1,
                        onClick = { controller.playSongList(page.songs, index) },
                        onAddToQueue = { controller.addToQueue(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailScreen(title: String, uiState: DesktopUiState, controller: DesktopAppController) {
    when {
        uiState.detailLoading -> LoadingBox("Loading artist \"$title\"...")
        uiState.detailError != null -> ErrorBox(uiState.detailError)
        uiState.artistPage != null -> {
            val page = uiState.artistPage
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    DetailHeader(
                        thumbnail = page.artist.thumbnail,
                        title = page.artist.title,
                        subtitle = page.description.orEmpty(),
                        onPlayAll = null,
                    )
                }
                items(page.sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(section.items) { item ->
                                MediaCard(item = item, onClick = { controller.openItem(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    if (uiState.queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Queue is empty. Play a song to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Up Next (${uiState.queue.size} tracks)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(uiState.queue) { index, song ->
            val isCurrent = index == uiState.currentQueueIndex || song.id == playbackState.currentSong?.id
            MediaListRow(
                item = song,
                index = index + 1,
                highlighted = isCurrent,
                onClick = { controller.playSongList(uiState.queue, index) },
                onAddToQueue = null,
            )
        }
    }
}

@Composable
private fun LyricsScreen(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val song = playbackState.currentSong
    if (song == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Play a song to view lyrics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    when {
        uiState.lyricsLoading -> LoadingBox("Fetching lyrics for \"${song.title}\"...")
        uiState.lyricsError != null -> ErrorBox(uiState.lyricsError, onRetry = controller::loadLyricsForCurrentSong)
        uiState.lyricsText != null -> {
            val cleanedLines = remember(uiState.lyricsText) {
                uiState.lyricsText.lines().map { line ->
                    line.replace(Regex("^\\[\\d+:\\d+(?:\\.\\d+)?]\\s*"), "")
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                items(cleanedLines) { line ->
                    Text(
                        text = line.ifBlank { " " },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
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
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbnailBox(url = thumbnail, size = 140.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            if (onPlayAll != null) {
                Button(onClick = onPlayAll) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play All")
                }
            }
        }
    }
}

@Composable
private fun MediaCard(item: YTItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(164.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThumbnailBox(url = item.thumbnail, size = 144.dp)
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = itemSubtitle(item),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaListRow(
    item: YTItem,
    index: Int? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)?,
) {
    val bgColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (index != null) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp),
            )
        }
        ThumbnailBox(url = item.thumbnail, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = itemSubtitle(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item is SongItem && item.duration != null) {
            Text(
                text = formatTimeMs(item.duration!! * 1000L),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onAddToQueue != null) {
            IconButton(onClick = onAddToQueue) {
                Icon(Icons.Default.Add, contentDescription = "Add to queue")
            }
        }
    }
}

@Composable
private fun ThumbnailBox(url: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBottomBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    var draggingPosition by remember { mutableStateOf<Float?>(null) }
    val song = playbackState.currentSong

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left: current track info
        Row(
            modifier = Modifier.weight(0.3f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThumbnailBox(url = song?.thumbnail, size = 56.dp)
            Column {
                Text(
                    text = song?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playbackState.errorMessage
                        ?: song?.artists?.joinToString { it.name }
                        ?: "Select a song to play",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (playbackState.errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Center: transport controls + seek bar
        Column(
            modifier = Modifier.weight(0.45f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(onClick = onPrevious, enabled = song != null) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = song != null && !playbackState.isBuffering, onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playbackState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = song != null) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val totalMs = playbackState.durationMs.coerceAtLeast(1L)
                val displayMs = draggingPosition?.toLong() ?: playbackState.positionMs
                Text(
                    text = formatTimeMs(displayMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = (draggingPosition ?: playbackState.positionMs.toFloat()).coerceIn(0f, totalMs.toFloat()),
                    onValueChange = { draggingPosition = it },
                    onValueChangeFinished = {
                        draggingPosition?.let { onSeek(it.toLong()) }
                        draggingPosition = null
                    },
                    valueRange = 0f..totalMs.toFloat(),
                    enabled = song != null && playbackState.durationMs > 0L,
                    modifier = Modifier.weight(1f).height(24.dp),
                )
                Text(
                    text = formatTimeMs(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Right: lyrics, queue, volume
        Row(
            modifier = Modifier.weight(0.25f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenLyrics) {
                Icon(Icons.Default.Lyrics, contentDescription = "Lyrics")
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
            }
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = playbackState.volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.width(100.dp).padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun LoadingBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error)
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

private fun itemSubtitle(item: YTItem): String = when (item) {
    is SongItem -> item.artists.joinToString { it.name }.ifBlank { "Song" }
    is AlbumItem -> "Album • " + (item.artists?.joinToString { it.name }.orEmpty())
    is PlaylistItem -> "Playlist • " + (item.author?.name ?: item.songCountText.orEmpty())
    is ArtistItem -> "Artist"
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
