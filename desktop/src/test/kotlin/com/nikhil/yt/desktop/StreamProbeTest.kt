package com.nikhil.yt.desktop

import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class StreamProbeTest {
    @Test
    fun resolveRealSongStream() = runBlocking {
        val player = DesktopAudioPlayer(onTrackEnded = {})
        try {
            val searchRes = YouTube.search("believer", YouTube.SearchFilter.FILTER_SONG).getOrThrow()
            val song = searchRes.items.filterIsInstance<SongItem>().first()
            println("Testing song: ${song.title} (id=${song.id})")

            val resolved = player.resolveStream(song.id)
            println("Resolved: ${resolved != null}, ua=${resolved?.userAgent}, durMs=${resolved?.durationMs}")
            assertNotNull("Expected resolved stream for ${song.id}", resolved)

            val ffplayProc = ProcessBuilder(
                "ffplay",
                "-nodisp",
                "-autoexit",
                "-loglevel",
                "error",
                "-vn",
                "-t",
                "1",
                "-user_agent",
                resolved!!.userAgent,
                resolved.url,
            ).redirectErrorStream(true).start()

            val output = ffplayProc.inputStream.bufferedReader().readText()
            val exitCode = ffplayProc.waitFor()
            println("ffplay exitCode=$exitCode output=$output")
            assert(exitCode == 0) { "ffplay failed with exitCode=$exitCode: $output" }
        } finally {
            player.shutdown()
        }
    }
}
