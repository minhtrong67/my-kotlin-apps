package com.minhtrong67.moody.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

val LocalIsDark = compositionLocalOf { false }

/** Role order matches the generated [Palettes] arrays. */
private fun scheme(a: LongArray): ColorScheme {
    fun c(i: Int) = Color(a[i])
    return lightColorScheme(
        primary = c(0), onPrimary = c(1), primaryContainer = c(2), onPrimaryContainer = c(3),
        secondary = c(4), onSecondary = c(5), secondaryContainer = c(6), onSecondaryContainer = c(7),
        tertiary = c(8), onTertiary = c(9), tertiaryContainer = c(10), onTertiaryContainer = c(11),
        error = c(12), onError = c(13), errorContainer = c(14), onErrorContainer = c(15),
        background = c(16), onBackground = c(23),
        surface = c(16), onSurface = c(23),
        surfaceVariant = c(22), onSurfaceVariant = c(24),
        surfaceTint = c(0),
        outline = c(25), outlineVariant = c(26),
        inverseSurface = c(27), inverseOnSurface = c(28), inversePrimary = c(29),
        surfaceDim = c(17), surfaceBright = c(16),
        surfaceContainerLowest = c(18), surfaceContainerLow = c(19), surfaceContainer = c(20),
        surfaceContainerHigh = c(21), surfaceContainerHighest = c(22),
    )
}

/** Material 3 shape scale used by the web app (xs 4 · sm 8 · md 12 · lg 16 · xl 28). */
private val MoodyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun resolveDark(themeMode: String): Boolean = when (themeMode) {
    "light" -> false
    "dark" -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun MoodyTheme(
    themeMode: String,
    themeColor: String,
    dynamicColor: Boolean,
    fontScale: Float,
    fontFamily: String,
    content: @Composable () -> Unit,
) {
    val dark = resolveDark(themeMode)
    // Remembered so a recomposition of the root never rebuilds the scheme / typography (they are static
    // composition locals: a new instance would force the whole UI tree to recompose).
    val ctx = LocalContext.current
    val colors = remember(dark, themeColor, dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
            // Material You: colors extracted from the wallpaper by the system
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            Palettes.byId(themeColor).let { scheme(if (dark) it.dark else it.light) }
        }
    }
    val typography = remember(fontFamily) { typographyFor(fontFamily) }
    val d = LocalDensity.current
    val density = remember(d.density, d.fontScale, fontScale) { Density(d.density, d.fontScale * fontScale) }
    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalDensity provides density,
    ) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = MoodyShapes, content = content)
    }
}
