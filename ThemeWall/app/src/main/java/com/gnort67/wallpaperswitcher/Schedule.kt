package com.gnort67.wallpaperswitcher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SLOT_NAMES = listOf(R.string.slot_morning, R.string.slot_afternoon, R.string.slot_evening, R.string.slot_night)
private val SLOT_HOURS = listOf("06–12", "12–18", "18–22", "22–06")

@Composable
fun ScheduleScreen(
    p: Prefs, version: Int,
    onToggle: (Boolean) -> Unit, onPick: (Boolean, Int) -> Unit, onClear: (Boolean, Int) -> Unit
) {
    val cur = WallpaperApplier.currentSlot()
    SwitchRow(R.string.schedule_title, R.string.schedule_desc, p.scheduleEnabled, true, onToggle)
    Text(
        stringResource(R.string.schedule_note), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Column(Modifier.alpha(if (p.scheduleEnabled) 1f else 0.5f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(false, true).forEach { dark ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null)
                        Text(
                            stringResource(if (dark) R.string.dark_wallpaper else R.string.light_wallpaper),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 1..4) SlotThumb(dark, slot, version, slot == cur, onPick, onClear)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SlotThumb(
    dark: Boolean, slot: Int, version: Int, current: Boolean,
    onPick: (Boolean, Int) -> Unit, onClear: (Boolean, Int) -> Unit
) {
    val ctx = LocalContext.current
    val file = remember(dark, slot) { WallpaperApplier.file(ctx, dark, slot) }
    val bmp by produceState<ImageBitmap?>(null, version) { value = withContext(Dispatchers.IO) { loadPreview(file, 240) } }
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPick(dark, slot) },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(bmp, animationSpec = tween(300), label = "thumb") { b ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (b != null) {
                        Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)).clickable { onClear(dark, slot) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(14.dp), tint = Color.White) }
                    } else Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            stringResource(SLOT_NAMES[slot - 1]), style = MaterialTheme.typography.labelMedium, maxLines = 1,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(SLOT_HOURS[slot - 1], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
