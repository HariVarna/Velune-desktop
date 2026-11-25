package com.nikhil.yt.desktop

import com.nikhil.yt.betterlyrics.BetterLyrics
import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.WatchEndpoint
import com.nikhil.yt.kugou.KuGou
import com.nikhil.yt.lrclib.LrcLib
import com.nikhil.yt.simpmusic.SimpMusicLyrics

data class SyncedLyricLine(
    val timeMs: Long,
    val text: String,
)

data class LyricsResult(
    val provider: String,
    val isSynced: Boolean,
    val lines: List<SyncedLyricLine>,
)

object DesktopLyricsEngine {
    suspend fun fetchLyrics(song: SongItem): Result<LyricsResult> {
        val artist = song.artists.joinToString(", ") { it.name }.ifBlank {
            song.artists.firstOrNull()?.name.orEmpty()
        }
        val firstArtist = song.artists.firstOrNull()?.name ?: artist
        val duration = song.duration ?: -1
        val album = song.album?.name

        // 1. BetterLyrics (TTML synced)
        if (firstArtist.isNotBlank()) {
            BetterLyrics.getLyrics(
                title = song.title,
                artist = firstArtist,
                album = album,
                durationSeconds = duration,
            ).getOrNull()?.let { ttml ->
                val parsed = parseTtml(ttml)
                if (parsed.isNotEmpty()) {
                    return Result.success(LyricsResult("BetterLyrics", isSynced = true, lines = parsed))
                }
            }
        }

        // 2. SimpMusic
        SimpMusicLyrics.getLyrics(song.id, duration.coerceAtLeast(0)).getOrNull()?.let { raw ->
            val parsed = parseLrcOrPlain(raw)
            if (parsed.lines.isNotEmpty()) {
                return Result.success(LyricsResult("SimpMusic", isSynced = parsed.isSynced, lines = parsed.lines))
            }
        }

        // 3. LrcLib
        if (firstArtist.isNotBlank()) {
            LrcLib.getLyrics(
                title = song.title,
                artist = firstArtist,
                duration = duration,
                album = album,
            ).getOrNull()?.let { raw ->
                val parsed = parseLrcOrPlain(raw)
                if (parsed.lines.isNotEmpty()) {
                    return Result.success(LyricsResult("LrcLib", isSynced = parsed.isSynced, lines = parsed.lines))
                }
            }
        }

        // 4. KuGou
        if (firstArtist.isNotBlank() && duration > 0) {
            KuGou.getLyrics(song.title, firstArtist, duration).getOrNull()?.let { raw ->
                val parsed = parseLrcOrPlain(raw)
                if (parsed.lines.isNotEmpty()) {
                    return Result.success(LyricsResult("KuGou", isSynced = parsed.isSynced, lines = parsed.lines))
                }
            }
        }

        // 5. YouTube Music native lyrics
        val next = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()
        val lyricsEndpoint = next?.lyricsEndpoint
        if (lyricsEndpoint != null) {
            YouTube.lyrics(lyricsEndpoint).getOrNull()?.let { ytText ->
                if (ytText.isNotBlank()) {
                    val plainLines = ytText.lines().map { SyncedLyricLine(-1L, it) }
                    return Result.success(LyricsResult("YouTube Music", isSynced = false, lines = plainLines))
                }
            }
        }

        return Result.failure(IllegalStateException("No lyrics found across BetterLyrics, SimpMusic, LrcLib, KuGou, or YouTube Music"))
    }

    private data class ParsedLrc(val isSynced: Boolean, val lines: List<SyncedLyricLine>)

    private fun parseLrcOrPlain(raw: String): ParsedLrc {
        val tagRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
        val synced = mutableListOf<SyncedLyricLine>()

        for (rawLine in raw.lines()) {
            val matches = tagRegex.findAll(rawLine).toList()
            if (matches.isNotEmpty()) {
                val text = rawLine.replace(tagRegex, "").trim()
                for (m in matches) {
                    val min = m.groupValues[1].toLongOrNull() ?: 0L
                    val sec = m.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = m.groupValues[3]
                    val millis = when (fracStr.length) {
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fracStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val totalMs = min * 60_000L + sec * 1000L + millis
                    synced.add(SyncedLyricLine(totalMs, text))
                }
            }
        }

        return if (synced.isNotEmpty()) {
            ParsedLrc(isSynced = true, lines = synced.sortedBy { it.timeMs })
        } else {
            val plain = raw.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { SyncedLyricLine(-1L, it) }
            ParsedLrc(isSynced = false, lines = plain)
        }
    }

    private fun parseTtml(ttml: String): List<SyncedLyricLine> {
        val pRegex = Regex("""<p[^>]*\bbegin="([^"]+)"[^>]*>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val tagStrip = Regex("""<[^>]+>""")
        val lines = mutableListOf<SyncedLyricLine>()

        for (match in pRegex.findAll(ttml)) {
            val beginMs = parseTtmlTimestamp(match.groupValues[1]) ?: continue
            val text = match.groupValues[2]
                .replace(tagStrip, " ")
                .replace("&amp;", "&")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotEmpty()) {
                lines.add(SyncedLyricLine(beginMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun parseTtmlTimestamp(ts: String): Long? {
        val trimmed = ts.trim()
        if (trimmed.endsWith("s")) {
            val secs = trimmed.removeSuffix("s").toDoubleOrNull() ?: return null
            return (secs * 1000.0).toLong()
        }
        val parts = trimmed.split(":")
        return when (parts.size) {
            3 -> {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val s = parts[2].toDoubleOrNull() ?: 0.0
                (h * 3600_000L) + (m * 60_000L) + (s * 1000.0).toLong()
            }
            2 -> {
                val m = parts[0].toLongOrNull() ?: 0L
                val s = parts[1].toDoubleOrNull() ?: 0.0
                (m * 60_000L) + (s * 1000.0).toLong()
            }
            else -> {
                val s = trimmed.toDoubleOrNull() ?: return null
                (s * 1000.0).toLong()
            }
        }
    }
}
