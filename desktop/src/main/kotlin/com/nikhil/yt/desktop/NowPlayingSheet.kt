package com.nikhil.yt.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nikhil.yt.innertube.toHighResThumbnail

private enum class PlayerSideTab(val label: String) {
    LYRICS("Lyrics"),
    QUEUE("Up Next"),
}

@Composable
fun MiniPlayerBar(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val song = playbackState.currentSong
    val settings = uiState.persisted.settings
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val currentFraction = dragFraction ?: (playbackState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val isLiked = song != null && song.id in uiState.likedIds

    val containerShape = when (settings.miniPlayerStyle) {
        MiniPlayerStyle.FLOATING_PILL -> RoundedCornerShape(22.dp)
        MiniPlayerStyle.GLASS -> RoundedCornerShape(18.dp)
        MiniPlayerStyle.CLASSIC -> RoundedCornerShape(0.dp)
    }
    val outerPadding = when (settings.miniPlayerStyle) {
        MiniPlayerStyle.FLOATING_PILL -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        MiniPlayerStyle.GLASS -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        MiniPlayerStyle.CLASSIC -> PaddingValues(0.dp)
    }
    val containerColor = when (settings.miniPlayerStyle) {
        MiniPlayerStyle.FLOATING_PILL -> MaterialTheme.colorScheme.surfaceContainerHigh
        MiniPlayerStyle.GLASS -> uiState.artworkPalette.primary.copy(alpha = 0.16f)
        MiniPlayerStyle.CLASSIC -> MaterialTheme.colorScheme.surfaceContainer
    }
    val borderStroke = when (settings.miniPlayerStyle) {
        MiniPlayerStyle.FLOATING_PILL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        MiniPlayerStyle.GLASS -> BorderStroke(1.dp, uiState.artworkPalette.primary.copy(alpha = 0.35f))
        MiniPlayerStyle.CLASSIC -> null
    }

    Box(modifier = Modifier.fillMaxWidth().padding(outerPadding)) {
        Surface(
            tonalElevation = if (settings.miniPlayerStyle == MiniPlayerStyle.CLASSIC) 6.dp else 10.dp,
            shadowElevation = if (settings.miniPlayerStyle == MiniPlayerStyle.CLASSIC) 0.dp else 12.dp,
            shape = containerShape,
            border = borderStroke,
            color = containerColor,
            modifier = Modifier.fillMaxWidth().height(92.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            // Track info (click to expand full-screen Now Playing)
            Row(
                modifier = Modifier
                    .weight(0.30f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = song != null) { controller.setPlayerExpanded(true) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VeluneThumbnail(
                    url = song?.thumbnail,
                    size = 56.dp,
                    cornerRadius = 10.dp,
                    isActive = song != null && playbackState.isPlaying,
                    isPlaying = playbackState.isPlaying,
                )
                Column(modifier = Modifier.weight(1f)) {
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
                if (song != null) {
                    IconButton(onClick = { controller.toggleLike(song) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Center controls + Squiggly/Default/Slim SeekBar
            Column(
                modifier = Modifier.weight(0.45f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(onClick = controller::toggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (settings.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = controller::playPrevious, enabled = song != null) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = song != null && !playbackState.isBuffering) {
                                controller.player.togglePlayPause()
                            },
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
                    IconButton(onClick = controller::playNext, enabled = song != null) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                    IconButton(onClick = controller::cycleRepeatMode) {
                        Icon(
                            imageVector = if (settings.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (settings.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val displayMs = ((dragFraction ?: currentFraction) * totalMs).toLong()
                    Text(
                        text = formatTimeMs(displayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VeluneSeekBar(
                        progress = currentFraction,
                        isPlaying = playbackState.isPlaying,
                        style = settings.sliderStyle,
                        enabled = song != null && playbackState.durationMs > 0L,
                        onSeekStart = { dragFraction = it },
                        onSeekDrag = { dragFraction = it },
                        onSeekEnd = {
                            dragFraction?.let { frac ->
                                controller.player.seekTo((frac * totalMs).toLong())
                            }
                            dragFraction = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTimeMs(playbackState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Right: Expand Now Playing, Portable MiniPlayer, Volume
            Row(
                modifier = Modifier.weight(0.25f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = controller::togglePortableMiniPlayer) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Portable MiniPlayer",
                        tint = if (uiState.isPortableMiniPlayerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { controller.setPlayerExpanded(true) }, enabled = song != null) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Now Playing & Lyrics")
                }
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = playbackState.volume,
                    onValueChange = controller::setVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.width(96.dp).padding(start = 6.dp),
                )
            }
        }
    }
    }
}

@Composable
fun PortableMiniPlayerWindow(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val settings = uiState.persisted.settings
    val song = playbackState.currentSong
    val isLiked = song != null && song.id in uiState.likedIds
    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val progress = (playbackState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val activeLyric = remember(uiState.lyricsResult, playbackState.positionMs) {
        currentLyricText(uiState.lyricsResult, playbackState.positionMs)
    }

    var islandCollapsed by remember { mutableStateOf(false) }

    val windowWidthDp = when (settings.portableMiniMode) {
        PortableMiniMode.DYNAMIC_ISLAND -> 510.dp
        PortableMiniMode.CARD -> settings.portableMiniSize.widthDp.dp
    }
    val windowHeightDp = when (settings.portableMiniMode) {
        PortableMiniMode.DYNAMIC_ISLAND -> if (settings.miniPlayerShowLyrics) 112.dp else 78.dp
        PortableMiniMode.CARD -> (settings.portableMiniSize.heightDp + if (settings.miniPlayerShowLyrics) 26 else 0).dp
    }

    val animatedIslandWidth by animateDpAsState(
        targetValue = if (islandCollapsed) 188.dp else 510.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
    )
    val animatedIslandHeight by animateDpAsState(
        targetValue = when {
            islandCollapsed -> 42.dp
            settings.miniPlayerShowLyrics -> 112.dp
            else -> 78.dp
        },
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
    )

    val anchorPos = remember(settings.portableMiniMode, settings.islandAnchor, windowWidthDp, windowHeightDp) {
        computeAnchorPosition(
            anchor = settings.islandAnchor,
            widthDp = windowWidthDp.value.toInt(),
            heightDp = windowHeightDp.value.toInt(),
        )
    }

    val windowState = rememberWindowState(
        size = DpSize(windowWidthDp, windowHeightDp),
        position = anchorPos,
    )

    LaunchedEffect(windowWidthDp, windowHeightDp) {
        windowState.size = DpSize(windowWidthDp, windowHeightDp)
    }

    LaunchedEffect(settings.islandAnchor, settings.portableMiniMode) {
        windowState.position = computeAnchorPosition(
            anchor = settings.islandAnchor,
            widthDp = windowWidthDp.value.toInt(),
            heightDp = windowHeightDp.value.toInt(),
        )
    }

    val miniIsDark = when (settings.miniPlayerThemeMode) {
        MiniPlayerThemeMode.MATCH_APP -> settings.themeMode == ThemeMode.DARK
        MiniPlayerThemeMode.LIGHT -> false
        MiniPlayerThemeMode.DARK, MiniPlayerThemeMode.AMOLED, MiniPlayerThemeMode.RETRO, MiniPlayerThemeMode.PIXEL -> true
    }
    val miniThemeStyle = when (settings.miniPlayerThemeMode) {
        MiniPlayerThemeMode.RETRO -> VeluneThemeStyle.RETRO
        MiniPlayerThemeMode.PIXEL -> VeluneThemeStyle.PIXEL
        MiniPlayerThemeMode.MATCH_APP -> settings.themeStyle
        else -> VeluneThemeStyle.MODERN
    }
    val activeThemeColor = if (settings.dynamicThemeFromArtwork && song != null) {
        uiState.artworkPalette.primary
    } else {
        Color(settings.themeColorArgb.toInt())
    }

    Window(
        onCloseRequest = { controller.setPortableMiniPlayerOpen(false) },
        title = "Velune Mini",
        undecorated = true,
        transparent = true,
        resizable = true,
        alwaysOnTop = settings.miniPlayerAlwaysOnTop || uiState.isFullscreen,
        state = windowState,
    ) {
        LaunchedEffect(settings.miniPlayerAlwaysOnTop, uiState.isFullscreen) {
            val shouldStayOnTop = settings.miniPlayerAlwaysOnTop || uiState.isFullscreen
            window.isAlwaysOnTop = shouldStayOnTop
            if (shouldStayOnTop) {
                window.toFront()
            }
        }

        VeluneDesktopTheme(
            darkTheme = miniIsDark,
            pureBlack = settings.miniPlayerThemeMode == MiniPlayerThemeMode.AMOLED || settings.pureBlack,
            themeColor = activeThemeColor,
            themeStyle = miniThemeStyle,
        ) {
            val contentColor = if (miniIsDark) Color.White else Color(0xFF111827)
            val secondaryTextColor = if (miniIsDark) Color.White.copy(alpha = 0.72f) else Color(0xFF4B5563)
            val surfaceBg = when (settings.miniPlayerThemeMode) {
                MiniPlayerThemeMode.AMOLED -> Color.Black
                MiniPlayerThemeMode.RETRO -> Color(0xFF1B1526)
                MiniPlayerThemeMode.PIXEL -> Color(0xFF111711)
                MiniPlayerThemeMode.LIGHT -> Color(0xFFF8FAFC)
                MiniPlayerThemeMode.DARK -> Color(0xFF121520)
                MiniPlayerThemeMode.MATCH_APP -> if (miniIsDark) Color(0xFF121520) else Color(0xFFF8FAFC)
            }

            WindowDraggableArea {
                if (settings.portableMiniMode == PortableMiniMode.DYNAMIC_ISLAND) {
                    DynamicIslandMiniContent(
                        song = song,
                        playbackState = playbackState,
                        settings = settings,
                        palette = uiState.artworkPalette,
                        collapsed = islandCollapsed,
                        islandWidth = animatedIslandWidth,
                        islandHeight = animatedIslandHeight,
                        contentColor = if (settings.miniPlayerThemeMode == MiniPlayerThemeMode.LIGHT) contentColor else Color.White,
                        secondaryColor = if (settings.miniPlayerThemeMode == MiniPlayerThemeMode.LIGHT) secondaryTextColor else Color.White.copy(alpha = 0.72f),
                        bgColor = if (settings.miniPlayerThemeMode == MiniPlayerThemeMode.LIGHT) Color(0xFFF3F4F6) else Color(0xFF07080D),
                        activeLyric = activeLyric,
                        progress = progress,
                        onToggleCollapse = { islandCollapsed = !islandCollapsed },
                        onToggleMode = {
                            islandCollapsed = false
                            controller.updateSettings { it.copy(portableMiniMode = PortableMiniMode.CARD) }
                        },
                        onToggleLyrics = {
                            controller.updateSettings { it.copy(miniPlayerShowLyrics = !it.miniPlayerShowLyrics) }
                        },
                        onTogglePlayPause = controller.player::togglePlayPause,
                        onPrevious = controller::playPrevious,
                        onNext = controller::playNext,
                        onClose = { controller.setPortableMiniPlayerOpen(false) },
                    )
                } else {
                    CardMiniContent(
                        song = song,
                        playbackState = playbackState,
                        settings = settings,
                        palette = uiState.artworkPalette,
                        surfaceBg = surfaceBg,
                        contentColor = contentColor,
                        secondaryColor = secondaryTextColor,
                        isLiked = isLiked,
                        activeLyric = activeLyric,
                        progress = progress,
                        totalMs = totalMs,
                        onToggleMode = {
                            controller.updateSettings { it.copy(portableMiniMode = PortableMiniMode.DYNAMIC_ISLAND) }
                        },
                        onToggleLyrics = {
                            controller.updateSettings { it.copy(miniPlayerShowLyrics = !it.miniPlayerShowLyrics) }
                        },
                        onCycleSize = {
                            val nextSize = when (settings.portableMiniSize) {
                                PortableMiniSize.COMPACT -> PortableMiniSize.MEDIUM
                                PortableMiniSize.MEDIUM -> PortableMiniSize.LARGE
                                PortableMiniSize.LARGE -> PortableMiniSize.COMPACT
                            }
                            controller.updateSettings { it.copy(portableMiniSize = nextSize) }
                        },
                        onToggleLike = { song?.let(controller::toggleLike) },
                        onTogglePlayPause = controller.player::togglePlayPause,
                        onPrevious = controller::playPrevious,
                        onNext = controller::playNext,
                        onSeek = controller.player::seekTo,
                        onClose = { controller.setPortableMiniPlayerOpen(false) },
                    )
                }
            }
        }
    }
}

private fun computeAnchorPosition(anchor: IslandAnchor, widthDp: Int, heightDp: Int): WindowPosition {
    val bounds = runCatching {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }.getOrNull() ?: return WindowPosition(Alignment.TopCenter)

    val margin = 14
    val x = when (anchor) {
        IslandAnchor.TOP_LEFT, IslandAnchor.BOTTOM_LEFT -> bounds.x + margin
        IslandAnchor.TOP_CENTER, IslandAnchor.BOTTOM_CENTER -> bounds.x + (bounds.width - widthDp) / 2
        IslandAnchor.TOP_RIGHT, IslandAnchor.BOTTOM_RIGHT -> bounds.x + bounds.width - widthDp - margin
    }
    val y = when (anchor) {
        IslandAnchor.TOP_LEFT, IslandAnchor.TOP_CENTER, IslandAnchor.TOP_RIGHT -> bounds.y + margin
        IslandAnchor.BOTTOM_LEFT, IslandAnchor.BOTTOM_CENTER, IslandAnchor.BOTTOM_RIGHT -> bounds.y + bounds.height - heightDp - margin
    }
    return WindowPosition(x.dp, y.dp)
}

@Composable
private fun DynamicIslandMiniContent(
    song: com.nikhil.yt.innertube.models.SongItem?,
    playbackState: PlaybackState,
    settings: DesktopSettings,
    palette: ArtworkPalette,
    collapsed: Boolean,
    islandWidth: androidx.compose.ui.unit.Dp,
    islandHeight: androidx.compose.ui.unit.Dp,
    contentColor: Color,
    secondaryColor: Color,
    bgColor: Color,
    activeLyric: String?,
    progress: Float,
    onToggleCollapse: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLyrics: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val shape = when (settings.miniPlayerThemeMode) {
        MiniPlayerThemeMode.PIXEL -> RoundedCornerShape(2.dp)
        MiniPlayerThemeMode.RETRO -> RoundedCornerShape(10.dp)
        else -> RoundedCornerShape(36.dp)
    }
    val prevIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.SkipPrevious else Icons.Default.SkipPrevious
    val nextIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.SkipNext else Icons.Default.SkipNext
    val lyricsIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.Lyrics else Icons.Default.Lyrics

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = shape,
            color = bgColor,
            shadowElevation = 0.dp,
            modifier = Modifier.width(islandWidth).height(islandHeight),
        ) {
        if (collapsed) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onToggleCollapse)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VeluneThumbnail(
                    url = song?.thumbnail,
                    size = 24.dp,
                    cornerRadius = 12.dp,
                    isCircle = true,
                )
                Text(
                    text = song?.title ?: "Velune",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (playbackState.isPlaying) {
                    VelunePlayingIndicator(isPlaying = true, color = palette.primary, modifier = Modifier.height(14.dp))
                }
                Icon(Icons.Default.ExpandMore, contentDescription = "Expand", tint = contentColor, modifier = Modifier.size(16.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.primary.copy(alpha = 0.20f), Color.Transparent, palette.secondary.copy(alpha = 0.12f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VeluneThumbnail(
                        url = song?.thumbnail,
                        size = 44.dp,
                        cornerRadius = 22.dp,
                        isCircle = true,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: "Velune Island",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song?.artists?.joinToString { it.name } ?: "Ready to play",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (settings.miniPlayerShowWaveform && playbackState.isPlaying) {
                        VelunePlayingIndicator(
                            isPlaying = true,
                            color = palette.primary,
                            modifier = Modifier.height(18.dp),
                        )
                    }
                    IconButton(onClick = onPrevious, enabled = song != null, modifier = Modifier.size(28.dp)) {
                        Icon(prevIcon, contentDescription = "Previous", tint = contentColor, modifier = Modifier.size(17.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.MINIMAL) Color.Transparent else contentColor)
                            .clickable(enabled = song != null && !playbackState.isBuffering, onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconTint = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.MINIMAL) contentColor else bgColor
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onNext, enabled = song != null, modifier = Modifier.size(28.dp)) {
                        Icon(nextIcon, contentDescription = "Next", tint = contentColor, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onToggleLyrics, modifier = Modifier.size(26.dp)) {
                        Icon(
                            lyricsIcon,
                            contentDescription = "Mini Lyrics",
                            tint = if (settings.miniPlayerShowLyrics) palette.primary else secondaryColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    IconButton(onClick = onToggleCollapse, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.ExpandLess, contentDescription = "Collapse Island", tint = contentColor, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleMode, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Default.PictureInPictureAlt,
                            contentDescription = "Switch to Card Mode",
                            tint = contentColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor, modifier = Modifier.size(15.dp))
                    }
                }

                if (settings.miniPlayerShowLyrics) {
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = activeLyric ?: "♪",
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    ) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = palette.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(contentColor.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(palette.primary),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun CardMiniContent(
    song: com.nikhil.yt.innertube.models.SongItem?,
    playbackState: PlaybackState,
    settings: DesktopSettings,
    palette: ArtworkPalette,
    surfaceBg: Color,
    contentColor: Color,
    secondaryColor: Color,
    isLiked: Boolean,
    activeLyric: String?,
    progress: Float,
    totalMs: Long,
    onToggleMode: () -> Unit,
    onToggleLyrics: () -> Unit,
    onCycleSize: () -> Unit,
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val cardShape = when (settings.miniPlayerThemeMode) {
        MiniPlayerThemeMode.PIXEL -> RoundedCornerShape(0.dp)
        MiniPlayerThemeMode.RETRO -> RoundedCornerShape(6.dp)
        else -> RoundedCornerShape(20.dp)
    }
    val prevIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.SkipPrevious else Icons.Default.SkipPrevious
    val nextIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.SkipNext else Icons.Default.SkipNext
    val lyricsIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.Lyrics else Icons.Default.Lyrics
    val unlikedIcon = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.OUTLINED) Icons.Outlined.FavoriteBorder else Icons.Default.FavoriteBorder

    Surface(
        shape = cardShape,
        color = surfaceBg,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(palette.primary.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VeluneThumbnail(
                        url = song?.thumbnail,
                        size = 54.dp,
                        cornerRadius = if (settings.miniPlayerThemeMode == MiniPlayerThemeMode.PIXEL) 0.dp else 12.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: "Velune Mini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song?.artists?.joinToString { it.name } ?: "No track playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (settings.miniPlayerShowWaveform && playbackState.isPlaying) {
                        VelunePlayingIndicator(
                            isPlaying = true,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(20.dp),
                        )
                    }
                    IconButton(onClick = onCycleSize, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Resize MiniPlayer", tint = contentColor, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onToggleLyrics, modifier = Modifier.size(28.dp)) {
                        Icon(
                            lyricsIcon,
                            contentDescription = "Toggle Mini Lyrics",
                            tint = if (settings.miniPlayerShowLyrics) MaterialTheme.colorScheme.primary else contentColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    IconButton(onClick = onToggleMode, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.PictureInPictureAlt,
                            contentDescription = "Mac Dynamic Island Mode",
                            tint = contentColor,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close MiniPlayer", tint = contentColor, modifier = Modifier.size(16.dp))
                    }
                }

                if (settings.miniPlayerShowLyrics) {
                    AnimatedContent(
                        targetState = activeLyric ?: "♪ No synced lyrics",
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    ) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        )
                    }
                }

                VeluneSeekBar(
                    progress = progress,
                    isPlaying = playbackState.isPlaying,
                    style = settings.sliderStyle,
                    enabled = song != null && playbackState.durationMs > 0L,
                    onSeekStart = { frac -> onSeek((frac * totalMs).toLong()) },
                    onSeekDrag = { frac -> onSeek((frac * totalMs).toLong()) },
                    onSeekEnd = {},
                    modifier = Modifier.height(18.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${formatTimeMs(playbackState.positionMs)} / ${formatTimeMs(playbackState.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (song != null) {
                            IconButton(onClick = onToggleLike, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else unlikedIcon,
                                    contentDescription = "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else contentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        IconButton(onClick = onPrevious, enabled = song != null, modifier = Modifier.size(30.dp)) {
                            Icon(prevIcon, contentDescription = "Previous", tint = contentColor, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.MINIMAL) Color.Transparent else MaterialTheme.colorScheme.primary)
                                .clickable(enabled = song != null && !playbackState.isBuffering, onClick = onTogglePlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            val playTint = if (settings.miniPlayerIconStyle == MiniPlayerIconStyle.MINIMAL) contentColor else MaterialTheme.colorScheme.onPrimary
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = playTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = onNext, enabled = song != null, modifier = Modifier.size(30.dp)) {
                            Icon(nextIcon, contentDescription = "Next", tint = contentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun currentLyricText(result: LyricsResult?, positionMs: Long): String? {
    if (result == null || result.lines.isEmpty()) return null
    if (!result.isSynced) return result.lines.firstOrNull { it.text.isNotBlank() }?.text
    val idx = result.lines.indexOfLast { it.timeMs <= positionMs + 150L }
    return if (idx >= 0) result.lines[idx].text.takeIf { it.isNotBlank() } else null
}

@Composable
fun NowPlayingOverlay(
    uiState: DesktopUiState,
    playbackState: PlaybackState,
    controller: DesktopAppController,
) {
    val song = playbackState.currentSong ?: return
    val settings = uiState.persisted.settings
    val palette = uiState.artworkPalette
    val isLiked = song.id in uiState.likedIds

    var sideTab by remember { mutableStateOf(PlayerSideTab.LYRICS) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val currentFraction = dragFraction ?: (playbackState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    val artScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1f else 0.93f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )

    val bgBrush = when (settings.playerBackgroundStyle) {
        PlayerBackgroundStyle.GRADIENT -> Brush.verticalGradient(palette.gradientColors)
        PlayerBackgroundStyle.GLOW -> Brush.radialGradient(
            colors = listOf(palette.primary.copy(alpha = 0.42f), palette.secondary.copy(alpha = 0.18f), Color(0xFF090A0F)),
        )
        PlayerBackgroundStyle.BLUR -> Brush.linearGradient(
            colors = listOf(palette.primary.copy(alpha = 0.35f), Color(0xFF10121B)),
        )
        PlayerBackgroundStyle.SOLID -> Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .background(bgBrush)
            .padding(28.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { controller.setPlayerExpanded(false) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Artwork + Info + Controls
                Column(
                    modifier = Modifier.weight(0.46f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .scale(artScale)
                            .shadow(24.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                    ) {
                        AsyncImage(
                            model = song.thumbnail.toHighResThumbnail(),
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { controller.toggleLike(song) }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        VeluneSeekBar(
                            progress = currentFraction,
                            isPlaying = playbackState.isPlaying,
                            style = settings.sliderStyle,
                            enabled = playbackState.durationMs > 0L,
                            onSeekStart = { dragFraction = it },
                            onSeekDrag = { dragFraction = it },
                            onSeekEnd = {
                                dragFraction?.let { frac ->
                                    controller.player.seekTo((frac * totalMs).toLong())
                                }
                                dragFraction = null
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val displayMs = ((dragFraction ?: currentFraction) * totalMs).toLong()
                            Text(formatTimeMs(displayMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatTimeMs(playbackState.durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        IconButton(onClick = controller::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (settings.shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(onClick = controller::playPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(enabled = !playbackState.isBuffering) { controller.player.togglePlayPause() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (playbackState.isBuffering) {
                                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = Color.Black)
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                        IconButton(onClick = controller::playNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = controller::cycleRepeatMode) {
                            Icon(
                                imageVector = if (settings.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (settings.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                // Right: Synced Lyrics + Up Next Queue
                Surface(
                    color = Color(0xFF131622),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(0.54f).fillMaxHeight(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = sideTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ) {
                            PlayerSideTab.entries.forEach { tab ->
                                Tab(
                                    selected = sideTab == tab,
                                    onClick = { sideTab = tab },
                                    text = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                )
                            }
                        }

                        when (sideTab) {
                            PlayerSideTab.LYRICS -> SyncedLyricsPanel(
                                uiState = uiState,
                                positionMs = playbackState.positionMs,
                                onSeek = controller.player::seekTo,
                                onRetry = controller::loadLyricsForCurrentSong,
                            )
                            PlayerSideTab.QUEUE -> NowPlayingQueuePanel(
                                uiState = uiState,
                                isPlaying = playbackState.isPlaying,
                                controller = controller,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsPanel(
    uiState: DesktopUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.lyricsLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Searching BetterLyrics, SimpMusic, LrcLib, KuGou...", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
        uiState.lyricsError != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(uiState.lyricsError, color = Color.White.copy(alpha = 0.7f))
                    Button(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
        }
        uiState.lyricsResult != null -> {
            val result = uiState.lyricsResult
            val activeIndex = remember(positionMs, result) {
                if (!result.isSynced) -1
                else result.lines.indexOfLast { it.timeMs <= positionMs + 150L }
            }
            val listState = rememberLazyListState()

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0) {
                    listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Source: ${result.provider}" + if (result.isSynced) " • Synced (click any line to seek)" else " • Plain",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(result.lines) { index, line ->
                        val isCurrent = index == activeIndex
                        val alpha by animateFloatAsState(
                            targetValue = when {
                                !result.isSynced -> 0.9f
                                isCurrent -> 1f
                                index < activeIndex -> 0.42f
                                else -> 0.6f
                            },
                            animationSpec = tween(250),
                        )
                        val color by animateColorAsState(
                            targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
                            animationSpec = tween(250),
                        )
                        Text(
                            text = line.text.ifBlank { "♪" },
                            style = if (isCurrent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(alpha)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = result.isSynced && line.timeMs >= 0L) {
                                    onSeek(line.timeMs)
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingQueuePanel(
    uiState: DesktopUiState,
    isPlaying: Boolean,
    controller: DesktopAppController,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(uiState.queue) { index, item ->
            val isCurrent = index == uiState.currentQueueIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { controller.jumpToQueueIndex(index) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VeluneThumbnail(
                    url = item.thumbnail,
                    size = 42.dp,
                    cornerRadius = 8.dp,
                    isActive = isCurrent,
                    isPlaying = isPlaying,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { controller.removeFromQueue(index) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
