@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.gnort67.wallpaperswitcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

private const val MAX_DIM = 0.6f
private const val REPO_URL = "https://github.com/minhtrong67/my-kotlin-apps"
private const val PROFILE_URL = "https://github.com/minhtrong67"

// ───────────── Theme (Material 3 Expressive: vibrant tonal palette + expressive motion) ─────────────
private val LightScheme = lightColorScheme(
    primary = Color(0xFF5A4FCF), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF), onPrimaryContainer = Color(0xFF170065),
    secondary = Color(0xFF5E5C71), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4DFF9), onSecondaryContainer = Color(0xFF1B192C),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF9B), onTertiaryContainer = Color(0xFF271900),
    background = Color(0xFFFCF8FF), onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFCF8FF), onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFE5E0EC), onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F2FF),
    surfaceContainer = Color(0xFFF0ECFA), surfaceContainerHigh = Color(0xFFEAE6F4),
    surfaceContainerHighest = Color(0xFFE4E0EE),
    outline = Color(0xFF787680), outlineVariant = Color(0xFFC9C5D0)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC7BFFF), onPrimary = Color(0xFF2A1B9D),
    primaryContainer = Color(0xFF4137B5), onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC8C3DC), onSecondary = Color(0xFF302E41),
    secondaryContainer = Color(0xFF474559), onSecondaryContainer = Color(0xFFE4DFF9),
    tertiary = Color(0xFFF0BF48), onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5B4300), onTertiaryContainer = Color(0xFFFFDF9B),
    background = Color(0xFF141318), onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF141318), onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF47464F), onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainer = Color(0xFF201F25), surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A),
    outline = Color(0xFF928F99), outlineVariant = Color(0xFF47464F)
)

@Composable
fun AppTheme(p: Prefs, content: @Composable () -> Unit) {
    val dark = when (p.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val scheme = when {
        p.dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    (ctx as? ComponentActivity)?.let { a ->
        LaunchedEffect(dark) {
            val st = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            a.enableEdgeToEdge(st, st)
        }
    }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), content = content)
}

// ───────────── Time helpers ─────────────
/** Wall-clock tick (every 30 s) so time-dependent UI stays current while the app is open. */
@Composable
internal fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }
    return now
}

/** Effective mode: system theme, or the user's custom clock times. */
@Composable
internal fun rememberIsDark(p: Prefs): Boolean {
    val sys = isSystemInDarkTheme()
    val tick = rememberNow()
    return if (p.modeSource == 1) {
        remember(tick, p.lightFrom, p.darkFrom) {
            WallpaperApplier.isDarkAt(p.lightFrom, p.darkFrom, WallpaperApplier.minutesNow())
        }
    } else sys
}

internal fun fmtTime(ctx: Context, minutes: Int): String {
    val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60) }
    return DateFormat.getTimeFormat(ctx).format(c.time)
}

