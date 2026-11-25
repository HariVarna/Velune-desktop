package com.nikhil.yt.desktop

import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.YouTubeClient
import com.nikhil.yt.innertube.models.response.PlayerResponse
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
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
 * Thread-safe desktop audio player backed by `ffplay` and `:innertube` stream resolution.
 * Guarantees at most one `ffplay` process is ever active and cleans up on JVM exit.
 */
class DesktopAudioPlayer(
    private val onTrackEnded: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val processLock = Any()
    private val activeProcesses = Collections.newSetFromMap(ConcurrentHashMap<Process, Boolean>())
    private val generation = AtomicLong(0L)
    private val isShutdown = AtomicBoolean(false)

    @Volatile private var currentProcess: Process? = null
    @Volatile private var monitorJob: Job? = null
    @Volatile private var loadJob: Job? = null
    @Volatile private var volumeApplyJob: Job? = null

    @Volatile private var activeStreamUrl: String? = null
    @Volatile private var activeUserAgent: String = YouTubeClient.USER_AGENT_WEB
    @Volatile private var playbackStartedAtMs: Long = 0L
    @Volatile private var playbackBaseOffsetMs: Long = 0L

    private val ffplayExecutable: String by lazy { findFfplayExecutable() }

    private val shutdownHook = Thread {
        killAllTrackedProcesses()
    }

    init {
        runCatching { Runtime.getRuntime().addShutdownHook(shutdownHook) }
    }

    fun play(song: SongItem) {
        if (isShutdown.get()) return
        val myGen = generation.incrementAndGet()
        loadJob?.cancel()
        volumeApplyJob?.cancel()
        killAllTrackedProcesses()
        activeStreamUrl = null

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
            if (myGen != generation.get() || isShutdown.get()) return@launch

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
            startFfplayAt(offsetMs = 0L, expectedGen = myGen)
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
        generation.incrementAndGet()
        volumeApplyJob?.cancel()
        val elapsed = computeCurrentPositionMs()
        killAllTrackedProcesses()
        _state.update { it.copy(isPlaying = false, positionMs = elapsed) }
    }

    fun resume() {
        val current = _state.value
        if (current.isPlaying || current.currentSong == null || isShutdown.get()) return
        val url = activeStreamUrl
        if (url == null) {
            play(current.currentSong)
            return
        }
        val myGen = generation.incrementAndGet()
        scope.launch {
            startFfplayAt(offsetMs = current.positionMs, expectedGen = myGen)
        }
    }

    fun seekTo(positionMs: Long) {
        val current = _state.value
        if (current.currentSong == null || activeStreamUrl == null || isShutdown.get()) return
        val clamped = positionMs.coerceIn(0L, current.durationMs.coerceAtLeast(1L))
        _state.update { it.copy(positionMs = clamped) }
        if (current.isPlaying) {
            val myGen = generation.incrementAndGet()
            scope.launch {
                startFfplayAt(offsetMs = clamped, expectedGen = myGen)
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.update { it.copy(volume = clamped) }
        if (!_state.value.isPlaying || activeStreamUrl == null || isShutdown.get()) return

        volumeApplyJob?.cancel()
        volumeApplyJob = scope.launch {
            delay(150L)
            if (!_state.value.isPlaying || activeStreamUrl == null || isShutdown.get()) return@launch
            val pos = computeCurrentPositionMs()
            val myGen = generation.incrementAndGet()
            startFfplayAt(offsetMs = pos, expectedGen = myGen)
        }
    }

    fun stop() {
        generation.incrementAndGet()
        loadJob?.cancel()
        volumeApplyJob?.cancel()
        killAllTrackedProcesses()
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
        if (!isShutdown.compareAndSet(false, true)) return
        generation.incrementAndGet()
        loadJob?.cancel()
        volumeApplyJob?.cancel()
        killAllTrackedProcesses()
        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
        scope.cancel()
    }

    private suspend fun startFfplayAt(offsetMs: Long, expectedGen: Long) = withContext(Dispatchers.IO) {
        val url = activeStreamUrl ?: return@withContext
        val userAgent = activeUserAgent
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
            add(userAgent)
            if (offsetMs > 0L) {
                add("-ss")
                add(offsetSeconds)
            }
            add("-volume")
            add(volPercent.toString())
            add(url)
        }

        val startedProcess: Process = synchronized(processLock) {
            if (expectedGen != generation.get() || isShutdown.get()) return@withContext
            killAllTrackedProcessesLocked()

            try {
                val proc = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                activeProcesses.add(proc)
                currentProcess = proc
                playbackBaseOffsetMs = offsetMs
                playbackStartedAtMs = System.currentTimeMillis()
                proc
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        errorMessage = "Failed to launch ffplay (${e.message ?: "unknown error"}). Make sure ffmpeg/ffplay is installed.",
                    )
                }
                return@withContext
            }
        }

        _state.update { it.copy(isPlaying = true, errorMessage = null) }

        monitorJob = scope.launch {
            while (isActive && startedProcess.isAlive && expectedGen == generation.get() && !isShutdown.get()) {
                val pos = computeCurrentPositionMs()
                val dur = _state.value.durationMs
                _state.update {
                    it.copy(positionMs = if (dur > 0L) pos.coerceAtMost(dur) else pos)
                }
                delay(250L)
            }

            if (expectedGen == generation.get() && !isShutdown.get()) {
                val exitCode = runCatching { startedProcess.waitFor() }.getOrDefault(-1)
                activeProcesses.remove(startedProcess)
                if (expectedGen == generation.get() && !isShutdown.get()) {
                    _state.update { it.copy(isPlaying = false) }
                    if (exitCode == 0) {
                        onTrackEnded()
                    }
                }
            }
        }
    }

    private fun computeCurrentPositionMs(): Long {
        val elapsed = (System.currentTimeMillis() - playbackStartedAtMs).coerceAtLeast(0L)
        return playbackBaseOffsetMs + elapsed
    }

    private fun killAllTrackedProcesses() {
        synchronized(processLock) {
            killAllTrackedProcessesLocked()
        }
    }

    private fun killAllTrackedProcessesLocked() {
        monitorJob?.cancel()
        monitorJob = null
        currentProcess = null

        val snapshot = activeProcesses.toList()
        activeProcesses.clear()
        for (proc in snapshot) {
            runCatching {
                proc.descendants().forEach { child ->
                    runCatching { child.destroyForcibly() }
                }
                if (proc.isAlive) {
                    proc.destroy()
                    if (!proc.waitFor(150, TimeUnit.MILLISECONDS)) {
                        proc.destroyForcibly()
                        proc.waitFor(350, TimeUnit.MILLISECONDS)
                    }
                }
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
                    compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
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
