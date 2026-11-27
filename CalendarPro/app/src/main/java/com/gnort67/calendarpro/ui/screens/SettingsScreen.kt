package com.gnort67.calendarpro.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.data.AppSettings
import com.gnort67.calendarpro.notify.Reminders
import com.gnort67.calendarpro.ui.components.*
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.*

@Composable
fun SettingsScreen(
    s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onReset: () -> Unit,
    bottomInset: Dp, onMessage: (String) -> Unit, onTestNotify: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val landscape = isLandscape()
    var confirmReset by remember { mutableStateOf(false) }
    val resetDone = stringResource(R.string.settings_reset_done)

    val appearance: @Composable ColumnScope.() -> Unit = {
        ModeCard(s, onChange)
        LanguageCard(s, onChange)
        SizeCard(s, onChange)
    }
    val personal: @Composable ColumnScope.() -> Unit = {
        ColorSection(s, onChange, onMessage)
        NotifCard(s, onChange, onMessage, onTestNotify)
    }
    val footer: @Composable ColumnScope.() -> Unit = {
        FilledTonalButton(
            onClick = { confirmReset = true }, Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer)
        ) {
            AppIcon("restart_alt", size = 22.dp); Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.settings_reset_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        CreditsCard()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = bottomInset + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { SettingsTitle(); appearance(); footer() }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = bottomInset + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { personal() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomInset + 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) { SettingsTitle(); appearance(); personal(); footer() }
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
        text = { Text(stringResource(R.string.settings_reset_confirm_msg)) },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset(); onMessage(resetDone) }) { Text(stringResource(R.string.common_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
private fun SettingsTitle() {
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.settings_subtitle_full), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModeCard(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val cs = MaterialTheme.colorScheme
    SectionCard(R.string.settings_theme_mode_title, R.string.settings_theme_mode_subtitle) {
        SegmentedControl(
            listOf(
                SegOption(stringResource(R.string.settings_light), "light_mode"),
                SegOption(stringResource(R.string.settings_dark), "dark_mode"),
                SegOption(stringResource(R.string.settings_system), "devices")
            ),
            when (s.themeMode) { "light" -> 0; "dark" -> 1; else -> 2 },
            { i -> onChange { it.copy(themeMode = listOf("light", "dark", "system")[i]) } },
            container = cs.surfaceContainerHighest
        )
    }
}

@Composable
private fun LanguageCard(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    SectionCard(R.string.settings_language_title, R.string.settings_language_subtitle) {
        SegmentedControl(
            listOf(SegOption("Tiếng Việt"), SegOption("English")), if (s.language == "en") 1 else 0,
            { i -> onChange { it.copy(language = if (i == 0) "vi" else "en") } },
            container = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun SizeCard(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ids = listOf("nho", "vua", "lon")
    SectionCard(R.string.settings_font_size_title, R.string.settings_font_size_subtitle) {
        SegmentedControl(
            listOf(
                SegOption(stringResource(R.string.settings_size_small)), SegOption(stringResource(R.string.settings_size_medium)),
                SegOption(stringResource(R.string.settings_size_large))
            ),
            ids.indexOf(s.fontSize).coerceAtLeast(0), { i -> onChange { it.copy(fontSize = ids[i]) } },
            container = cs.surfaceContainerHighest
        )
        Text(
            "${stringResource(R.string.settings_font_size_preview)} ${stringResource(R.string.settings_font_size_preview_note)}",
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cs.surfaceContainerHighest).padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/** Thẻ có tiêu đề + mô tả ngắn và phần nội dung bên dưới. */
@Composable
private fun SectionCard(title: Int, subtitle: Int, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

// ---------------------------------------------------------------- Màu sắc

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onMessage: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val vi = s.language != "en"
    val dark = LocalIsDark.current
    val dynamicOn = s.dynamicColor && DYNAMIC_COLOR_SUPPORTED
    val daily = dailyPalette()
    val selectedId = when { dynamicOn -> ""; s.dailyRotation -> daily.id; else -> s.colorId }
    val changedTpl = stringResource(R.string.settings_color_changed, "%s")

    SectionCard(R.string.settings_color_title, R.string.settings_color_subtitle) {
        if (DYNAMIC_COLOR_SUPPORTED) {
            ToggleRow(
                stringResource(R.string.settings_material_you_title), stringResource(R.string.settings_material_you_subtitle),
                s.dynamicColor
            ) { on -> onChange { it.copy(dynamicColor = on, dailyRotation = if (on) false else it.dailyRotation) } }
        }
        FlowRow(
            Modifier.fillMaxWidth().alpha(if (dynamicOn) 0.4f else 1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PALETTES.forEach { p ->
                val sel = p.id == selectedId
                val arr = if (dark) p.dark else p.light
                val fill = Color(arr[PALETTE_ROLES.indexOf("primary")].toInt())
                val onFill = Color(arr[PALETTE_ROLES.indexOf("on-primary")].toInt())
                val scale by animateFloatAsState(if (sel) 1.1f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sw")
                val shape = if (sel) ExpressiveShapes.Cookie else CircleShape
                Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(52.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(shape).background(fill)
                            .pressable(shape) { onChange { it.copy(colorId = p.id, dynamicColor = false, dailyRotation = false) } },
                        contentAlignment = Alignment.Center
                    ) { if (sel) AppIcon("check", tint = onFill, size = 24.dp) }
                    Text(
                        if (vi) p.labelVi else p.label, Modifier.padding(top = 4.dp), maxLines = 1,
                        style = MaterialTheme.typography.labelSmall, color = if (sel) cs.onSurface else cs.onSurfaceVariant,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        ToggleRow(
            stringResource(R.string.settings_daily_rotation_title),
            if (s.dailyRotation) stringResource(R.string.settings_daily_rotation_on, if (vi) daily.labelVi else daily.label)
            else stringResource(R.string.settings_daily_rotation_off),
            s.dailyRotation
        ) { on -> onChange { it.copy(dailyRotation = on, dynamicColor = if (on) false else it.dynamicColor) } }
        FilledTonalButton(
            onClick = {
                val p = PALETTES.filter { it.id != selectedId }.random()
                onChange { it.copy(colorId = p.id, dynamicColor = false, dailyRotation = false) }
                onMessage(changedTpl.replace("%s", if (vi) p.labelVi else p.label))
            },
            Modifier.fillMaxWidth().height(52.dp)
        ) { AppIcon("shuffle", size = 20.dp); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_random_color_now)) }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onToggle)
    }
}

// ---------------------------------------------------------------- Thông báo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifCard(
    s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit,
    onMessage: (String) -> Unit, onTest: () -> Unit
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var timeDialog by remember { mutableStateOf(false) }
    val denied = stringResource(R.string.settings_notif_denied)
    val timeText = "%02d:%02d".format(s.notifyHour, s.notifyMinute)

    fun openSystemSettings() {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onChange { it.copy(notifyEnabled = true) } else onMessage(denied)
    }
    fun enable() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        when {
            needsPermission -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !Reminders.canNotify(ctx) -> { onMessage(denied); openSystemSettings() }
            else -> onChange { it.copy(notifyEnabled = true) }
        }
    }
    val blocked = s.notifyEnabled && !Reminders.canNotify(ctx)

    SectionCard(R.string.settings_notif_title, R.string.settings_notif_subtitle) {
        ToggleRow(
            stringResource(R.string.settings_notif_daily),
            if (s.notifyEnabled) stringResource(R.string.settings_notif_on_at, timeText) else stringResource(R.string.settings_notif_off),
            s.notifyEnabled
        ) { on -> if (on) enable() else onChange { it.copy(notifyEnabled = false) } }

        AnimatedVisibility(
            s.notifyEnabled, enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().pressable(RoundedCornerShape(22.dp)) { timeDialog = true }
                        .background(cs.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon("schedule", tint = cs.onSurfaceVariant)
                    Text(stringResource(R.string.settings_notif_time), Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(timeText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = cs.primary)
                }
                FilledTonalButton(onClick = onTest, Modifier.fillMaxWidth().height(52.dp)) {
                    AppIcon("notifications", size = 20.dp); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_notif_test))
                }
                if (blocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(denied, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = cs.error)
                        TextButton(onClick = { openSystemSettings() }) { Text(stringResource(R.string.settings_notif_open)) }
                    }
                }
            }
        }
    }

    if (timeDialog) {
        val state = rememberTimePickerState(s.notifyHour, s.notifyMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { timeDialog = false },
            title = { Text(stringResource(R.string.settings_notif_time)) },
            text = {
                Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    TimePicker(state, layoutType = if (isLandscape()) TimePickerLayoutType.Horizontal else TimePickerLayoutType.Vertical)
                }
            },
            confirmButton = {
                TextButton(onClick = { onChange { it.copy(notifyHour = state.hour, notifyMinute = state.minute) }; timeDialog = false }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = { TextButton(onClick = { timeDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

// ---------------------------------------------------------------- Credits

@Composable
private fun CreditsCard() {
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp), color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
        Box {
            BlobDecoration(Modifier.matchParentSize(), cs.primary)
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Pill(stringResource(R.string.settings_credits_heading), cs.surfaceContainerHighest.copy(alpha = 0.5f), cs.onPrimaryContainer, "favorite")
                Credit("M", "minhtrong67", stringResource(R.string.settings_credits_author_role), ExpressiveShapes.Scallop, cs.primary, cs.onPrimary)
                Credit(null, stringResource(R.string.settings_credits_ai_name), stringResource(R.string.settings_credits_ai_role), ExpressiveShapes.Flower, MaterialTheme.extra.chay, MaterialTheme.extra.onChay)
                HorizontalDivider(color = cs.onPrimaryContainer.copy(alpha = 0.2f))
                Text(stringResource(R.string.settings_credits_thanks), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.settings_about_desc), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Credit(letter: String?, name: String, role: String, shape: androidx.compose.ui.graphics.Shape, bg: Color, fg: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(shape).background(bg), contentAlignment = Alignment.Center) {
            if (letter != null) Text(letter, color = fg, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            else AppIcon("auto_awesome", tint = fg, size = 32.dp, filled = true)
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(role, style = MaterialTheme.typography.bodySmall)
        }
    }
}
