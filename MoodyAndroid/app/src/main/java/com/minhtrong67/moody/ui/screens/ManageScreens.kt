@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.ActivityDef
import com.minhtrong67.moody.data.MoodDef
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.ConfirmDialog
import com.minhtrong67.moody.ui.components.EmptyText
import com.minhtrong67.moody.ui.components.MoodBubble
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SwipeActionRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Emoji + name editor shared by activities and moods (labels above the fields, like the web dialog). */
@Composable
fun EmojiNameDialog(
    title: String,
    initialEmoji: String,
    initialLabel: String,
    placeholder: String? = null,
    onSave: (emoji: String, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var emoji by rememberSaveable { mutableStateOf(initialEmoji) }
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    val valid = emoji.isNotBlank() && label.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(92.dp)) {
                    Text(stringResource(R.string.manage_emoji), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                    OutlinedTextField(
                        value = emoji, onValueChange = { emoji = it.take(8) }, singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.manage_displayName), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                    OutlinedTextField(
                        value = label, onValueChange = { label = it.take(60) }, singleLine = true,
                        placeholder = if (placeholder != null) ({ Text(placeholder) }) else null,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
            }
        },
        dismissButton = { ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, onDismiss, tonal = true) },
        confirmButton = { ActionButton(stringResource(R.string.common_save), Icons.Rounded.Check, { onSave(emoji.trim(), label.trim()) }, enabled = valid) },
    )
}

@Composable
fun ManageActivitiesScreen(
    onAdd: (emoji: String, label: String) -> Unit,
    onUpdate: (id: String, emoji: String, label: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activities = LocalActivities.current
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    var search by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ActivityDef?>(null) }
    var pendingUpdate by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var deleting by remember { mutableStateOf<ActivityDef?>(null) }

    // Local copy while dragging so the list reorders instantly; persisted when the drag ends.
    var local by remember { mutableStateOf(activities) }
    LaunchedEffect(activities) { local = activities }
    val lazy = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazy) { from, to ->
        local = local.toMutableList().apply { add(to.index, removeAt(from.index)) }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    if (adding) EmojiNameDialog(
        stringResource(R.string.manageActs_new), "🙂", "", stringResource(R.string.manageActs_example),
        onSave = { e, l -> onAdd(e, l); adding = false }, onDismiss = { adding = false },
    )
    editing?.let { a ->
        EmojiNameDialog(
            stringResource(R.string.manageActs_edit), a.emoji, a.label,
            onSave = { e, l -> pendingUpdate = Triple(a.id, e, l); editing = null }, onDismiss = { editing = null },
        )
    }
    pendingUpdate?.let { (id, e, l) ->
        ConfirmDialog(
            stringResource(R.string.manageActs_updateTitle), stringResource(R.string.manage_updateText), stringResource(R.string.common_update),
            onConfirm = { onUpdate(id, e, l); pendingUpdate = null }, onDismiss = { pendingUpdate = null },
        )
    }
    deleting?.let { a ->
        ConfirmDialog(
            stringResource(R.string.manageActs_deleteTitle), stringResource(R.string.common_irreversible), stringResource(R.string.common_delete),
            danger = true, headerIcon = Icons.Rounded.WarningAmber, onConfirm = { onRemove(a.id); deleting = null }, onDismiss = { deleting = null },
        )
    }

    val q = search.trim().lowercase()
    val shown = if (q.isEmpty()) local else local.filter { it.label.lowercase().contains(q) }
    val canReorder = q.isEmpty()

    Column(modifier.padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.nav_activities), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.manageActs_intro), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text(stringResource(R.string.manageActs_searchPlaceholder), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.manageActs_clearSearch)) } },
            singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        ActionButton(stringResource(R.string.manageActs_add), Icons.Rounded.Add, { adding = true }, modifier = Modifier.fillMaxWidth().height(56.dp))
        Spacer(Modifier.height(12.dp))
        if (shown.isEmpty()) EmptyText(stringResource(R.string.manageActs_noResults))
        LazyColumn(state = lazy, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(shown, key = { _, a -> a.id }) { index, a ->
                ReorderableItem(reorder, key = a.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "dragElev")
                    SwipeActionRow(onEdit = { editing = a }, onDelete = { deleting = a }) {
                        MoodyCard(Modifier.fillMaxWidth().shadow(elevation, RoundedCornerShape(28.dp)), contentPadding = 8.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    enabled = canReorder,
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        enabled = canReorder,
                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                        onDragStopped = { onReorder(local.map { it.id }) },
                                    ),
                                ) { Icon(Icons.Rounded.DragIndicator, stringResource(R.string.manageActs_drag), tint = cs.onSurfaceVariant) }
                                Text(a.emoji, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(a.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        stringResource(if (a.custom) R.string.manageActs_custom else R.string.manageActs_default),
                                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { local = local.toMutableList().also { it.add(index - 1, it.removeAt(index)) }; onReorder(local.map { it.id }) },
                                    enabled = canReorder && index > 0,
                                ) { Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.manageActs_up)) }
                                IconButton(
                                    onClick = { local = local.toMutableList().also { it.add(index + 1, it.removeAt(index)) }; onReorder(local.map { it.id }) },
                                    enabled = canReorder && index < local.lastIndex,
                                ) { Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.manageActs_down)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageMoodsScreen(
    overridden: Set<String>,
    onUpdate: (key: String, emoji: String, label: String) -> Unit,
    onReset: (key: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val moods = LocalMoods.current
    val cs = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<MoodDef?>(null) }
    var pending by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var resetting by remember { mutableStateOf<MoodDef?>(null) }

    editing?.let { m ->
        EmojiNameDialog(
            stringResource(R.string.manageMoods_edit), m.emoji, m.label,
            onSave = { e, l -> pending = Triple(m.key, e, l); editing = null }, onDismiss = { editing = null },
        )
    }
    pending?.let { (k, e, l) ->
        ConfirmDialog(
            stringResource(R.string.manageMoods_updateTitle), stringResource(R.string.manage_updateText), stringResource(R.string.common_update),
            onConfirm = { onUpdate(k, e, l); pending = null }, onDismiss = { pending = null },
        )
    }
    resetting?.let { m ->
        ConfirmDialog(
            stringResource(R.string.manageMoods_resetTitle), stringResource(R.string.manageMoods_resetText), stringResource(R.string.manageMoods_reset),
            confirmIcon = Icons.Rounded.RestartAlt,
            onConfirm = { onReset(m.key); resetting = null }, onDismiss = { resetting = null },
        )
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.nav_moods), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.manageMoods_intro), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        moods.forEach { m ->
            MoodyCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MoodBubble(m)
                    Column(Modifier.weight(1f)) {
                        Text(m.label, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.manageMoods_score, m.score), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    if (m.key in overridden) {
                        IconButton(onClick = { resetting = m }) { Icon(Icons.Rounded.RestartAlt, stringResource(R.string.manageMoods_reset)) }
                    }
                    IconButton(onClick = { editing = m }) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), tint = cs.primary) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
