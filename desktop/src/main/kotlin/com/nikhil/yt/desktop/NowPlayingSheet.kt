package com.nikhil.yt.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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

    Surface(
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(94.dp),
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

            // Right: Expand Now Playing, Volume
            Row(
                modifier = Modifier.weight(0.25f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
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
                    modifier = Modifier.width(100.dp).padding(start = 6.dp),
                )
            }
        }
    }
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
                    color = Color.Black.copy(alpha = 0.28f),
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
