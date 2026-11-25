package com.nikhil.yt.desktop

import androidx.compose.ui.graphics.Color
import com.nikhil.yt.innertube.models.Album
import com.nikhil.yt.innertube.models.Artist
import com.nikhil.yt.innertube.models.SongItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalTime
import java.util.UUID

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
}

enum class PlayerBackgroundStyle(val label: String) {
    GRADIENT("Gradient"),
    GLOW("Ambient Glow"),
    BLUR("Artwork Tint"),
    SOLID("Default"),
}

enum class SliderStyle(val label: String) {
    SQUIGGLY("Squiggly"),
    DEFAULT("Default"),
    SLIM("Slim"),
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

@Serializable
data class StoredArtist(
    val name: String,
    val id: String? = null,
)

@Serializable
data class StoredAlbum(
    val name: String,
    val id: String,
)

@Serializable
data class StoredSong(
    val id: String,
    val title: String,
    val artists: List<StoredArtist>,
    val album: StoredAlbum? = null,
    val duration: Int? = null,
    val thumbnail: String,
    val explicit: Boolean = false,
) {
    fun toSongItem(): SongItem = SongItem(
        id = id,
        title = title,
        artists = artists.map { Artist(it.name, it.id) },
        album = album?.let { Album(it.name, it.id) },
        duration = duration,
        thumbnail = thumbnail,
        explicit = explicit,
    )

    companion object {
        fun from(song: SongItem): StoredSong = StoredSong(
            id = song.id,
            title = song.title,
            artists = song.artists.map { StoredArtist(it.name, it.id) },
            album = song.album?.let { StoredAlbum(it.name, it.id) },
            duration = song.duration,
            thumbnail = song.thumbnail,
            explicit = song.explicit,
        )
    }
}

@Serializable
data class SongPlayStats(
    val song: StoredSong,
    val playCount: Int = 0,
    val totalPlayTimeMs: Long = 0L,
    val skipCount: Int = 0,
    val lastPlayedAtMs: Long = 0L,
    val morningPlays: Int = 0,
    val afternoonPlays: Int = 0,
    val eveningPlays: Int = 0,
    val nightPlays: Int = 0,
)

@Serializable
data class StoredPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songs: List<StoredSong> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Serializable
data class DesktopSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val pureBlack: Boolean = false,
    val dynamicThemeFromArtwork: Boolean = true,
    val themeColorArgb: Long = 0xFFED5564,
    val playerBackgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.GRADIENT,
    val sliderStyle: SliderStyle = SliderStyle.SQUIGGLY,
    val hideExplicit: Boolean = false,
    val volume: Float = 0.85f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

@Serializable
data class DesktopPersistedState(
    val likedSongs: List<StoredSong> = emptyList(),
    val playlists: List<StoredPlaylist> = emptyList(),
    val playStats: Map<String, SongPlayStats> = emptyMap(),
    val history: List<StoredSong> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val settings: DesktopSettings = DesktopSettings(),
)

class DesktopStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".velune-desktop")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "state.json")
    }

    fun load(): DesktopPersistedState {
        return runCatching {
            if (storageFile.exists()) {
                json.decodeFromString<DesktopPersistedState>(storageFile.readText())
            } else {
                DesktopPersistedState()
            }
        }.getOrDefault(DesktopPersistedState())
    }

    fun save(state: DesktopPersistedState) {
        runCatching {
            storageFile.writeText(json.encodeToString(state))
        }
    }

    fun scoreForYouSongs(state: DesktopPersistedState): List<SongItem> {
        val likedIds = state.likedSongs.map { it.id }.toSet()
        val recentIds = state.history.take(20).map { it.id }.toSet()
        val hour = LocalTime.now().hour
        val bucket = when (hour) {
            in 6..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> "night"
        }

        return state.playStats.values
            .map { stats ->
                var score = stats.playCount * 2.0f + (stats.totalPlayTimeMs / 60_000f) * 0.5f
                score -= stats.skipCount * 3.0f
                if (stats.song.id in likedIds) score += 5.0f
                if (stats.song.id in recentIds) score += 1.5f
                val bucketBonus = when (bucket) {
                    "morning" -> if (stats.morningPlays > 0 || stats.song.id in recentIds) 1.3f else 1.0f
                    "afternoon" -> if (stats.afternoonPlays > 0) 1.25f else 1.0f
                    "evening" -> if (stats.eveningPlays > 0) 1.25f else 1.0f
                    else -> if (stats.nightPlays > 0 || stats.song.id in likedIds) 1.3f else 1.0f
                }
                stats.song.toSongItem() to (score * bucketBonus).coerceAtLeast(0f)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun getQuickPicks(state: DesktopPersistedState): List<SongItem> {
        val scored = scoreForYouSongs(state)
        val combined = (scored + state.likedSongs.map { it.toSongItem() } + state.history.map { it.toSongItem() })
            .distinctBy { it.id }
        return combined.take(20)
    }

    fun getForgottenFavorites(state: DesktopPersistedState): List<SongItem> {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return state.playStats.values
            .filter { it.playCount >= 2 && it.lastPlayedAtMs < cutoff }
            .sortedByDescending { it.playCount }
            .map { it.song.toSongItem() }
            .take(16)
    }
}

val VelunePresetColors = listOf(
    "Velune Coral" to Color(0xFFED5564),
    "Electric Indigo" to Color(0xFF6366F1),
    "Ocean Cyan" to Color(0xFF06B6D4),
    "Emerald" to Color(0xFF10B981),
    "Sunset Amber" to Color(0xFFF59E0B),
    "Violet Pulse" to Color(0xFFA855F7),
    "Rose Pink" to Color(0xFFEC4899),
)
