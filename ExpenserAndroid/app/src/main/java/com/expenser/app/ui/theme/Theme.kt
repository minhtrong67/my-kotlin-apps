package com.expenser.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.expenser.app.data.AppSettings

/** Semantic income colours (expense uses the scheme's error roles). */
@Immutable
data class IncomeColors(val income: Color, val onIncome: Color, val container: Color, val onContainer: Color)

val LocalIncomeColors = staticCompositionLocalOf {
    IncomeColors(Color(0xFF006D38), Color.White, Color(0xFF8DF9AC), Color(0xFF00210D))
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

// Default scheme: the web app's Material 3 palette generated from seed #4CAF50.
private val DefaultLight = lightColorScheme(
    primary = c(0x006E1C), onPrimary = c(0xFFFFFF), primaryContainer = c(0x70FF77), onPrimaryContainer = c(0x005313),
    secondary = c(0x426650), onSecondary = c(0xFFFFFF), secondaryContainer = c(0xC3ECD0), onSecondaryContainer = c(0x2A4E3A),
    tertiary = c(0x256958), onTertiary = c(0xFFFFFF), tertiaryContainer = c(0xACF0DA), onTertiaryContainer = c(0x005141),
    error = c(0xBA1A1A), onError = c(0xFFFFFF), errorContainer = c(0xFFDAD6), onErrorContainer = c(0x93000A),
    background = c(0xF3FCEC), onBackground = c(0x161E14), surface = c(0xF3FCEC), onSurface = c(0x161E14),
    surfaceVariant = c(0xDAE6D4), onSurfaceVariant = c(0x3F4A3C), outline = c(0x6F7A6B), outlineVariant = c(0xBECAB8),
    inverseSurface = c(0x2A3328), inverseOnSurface = c(0xEAF4E4), inversePrimary = c(0x00E546), surfaceTint = c(0x006E1C),
    surfaceDim = c(0xD4DDCE), surfaceBright = c(0xF3FCEC), surfaceContainerLowest = c(0xFFFFFF), surfaceContainerLow = c(0xEDF6E7),
    surfaceContainer = c(0xE7F1E1), surfaceContainerHigh = c(0xE2EBDC), surfaceContainerHighest = c(0xDCE5D6),
)

private val DefaultDark = darkColorScheme(
    primary = c(0x00E546), onPrimary = c(0x00390A), primaryContainer = c(0x005313), onPrimaryContainer = c(0x70FF77),
    secondary = c(0xA8D0B5), onSecondary = c(0x133724), secondaryContainer = c(0x2A4E3A), onSecondaryContainer = c(0xC3ECD0),
    tertiary = c(0x91D4BE), onTertiary = c(0x00382C), tertiaryContainer = c(0x005141), onTertiaryContainer = c(0xACF0DA),
    error = c(0xFFB4AB), onError = c(0x690005), errorContainer = c(0x93000A), onErrorContainer = c(0xFFDAD6),
    background = c(0x0E150C), onBackground = c(0xDCE5D6), surface = c(0x0E150C), onSurface = c(0xDCE5D6),
    surfaceVariant = c(0x3F4A3C), onSurfaceVariant = c(0xBECAB8), outline = c(0x889484), outlineVariant = c(0x3F4A3C),
    inverseSurface = c(0xDCE5D6), inverseOnSurface = c(0x2A3328), inversePrimary = c(0x006E1C), surfaceTint = c(0x00E546),
    surfaceDim = c(0x0E150C), surfaceBright = c(0x333B31), surfaceContainerLowest = c(0x081008), surfaceContainerLow = c(0x161E14),
    surfaceContainer = c(0x1A2218), surfaceContainerHigh = c(0x242C22), surfaceContainerHighest = c(0x2F372C),
)

private fun hsl(h: Float, s: Float, l: Float): Color = Color(ColorUtils.HSLToColor(floatArrayOf(((h % 360) + 360) % 360, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

/**
 * Approximates a Material 3 tonal scheme from any seed colour (hue-preserving HSL tones).
 * Good enough for an accent picker; the default green above uses the exact generated palette.
 */
fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.35f, 0.9f)
    val th = h + 60f // tertiary hue
    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, s, 0.36f), onPrimary = Color.White,
            primaryContainer = hsl(h, s, 0.88f), onPrimaryContainer = hsl(h, s, 0.12f),
            secondary = hsl(h, 0.22f, 0.40f), onSecondary = Color.White,
            secondaryContainer = hsl(h, 0.30f, 0.88f), onSecondaryContainer = hsl(h, 0.30f, 0.14f),
            tertiary = hsl(th, 0.30f, 0.38f), onTertiary = Color.White,
            tertiaryContainer = hsl(th, 0.40f, 0.88f), onTertiaryContainer = hsl(th, 0.40f, 0.13f),
            background = hsl(h, 0.30f, 0.97f), onBackground = hsl(h, 0.15f, 0.10f),
            surface = hsl(h, 0.30f, 0.97f), onSurface = hsl(h, 0.15f, 0.10f),
            surfaceVariant = hsl(h, 0.18f, 0.88f), onSurfaceVariant = hsl(h, 0.12f, 0.27f),
            outline = hsl(h, 0.08f, 0.45f), outlineVariant = hsl(h, 0.12f, 0.78f),
            surfaceContainerLowest = Color.White, surfaceContainerLow = hsl(h, 0.30f, 0.945f),
            surfaceContainer = hsl(h, 0.28f, 0.925f), surfaceContainerHigh = hsl(h, 0.25f, 0.905f),
            surfaceContainerHighest = hsl(h, 0.22f, 0.885f),
            inversePrimary = hsl(h, s, 0.75f), surfaceTint = hsl(h, s, 0.36f),
            error = DefaultLight.error, onError = DefaultLight.onError,
            errorContainer = DefaultLight.errorContainer, onErrorContainer = DefaultLight.onErrorContainer,
        )
    } else {
        darkColorScheme(
            primary = hsl(h, s, 0.76f), onPrimary = hsl(h, s, 0.14f),
            primaryContainer = hsl(h, s, 0.24f), onPrimaryContainer = hsl(h, s, 0.88f),
            secondary = hsl(h, 0.30f, 0.78f), onSecondary = hsl(h, 0.30f, 0.16f),
            secondaryContainer = hsl(h, 0.25f, 0.26f), onSecondaryContainer = hsl(h, 0.30f, 0.88f),
            tertiary = hsl(th, 0.40f, 0.78f), onTertiary = hsl(th, 0.40f, 0.16f),
            tertiaryContainer = hsl(th, 0.30f, 0.26f), onTertiaryContainer = hsl(th, 0.40f, 0.88f),
            background = hsl(h, 0.20f, 0.07f), onBackground = hsl(h, 0.12f, 0.88f),
            surface = hsl(h, 0.20f, 0.07f), onSurface = hsl(h, 0.12f, 0.88f),
            surfaceVariant = hsl(h, 0.12f, 0.26f), onSurfaceVariant = hsl(h, 0.12f, 0.76f),
            outline = hsl(h, 0.08f, 0.55f), outlineVariant = hsl(h, 0.12f, 0.26f),
            surfaceContainerLowest = hsl(h, 0.20f, 0.04f), surfaceContainerLow = hsl(h, 0.20f, 0.09f),
            surfaceContainer = hsl(h, 0.20f, 0.11f), surfaceContainerHigh = hsl(h, 0.18f, 0.14f),
            surfaceContainerHighest = hsl(h, 0.16f, 0.17f),
            inversePrimary = hsl(h, s, 0.36f), surfaceTint = hsl(h, s, 0.76f),
            error = DefaultDark.error, onError = DefaultDark.onError,
            errorContainer = DefaultDark.errorContainer, onErrorContainer = DefaultDark.onErrorContainer,
        )
    }
}

private val ExpenserShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun ExpenserTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        settings.accent.isNotBlank() -> schemeFromSeed(
            runCatching { Color(android.graphics.Color.parseColor(settings.accent)) }.getOrDefault(Color(0xFF4CAF50)), dark,
        )
        dark -> DefaultDark
        else -> DefaultLight
    }
    val income = if (dark) IncomeColors(c(0x71DC92), c(0x00391A), c(0x005229), c(0x8DF9AC))
    else IncomeColors(c(0x006D38), Color.White, c(0x8DF9AC), c(0x00210D))

    val scale = when (settings.fontSize) { "sm" -> 0.92f; "lg" -> 1.1f; else -> 1f }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalIncomeColors provides income,
        LocalDensity provides Density(base.density, base.fontScale * scale),
    ) {
        MaterialTheme(colorScheme = scheme, shapes = ExpenserShapes, content = content)
    }
}