// ───────────── Root ─────────────
@Composable
fun AppRoot(p: Prefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    val snack = remember { SnackbarHostState() }
    val pager = rememberPagerState { 4 }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var version by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Triple<String, Boolean, Int>?>(null) }
    var crop by remember { mutableStateOf<CropJob?>(null) }

    fun say(id: Int) {
        scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(ctx.getString(id)) }
    }
    /** Re-evaluates the wallpaper in the background service after any relevant change. */
    fun refresh() {
        if (p.autoSwitch) { p.lastApplied = -1; ThemeWatcherService.start(ctx) }
    }
    fun applyNow(dark: Boolean) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { WallpaperApplier.apply(ctx, dark) }
            if (ok) p.markApplied(WallpaperApplier.stateKey(ctx, dark)) else say(R.string.msg_failed)
        }
    }
    fun goTo(page: Int) { scope.launch { pager.animateScrollToPage(page) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val t = pending
        if (uri != null && t != null) crop = CropJob(uri, t.first, t.second, t.third)
    }
    fun pick(dark: Boolean, slot: Int) {
        pending = Triple(p.activeId, dark, slot)
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun clear(dark: Boolean, slot: Int) {
        WallpaperApplier.file(ctx, p.activeId, dark, slot).delete()
        version++
        refresh()
    }
    fun select(id: String) {
        p.selectSet(id)
        version++
        if (p.autoSwitch) refresh() else applyNow(WallpaperApplier.isDarkNow(ctx))
    }
    fun create(name: String) {
        p.selectSet(p.addSet(name))
        version++
        goTo(0)
    }
    fun delete(id: String) {
        val wasActive = p.activeId == id
        WallpaperApplier.deleteSet(ctx, id)
        p.removeSet(id)
        version++
        if (wasActive) refresh()
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun setAuto(on: Boolean) {
        p.updateAuto(on)
        if (on) {
            if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            ThemeWatcherService.start(ctx)
        } else {
            ThemeWatcherService.stop(ctx)
            p.serviceMarker = false
            p.updateKilledHint(false)
        }
    }

    val isDark = rememberIsDark(p)
    val subtitle = stringResource(
        R.string.top_subtitle, p.activeSet.label(),
        stringResource(if (isDark) R.string.mode_dark else R.string.mode_light)
    )
    val tabs = listOf(
        Icons.Rounded.Home to R.string.nav_home,
        Icons.Rounded.PhotoLibrary to R.string.nav_sets,
        Icons.Rounded.Schedule to R.string.nav_schedule,
        Icons.Rounded.Settings to R.string.nav_settings
    )
    AnimatedContent(
        targetState = crop,
        contentKey = { it != null },
        transitionSpec = {
            (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.92f)) togetherWith
                fadeOut(motion.fastEffectsSpec())
        },
        label = "crop"
    ) { job ->
        if (job != null) {
            CropScreen(
                job,
                onFinish = { ok ->
                    crop = null
                    if (ok) { version++; refresh() } else say(R.string.msg_failed)
                },
                onCancel = { crop = null }
            )
        } else {
            BackHandler(enabled = pager.currentPage != 0) { goTo(0) }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                        subtitle = { Text(subtitle) },
                        scrollBehavior = scrollBehavior
                    )
                },
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    ShortNavigationBar {
                        tabs.forEachIndexed { i, (icon, label) ->
                            ShortNavigationBarItem(
                                selected = pager.targetPage == i,
                                onClick = { goTo(i) },
                                icon = { Icon(icon, null) },
                                label = { Text(stringResource(label), maxLines = 1) }
                            )
                        }
                    }
                }
            ) { pad ->
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    beyondViewportPageCount = 3
                ) { page ->
                    if (page == 1) {
                        SetsScreen(p, version, { select(it) }, { create(it) }, { id, n -> p.renameSet(id, n) }, { delete(it) })
                    } else Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> HomeScreen(
                                p, version, { setAuto(it) }, { applyNow(it) }, { pick(it, 0) }, { clear(it, 0) },
                                { p.updateTarget(it); refresh() },
                                { p.updateKilledHint(false); openAppInfo(ctx) }, { p.updateKilledHint(false) },
                                { goTo(1) }
                            )
                            2 -> ScheduleScreen(
                                p, version,
                                { p.updateModeSource(if (it) 1 else 0); refresh() },
                                { p.updateLightFrom(it); refresh() },
                                { p.updateDarkFrom(it); refresh() },
                                { p.updateSchedule(it); refresh() },
                                { d, s -> pick(d, s) }, { d, s -> clear(d, s) }
                            )
                            else -> SettingsScreen(p) { p.updateDim(it); refresh() }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Home ─────────────
@Composable
private fun HomeScreen(
    p: Prefs, version: Int, onAuto: (Boolean) -> Unit, onApply: (Boolean) -> Unit,
    onPick: (Boolean) -> Unit, onClear: (Boolean) -> Unit, onTarget: (Int) -> Unit,
    onFix: () -> Unit, onDismissHint: () -> Unit, onOpenSets: () -> Unit
) {
    val ctx = LocalContext.current
    val dark = rememberIsDark(p)
    val ready = remember(version, p.activeId) {
        WallpaperApplier.file(ctx, p.activeId, false).exists() && WallpaperApplier.file(ctx, p.activeId, true).exists()
    }
    val time = remember(p.lastAppliedAt) { DateFormat.getTimeFormat(ctx).format(Date(p.lastAppliedAt)) }
    val mode = stringResource(R.string.status_now, stringResource(if (dark) R.string.mode_dark else R.string.mode_light))
    val status = when {
        !p.autoSwitch -> null
        !ThemeWatcherService.running -> stringResource(R.string.service_stopped)
        p.lastAppliedAt > 0L -> stringResource(R.string.service_active_at, time)
        else -> stringResource(R.string.service_active)
    }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShapeIcon(
                if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                MaterialShapes.Cookie9Sided.toShape(),
                MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, 56.dp
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.auto_switch), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.auto_switch_desc), style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(mode, status).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
                )
            }
            AppSwitch(p.autoSwitch, onChange = onAuto)
        }
    }
    if (p.autoSwitch && p.killedHint) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.killed_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.killed_text), style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = onFix, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.open_app_info)) }
                    TextButton(onClick = onDismissHint, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.dismiss)) }
                }
            }
        }
    }
    if (!ready) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Image, null)
                Text(stringResource(R.string.home_hint), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    AssistChip(
        onClick = onOpenSets,
        label = { Text(p.activeSet.label(), maxLines = 1) },
        leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp)) }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WallpaperCard(Modifier.weight(1f), p.activeId, false, !dark, version, 0f, onPick, onApply, onClear)
        WallpaperCard(Modifier.weight(1f), p.activeId, true, dark, version, p.dim, onPick, onApply, onClear)
    }
    Text(stringResource(R.string.apply_to), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Segmented(
        listOf(R.string.target_both, R.string.target_home, R.string.target_lock).map { stringResource(it) },
        p.target, onTarget
    )
}

