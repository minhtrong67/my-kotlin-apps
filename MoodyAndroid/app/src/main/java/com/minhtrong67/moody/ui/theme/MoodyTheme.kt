package com.minhtrong67.moody.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val dark = resolveDark(themeMode)
    val palette = Palettes.byId(themeColor)
    val colors = scheme(if (dark) palette.dark else palette.light)
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalDensity provides Density(d.density, d.fontScale * fontScale),
    ) {
        MaterialTheme(colorScheme = colors, shapes = MoodyShapes, content = content)
    }
}
