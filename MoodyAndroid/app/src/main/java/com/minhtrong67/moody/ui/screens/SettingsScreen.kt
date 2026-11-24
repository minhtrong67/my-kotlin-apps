@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.minhtrong67.moody.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.ActivityDef
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.Entry
import com.minhtrong67.moody.data.local.START_PAGES
import com.minhtrong67.moody.data.local.Settings
import com.minhtrong67.moody.notify.Notifications
import com.minhtrong67.moody.notify.ReminderScheduler
import com.minhtrong67.moody.transfer.DataTransfer
import com.minhtrong67.moody.transfer.DataTransfer.Dataset
import com.minhtrong67.moody.ui.LocalActivities
import com.minhtrong67.moody.ui.LocalActivityContext
import com.minhtrong67.moody.ui.LocalMoods
import com.minhtrong67.moody.ui.LocalToaster
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.components.ActionButton
import com.minhtrong67.moody.ui.components.DropdownField
import com.minhtrong67.moody.ui.components.GroupTitle
import com.minhtrong67.moody.ui.components.MoodyCard
import com.minhtrong67.moody.ui.components.PalettePreview
import com.minhtrong67.moody.ui.components.ScrollToTopButton
import com.minhtrong67.moody.ui.components.SegmentedTabs
import com.minhtrong67.moody.ui.components.StaggerIn
import com.minhtrong67.moody.ui.components.TimePickerDialog
import com.minhtrong67.moody.ui.components.UserAvatar
import com.minhtrong67.moody.ui.theme.Fonts
import com.minhtrong67.moody.ui.theme.LocalIsDark
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
    val activityCtx = LocalActivityContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val moods = LocalMoods.current
    val activities = LocalActivities.current
    val cs = MaterialTheme.colorScheme
    val dark = LocalIsDark.current

    var colorsOpen by rememberSaveable { mutableStateOf(false) }
    var dataOpen by rememberSaveable { mutableStateOf(false) }
    var format by rememberSaveable { mutableStateOf("csv") }
    var showTime by remember { mutableStateOf(false) }

    // ---------------- reminders: permission state (refreshed whenever we come back from system settings) ----------------
    var notifOk by remember { mutableStateOf(Notifications.canPost(ctx)) }
    var exactOk by remember { mutableStateOf(ReminderScheduler.canScheduleExact(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifOk = Notifications.canPost(ctx)
        exactOk = ReminderScheduler.canScheduleExact(ctx)
        vm.rescheduleReminders()
    }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deniedMsg = stringResource(R.string.settings_notifDenied)
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifOk = Notifications.canPost(ctx)
        if (granted) afterPermission?.invoke() else toaster.show(deniedMsg)
        afterPermission = null
    }
    fun withNotifPermission(then: () -> Unit) {
        val needs = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needs) { afterPermission = then; permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } else then()
    }
    fun launchSystem(intent: Intent) { runCatching { activityCtx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

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
                var known: List<ActivityDef> = activities
                data[Dataset.Activities]?.let { raw ->
                    val n = DataTransfer.normalizeActivities(raw)
                    skipped += n.skipped
                    if (n.items.isNotEmpty()) {
                        val (a, u) = vm.importActivities(n.items)
                        added += a; updated += u
                        val base = known
                        known = base + n.items.filter { i -> i.id != null && base.none { it.id == i.id } }
                            .map { ActivityDef(it.id!!, it.emoji, it.label, true) }
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

    val scroll = rememberScrollState()
    val showTop by remember { derivedStateOf { scroll.value > 500 } }
    val anyReminder = settings.dailyEnabled || settings.weeklyEnabled || settings.monthlyEnabled

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))

            // Account
            StaggerIn(0) {
                MoodyCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (user != null) UserAvatar(user, 48.dp) else Icon(Icons.Rounded.AccountCircle, null, Modifier.size(48.dp), tint = cs.primary)
                        Column(Modifier.weight(1f)) {
                            if (user != null) {
                                Text(user.displayName ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(user.email ?: "", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(stringResource(R.string.settings_account), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.settings_guestMode), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = { vm.logout() }) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp), tint = cs.error)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.common_logout), color = cs.error)
                        }
                    }
                    if (user == null && vm.firebaseReady) {
                        Spacer(Modifier.height(12.dp))
                        ActionButton(stringResource(R.string.settings_signIn), Icons.Rounded.AccountCircle, { vm.signInWithGoogle(activityCtx) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Column {
                GroupTitle(stringResource(R.string.settings_appearance))
                SegmentedTabs(
                    listOf("light" to stringResource(R.string.theme_lightShort), "dark" to stringResource(R.string.theme_darkShort), "system" to stringResource(R.string.theme_systemShort)),
                    settings.themeMode, { vm.setThemeMode(it) },
                    icons = mapOf("light" to Icons.Rounded.LightMode, "dark" to Icons.Rounded.DarkMode, "system" to Icons.Rounded.DesktopWindows),
                )
            }

            Column {
                GroupTitle(stringResource(R.string.settings_themeColor))
                MoodyCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { colorsOpen = !colorsOpen }, contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val current = Palettes.byId(settings.themeColor)
                        PalettePreview(current, dark, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(colorName(ctx, current.id), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(if (colorsOpen) R.string.settings_themeColorCollapse else R.string.settings_themeColorExpand), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        val rot by animateFloatAsState(if (colorsOpen) 180f else 0f, label = "chev")
                        Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(rot), tint = cs.onSurfaceVariant)
                    }
                    AnimatedVisibility(colorsOpen) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Palettes.all.forEach { p ->
                                    val sel = p.id == settings.themeColor
                                    Box(
                                        Modifier.size(48.dp).clip(CircleShape)
                                            .then(if (sel) Modifier.border(3.dp, cs.onSurface, CircleShape) else Modifier)
                                            .clickable { vm.setThemeColor(p.id) }.padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        PalettePreview(p, dark, 40.dp)
                                        if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.settings_themeColorHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }

            Column {
                GroupTitle(stringResource(R.string.settings_language))
                SegmentedTabs(
                    listOf("auto" to stringResource(R.string.settings_langAuto), "vi" to "Tiếng Việt", "en" to "English"),
                    settings.language, { vm.setLanguage(it) },
                )
                if (settings.language == "auto") {
                    Text(stringResource(R.string.settings_langAutoHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                }
            }

            Column {
                GroupTitle(stringResource(R.string.settings_fontFamily))
                DropdownField(
                    stringResource(R.string.settings_fontFamily),
                    Fonts.ids.map { it to fontLabel(it) },
                    settings.fontFamily, { vm.setFontFamily(it) }, Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                MoodyCard(Modifier.fillMaxWidth(), containerColor = cs.surfaceContainerHigh, contentPadding = 16.dp) {
                    Text(stringResource(R.string.settings_fontPreview), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_fontFamilyHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }

            Column {
                GroupTitle(stringResource(R.string.settings_fontSize))
                val tf = Icons.Rounded.TextFields
                SegmentedTabs(
                    listOf("0.9" to stringResource(R.string.settings_font_0_9), "1.0" to stringResource(R.string.settings_font_1), "1.15" to stringResource(R.string.settings_font_1_15)),
                    settings.fontScale.toString(), { vm.setFontScale(it.toFloat()) },
                    icons = mapOf("0.9" to tf, "1.0" to tf, "1.15" to tf),
                )
            }

            Column {
                GroupTitle(stringResource(R.string.settings_startPage))
                Text(stringResource(R.string.settings_startPageText), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                DropdownField(
                    stringResource(R.string.settings_startPage),
                    START_PAGES.map { it to stringResource(startPageLabel(it)) },
                    settings.startPage, { vm.setStartPage(it) }, Modifier.fillMaxWidth(),
                )
            }

            Column {
                GroupTitle(stringResource(R.string.nav_manage))
                MoodyCard(Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    NavRow(Icons.Rounded.Checklist, stringResource(R.string.nav_activities), onOpenActivities)
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                    NavRow(Icons.Rounded.EmojiEmotions, stringResource(R.string.nav_moods), onOpenMoods)
                }
            }

            // Reminders
            Column {
                GroupTitle(stringResource(R.string.settings_reminders))
                Text(stringResource(R.string.settings_remindersHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                MoodyCard(Modifier.fillMaxWidth().animateContentSize(), contentPadding = 8.dp) {
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
                }
                if (anyReminder) {
                    Spacer(Modifier.height(12.dp))
                    MoodyCard(Modifier.fillMaxWidth(), containerColor = cs.surfaceContainerHigh, contentPadding = 14.dp) {
                        if (!notifOk) {
                            Text(stringResource(R.string.settings_notifBlocked), style = MaterialTheme.typography.bodyMedium, color = cs.error)
                            Spacer(Modifier.height(8.dp))
                            ActionButton(
                                stringResource(R.string.settings_notifOpenSettings), Icons.Rounded.Notifications,
                                {
                                    launchSystem(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName))
                                },
                                tonal = true, modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        if (Build.VERSION.SDK_INT >= 31) {
                            if (exactOk) {
                                Text(stringResource(R.string.settings_exactAlarmOk), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            } else {
                                Text(stringResource(R.string.settings_exactAlarmHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                ActionButton(
                                    stringResource(R.string.settings_exactAlarm), Icons.Rounded.Check,
                                    {
                                        launchSystem(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                                    },
                                    tonal = true, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(stringResource(R.string.settings_batteryHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        ActionButton(
                            stringResource(R.string.settings_batteryBtn), Icons.Rounded.Notifications,
                            { launchSystem(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                            tonal = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        stringResource(R.string.settings_remindTest), Icons.Rounded.Notifications,
                        {
                            withNotifPermission {
                                toaster.show(ctx.getString(if (vm.sendTestNotification(settings.language)) R.string.settings_testSent else R.string.settings_notifDenied))
                            }
                        },
                        tonal = true, modifier = Modifier.weight(1f),
                    )
                    ActionButton(
                        stringResource(R.string.settings_remindTestAlarm), Icons.Rounded.Notifications,
                        { withNotifPermission { vm.scheduleTestAlarm(); toaster.show(ctx.getString(R.string.settings_testScheduled)) } },
                        tonal = true, modifier = Modifier.weight(1f),
                    )
                }
            }

            // Data
            Column {
                GroupTitle(stringResource(R.string.settings_data))
                MoodyCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { dataOpen = !dataOpen }, contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconDisc(Icons.Rounded.FileDownload)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_exportImport), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.settings_exportImportSub), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val rot by animateFloatAsState(if (dataOpen) 180f else 0f, label = "chev2")
                        Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(rot), tint = cs.onSurfaceVariant)
                    }
                    AnimatedVisibility(dataOpen) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            Text(stringResource(R.string.settings_dataHint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            SegmentedTabs(listOf("csv" to "CSV", "json" to "JSON"), format, { format = it })
                            Spacer(Modifier.height(8.dp))
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
                            DataRow(
                                stringResource(R.string.settings_all), onExport = { exportAll() }, onImport = { pickImport("all") },
                                exportLabel = stringResource(R.string.settings_exportAll), importLabel = stringResource(R.string.settings_importAll),
                            )
                            Text(
                                stringResource(if (format == "json") R.string.settings_allHint_json else R.string.settings_allHint_csv),
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            Column {
                GroupTitle(stringResource(R.string.settings_about))
                MoodyCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconDisc(Icons.Rounded.Favorite)
                        Column(Modifier.weight(1f)) {
                            Text("Moody", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            CreditLine()
                            Text("v1.0.0", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        ScrollToTopButton(showTop, { scope.launch { scroll.animateScrollTo(0) } }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

private fun fontLabel(id: String) = when (id) {
    "google-sans-flex" -> "Google Sans Flex"
    "roboto" -> "Roboto"
    "inter" -> "Inter"
    "be-vietnam-pro" -> "Be Vietnam Pro"
    else -> "System"
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
private fun IconDisc(icon: ImageVector) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** "Developed by [minhtrong67], with help from [✨ Claude AI]." — names rendered as chips, words wrap naturally. */
@Composable
private fun CreditLine() {
    val cs = MaterialTheme.colorScheme
    val a = "\u0001a\u0001"
    val b = "\u0001b\u0001"
    val raw = stringResource(R.string.settings_credit, a, b)
    val tokens = remember(raw) {
        raw.split(Regex("(?=\u0001[ab]\u0001)|(?<=\u0001[ab]\u0001)")).filter { it.isNotEmpty() }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tokens.forEach { t ->
            when (t) {
                a -> NameChip("minhtrong67", null)
                b -> NameChip("Claude AI", Icons.Rounded.AutoAwesome)
                else -> t.trim().split(" ").filter { it.isNotEmpty() }.forEach { w ->
                    Text(w, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NameChip(text: String, icon: ImageVector?) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).background(cs.primary.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = cs.primary)
        Text(text, style = MaterialTheme.typography.labelLarge, color = cs.primary)
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
