@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.local.START_PAGES
import com.minhtrong67.moody.data.local.Settings
import com.minhtrong67.moody.transfer.DataTransfer
import com.minhtrong67.moody.transfer.DataTransfer.Dataset
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.LocalToaster
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.DropdownField
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.SegmentedTabs
import com.minhtrong67.moody.ui.components.TimePickerDialog
import com.minhtrong67.moody.ui.theme.Palettes
import com.minhtrong67.moody.util.todayISO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class ExportJob(val fileName: String, val bytes: ByteArray)

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    settings: Settings,
    user: AppUser?,
    entries: List<Entry>?,
    onOpenActivities: () -> Unit,
    onOpenMoods: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val cs = MaterialTheme.colorScheme

    var colorsOpen by rememberSaveable { mutableStateOf(false) }
    var format by rememberSaveable { mutableStateOf("csv") }
    var showTime by remember { mutableStateOf(false) }

    // ---------------- notification permission (Android 13+) ----------------
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deniedMsg = stringResource(R.string.settings_notifDenied)
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterPermission?.invoke() else toaster.show(deniedMsg)
        afterPermission = null
    }
    fun withNotifPermission(then: () -> Unit) {
        val needs = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needs) { afterPermission = then; permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } else then()
    }

    // ---------------- export (Storage Access Framework) ----------------
    var job by remember { mutableStateOf<ExportJob?>(null) }
    fun writeJob(uri: Uri?) {
        val j = job; job = null
        if (uri == null || j == null) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(j.bytes) } ?: error("no stream") }.isSuccess
            }
            toaster.show(if (ok) ctx.getString(R.string.settings_exported, j.fileName) else ctx.getString(R.string.settings_exportFailed))
        }
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writeJob(it) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { writeJob(it) }
    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { writeJob(it) }

    fun rowsFor(ds: Dataset) = when (ds) {
        Dataset.Entries -> DataTransfer.entriesToRows(entries ?: emptyList(), activities.associateBy { it.id })
        Dataset.Activities -> DataTransfer.activitiesToRows(activities)
        Dataset.Moods -> DataTransfer.moodsToRows(moods)
    }

    fun exportDataset(ds: Dataset) {
        val rows = rowsFor(ds)
        if (rows.isEmpty()) { toaster.show(ctx.getString(R.string.settings_exportEmpty)); return }
        val name = "moody-${ds.key}-${todayISO()}.$format"
        job = ExportJob(name, if (format == "json") DataTransfer.buildJson(ds, rows) else DataTransfer.buildCsv(ds, rows))
        if (format == "json") saveJson.launch(name) else saveCsv.launch(name)
    }

    fun exportAll() {
        val all = Dataset.entries.associateWith { rowsFor(it) }
        if (format == "json") {
            val name = "moody-all-${todayISO()}.json"
            job = ExportJob(name, DataTransfer.buildAllJson(all)); saveJson.launch(name)
        } else {
            val name = "moody-all-${todayISO()}.zip"
            job = ExportJob(name, DataTransfer.buildAllZip(all)); saveZip.launch(name)
        }
    }

    // ---------------- import ----------------
    var importTarget by remember { mutableStateOf<String?>(null) }   // dataset key or "all"

    fun displayName(uri: Uri): String =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "import"

    fun handleImport(uri: Uri) {
        val target = importTarget ?: return
        scope.launch {
            val only = if (target == "all") null else Dataset.of(target)
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw DataTransfer.ImportException("read")
                    DataTransfer.parseImport(displayName(uri), bytes, only)
                }
            }
            val err = parsed.exceptionOrNull()
            if (err != null) {
                val code = (err as? DataTransfer.ImportException)?.code ?: "read"
                toaster.show(ctx.getString(when (code) {
                    "format" -> R.string.settings_importErr_format
                    "size" -> R.string.settings_importErr_size
                    "invalid" -> R.string.settings_importErr_invalid
                    "mismatch" -> R.string.settings_importErr_mismatch
                    else -> R.string.settings_importErr_read
                }))
                return@launch
            }
            val data = parsed.getOrThrow()
            var added = 0; var updated = 0; var skipped = 0
            try {
                // activities -> moods -> entries (entries reference activities, so they go last)
                var known = activities
                data[Dataset.Activities]?.let { raw ->
                    val n = DataTransfer.normalizeActivities(raw)
                    skipped += n.skipped
                    if (n.items.isNotEmpty()) {
                        val (a, u) = vm.importActivities(n.items)
                        added += a; updated += u
                        known = known + n.items.filter { i -> i.id != null && known.none { it.id == i.id } }
                            .map { com.minhtrong67.moody.data.ActivityDef(it.id!!, it.emoji, it.label, true) }
                    }
                }
                data[Dataset.Moods]?.let { raw ->
                    val n = DataTransfer.normalizeMoods(raw)
                    skipped += n.skipped
                    if (n.items.isNotEmpty()) updated += vm.importMoods(n.items)
                }
                data[Dataset.Entries]?.let { raw ->
                    val n = DataTransfer.normalizeEntries(raw, moods, known)
                    skipped += n.skipped
                    if (n.items.isNotEmpty()) { val (a, u) = vm.importEntries(n.items); added += a; updated += u }
                }
            } catch (e: Exception) {
                toaster.show(ctx.getString(R.string.settings_importErr_write)); return@launch
            }
            toaster.show(
                if (added + updated == 0) ctx.getString(R.string.settings_importNothing)
                else ctx.getString(R.string.settings_importDone, added, updated, skipped)
            )
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) handleImport(uri) }
    fun pickImport(target: String) { importTarget = target; picker.launch(arrayOf("*/*")) }

    // ---------------- UI ----------------
    if (showTime) {
        val (h, m) = settings.dailyTime.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 20) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        TimePickerDialog(h, m, onConfirm = { hh, mm -> vm.setDailyTime("%02d:%02d".format(hh, mm)); showTime = false }, onDismiss = { showTime = false })
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))

        // Account
        MoodyCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (user?.photoUrl != null) AsyncImage(user.photoUrl, null, Modifier.size(48.dp).clip(CircleShape))
                else Icon(Icons.Rounded.AccountCircle, null, Modifier.size(48.dp), tint = cs.primary)
                Column(Modifier.weight(1f)) {
                    if (user != null) {
                        Text(user.displayName ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(user.email ?: "", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(stringResource(R.string.settings_account), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_guestMode), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (user != null) {
                ActionButton(stringResource(R.string.common_logout), Icons.AutoMirrored.Rounded.Logout, { vm.logout() }, tonal = true, danger = true, modifier = Modifier.fillMaxWidth())
            } else {
                if (vm.firebaseReady) ActionButton(stringResource(R.string.settings_signIn), Icons.Rounded.AccountCircle, { vm.signInWithGoogle(ctx) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ActionButton(stringResource(R.string.common_logout), Icons.AutoMirrored.Rounded.Logout, { vm.logout() }, tonal = true, modifier = Modifier.fillMaxWidth())
            }
        }

        Group(stringResource(R.string.settings_appearance)) {
            SegmentedTabs(
                listOf("light" to stringResource(R.string.theme_lightShort), "dark" to stringResource(R.string.theme_darkShort), "system" to stringResource(R.string.theme_systemShort)),
                settings.themeMode, { vm.setThemeMode(it) },
            )
        }

        Group(stringResource(R.string.settings_themeColor)) {
            MoodyCard(Modifier.fillMaxWidth(), onClick = { colorsOpen = !colorsOpen }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val current = Palettes.byId(settings.themeColor)
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(current.light[0])))
                    Column(Modifier.weight(1f)) {
                        Text(colorName(ctx, current.id), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(if (colorsOpen) R.string.settings_themeColorCollapse else R.string.settings_themeColorExpand), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
                AnimatedVisibility(colorsOpen) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Palettes.all.forEach { p ->
                                val sel = p.id == settings.themeColor
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(Color(p.seed))
                                        .then(if (sel) Modifier.border(3.dp, cs.onSurface, CircleShape) else Modifier)
                                        .clickable { vm.setThemeColor(p.id) },
                                    contentAlignment = Alignment.Center,
                                ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.settings_themeColorHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }

        Group(stringResource(R.string.settings_language)) {
            SegmentedTabs(
                listOf("auto" to stringResource(R.string.settings_langAuto), "vi" to "Tiếng Việt", "en" to "English"),
                settings.language, { vm.setLanguage(it) },
            )
        }

        Group(stringResource(R.string.settings_fontSize)) {
            SegmentedTabs(
                listOf("0.9" to stringResource(R.string.settings_font_0_9), "1.0" to stringResource(R.string.settings_font_1), "1.15" to stringResource(R.string.settings_font_1_15)),
                settings.fontScale.toString(), { vm.setFontScale(it.toFloat()) },
            )
        }

        Group(stringResource(R.string.settings_startPage), stringResource(R.string.settings_startPageText)) {
            DropdownField(
                stringResource(R.string.settings_startPage),
                START_PAGES.map { it to stringResource(startPageLabel(it)) },
                settings.startPage, { vm.setStartPage(it) }, Modifier.fillMaxWidth(),
            )
        }

        Group(stringResource(R.string.nav_manage)) {
            MoodyCard(Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                NavRow(Icons.Rounded.Checklist, stringResource(R.string.nav_activities), onOpenActivities)
                HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                NavRow(Icons.Rounded.EmojiEmotions, stringResource(R.string.nav_moods), onOpenMoods)
            }
        }

        Group(stringResource(R.string.settings_reminders), stringResource(R.string.settings_remindersHint)) {
            MoodyCard(Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                SwitchRow(stringResource(R.string.settings_remindDaily), settings.dailyEnabled) { on ->
                    if (on) withNotifPermission { vm.setDailyEnabled(true) } else vm.setDailyEnabled(false)
                }
                if (settings.dailyEnabled) {
                    TextButton(onClick = { showTime = true }, modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.settings_remindTime) + ": " + settings.dailyTime)
                    }
                }
                SwitchRow(stringResource(R.string.settings_remindWeekly), settings.weeklyEnabled) { on ->
                    if (on) withNotifPermission { vm.setWeeklyEnabled(true) } else vm.setWeeklyEnabled(false)
                }
                SwitchRow(stringResource(R.string.settings_remindMonthly), settings.monthlyEnabled) { on ->
                    if (on) withNotifPermission { vm.setMonthlyEnabled(true) } else vm.setMonthlyEnabled(false)
                }
                Spacer(Modifier.height(4.dp))
                ActionButton(
                    stringResource(R.string.settings_remindTest), Icons.Rounded.Notifications,
                    { withNotifPermission { vm.sendTestNotification() } }, tonal = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Group(stringResource(R.string.settings_exportImport), stringResource(R.string.settings_dataHint)) {
            SegmentedTabs(listOf("csv" to "CSV", "json" to "JSON"), format, { format = it })
            Spacer(Modifier.height(12.dp))
            MoodyCard(Modifier.fillMaxWidth()) {
                Dataset.entries.forEach { ds ->
                    DataRow(
                        stringResource(when (ds) {
                            Dataset.Entries -> R.string.settings_dataset_entries
                            Dataset.Activities -> R.string.settings_dataset_activities
                            Dataset.Moods -> R.string.settings_dataset_moods
                        }),
                        onExport = { exportDataset(ds) }, onImport = { pickImport(ds.key) },
                    )
                    HorizontalDivider()
                }
                DataRow(stringResource(R.string.settings_all), onExport = { exportAll() }, onImport = { pickImport("all") },
                    exportLabel = stringResource(R.string.settings_exportAll), importLabel = stringResource(R.string.settings_importAll))
                Text(
                    stringResource(if (format == "json") R.string.settings_allHint_json else R.string.settings_allHint_csv),
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Group(stringResource(R.string.settings_about)) {
            MoodyCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_credit, "minhtrong67", "Claude AI"), style = MaterialTheme.typography.bodyMedium)
                Text("Moody · v1.0.0", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun startPageLabel(key: String): Int = when (key) {
    "history" -> R.string.nav_history
    "stats" -> R.string.nav_stats
    "checkin" -> R.string.nav_newCheckin
    "activities" -> R.string.nav_activities
    "moods" -> R.string.nav_moods
    "settings" -> R.string.nav_settings
    else -> R.string.nav_today
}

private fun colorName(ctx: Context, id: String): String {
    val res = ctx.resources.getIdentifier("themeColor_" + id.replace('-', '_'), "string", ctx.packageName)
    return if (res != 0) ctx.getString(res) else id
}

@Composable
private fun Group(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DataRow(
    title: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    exportLabel: String = stringResource(R.string.settings_exportBtn),
    importLabel: String = stringResource(R.string.settings_importBtn),
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(exportLabel, Icons.Rounded.FileDownload, onExport, modifier = Modifier.weight(1f))
            ActionButton(importLabel, Icons.Rounded.FileUpload, onImport, tonal = true, modifier = Modifier.weight(1f))
        }
    }
}
