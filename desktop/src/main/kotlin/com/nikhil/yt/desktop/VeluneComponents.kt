package com.nikhil.yt.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nikhil.yt.innertube.models.AlbumItem
import com.nikhil.yt.innertube.models.ArtistItem
import com.nikhil.yt.innertube.models.PlaylistItem
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.YTItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated 3-bar playing equalizer matching Velune Android's PlayingIndicator.
 */
@Composable
fun VelunePlayingIndicator(
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val animatables = remember { List(3) { Animatable(0.2f) } }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            animatables.forEach { it.snapTo(0.22f) }
            return@LaunchedEffect
        }
        animatables.forEach { anim ->
            launch {
                while (true) {
                    anim.animateTo(Random.nextFloat() * 0.85f + 0.15f, animationSpec = tween(220))
                    delay(40)
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        animatables.forEach { anim ->
            Canvas(modifier = Modifier.fillMaxHeight().width(3.dp)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, size.height * (1f - anim.value)),
                    size = size.copy(height = anim.value * size.height),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

/**
 * Squiggly / Default / Slim seek bar matching Velune Android's SliderStyle options.
 */
@Composable
fun VeluneSeekBar(
    progress: Float,
    isPlaying: Boolean,
    style: SliderStyle,
    enabled: Boolean,
    onSeekStart: (Float) -> Unit,
    onSeekDrag: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val thumbColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart,
        ),
    )

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying && style == SliderStyle.SQUIGGLY) 1f else 0f,
        animationSpec = tween(350),
    )

    var widthPx by remember { mutableStateOf(1f) }
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    onSeekStart(fraction)
                    onSeekEnd()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        onSeekStart((offset.x / widthPx).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onSeekDrag((change.position.x / widthPx).coerceIn(0f, 1f))
                    },
                    onDragEnd = onSeekEnd,
                    onDragCancel = onSeekEnd,
                )
            },
    ) {
        val cy = size.height / 2f
        val activeEndX = size.width * clamped
        val strokeW = when (style) {
            SliderStyle.SLIM -> 3.dp.toPx()
            SliderStyle.DEFAULT -> 5.dp.toPx()
            SliderStyle.SQUIGGLY -> 3.5.dp.toPx()
        }

        // Inactive track
        drawLine(
            color = inactiveColor,
            start = Offset(activeEndX, cy),
            end = Offset(size.width, cy),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
        )

        // Active track
        if (style == SliderStyle.SQUIGGLY && waveAmplitudeFactor > 0.02f && activeEndX > 4f) {
            val amplitude = 4.dp.toPx() * waveAmplitudeFactor
            val wavelength = 24.dp.toPx()
            val path = Path().apply {
                moveTo(0f, cy + sin(phase) * amplitude)
                var x = 0f
                while (x <= activeEndX) {
                    val y = cy + sin((x / wavelength) * 2f * PI.toFloat() - phase) * amplitude
                    lineTo(x, y)
                    x += 3f
                }
                lineTo(activeEndX, cy + sin((activeEndX / wavelength) * 2f * PI.toFloat() - phase) * amplitude)
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
        } else if (activeEndX > 0f) {
            drawLine(
                color = activeColor,
                start = Offset(0f, cy),
                end = Offset(activeEndX, cy),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
        }

        // Thumb
        if (enabled) {
            val thumbRadius = if (style == SliderStyle.SLIM) 5.dp.toPx() else 7.dp.toPx()
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(activeEndX, cy),
            )
        }
    }
}

@Composable
fun ShimmerPlaceholderBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition()
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart,
        ),
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(shift - 300f, 0f),
                    end = Offset(shift, 300f),
                )
            ),
    )
}

@Composable
fun HomeShimmerSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) {
                ShimmerPlaceholderBox(Modifier.width(88.dp).height(34.dp), RoundedCornerShape(18.dp))
            }
        }
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerPlaceholderBox(Modifier.width(180.dp).height(24.dp), RoundedCornerShape(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(5) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShimmerPlaceholderBox(Modifier.size(164.dp), RoundedCornerShape(14.dp))
                            ShimmerPlaceholderBox(Modifier.width(130.dp).height(14.dp), RoundedCornerShape(4.dp))
                            ShimmerPlaceholderBox(Modifier.width(90.dp).height(12.dp), RoundedCornerShape(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VeluneThumbnail(
    url: String?,
    size: Dp,
    cornerRadius: Dp = 10.dp,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isCircle: Boolean = false,
) {
    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                VelunePlayingIndicator(
                    isPlaying = isPlaying,
                    color = Color.White,
                    modifier = Modifier.height(size * 0.36f),
                )
            }
        }
    }
}

@Composable
fun VeluneGridCard(
    item: YTItem,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VeluneThumbnail(
            url = item.thumbnail,
            size = 152.dp,
            cornerRadius = 10.dp,
            isActive = isActive,
            isPlaying = isPlaying,
            isCircle = item is ArtistItem,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = ytItemSubtitle(item),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun VeluneSongRow(
    item: YTItem,
    index: Int? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isLiked: Boolean = false,
    playlists: List<StoredPlaylist> = emptyList(),
    onClick: () -> Unit,
    onToggleLike: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToPlaylist: ((String) -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val rowBg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (index != null) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.width(26.dp),
            )
        }
        VeluneThumbnail(
            url = item.thumbnail,
            size = 48.dp,
            cornerRadius = 8.dp,
            isActive = isActive,
            isPlaying = isPlaying,
            isCircle = item is ArtistItem,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ytItemSubtitle(item),
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

        if (onToggleLike != null) {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (item is SongItem && (onAddToQueue != null || onPlayNext != null || onAddToPlaylist != null)) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            },
                        )
                    }
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            },
                        )
                    }
                    if (onAddToPlaylist != null && playlists.isNotEmpty()) {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text("Add to \"${playlist.name}\"") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist(playlist.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

fun ytItemSubtitle(item: YTItem): String = when (item) {
    is SongItem -> item.artists.joinToString { it.name }.ifBlank { "Song" }
    is AlbumItem -> "Album" + (item.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: "")
    is PlaylistItem -> "Playlist" + ((item.author?.name ?: item.songCountText)?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: "")
    is ArtistItem -> "Artist"
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
