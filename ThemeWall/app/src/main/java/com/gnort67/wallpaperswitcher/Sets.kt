@file:OptIn(ExperimentalMaterial3Api::class)

package com.gnort67.wallpaperswitcher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun WallSet.label(): String = name.ifBlank { stringResource(R.string.default_set) }

private sealed interface SetDialog {
    data object New : SetDialog
    class Rename(val set: WallSet) : SetDialog
    class Delete(val set: WallSet) : SetDialog
}

@Composable
fun SetsScreen(
    p: Prefs, version: Int,
    onSelect: (String) -> Unit, onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit, onDelete: (String) -> Unit
) {
    var dialog by remember { mutableStateOf<SetDialog?>(null) }
    val haptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(p.sets, key = { it.id }) { s ->
                SetCard(
                    s, s.id == p.activeId, version, p.sets.size > 1,
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(s.id) },
                    onRename = { dialog = SetDialog.Rename(s) },
                    onDelete = { dialog = SetDialog.Delete(s) }
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { dialog = SetDialog.New },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text(stringResource(R.string.new_set)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
    when (val d = dialog) {
        null -> {}
        is SetDialog.New -> NameDialog(R.string.new_set, "", R.string.create, { dialog = null }) {
            onCreate(it); dialog = null
        }
        is SetDialog.Rename -> NameDialog(R.string.rename, d.set.name, R.string.save, { dialog = null }) {
            onRename(d.set.id, it); dialog = null
        }
        is SetDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.delete_set_title)) },
            text = { Text(stringResource(R.string.delete_set_text)) },
            confirmButton = { TextButton({ onDelete(d.set.id); dialog = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun NameDialog(title: Int, initial: String, confirm: Int, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                text, { if (it.length <= 24) text = it }, singleLine = true,
                label = { Text(stringResource(R.string.set_name)) }
            )
        },
        confirmButton = { TextButton({ onOk(text) }, enabled = text.isNotBlank()) { Text(stringResource(confirm)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun SetCard(
    set: WallSet, active: Boolean, version: Int, canDelete: Boolean,
    onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val light by produceState<ImageBitmap?>(null, version, set.id) {
        value = withContext(Dispatchers.IO) { loadPreview(WallpaperApplier.file(ctx, set.id, false), 240) }
    }
    val dark by produceState<ImageBitmap?>(null, version, set.id) {
        value = withContext(Dispatchers.IO) { loadPreview(WallpaperApplier.file(ctx, set.id, true), 240) }
    }
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(300), label = "set"
    )
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick, shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MiniThumb(light); MiniThumb(dark) }
            Column(Modifier.weight(1f)) {
                Text(set.label(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val (text, color) = when {
                    active -> R.string.set_in_use to MaterialTheme.colorScheme.primary
                    light == null || dark == null -> R.string.set_incomplete to MaterialTheme.colorScheme.error
                    else -> R.string.set_tap to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(stringResource(text), style = MaterialTheme.typography.labelMedium, color = color)
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_options)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onRename() }
                    )
                    if (canDelete) DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniThumb(bmp: ImageBitmap?) {
    Box(
        Modifier.width(48.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.Image, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
