package com.gnort67.calendarpro.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.data.AppSettings
import java.time.LocalDate

/** Màu bổ sung ngoài ColorScheme chuẩn: ngày chay (xanh lá) và ngày lễ (vàng). */
@Immutable
class ExtraColors(
    val chay: Color, val onChay: Color, val chayContainer: Color, val onChayContainer: Color,
    val holiday: Color, val onHoliday: Color, val holidayContainer: Color, val onHolidayContainer: Color
)

val LocalExtraColors = staticCompositionLocalOf<ExtraColors> { error("ExtraColors chưa được cung cấp") }
val LocalIsDark = staticCompositionLocalOf { false }

val MaterialTheme.extra: ExtraColors @androidx.compose.runtime.Composable get() = LocalExtraColors.current

class FontOption(val id: String, val label: String, val noteVi: String, val noteEn: String, val family: FontFamily)

val FONT_OPTIONS: List<FontOption> = listOf(
    FontOption("system", "System", "Font của hệ điều hành", "Your device's system font", FontFamily.Default),
    FontOption("google-sans-flex", "Google Sans Flex", "Font của Material 3 Expressive", "The Material 3 Expressive font",
        FontFamily(
            Font(R.font.google_sans_flex_regular, FontWeight.Normal),
            Font(R.font.google_sans_flex_medium, FontWeight.Medium),
            Font(R.font.google_sans_flex_bold, FontWeight.Bold)
        )),
    FontOption("roboto-flex", "Roboto Flex", "Biến thể linh hoạt của Roboto", "A flexible take on Roboto",
        FontFamily(Font(R.font.roboto_flex_variable, FontWeight.Normal))),
    FontOption("roboto", "Roboto", "Font Material cổ điển", "The classic Material font",
        FontFamily(
            Font(R.font.roboto_regular, FontWeight.Normal),
            Font(R.font.roboto_medium, FontWeight.Medium),
            Font(R.font.roboto_bold, FontWeight.Bold)
        )),
    FontOption("serif", "Serif", "Chữ có chân, dễ đọc đoạn dài", "Serif, comfortable for long text", FontFamily.Serif),
    FontOption("inter", "Inter", "Gọn, dễ đọc trên màn hình", "Clean and screen-friendly",
        FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal),
            Font(R.font.inter_medium, FontWeight.Medium),
            Font(R.font.inter_bold, FontWeight.Bold)
        )),
    FontOption("be-vietnam-pro", "Be Vietnam Pro", "Thiết kế riêng cho tiếng Việt", "Designed for Vietnamese",
        FontFamily(
            Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
            Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
            Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
            Font(R.font.be_vietnam_pro_bold, FontWeight.Bold)
        ))
)

fun fontOf(id: String) = FONT_OPTIONS.firstOrNull { it.id == id } ?: FONT_OPTIONS[0]

fun paletteOf(id: String) = PALETTES.firstOrNull { it.id == id } ?: PALETTES.first { it.id == "blue" }

/** Màu xoay vòng theo ngày (khi bật "Đổi màu ngẫu nhiên mỗi ngày"). */
fun dailyPalette(date: LocalDate = LocalDate.now()) = PALETTES[(date.toEpochDay() % PALETTES.size).toInt()]

private fun typographyFor(family: FontFamily): Typography {
    val t = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = t.displayLarge.f(), displayMedium = t.displayMedium.f(), displaySmall = t.displaySmall.f(),
        headlineLarge = t.headlineLarge.f(), headlineMedium = t.headlineMedium.f(), headlineSmall = t.headlineSmall.f(),
        titleLarge = t.titleLarge.f(), titleMedium = t.titleMedium.f(), titleSmall = t.titleSmall.f(),
        bodyLarge = t.bodyLarge.f(), bodyMedium = t.bodyMedium.f(), bodySmall = t.bodySmall.f(),
        labelLarge = t.labelLarge.f(), labelMedium = t.labelMedium.f(), labelSmall = t.labelSmall.f()
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)
)

@Composable
private fun anim(c: Long): Color {
    val s: State<Color> = animateColorAsState(Color(c.toInt()), tween(450), label = "theme-color")
    return s.value
}

@Composable
private fun schemeFor(p: PaletteOption, dark: Boolean): Pair<ColorScheme, ExtraColors> {
    val a = if (dark) p.dark else p.light
    // mapIndexed là inline nên gọi được hàm @Composable bên trong
    val animated = PALETTE_ROLES.mapIndexed { i, n -> n to anim(a[i]) }.toMap()
    fun role(name: String): Color = animated.getValue(name)
    val surface = role("surface")
    val highest = role("surface-highest")
    val scheme = lightColorScheme(
        primary = role("primary"), onPrimary = role("on-primary"),
        primaryContainer = role("primary-container"), onPrimaryContainer = role("on-primary-container"),
        inversePrimary = role("inverse-primary"),
        secondary = role("secondary"), onSecondary = role("on-secondary"),
        secondaryContainer = role("secondary-container"), onSecondaryContainer = role("on-secondary-container"),
        tertiary = role("tertiary"), onTertiary = role("on-tertiary"),
        tertiaryContainer = role("tertiary-container"), onTertiaryContainer = role("on-tertiary-container"),
        background = surface, onBackground = role("on-surface"),
        surface = surface, onSurface = role("on-surface"),
        surfaceVariant = highest, onSurfaceVariant = role("on-surface-variant"),
        surfaceTint = role("surface-tint"),
        inverseSurface = role("inverse-surface"), inverseOnSurface = role("inverse-on-surface"),
        error = role("error"), onError = role("on-error"),
        errorContainer = role("error-container"), onErrorContainer = role("on-error-container"),
        outline = role("outline"), outlineVariant = role("outline-variant"), scrim = role("scrim"),
        surfaceBright = role("surface-bright"), surfaceDim = role("surface-dim"),
        surfaceContainer = role("surface-container"), surfaceContainerHigh = role("surface-high"),
        surfaceContainerHighest = highest, surfaceContainerLow = role("surface-low"),
        surfaceContainerLowest = role("surface-lowest")
    )
    val extra = ExtraColors(
        role("chay"), role("on-chay"), role("chay-container"), role("on-chay-container"),
        role("holiday"), role("on-holiday"), role("holiday-container"), role("on-holiday-container")
    )
    return scheme to extra
}

@Composable
fun CalendarProTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val palette = if (settings.dailyRotation) dailyPalette() else paletteOf(settings.colorId)
    val (scheme, extra) = schemeFor(palette, dark)
    val typography = typographyFor(fontOf(settings.fontFamily).family)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalExtraColors provides extra,
        LocalIsDark provides dark,
        LocalDensity provides Density(base.density, base.fontScale * settings.fontScale)
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = AppShapes, content = content)
    }
}
