package com.gnort67.calendarpro.ui.screens

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
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.data.AppSettings
import com.gnort67.calendarpro.ui.components.*
import com.gnort67.calendarpro.ui.shapes.ExpressiveShapes
import com.gnort67.calendarpro.ui.theme.*
import java.time.LocalDate

@Composable
fun SettingsScreen(
    s: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onReset: () -> Unit,
    bottomInset: Dp, onMessage: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val vi = s.language != "en"
    var colorOpen by rememberSaveable { mutableStateOf(false) }
    var fontOpen by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val resetDone = stringResource(R.string.settings_reset_done)
    val daily = dailyPalette()
    val pal = if (s.dailyRotation) daily else paletteOf(s.colorId)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.settings_subtitle_full), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }

        Section(R.string.settings_theme_mode_title, R.string.settings_theme_mode_subtitle)
        Card {
            SegmentedControl(
                listOf(
                    SegOption(stringResource(R.string.settings_light), "light_mode"),
                    SegOption(stringResource(R.string.settings_dark), "dark_mode"),
                    SegOption(stringResource(R.string.settings_system), "devices")
                ),
                when (s.themeMode) { "light" -> 0; "dark" -> 1; else -> 2 },
                { i -> onChange { it.copy(themeMode = listOf("light", "dark", "system")[i]) } },
                container = cs.secondaryContainer.copy(alpha = 0.5f),
                indicator = cs.primaryContainer
            )
        }

        Section(R.string.settings_language_title, R.string.settings_language_subtitle)
        Card {
            SegmentedControl(
                listOf(SegOption("Tiếng Việt"), SegOption("English")), if (vi) 0 else 1,
                { i -> onChange { it.copy(language = if (i == 0) "vi" else "en") } },
                container = cs.secondaryContainer.copy(alpha = 0.5f)
            )
        }

        ExpandCard("palette", R.string.settings_color_title, R.string.settings_color_subtitle, colorOpen) { colorOpen = !colorOpen }
        AnimatedVisibility(colorOpen, enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SwatchGrid(s, pal.id) { id -> onChange { it.copy(colorId = id, dailyRotation = false) } }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_daily_rotation_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (s.dailyRotation) stringResource(R.string.settings_daily_rotation_on, if (vi) daily.labelVi else daily.label) else stringResource(R.string.settings_daily_rotation_off),
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                            )
                        }
                        Switch(s.dailyRotation, { on -> onChange { it.copy(dailyRotation = on) } })
                    }
                    val changed = stringResource(R.string.settings_color_changed, "%s")
                    FilledTonalButton(
                        onClick = {
                            val p = PALETTES.filter { it.id != pal.id }.random()
                            onChange { it.copy(colorId = p.id, dailyRotation = false) }
                            onMessage(changed.replace("%s", if (vi) p.labelVi else p.label))
                        },
                        Modifier.fillMaxWidth().height(52.dp)
                    ) { AppIcon("shuffle", size = 20.dp); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_random_color_now)) }
                }
            }
        }

        ExpandCard("text_fields", R.string.settings_font_title, R.string.settings_font_subtitle, fontOpen) { fontOpen = !fontOpen }
        AnimatedVisibility(fontOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FONT_OPTIONS.forEach { f ->
                        val sel = f.id == s.fontFamily
                        Row(
                            Modifier.fillMaxWidth().pressable(RoundedCornerShape(22.dp)) { onChange { it.copy(fontFamily = f.id) } }
                                .background(if (sel) cs.primaryContainer else cs.surfaceContainerHighest).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(f.label, fontFamily = f.family, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (sel) cs.onPrimaryContainer else cs.onSurface)
                                Text(if (vi) f.noteVi else f.noteEn, style = MaterialTheme.typography.bodySmall, color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant)
                                Text(stringResource(R.string.settings_font_preview_sample), fontFamily = f.family, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp), color = if (sel) cs.onPrimaryContainer else cs.onSurface)
                            }
                            if (sel) AppIcon("check_circle", tint = cs.onPrimaryContainer, filled = true)
                        }
                    }
                }
            }
        }

        Section(R.string.settings_font_size_title, R.string.settings_font_size_subtitle)
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val ids = listOf("nho", "vua", "lon")
                SegmentedControl(
                    listOf(SegOption(stringResource(R.string.settings_size_small), "text_fields"), SegOption(stringResource(R.string.settings_size_medium), "text_fields"), SegOption(stringResource(R.string.settings_size_large), "text_fields")),
                    ids.indexOf(s.fontSize).coerceAtLeast(0), { i -> onChange { it.copy(fontSize = ids[i]) } },
                    container = cs.secondaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    "${stringResource(R.string.settings_font_size_preview)} ${stringResource(R.string.settings_font_size_preview_note)}",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cs.surfaceContainerHighest).padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        FilledTonalButton(
            onClick = { confirmReset = true }, Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer)
        ) {
            AppIcon("restart_alt", size = 22.dp); Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.settings_reset_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

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

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
        text = { Text(stringResource(R.string.settings_reset_confirm_msg)) },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset(); onMessage(resetDone) }) { Text(stringResource(R.string.common_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
private fun Section(title: Int, subtitle: Int) {
    Column(Modifier.padding(start = 8.dp, top = 6.dp)) {
        SectionTitle(stringResource(title))
        Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ExpandCard(icon: String, title: Int, subtitle: Int, open: Boolean, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rot by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.7f), label = "chevron")
    Surface(Modifier.fillMaxWidth().pressable(RoundedCornerShape(32.dp), onClick = onToggle), shape = RoundedCornerShape(32.dp), color = cs.surfaceContainer) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(icon, CircleShape, cs.secondaryContainer, cs.onSecondaryContainer, size = 52.dp, iconSize = 26.dp, filled = true)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                AppIcon("expand_more", Modifier.rotate(rot), size = 22.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(s: AppSettings, selectedId: String, onPick: (String) -> Unit) {
    val dark = when (s.themeMode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        PALETTES.forEach { p ->
            val sel = p.id == selectedId && !s.dailyRotation
            val role = PALETTE_ROLES.indexOf("primary")
            val c = Color(((if (dark) p.dark else p.light)[role]).toInt())
            val scale by animateFloatAsState(if (sel) 1.08f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sw")
            Box(
                Modifier.size(52.dp).graphicsScale(scale).clip(if (sel) ExpressiveShapes.Cookie else CircleShape).background(c)
                    .pressable(if (sel) ExpressiveShapes.Cookie else CircleShape) { onPick(p.id) },
                contentAlignment = Alignment.Center
            ) { if (sel) AppIcon("check", tint = Color.White, size = 24.dp) }
        }
    }
}

private fun Modifier.graphicsScale(s: Float) = this.graphicsLayer { scaleX = s; scaleY = s }

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
