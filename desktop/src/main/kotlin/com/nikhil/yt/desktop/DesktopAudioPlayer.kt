package com.nikhil.yt.desktop

import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.YouTubeClient
import com.nikhil.yt.innertube.pages.NewPipeUtils
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class PlaybackState(
    val currentSong: SongItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.85f,
    val errorMessage: String? = null,
)

/**
 * Desktop audio player backed by `ffplay` and `:innertube` stream resolution.
 */
class DesktopAudioPlayer(
    private val onTrackEnded: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var process: Process? = null
    private var monitorJob: Job? = null
    private var loadJob: Job? = null

    private var activeStreamUrl: String? = null
    private var activeUserAgent: String = YouTubeClient.USER_AGENT_WEB
    private var playbackStartedAtMs: Long = 0L
    private var playbackBaseOffsetMs: Long = 0L

    private val ffplayExecutable: String by lazy { findFfplayExecutable() }

    fun play(song: SongItem) {
        loadJob?.cancel()
        stopProcessOnly()

        _state.update {
            it.copy(
                currentSong = song,
                isPlaying = false,
                isBuffering = true,
                positionMs = 0L,
                durationMs = (song.duration ?: 0) * 1000L,
                errorMessage = null,
            )
        }

        loadJob = scope.launch {
            val resolved = resolveStream(song.id)
            if (resolved == null) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Could not resolve audio stream for \"${song.title}\"",
                    )
                }
                return@launch
            }

            activeStreamUrl = resolved.url
            activeUserAgent = resolved.userAgent
            val resolvedDurationMs = resolved.durationMs ?: ((song.duration ?: 0) * 1000L)
            _state.update {
                it.copy(
                    durationMs = resolvedDurationMs,
                    isBuffering = false,
                )
            }
            startFfplayAt(0L)
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (current.currentSong == null || current.isBuffering) return
        if (current.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        val current = _state.value
        if (!current.isPlaying) return
        val elapsed = computeCurrentPositionMs()
        stopProcessOnly()
        _state.update { it.copy(isPlaying = false, positionMs = elapsed) }
    }

    fun resume() {
        val current = _state.value
        if (current.isPlaying || current.currentSong == null) return
        if (activeStreamUrl == null) {
            play(current.currentSong)
            return
        }
        scope.launch {
            startFfplayAt(current.positionMs)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = _state.value
        if (current.currentSong == null || activeStreamUrl == null) return
        val clamped = positionMs.coerceIn(0L, current.durationMs.coerceAtLeast(1L))
        _state.update { it.copy(positionMs = clamped) }
        if (current.isPlaying) {
            scope.launch {
                startFfplayAt(clamped)
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        val current = _state.value
        _state.update { it.copy(volume = clamped) }
        if (current.isPlaying && activeStreamUrl != null) {
            val pos = computeCurrentPositionMs()
            scope.launch {
                startFfplayAt(pos)
            }
        }
    }

    fun stop() {
        loadJob?.cancel()
        stopProcessOnly()
        activeStreamUrl = null
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                positionMs = 0L,
            )
        }
    }

    fun shutdown() {
        loadJob?.cancel()
        stopProcessOnly()
        scope.cancel()
    }

    private suspend fun startFfplayAt(offsetMs: Long) = withContext(Dispatchers.IO) {
        val url = activeStreamUrl ?: return@withContext
        stopProcessOnly()

        val volPercent = (_state.value.volume * 100f).toInt().coerceIn(0, 100)
        val offsetSeconds = String.format(Locale.US, "%.3f", offsetMs / 1000.0)

        val command = buildList {
            add(ffplayExecutable)
            add("-nodisp")
            add("-autoexit")
            add("-loglevel")
            add("quiet")
            add("-vn")
            add("-user_agent")
            add(activeUserAgent)
            if (offsetMs > 0L) {
                add("-ss")
                add(offsetSeconds)
            }
            add("-volume")
            add(volPercent.toString())
            add(url)
        }

        try {
            val startedProcess = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            process = startedProcess
            playbackBaseOffsetMs = offsetMs
            playbackStartedAtMs = System.currentTimeMillis()
            _state.update { it.copy(isPlaying = true, errorMessage = null) }

            monitorJob = scope.launch {
                while (isActive && startedProcess.isAlive) {
                    val pos = computeCurrentPositionMs()
                    val dur = _state.value.durationMs
                    _state.update {
                        it.copy(positionMs = if (dur > 0L) pos.coerceAtMost(dur) else pos)
                    }
                    delay(250L)
                }

                if (isActive && process === startedProcess) {
                    val exitCode = runCatching { startedProcess.waitFor() }.getOrDefault(0)
                    _state.update { it.copy(isPlaying = false) }
                    if (exitCode == 0) {
                        onTrackEnded()
                    }
                }
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = "Failed to launch ffplay (${e.message ?: "unknown error"}). Make sure ffmpeg/ffplay is installed.",
                )
            }
        }
    }

    private fun computeCurrentPositionMs(): Long {
        val elapsed = (System.currentTimeMillis() - playbackStartedAtMs).coerceAtLeast(0L)
        return playbackBaseOffsetMs + elapsed
    }

    private fun stopProcessOnly() {
        monitorJob?.cancel()
        monitorJob = null
        val old = process
        process = null
        if (old != null && old.isAlive) {
            old.destroy()
            if (old.isAlive) {
                old.destroyForcibly()
            }
        }
    }

    internal data class ResolvedStream(
        val url: String,
        val userAgent: String,
        val durationMs: Long?,
    )

    internal suspend fun resolveStream(videoId: String): ResolvedStream? {
        if (YouTube.visitorData.isNullOrBlank()) {
            YouTube.visitorData().getOrNull()?.let { YouTube.visitorData = it }
        }

        val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
        val clients = listOf(
            YouTubeClient.VISIONOS,
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.ANDROID_VR_1_61_48,
            YouTubeClient.ANDROID_VR_1_43_32,
            YouTubeClient.ANDROID_TESTSUITE,
            YouTubeClient.ANDROID_UNPLUGGED,
            YouTubeClient.WEB_EMBEDDED,
            YouTubeClient.WEB_CREATOR,
            YouTubeClient.MWEB,
            YouTubeClient.IOS,
            YouTubeClient.ANDROID_MUSIC,
            YouTubeClient.WEB_REMIX,
            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        )

        var lastReason: String? = null
        for (client in clients) {
            val responseResult = YouTube.player(
                videoId = videoId,
                client = client,
                signatureTimestamp = signatureTimestamp,
            )
            val response = responseResult.getOrNull()
            if (response == null) {
                lastReason = responseResult.exceptionOrNull()?.message
                continue
            }

            if (response.playabilityStatus.status != "OK") {
                lastReason = "${client.clientName}: ${response.playabilityStatus.status} (${response.playabilityStatus.reason})"
                continue
            }

            val audioFormats = response.streamingData?.adaptiveFormats
                ?.filter { it.isAudio && it.bitrate > 0 && (it.url != null || it.signatureCipher != null || it.cipher != null) }
                ?.sortedWith(
                    compareByDescending<com.nikhil.yt.innertube.models.response.PlayerResponse.StreamingData.Format> { it.url != null }
                        .thenByDescending { it.bitrate }
                )
                .orEmpty()

            val fallbackFormats = response.streamingData?.formats
                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                .orEmpty()
            val candidates = audioFormats + fallbackFormats

            for (format in candidates.take(6)) {
                val url = NewPipeUtils.getStreamUrl(format, videoId, client).getOrNull() ?: continue
                val durationMs = format.approxDurationMs?.toLongOrNull()
                    ?: response.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000L)
                return ResolvedStream(
                    url = url,
                    userAgent = client.userAgent,
                    durationMs = durationMs,
                )
            }
        }
        if (lastReason != null) {
            System.err.println("VeluneDesktop: Failed to resolve stream for $videoId -> $lastReason")
        }
        return null
    }

    private fun findFfplayExecutable(): String {
        val localAppData = System.getenv("LOCALAPPDATA")
        if (!localAppData.isNullOrBlank()) {
            val wingetLink = File(localAppData, "Microsoft/WinGet/Links/ffplay.exe")
            if (wingetLink.exists()) return wingetLink.absolutePath
        }
        return "ffplay"
    }
}
