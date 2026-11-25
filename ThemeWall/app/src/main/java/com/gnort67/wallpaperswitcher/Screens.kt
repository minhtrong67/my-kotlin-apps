@file:OptIn(ExperimentalMaterial3Api::class)

package com.gnort67.wallpaperswitcher

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

// ───────────── Theme ─────────────
@Composable
fun AppTheme(p: Prefs, content: @Composable () -> Unit) {
    val dark = when (p.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val scheme = when {
        p.dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(
            primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF08218A),
            primaryContainer = Color(0xFF293CA0), onPrimaryContainer = Color(0xFFDEE0FF)
        )
        else -> lightColorScheme(
            primary = Color(0xFF4355B9), onPrimary = Color.White,
            primaryContainer = Color(0xFFDEE0FF), onPrimaryContainer = Color(0xFF00105C)
        )
    }
    (ctx as? ComponentActivity)?.let { a ->
        LaunchedEffect(dark) {
            val st = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            a.enableEdgeToEdge(st, st)
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ───────────── Root ─────────────
@Composable
fun AppRoot(p: Prefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val pager = rememberPagerState { 3 }
    var version by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val t = pending
        if (uri != null && t != null) crop = CropJob(uri, t.first, t.second)
    }
    fun pick(dark: Boolean, slot: Int) {
        pending = dark to slot
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun clear(dark: Boolean, slot: Int) {
        WallpaperApplier.file(ctx, dark, slot).delete()
        version++
        refresh()
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

    val tabs = listOf(
        Icons.Rounded.Wallpaper to R.string.nav_home,
        Icons.Rounded.Schedule to R.string.nav_schedule,
        Icons.Rounded.Settings to R.string.nav_settings
    )
    AnimatedContent(
        targetState = crop,
        contentKey = { it != null },
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.94f)) togetherWith fadeOut(tween(160))
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
            BackHandler(enabled = pager.currentPage != 0) { scope.launch { pager.animateScrollToPage(0) } }
            Scaffold(
                topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(
                                pager.targetPage == i,
                                { scope.launch { pager.animateScrollToPage(i) } },
                                { Icon(icon, null) },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                }
            ) { pad ->
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    beyondViewportPageCount = 2
                ) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> HomeScreen(
                                p, version, { setAuto(it) }, { applyNow(it) }, { pick(it, 0) }, { clear(it, 0) },
                                { p.updateTarget(it); refresh() },
                                { p.updateKilledHint(false); openAppInfo(ctx) }, { p.updateKilledHint(false) }
                            )
                            1 -> ScheduleScreen(p, version, { p.updateSchedule(it); refresh() }, { d, s -> pick(d, s) }, { d, s -> clear(d, s) })
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
    onFix: () -> Unit, onDismissHint: () -> Unit
) {
    val ctx = LocalContext.current
    val sysDark = isSystemInDarkTheme()
    val desc = stringResource(R.string.auto_switch_desc)
    val mode = stringResource(R.string.status_now,
        stringResource(if (sysDark) R.string.mode_dark else R.string.mode_light))
    val time = remember(p.lastAppliedAt) { DateFormat.getTimeFormat(ctx).format(Date(p.lastAppliedAt)) }
    val status = when {
        !p.autoSwitch -> null
        !ThemeWatcherService.running -> stringResource(R.string.service_stopped)
        p.lastAppliedAt > 0L -> stringResource(R.string.service_active_at, time)
        else -> stringResource(R.string.service_active)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { Icon(if (sysDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null) },
            headlineContent = { Text(stringResource(R.string.auto_switch), style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(listOfNotNull(desc, mode, status).joinToString("\n")) },
            trailingContent = { Switch(p.autoSwitch, onAuto) }
        )
    }
    if (p.autoSwitch && p.killedHint) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.killed_title), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.killed_text), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Row {
                    TextButton(onFix) { Text(stringResource(R.string.open_app_info)) }
                    TextButton(onDismissHint) { Text(stringResource(R.string.dismiss)) }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WallpaperCard(Modifier.weight(1f), false, !sysDark, version, 0f, onPick, onApply, onClear)
        WallpaperCard(Modifier.weight(1f), true, sysDark, version, p.dim, onPick, onApply, onClear)
    }
    Text(stringResource(R.string.apply_to), style = MaterialTheme.typography.titleSmall)
    Segmented(
        listOf(R.string.target_both, R.string.target_home, R.string.target_lock).map { stringResource(it) },
        p.target, onTarget
    )
}

@Composable
private fun WallpaperCard(
    modifier: Modifier, dark: Boolean, active: Boolean, version: Int, dim: Float,
    onPick: (Boolean) -> Unit, onApply: (Boolean) -> Unit, onClear: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val file = remember(dark) { WallpaperApplier.file(ctx, dark, 0) }
    val bmp by produceState<ImageBitmap?>(null, version) { value = withContext(Dispatchers.IO) { loadPreview(file) } }
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(300), label = "card"
    )
    Card(modifier, colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null)
                Text(
                    stringResource(if (dark) R.string.dark_wallpaper else R.string.light_wallpaper),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(bmp, animationSpec = tween(300), label = "preview") { b ->
                    if (b != null) Box(Modifier.fillMaxSize()) {
                        Image(
                            b, stringResource(if (dark) R.string.cd_preview_dark else R.string.cd_preview_light),
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)).clickable { onClear(dark) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(16.dp), tint = Color.White) }
                    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FilledTonalButton({ onPick(dark) }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(stringResource(if (bmp == null) R.string.choose else R.string.change), maxLines = 1)
            }
            OutlinedButton(
                { onApply(dark) }, Modifier.fillMaxWidth(), enabled = bmp != null,
                contentPadding = PaddingValues(horizontal = 8.dp)
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
    Section(R.string.appearance)
    Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleSmall)
    Segmented(
        listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).map { stringResource(it) },
        p.themeMode, p::updateTheme
    )
    SwitchRow(R.string.dynamic_color, R.string.dynamic_color_desc, p.dynamicColor,
        Build.VERSION.SDK_INT >= 31, p::updateDynamic)

    Section(R.string.wallpaper_section)
    var tmp by remember { mutableFloatStateOf(p.dim) }
    Text(stringResource(R.string.dim_label, (tmp * 100).roundToInt()), style = MaterialTheme.typography.titleSmall)
    Slider(
        value = tmp, onValueChange = { tmp = it }, valueRange = 0f..0.6f, steps = 11,
        onValueChangeFinished = { onDim(tmp) }
    )
    Text(
        stringResource(R.string.dim_desc), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Section(R.string.language)
    Segmented(
        listOf(R.string.lang_system, R.string.lang_en, R.string.lang_vi).map { stringResource(it) },
        p.language, p::updateLanguage
    )

    Section(R.string.behavior)
    SwitchRow(R.string.start_on_boot, R.string.start_on_boot_desc, p.startOnBoot, true, p::updateBoot)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tip_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.tip_text), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                )
            }) { Text(stringResource(R.string.open_app_info)) }
        }
    }

    Section(R.string.about)
    Text(
        stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun Section(res: Int) =
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

@Composable
internal fun SwitchRow(title: Int, desc: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(stringResource(title)) },
            supportingContent = { Text(stringResource(desc)) },
            trailingContent = { Switch(checked, onChange, enabled = enabled) }
        )
    }
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

internal fun openAppInfo(ctx: android.content.Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
    )
}
