package com.nikhil.yt.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess

fun main() = application {
    val controller = remember { DesktopAppController() }

    DisposableEffect(Unit) {
        onDispose {
            controller.shutdown()
        }
    }

    val uiState by controller.uiState.collectAsState()
    val playbackState by controller.player.state.collectAsState()
    val settings = uiState.persisted.settings

    val windowState = rememberWindowState(
        width = settings.windowWidthDp.dp,
        height = settings.windowHeightDp.dp,
        placement = if (uiState.isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
    )

    LaunchedEffect(uiState.isFullscreen) {
        windowState.placement = if (uiState.isFullscreen) {
            WindowPlacement.Fullscreen
        } else {
            WindowPlacement.Floating
        }
    }

    LaunchedEffect(settings.windowWidthDp, settings.windowHeightDp) {
        if (!uiState.isFullscreen) {
            windowState.size = DpSize(settings.windowWidthDp.dp, settings.windowHeightDp.dp)
        }
    }

    Window(
        onCloseRequest = {
            controller.shutdown()
            exitApplication()
            exitProcess(0)
        },
        title = "Velune",
        state = windowState,
        onPreviewKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@Window false
            when {
                event.key == Key.F11 -> {
                    controller.toggleFullscreen()
                    true
                }
                event.key == Key.Escape && uiState.isFullscreen -> {
                    controller.toggleFullscreen()
                    true
                }
                event.key == Key.Escape && uiState.isPlayerExpanded -> {
                    controller.setPlayerExpanded(false)
                    true
                }
                event.isCtrlPressed && event.key == Key.DirectionRight -> {
                    controller.playNext()
                    true
                }
                event.isCtrlPressed && event.key == Key.DirectionLeft -> {
                    controller.playPrevious()
                    true
                }
                event.isCtrlPressed && event.key == Key.DirectionUp -> {
                    controller.setVolume(playbackState.volume + 0.05f)
                    true
                }
                event.isCtrlPressed && event.key == Key.DirectionDown -> {
                    controller.setVolume(playbackState.volume - 0.05f)
                    true
                }
                event.isCtrlPressed && event.key == Key.Spacebar -> {
                    controller.player.togglePlayPause()
                    true
                }
                event.isCtrlPressed && event.key == Key.L -> {
                    if (playbackState.currentSong != null) {
                        controller.setPlayerExpanded(!uiState.isPlayerExpanded)
                    }
                    true
                }
                event.isCtrlPressed && event.key == Key.M -> {
                    controller.togglePortableMiniPlayer()
                    true
                }
                event.isCtrlPressed && (event.key == Key.Equals || event.key == Key.Plus || event.key == Key.NumPadAdd) -> {
                    if (uiState.isFullscreen) controller.toggleFullscreen()
                    controller.resizeWindow(100, 70)
                    true
                }
                event.isCtrlPressed && (event.key == Key.Minus || event.key == Key.NumPadSubtract) -> {
                    if (uiState.isFullscreen) controller.toggleFullscreen()
                    controller.resizeWindow(-100, -70)
                    true
                }
                else -> false
            }
        },
    ) {
        val activeThemeColor = if (settings.dynamicThemeFromArtwork && playbackState.currentSong != null) {
            uiState.artworkPalette.primary
        } else {
            Color(settings.themeColorArgb.toInt())
        }

        VeluneDesktopTheme(
            darkTheme = settings.themeMode == ThemeMode.DARK,
            pureBlack = settings.pureBlack,
            themeColor = activeThemeColor,
            themeStyle = settings.themeStyle,
        ) {
            VeluneDesktopRoot(controller)
        }
    }

    if (uiState.isPortableMiniPlayerOpen) {
        PortableMiniPlayerWindow(
            uiState = uiState,
            playbackState = playbackState,
            controller = controller,
        )
    }
}

