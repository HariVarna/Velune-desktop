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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
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

    Window(
        onCloseRequest = {
            controller.shutdown()
            exitApplication()
            exitProcess(0)
        },
        title = "Velune",
        state = rememberWindowState(width = 1280.dp, height = 840.dp),
    ) {
        val uiState by controller.uiState.collectAsState()
        val settings = uiState.persisted.settings
        val activeThemeColor = if (settings.dynamicThemeFromArtwork && controller.player.state.collectAsState().value.currentSong != null) {
            uiState.artworkPalette.primary
        } else {
            Color(settings.themeColorArgb.toInt())
        }

        VeluneDesktopTheme(
            darkTheme = settings.themeMode == ThemeMode.DARK,
            pureBlack = settings.pureBlack,
            themeColor = activeThemeColor,
        ) {
            VeluneDesktopRoot(controller)
        }
    }
}

@Composable
private fun VeluneDesktopRoot(controller: DesktopAppController) {
    val uiState by controller.uiState.collectAsState()
    val playbackState by controller.player.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
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

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TopSearchBar(
                            uiState = uiState,
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
                                                    .clickable { controller.performSearch(q) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(q, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        suggestions.recommendedItems.take(3).forEach { item ->
                                            VeluneSongRow(
                                                item = item,
                                                onClick = { controller.openItem(item) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
private fun TopSearchBar(
    uiState: DesktopUiState,
    controller: DesktopAppController,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
}
