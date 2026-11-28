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
data class IncomeColors(val income: Color, val onIncome: Color, val container: Color, val onContainer: Color, val expense: Color = Color(0xFFC62828))

val LocalIncomeColors = staticCompositionLocalOf {
    IncomeColors(Color(0xFF0B7A3B), Color.White, Color(0xFF8DF9AC), Color(0xFF00210D), Color(0xFFC62828))
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

// Default scheme: Material 3 blue (tonal palette of seed #0061A4 — M3 "baseline blue").
private val DefaultLight = lightColorScheme(
    primary = c(0x0061A4), onPrimary = c(0xFFFFFF), primaryContainer = c(0xD1E4FF), onPrimaryContainer = c(0x001D36),
    secondary = c(0x535F70), onSecondary = c(0xFFFFFF), secondaryContainer = c(0xD7E3F8), onSecondaryContainer = c(0x101C2B),
    tertiary = c(0x6B5778), onTertiary = c(0xFFFFFF), tertiaryContainer = c(0xF2DAFF), onTertiaryContainer = c(0x251431),
    error = c(0xC62828), onError = c(0xFFFFFF), errorContainer = c(0xFFDAD6), onErrorContainer = c(0x410002),
    background = c(0xF8F9FF), onBackground = c(0x191C20), surface = c(0xF8F9FF), onSurface = c(0x191C20),
    surfaceVariant = c(0xDFE2EB), onSurfaceVariant = c(0x43474E), outline = c(0x73777F), outlineVariant = c(0xC3C7CF),
    inverseSurface = c(0x2E3135), inverseOnSurface = c(0xEFF0F7), inversePrimary = c(0x9FCAFF), surfaceTint = c(0x0061A4),
    surfaceDim = c(0xD8DAE0), surfaceBright = c(0xF8F9FF), surfaceContainerLowest = c(0xFFFFFF), surfaceContainerLow = c(0xF2F3FA),
    surfaceContainer = c(0xECEEF4), surfaceContainerHigh = c(0xE6E8EE), surfaceContainerHighest = c(0xE1E2E8),
)

private val DefaultDark = darkColorScheme(
    primary = c(0x9FCAFF), onPrimary = c(0x003258), primaryContainer = c(0x00497D), onPrimaryContainer = c(0xD1E4FF),
    secondary = c(0xBBC7DB), onSecondary = c(0x253140), secondaryContainer = c(0x3B4858), onSecondaryContainer = c(0xD7E3F8),
    tertiary = c(0xD7BEE4), onTertiary = c(0x3B2948), tertiaryContainer = c(0x523F5F), onTertiaryContainer = c(0xF2DAFF),
    error = c(0xFF5449), onError = c(0x410002), errorContainer = c(0x93000A), onErrorContainer = c(0xFFDAD6),
    background = c(0x101418), onBackground = c(0xE1E2E8), surface = c(0x101418), onSurface = c(0xE1E2E8),
    surfaceVariant = c(0x43474E), onSurfaceVariant = c(0xC3C7CF), outline = c(0x8D9199), outlineVariant = c(0x43474E),
    inverseSurface = c(0xE1E2E8), inverseOnSurface = c(0x2E3135), inversePrimary = c(0x0061A4), surfaceTint = c(0x9FCAFF),
    surfaceDim = c(0x101418), surfaceBright = c(0x36393E), surfaceContainerLowest = c(0x0B0E12), surfaceContainerLow = c(0x191C20),
    surfaceContainer = c(0x1D2024), surfaceContainerHigh = c(0x272A2F), surfaceContainerHighest = c(0x32353A),
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

// M3 Expressive: rounder, more varied corner radii (up to 48dp).
private val ExpenserShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp),
)

/** M3 Expressive type: the baseline scale with heavier "emphasized" weights on headlines, titles and labels. */
private val ExpressiveTypography = with(androidx.compose.material3.Typography()) {
    val bold = androidx.compose.ui.text.font.FontWeight.Bold
    val semi = androidx.compose.ui.text.font.FontWeight.SemiBold
    copy(
        displayLarge = displayLarge.copy(fontWeight = bold), displayMedium = displayMedium.copy(fontWeight = bold), displaySmall = displaySmall.copy(fontWeight = bold),
        headlineLarge = headlineLarge.copy(fontWeight = bold), headlineMedium = headlineMedium.copy(fontWeight = bold), headlineSmall = headlineSmall.copy(fontWeight = bold),
        titleLarge = titleLarge.copy(fontWeight = bold), titleMedium = titleMedium.copy(fontWeight = semi), titleSmall = titleSmall.copy(fontWeight = semi),
        labelLarge = labelLarge.copy(fontWeight = semi), labelMedium = labelMedium.copy(fontWeight = semi),
    )
}

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
    val income = if (dark) IncomeColors(c(0x1E9E4A), c(0x00210D), c(0x005229), c(0x8DF9AC), c(0xD7363B))
    else IncomeColors(c(0x0B7A3B), Color.White, c(0x8DF9AC), c(0x00210D), c(0xC62828))

    val scale = when (settings.fontSize) { "sm" -> 0.92f; "lg" -> 1.1f; else -> 1f }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalIncomeColors provides income,
        LocalDensity provides Density(base.density, base.fontScale * scale),
    ) {
        MaterialTheme(colorScheme = scheme, shapes = ExpenserShapes, typography = ExpressiveTypography, content = content)
    }
}

/** Cross-fades every colour role when the theme / accent changes. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = androidx.compose.animation.core.tween<Color>(350)
    @Composable fun a(c: Color) = androidx.compose.animation.animateColorAsState(c, spec, label = "scheme").value
    return copy(
        primary = a(primary), onPrimary = a(onPrimary), primaryContainer = a(primaryContainer), onPrimaryContainer = a(onPrimaryContainer),
        secondary = a(secondary), onSecondary = a(onSecondary), secondaryContainer = a(secondaryContainer), onSecondaryContainer = a(onSecondaryContainer),
        tertiary = a(tertiary), onTertiary = a(onTertiary), tertiaryContainer = a(tertiaryContainer), onTertiaryContainer = a(onTertiaryContainer),
        error = a(error), errorContainer = a(errorContainer), onErrorContainer = a(onErrorContainer),
        background = a(background), onBackground = a(onBackground), surface = a(surface), onSurface = a(onSurface),
        surfaceVariant = a(surfaceVariant), onSurfaceVariant = a(onSurfaceVariant), outline = a(outline), outlineVariant = a(outlineVariant),
        surfaceContainerLowest = a(surfaceContainerLowest), surfaceContainerLow = a(surfaceContainerLow), surfaceContainer = a(surfaceContainer),
        surfaceContainerHigh = a(surfaceContainerHigh), surfaceContainerHighest = a(surfaceContainerHighest),
        inverseSurface = a(inverseSurface), inverseOnSurface = a(inverseOnSurface), inversePrimary = a(inversePrimary),
    )
}