@Composable
private fun VeluneDesktopRoot(controller: DesktopAppController) {
    val uiState by controller.uiState.collectAsState()
    val playbackState by controller.player.state.collectAsState()
    val navStyle = uiState.persisted.settings.navigationLayoutStyle

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (navStyle) {
                        NavigationLayoutStyle.RAIL -> CompactNavRail(uiState, controller)
                        NavigationLayoutStyle.SIDEBAR -> ExpandedNavSidebar(uiState, controller)
                        NavigationLayoutStyle.TOP_TABS -> Unit
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TopSearchBar(
                            uiState = uiState,
                            showTopNavTabs = navStyle == NavigationLayoutStyle.TOP_TABS,
                            controller = controller,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AnimatedContent(
                                targetState = uiState.currentDestination,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                            ) { dest ->
                                when (dest) {
                                    DesktopDestination.Home -> HomeScreen(uiState, playbackState, controller)
                                    DesktopDestination.Explore -> ExploreScreen(uiState, playbackState, controller)
                                    DesktopDestination.Search -> SearchScreen(uiState, playbackState, controller)
                                    DesktopDestination.Library -> LibraryScreen(uiState, playbackState, controller)
                                    DesktopDestination.Settings -> SettingsScreen(uiState, controller)
                                    is DesktopDestination.AlbumDetail -> AlbumDetailScreen(dest.title, uiState, playbackState, controller)
                                    is DesktopDestination.PlaylistDetail -> PlaylistDetailScreen(dest.title, uiState, playbackState, controller)
                                    is DesktopDestination.ArtistDetail -> ArtistDetailScreen(dest.title, uiState, playbackState, controller)
                                    is DesktopDestination.BrowseDetail -> BrowseDetailScreen(dest.title, uiState, playbackState, controller)
                                }
                            }

                            // Floating live search suggestions dropdown when typing in top bar outside Search screen
                            val suggestions = uiState.searchSuggestions
                            if (uiState.searchFocused &&
                                uiState.currentDestination !is DesktopDestination.Search &&
                                uiState.searchQuery.isNotBlank() &&
                                suggestions != null &&
                                (suggestions.queries.isNotEmpty() || suggestions.recommendedItems.isNotEmpty())
                            ) {
                                Surface(
                                    tonalElevation = 8.dp,
                                    shadowElevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .fillMaxWidth(0.75f),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        suggestions.queries.take(5).forEach { q ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { controller.performSearch(q) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(q, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        suggestions.recommendedItems.take(4).forEach { item ->
                                            VeluneSongRow(
                                                item = item,
                                                isActive = item.id == playbackState.currentSong?.id,
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

                if (uiState.persisted.settings.miniPlayerStyle == MiniPlayerStyle.CLASSIC) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
                MiniPlayerBar(uiState = uiState, playbackState = playbackState, controller = controller)
            }

            // Full-screen animated Now Playing sheet
            AnimatedVisibility(
                visible = uiState.isPlayerExpanded && playbackState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                NowPlayingOverlay(
                    uiState = uiState,
                    playbackState = playbackState,
                    controller = controller,
                )
            }
        }
    }
}

@Composable
private fun CompactNavRail(uiState: DesktopUiState, controller: DesktopAppController) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxHeight().width(88.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "VELUNE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 18.dp),
        )
        NavigationRailItem(
            selected = uiState.currentDestination is DesktopDestination.Home,
            onClick = { controller.navigateTopLevel(DesktopDestination.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
        )
        NavigationRailItem(
            selected = uiState.currentDestination is DesktopDestination.Explore,
            onClick = { controller.navigateTopLevel(DesktopDestination.Explore) },
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("Explore") },
        )
        NavigationRailItem(
            selected = uiState.currentDestination is DesktopDestination.Search,
            onClick = { controller.navigateTopLevel(DesktopDestination.Search) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
        )
        NavigationRailItem(
            selected = uiState.currentDestination is DesktopDestination.Library,
            onClick = { controller.navigateTopLevel(DesktopDestination.Library) },
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") },
        )
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = uiState.currentDestination is DesktopDestination.Settings,
            onClick = { controller.navigateTopLevel(DesktopDestination.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ExpandedNavSidebar(uiState: DesktopUiState, controller: DesktopAppController) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(210.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "VELUNE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
        NavigationDrawerItem(
            selected = uiState.currentDestination is DesktopDestination.Home,
            onClick = { controller.navigateTopLevel(DesktopDestination.Home) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
        )
        NavigationDrawerItem(
            selected = uiState.currentDestination is DesktopDestination.Explore,
            onClick = { controller.navigateTopLevel(DesktopDestination.Explore) },
            icon = { Icon(Icons.Default.Explore, null) },
            label = { Text("Explore") },
        )
        NavigationDrawerItem(
            selected = uiState.currentDestination is DesktopDestination.Search,
            onClick = { controller.navigateTopLevel(DesktopDestination.Search) },
            icon = { Icon(Icons.Default.Search, null) },
            label = { Text("Search") },
        )
        NavigationDrawerItem(
            selected = uiState.currentDestination is DesktopDestination.Library,
            onClick = { controller.navigateTopLevel(DesktopDestination.Library) },
            icon = { Icon(Icons.Default.LibraryMusic, null) },
            label = { Text("Library") },
        )
        Spacer(Modifier.weight(1f))
        NavigationDrawerItem(
            selected = uiState.currentDestination is DesktopDestination.Settings,
            onClick = { controller.navigateTopLevel(DesktopDestination.Settings) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") },
        )
    }
}

@Composable
private fun TopSearchBar(
    uiState: DesktopUiState,
    showTopNavTabs: Boolean,
    controller: DesktopAppController,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.backStack.size > 1 || uiState.selectedLocalPlaylistId != null) {
                IconButton(onClick = controller::navigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = controller::updateSearchQuery,
                placeholder = { Text("Search songs, albums, artists, playlists...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { controller.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { controller.performSearch() }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { controller.setSearchFocused(it.isFocused) },
            )
            Button(onClick = { controller.performSearch() }) {
                Text("Search")
            }
        }

        if (showTopNavTabs) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tabs = listOf(
                    "Home" to DesktopDestination.Home,
                    "Explore" to DesktopDestination.Explore,
                    "Search" to DesktopDestination.Search,
                    "Library" to DesktopDestination.Library,
                    "Settings" to DesktopDestination.Settings,
                )
                tabs.forEach { (label, dest) ->
                    FilterChip(
                        selected = uiState.currentDestination == dest,
                        onClick = { controller.navigateTopLevel(dest) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
