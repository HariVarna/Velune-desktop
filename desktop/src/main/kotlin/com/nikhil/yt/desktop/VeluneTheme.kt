package com.nikhil.yt.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

val DefaultVeluneThemeColor = Color(0xFFED5564)

data class ArtworkPalette(
    val primary: Color = DefaultVeluneThemeColor,
    val secondary: Color = Color(0xFF38BDF8),
    val gradientColors: List<Color> = listOf(Color(0xFF281828), Color(0xFF141824), Color(0xFF0A0B10)),
)

@Composable
fun VeluneDesktopTheme(
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    themeColor: Color = DefaultVeluneThemeColor,
    content: @Composable () -> Unit,
) {
    val baseScheme = remember(themeColor, darkTheme) {
        buildM3TonalScheme(keyColor = themeColor, isDark = darkTheme)
    }

    val finalScheme = remember(baseScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF080808),
                surfaceContainer = Color(0xFF101010),
                surfaceContainerHigh = Color(0xFF181818),
                surfaceContainerHighest = Color(0xFF202020),
            )
        } else {
            baseScheme
        }
    }

    val animatedScheme = animateColorScheme(finalScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography(),
        content = content,
    )
}

private fun buildM3TonalScheme(keyColor: Color, isDark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(keyColor.toArgb()), isDark, 0.0)
    fun Int.c() = Color(this)
    return ColorScheme(
        primary = scheme.primary.c(),
        onPrimary = scheme.onPrimary.c(),
        primaryContainer = scheme.primaryContainer.c(),
        onPrimaryContainer = scheme.onPrimaryContainer.c(),
        inversePrimary = scheme.inversePrimary.c(),
        secondary = scheme.secondary.c(),
        onSecondary = scheme.onSecondary.c(),
        secondaryContainer = scheme.secondaryContainer.c(),
        onSecondaryContainer = scheme.onSecondaryContainer.c(),
        tertiary = scheme.tertiary.c(),
        onTertiary = scheme.onTertiary.c(),
        tertiaryContainer = scheme.tertiaryContainer.c(),
        onTertiaryContainer = scheme.onTertiaryContainer.c(),
        background = scheme.background.c(),
        onBackground = scheme.onBackground.c(),
        surface = scheme.surface.c(),
        onSurface = scheme.onSurface.c(),
        surfaceVariant = scheme.surfaceVariant.c(),
        onSurfaceVariant = scheme.onSurfaceVariant.c(),
        surfaceTint = scheme.surfaceTint.c(),
        inverseSurface = scheme.inverseSurface.c(),
        inverseOnSurface = scheme.inverseOnSurface.c(),
        error = scheme.error.c(),
        onError = scheme.onError.c(),
        errorContainer = scheme.errorContainer.c(),
        onErrorContainer = scheme.onErrorContainer.c(),
        outline = scheme.outline.c(),
        outlineVariant = scheme.outlineVariant.c(),
        scrim = scheme.scrim.c(),
        surfaceBright = scheme.surfaceBright.c(),
        surfaceDim = scheme.surfaceDim.c(),
        surfaceContainer = scheme.surfaceContainer.c(),
        surfaceContainerLow = scheme.surfaceContainerLow.c(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.c(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.c(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.c(),
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = spring<Color>(stiffness = Spring.StiffnessLow)
    return ColorScheme(
        primary = animateColorAsState(target.primary, spec).value,
        onPrimary = animateColorAsState(target.onPrimary, spec).value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec).value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec).value,
        inversePrimary = animateColorAsState(target.inversePrimary, spec).value,
        secondary = animateColorAsState(target.secondary, spec).value,
        onSecondary = animateColorAsState(target.onSecondary, spec).value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec).value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec).value,
        tertiary = animateColorAsState(target.tertiary, spec).value,
        onTertiary = animateColorAsState(target.onTertiary, spec).value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, spec).value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, spec).value,
        background = animateColorAsState(target.background, spec).value,
        onBackground = animateColorAsState(target.onBackground, spec).value,
        surface = animateColorAsState(target.surface, spec).value,
        onSurface = animateColorAsState(target.onSurface, spec).value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec).value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec).value,
        surfaceTint = animateColorAsState(target.surfaceTint, spec).value,
        inverseSurface = animateColorAsState(target.inverseSurface, spec).value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, spec).value,
        error = animateColorAsState(target.error, spec).value,
        onError = animateColorAsState(target.onError, spec).value,
        errorContainer = animateColorAsState(target.errorContainer, spec).value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec).value,
        outline = animateColorAsState(target.outline, spec).value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec).value,
        scrim = animateColorAsState(target.scrim, spec).value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec).value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec).value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec).value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec).value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec).value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec).value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec).value,
    )
}

suspend fun extractArtworkPalette(imageUrl: String?): ArtworkPalette = withContext(Dispatchers.IO) {
    if (imageUrl.isNullOrBlank()) return@withContext ArtworkPalette()
    runCatching {
        val img: BufferedImage = URI.create(imageUrl).toURL().openStream().use { ImageIO.read(it) }
            ?: return@withContext ArtworkPalette()

        val buckets = HashMap<Int, Int>()
        val stepX = (img.width / 24).coerceAtLeast(1)
        val stepY = (img.height / 24).coerceAtLeast(1)

        for (y in 0 until img.height step stepY) {
            for (x in 0 until img.width step stepX) {
                val rgb = img.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                // Quantize to 12-bit buckets
                val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                buckets[key] = (buckets[key] ?: 0) + 1
            }
        }

        val candidates = buckets.entries
            .map { (key, count) ->
                val r = ((key shr 8) and 0xF) * 17
                val g = ((key shr 4) and 0xF) * 17
                val b = (key and 0xF) * 17
                val color = Color(r, g, b)
                val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
                val vibrancyScore = count * (0.4f + hsb[1] * 2.2f) * (if (hsb[2] in 0.18f..0.92f) 1.3f else 0.4f)
                color to vibrancyScore
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val primary = candidates.firstOrNull { it.luminance() in 0.06f..0.78f } ?: DefaultVeluneThemeColor
        val secondary = candidates.drop(1).firstOrNull {
            it.luminance() in 0.05f..0.75f &&
                kotlin.math.abs(it.red - primary.red) + kotlin.math.abs(it.green - primary.green) + kotlin.math.abs(it.blue - primary.blue) > 0.35f
        } ?: primary.copy(alpha = 0.7f)

        val dark1 = Color(
            red = (primary.red * 0.34f).coerceIn(0.04f, 0.35f),
            green = (primary.green * 0.34f).coerceIn(0.04f, 0.35f),
            blue = (primary.blue * 0.34f).coerceIn(0.04f, 0.35f),
        )
        val dark2 = Color(
            red = (secondary.red * 0.22f).coerceIn(0.03f, 0.24f),
            green = (secondary.green * 0.22f).coerceIn(0.03f, 0.24f),
            blue = (secondary.blue * 0.22f).coerceIn(0.03f, 0.24f),
        )
        ArtworkPalette(
            primary = primary,
            secondary = secondary,
            gradientColors = listOf(dark1, dark2, Color(0xFF090A0F)),
        )
    }.getOrDefault(ArtworkPalette())
}
