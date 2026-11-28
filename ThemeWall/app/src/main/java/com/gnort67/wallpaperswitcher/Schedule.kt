@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.gnort67.wallpaperswitcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SLOT_NAMES = listOf(R.string.slot_morning, R.string.slot_afternoon, R.string.slot_evening, R.string.slot_night)
private val SLOT_HOURS = listOf("06–12", "12–18", "18–22", "22–06")

@Composable
fun ScheduleScreen(
    p: Prefs, version: Int,
    onCustomTimes: (Boolean) -> Unit, onLightFrom: (Int) -> Unit, onDarkFrom: (Int) -> Unit,
    onToggle: (Boolean) -> Unit, onPick: (Boolean, Int) -> Unit, onClear: (Boolean, Int) -> Unit
) {
    val ctx = LocalContext.current
    val now = rememberNow()
    val cur = remember(now) { WallpaperApplier.currentSlot() }
    var editing by remember { mutableStateOf<Boolean?>(null) } // false = light time, true = dark time

    Group(R.string.section_modes) {
        SwitchItem(R.string.custom_times, R.string.custom_times_desc, p.modeSource == 1, true, onCustomTimes)
        AnimatedVisibility(p.modeSource == 1) {
            Column {
                GroupDivider()
                TimeRow(R.string.light_from, fmtTime(ctx, p.lightFrom), Icons.Rounded.LightMode) { editing = false }
                GroupDivider()
                TimeRow(R.string.dark_from, fmtTime(ctx, p.darkFrom), Icons.Rounded.DarkMode) { editing = true }
            }
        }
    }

    Group(R.string.section_slots) {
        SwitchItem(R.string.schedule_title, R.string.schedule_desc, p.scheduleEnabled, true, onToggle)
    }
    Text(
        stringResource(R.string.schedule_for, p.activeSet.label()),
        Modifier.padding(start = 20.dp), style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
    )
    Text(
        stringResource(R.string.schedule_note), Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(Modifier.alpha(if (p.scheduleEnabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(false, true).forEach { dark ->
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null)
                        Text(
                            stringResource(if (dark) R.string.dark_wallpaper else R.string.light_wallpaper),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 1..4) SlotThumb(p.activeId, dark, slot, version, slot == cur, onPick, onClear)
                    }
                }
            }
        }
    }

    editing?.let { isDark ->
        TimeDialog(
            title = if (isDark) R.string.dark_from else R.string.light_from,
            initialMinutes = if (isDark) p.darkFrom else p.lightFrom,
            onDismiss = { editing = null },
            onConfirm = { m -> if (isDark) onDarkFrom(m) else onLightFrom(m); editing = null }
        )
    }
}

@Composable
private fun TimeRow(title: Int, time: String, icon: ImageVector, onClick: () -> Unit) {
    SettingRow(
        title = stringResource(title), desc = null,
        modifier = Modifier.clickable(onClick = onClick),
        leading = {
            ShapeIcon(icon, CircleShape, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, 40.dp)
        },
        trailing = {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    time, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    )
}

@Composable
private fun TimeDialog(title: Int, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val ctx = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60,
        is24Hour = android.text.format.DateFormat.is24HourFormat(ctx)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
        confirmButton = { TextButton({ onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun RowScope.SlotThumb(
    setId: String, dark: Boolean, slot: Int, version: Int, current: Boolean,
    onPick: (Boolean, Int) -> Unit, onClear: (Boolean, Int) -> Unit
) {
    val ctx = LocalContext.current
    val file = remember(setId, dark, slot) { WallpaperApplier.file(ctx, setId, dark, slot) }
    val bmp by produceState<ImageBitmap?>(null, version, setId) { value = withContext(Dispatchers.IO) { loadPreview(file, 240) } }
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPick(dark, slot) },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(bmp, animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "thumb") { b ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (b != null) {
                        Image(b, stringResource(SLOT_NAMES[slot - 1]), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)).clickable { onClear(dark, slot) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(14.dp), tint = Color.White) }
                    } else Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            stringResource(SLOT_NAMES[slot - 1]), style = MaterialTheme.typography.labelMedium, maxLines = 1,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(SLOT_HOURS[slot - 1], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
