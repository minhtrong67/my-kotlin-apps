@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

/** Emoji + name editor shared by activities and moods. */
@Composable
private fun EmojiNameDialog(
    title: String,
    initialEmoji: String,
    initialLabel: String,
    placeholder: String? = null,
    onSave: (emoji: String, label: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var emoji by rememberSaveable { mutableStateOf(initialEmoji) }
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji, onValueChange = { emoji = it.take(8) },
                    label = { Text(stringResource(R.string.manage_emoji)) }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(60) },
                    label = { Text(stringResource(R.string.manage_displayName)) },
                    placeholder = if (placeholder != null) ({ Text(placeholder) }) else null, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = { ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, onDismiss, tonal = true) },
        confirmButton = {
            ActionButton(
                stringResource(R.string.common_save), Icons.Rounded.Check,
                onClick = { onSave(emoji.trim(), label.trim()) },
                enabled = emoji.isNotBlank() && label.isNotBlank(),
            )
        },
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
    var search by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ActivityDef?>(null) }
    var pendingUpdate by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var deleting by remember { mutableStateOf<ActivityDef?>(null) }

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
            danger = true, onConfirm = { onRemove(a.id); deleting = null }, onDismiss = { deleting = null },
        )
    }

    val q = search.trim().lowercase()
    val shown = if (q.isEmpty()) activities else activities.filter { it.label.lowercase().contains(q) }

    Column(modifier.padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.manageActs_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text(stringResource(R.string.manageActs_searchPlaceholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.manageActs_clearSearch)) } },
            singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        ActionButton(stringResource(R.string.manageActs_add), Icons.Rounded.Add, { adding = true }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        if (shown.isEmpty()) EmptyText(stringResource(R.string.manageActs_noResults))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { a ->
                val idx = activities.indexOfFirst { it.id == a.id }
                MoodyCard(Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(36.dp)) {
                            // Reordering is disabled while a search filter is active (indexes would be misleading).
                            IconButton(onClick = {
                                val ids = activities.map { it.id }.toMutableList()
                                ids.add(idx - 1, ids.removeAt(idx)); onReorder(ids)
                            }, enabled = q.isEmpty() && idx > 0, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.manageActs_up)) }
                            IconButton(onClick = {
                                val ids = activities.map { it.id }.toMutableList()
                                ids.add(idx + 1, ids.removeAt(idx)); onReorder(ids)
                            }, enabled = q.isEmpty() && idx < activities.lastIndex, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.manageActs_down)) }
                        }
                        Text(a.emoji, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(if (a.custom) R.string.manageActs_custom else R.string.manageActs_default),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editing = a }) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { deleting = a }) { Icon(Icons.Rounded.Delete, stringResource(R.string.common_delete)) }
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
        Text(stringResource(R.string.manageMoods_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(8.dp))
        moods.forEach { m ->
            MoodyCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MoodBubble(m)
                    Column(Modifier.weight(1f)) {
                        Text(m.label, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.manageMoods_score, m.score), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (m.key in overridden) {
                        IconButton(onClick = { resetting = m }) { Icon(Icons.Rounded.RestartAlt, stringResource(R.string.manageMoods_reset)) }
                    }
                    IconButton(onClick = { editing = m }) { Icon(Icons.Rounded.Edit, stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
