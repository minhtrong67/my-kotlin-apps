@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.EntryData
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalLocale
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.ConfirmDialog
import com.minhtrong67.moody.ui.components.MoodPickBubble
import com.minhtrong67.moody.ui.components.TimePickerDialog
import com.minhtrong67.moody.util.fullDate
import com.minhtrong67.moody.util.todayISO
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Full-screen check-in / edit form. With no [id] it edits the check-in of [dateArg] (default: today) when one
 * already exists — exactly like the web app — otherwise it creates a new one.
 */
@Composable
fun CheckInScreen(
    entries: List<Entry>?,
    id: String?,
    dateArg: String?,
    onSave: (existingId: String?, data: EntryData, isUpdate: Boolean) -> Unit,
    onClose: () -> Unit,
    onManageActivities: () -> Unit,
) {
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val locale = LocalLocale.current

    val existing: Entry? = remember(entries, id, dateArg) {
        entries?.let { list ->
            if (id != null) list.firstOrNull { it.id == id }
            else list.filter { it.date == (dateArg ?: todayISO()) }.maxByOrNull { it.instant }
        }
    }

    var mood by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var dtText by rememberSaveable {
        mutableStateOf(
            LocalDate.parse(dateArg ?: todayISO()).atTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES)).toString()
        )
    }
    var synced by rememberSaveable { mutableStateOf(false) }

    // Data can arrive after the screen opened -> fill the form once when the record appears.
    LaunchedEffect(existing?.id) {
        if (existing != null && !synced) {
            mood = existing.mood
            selected = existing.activities
            note = existing.note
            dtText = existing.instant.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES).toString()
            synced = true
        }
    }

    val dt = remember(dtText) { runCatching { LocalDateTime.parse(dtText) }.getOrDefault(LocalDateTime.now()) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    fun performSave() {
        val m = moods.firstOrNull { it.key == mood } ?: return
        val data = EntryData(
            date = dt.toLocalDate().toString(),
            time = dt.atZone(ZoneId.systemDefault()).toInstant().toString(),
            mood = m.key,
            score = m.score,
            activities = selected,
            note = note,
        )
        onSave(existing?.id, data, existing != null)
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                ActionButton(stringResource(R.string.common_ok), Icons.Rounded.Check, {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        dtText = d.atTime(dt.toLocalTime()).toString()
                    }
                    showDate = false
                })
            },
            dismissButton = { ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, { showDate = false }, tonal = true) },
        ) { DatePicker(state = state) }
    }
    if (showTime) {
        TimePickerDialog(
            initialHour = dt.hour, initialMinute = dt.minute,
            onConfirm = { h, min -> dtText = dt.withHour(h).withMinute(min).toString(); showTime = false },
            onDismiss = { showTime = false },
        )
    }
    if (showConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.checkin_confirmTitle),
            text = stringResource(R.string.checkin_confirmText),
            confirmLabel = stringResource(R.string.common_update),
            onConfirm = { showConfirm = false; performSave() },
            onDismiss = { showConfirm = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing != null) R.string.checkin_editTitle else R.string.checkin_title)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close)) } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionButton(stringResource(R.string.common_cancel), Icons.Rounded.Close, onClose, tonal = true, modifier = Modifier.weight(1f).height(52.dp))
                    ActionButton(
                        stringResource(if (existing != null) R.string.common_update else R.string.checkin_save),
                        Icons.Rounded.Check,
                        onClick = { if (existing != null) showConfirm = true else performSave() },
                        enabled = mood != null && entries != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                    )
                }
            }
        },
    ) { inner ->
        if (entries == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.checkin_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(dt.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)), maxLines = 1)
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(dt.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), maxLines = 1)
                }
            }
            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                moods.forEach { m -> MoodPickBubble(m, selected = mood == m.key, onClick = { mood = m.key }) }
            }
            Spacer(Modifier.height(32.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.checkin_activities), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onManageActivities) {
                    Icon(Icons.Rounded.Settings, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.checkin_manage))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activities.forEach { a ->
                    val active = a.id in selected
                    FilterChip(
                        selected = active,
                        onClick = { selected = if (active) selected - a.id else selected + a.id },
                        label = { Text(a.label) },
                        leadingIcon = { if (active) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) else Text(a.emoji) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.checkin_note)) },
                placeholder = { Text(stringResource(R.string.checkin_notePlaceholder)) },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