@Composable
private fun WallpaperCard(
    modifier: Modifier, setId: String, dark: Boolean, active: Boolean, version: Int, dim: Float,
    onPick: (Boolean) -> Unit, onApply: (Boolean) -> Unit, onClear: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val file = remember(setId, dark) { WallpaperApplier.file(ctx, setId, dark, 0) }
    val bmp by produceState<ImageBitmap?>(null, version, setId) { value = withContext(Dispatchers.IO) { loadPreview(file) } }
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = motion.defaultEffectsSpec(), label = "card"
    )
    Card(modifier, shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null)
                Text(
                    stringResource(if (dark) R.string.dark_wallpaper else R.string.light_wallpaper),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(bmp, animationSpec = motion.defaultEffectsSpec(), label = "preview") { b ->
                    if (b != null) Box(Modifier.fillMaxSize()) {
                        Image(
                            b, stringResource(if (dark) R.string.cd_preview_dark else R.string.cd_preview_light),
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)).clickable { onClear(dark) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(18.dp), tint = Color.White) }
                    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FilledTonalButton(
                onClick = { onPick(dark) }, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(stringResource(if (bmp == null) R.string.choose else R.string.change), maxLines = 1) }
            OutlinedButton(
                onClick = { onApply(dark) }, modifier = Modifier.fillMaxWidth(), enabled = bmp != null,
                shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text(stringResource(R.string.apply_now), maxLines = 1) }
        }
    }
}

/** Small, memory-cheap preview: power-of-two subsampling + RGB_565. */
internal fun loadPreview(f: File, target: Int = 480): ImageBitmap? {
    if (!f.exists()) return null
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var s = 1
    while (o.outWidth / (s * 2) >= target) s *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.RGB_565 }
    return BitmapFactory.decodeFile(f.path, opts)?.asImageBitmap()
}

