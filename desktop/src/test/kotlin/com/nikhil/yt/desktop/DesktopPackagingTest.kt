package com.nikhil.yt.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopPackagingTest {
    @Test
    fun windowIconResourceIsBundled() {
        val stream = DesktopAppController::class.java.classLoader.getResourceAsStream("velune.png")
        assertNotNull("Expected velune.png resource on desktop classpath", stream)
        val bytes = stream!!.use { it.readBytes() }
        assertTrue("Expected non-empty velune.png icon", bytes.isNotEmpty())
    }

    @Test
    fun persistedSettingsDefaultsAreValid() {
        val state = DesktopPersistedState()
        assertEquals(ThemeMode.DARK, state.settings.themeMode)
        assertEquals(VeluneThemeStyle.MODERN, state.settings.themeStyle)
        assertEquals(IslandAnchor.TOP_CENTER, state.settings.islandAnchor)
    }

    @Test
    fun timeFormattingWorks() {
        assertEquals("0:00", formatTimeMs(0L))
        assertEquals("3:25", formatTimeMs(205_000L))
    }
}