// ───────────── Settings ─────────────
@Composable
private fun SettingsScreen(p: Prefs, onDim: (Float) -> Unit) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }

    Group(R.string.settings_appearance) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium)
            Segmented(
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).map { stringResource(it) },
                p.themeMode, p::updateTheme
            )
        }
        GroupDivider()
        SwitchItem(R.string.dynamic_color, R.string.dynamic_color_desc, p.dynamicColor,
            Build.VERSION.SDK_INT >= 31, p::updateDynamic)
    }

    Group(R.string.language) {
        Box(Modifier.padding(20.dp)) {
            Segmented(
                listOf(R.string.lang_system, R.string.lang_en, R.string.lang_vi).map { stringResource(it) },
                p.language, p::updateLanguage
            )
        }
    }

    Group(R.string.wallpaper_section) {
        val slider = remember { SliderState(value = p.dim / MAX_DIM, steps = 11) }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dim_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.dim_value, (slider.value * MAX_DIM * 100).roundToInt()),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(state = slider, onValueChange = {}, onValueChangeFinished = { onDim(slider.value * MAX_DIM) })
            Text(
                stringResource(R.string.dim_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Group(R.string.background) {
        SwitchItem(R.string.start_on_boot, R.string.start_on_boot_desc, p.startOnBoot, true, p::updateBoot)
        GroupDivider()
        SettingRow(
            title = stringResource(R.string.keep_alive), desc = stringResource(R.string.keep_alive_desc),
            modifier = Modifier.clickable { openAppInfo(ctx) },
            trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
    }

    Group(R.string.about) {
        SettingRow(
            title = stringResource(R.string.app_name), desc = stringResource(R.string.about_version, version),
            leading = {
                ShapeIcon(
                    Icons.Rounded.Wallpaper, MaterialShapes.Cookie9Sided.toShape(),
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        )
        GroupDivider()
        LinkRow(Icons.Rounded.Code, R.string.source_code, R.string.source_code_desc, REPO_URL)
        GroupDivider()
        LinkRow(Icons.Rounded.Person, R.string.author, R.string.author_desc, PROFILE_URL)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: Int, desc: Int, url: String) {
    val ctx = LocalContext.current
    SettingRow(
        title = stringResource(title), desc = stringResource(desc),
        modifier = Modifier.clickable { openUrl(ctx, url) },
        leading = {
            ShapeIcon(icon, CircleShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        },
        trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

// ───────────── Shared building blocks ─────────────
@Composable
internal fun Group(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(title), Modifier.padding(start = 20.dp),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            content = content
        )
    }
}

@Composable
internal fun GroupDivider() =
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
internal fun SettingRow(
    title: String, desc: String?, modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc != null) Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun ShapeIcon(icon: ImageVector, shape: Shape, container: Color, content: Color, size: Dp = 48.dp) {
    Box(Modifier.size(size).clip(shape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = content)
    }
}

@Composable
private fun rememberToggle(onChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(onChange) {
        { v: Boolean ->
            haptic.performHapticFeedback(if (v) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onChange(v)
        }
    }
}

@Composable
internal fun AppSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val toggle = rememberToggle(onChange)
    Switch(
        checked = checked, onCheckedChange = toggle, enabled = enabled,
        thumbContent = if (checked) {
            { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
        } else null
    )
}

@Composable
internal fun SwitchItem(title: Int, desc: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val toggle = rememberToggle(onChange)
    SettingRow(
        title = stringResource(title), desc = stringResource(desc),
        modifier = Modifier.clickable(enabled = enabled) { toggle(!checked) },
        trailing = {
            Switch(
                checked = checked, onCheckedChange = toggle, enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        }
    )
}

@Composable
internal fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected == i, { onSelect(i) },
                SegmentedButtonDefaults.itemShape(i, options.size)
            ) { Text(label, maxLines = 1) }
        }
    }
}

internal fun openAppInfo(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
    )
}

internal fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
