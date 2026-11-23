package com.expenser.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenser.app.BuildConfig
import com.expenser.app.R
import com.expenser.app.data.CsvIo
import com.expenser.app.data.TxType
import com.expenser.app.ui.*
import com.expenser.app.ui.components.*
import com.expenser.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private val ACCENTS = listOf(
    "" to "#4CAF50", "#F44336" to "#F44336", "#E91E63" to "#E91E63", "#9C27B0" to "#9C27B0", "#673AB7" to "#673AB7",
    "#3F51B5" to "#3F51B5", "#2196F3" to "#2196F3", "#03A9F4" to "#03A9F4", "#00BCD4" to "#00BCD4", "#009688" to "#009688",
    "#8BC34A" to "#8BC34A", "#CDDC39" to "#CDDC39", "#FFEB3B" to "#FFEB3B", "#FFC107" to "#FFC107", "#FF9800" to "#FF9800",
    "#FF5722" to "#FF5722", "#795548" to "#795548", "#607D8B" to "#607D8B",
)

@Composable
private fun Group(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }?.substringBefore('-')
        ?: context.resources.configuration.locales[0].language.let { if (it == "en") "en" else "vi" }

    // --- export / import ---
    val headers = listOf(R.string.export_col_date, R.string.export_col_time, R.string.export_col_type, R.string.export_col_category, R.string.export_col_wallet, R.string.export_col_amount, R.string.export_col_note).map { stringResource(it) }
    val typeLabels = mapOf(TxType.INCOME to stringResource(R.string.export_type_income), TxType.EXPENSE to stringResource(R.string.export_type_expense), TxType.TRANSFER to stringResource(R.string.tx_transfer))
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val cats = vm.categories.value.associateBy { it.id }; val wal = vm.wallets.value.associateBy { it.id }
                val csv = CsvIo.build(headers, vm.transactions.value, { cats[it]?.name ?: "" }, { wal[it]?.name ?: "" }, { typeLabels[it] ?: it })
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) } }
                vm.toast(R.string.settings_exported)
            } catch (e: Exception) { vm.toast(R.string.settings_export_error) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: "" }
                vm.importCsv(text)
            } catch (e: Exception) { vm.toast(R.string.import_error) }
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminder(true, settings.reminderTime) else vm.toast(R.string.reminder_denied)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Group(stringResource(R.string.settings_manage), Icons.Rounded.Tune) {
            @Composable fun Link(label: String, icon: ImageVector, route: String) {
                ListItem(
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { onNavigate(route) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(icon, null) }, headlineContent = { Text(label) }, trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                )
            }
            Link(stringResource(R.string.nav_categories), Icons.Rounded.Category, ROUTE_CATEGORIES)
            Link(stringResource(R.string.nav_cafe), Icons.Rounded.LocalCafe, ROUTE_CAFE)
            Link(stringResource(R.string.nav_cafe_shops), Icons.Rounded.Storefront, ROUTE_CAFE_SHOPS)
            Link(stringResource(R.string.nav_cafe_drinks), Icons.Rounded.Coffee, ROUTE_CAFE_DRINKS)
        }

        Group(stringResource(R.string.settings_language), Icons.Rounded.Language) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("vi" to "Tiếng Việt", "en" to "English").forEach { (code, label) ->
                    FilterChip(currentLang == code, { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code)) }, { Text(label) })
                }
            }
        }

        Group(stringResource(R.string.settings_appearance), Icons.Rounded.Palette) {
            Text(stringResource(R.string.settings_mode), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("light" to R.string.settings_mode_light, "dark" to R.string.settings_mode_dark, "system" to R.string.settings_mode_system).forEachIndexed { i, (id, res) ->
                    SegmentedButton(settings.themeMode == id, { vm.setThemeMode(id) }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(stringResource(res)) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_dynamic), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.settings_dynamic_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(settings.dynamicColor, { vm.setDynamicColor(it) })
                }
            }
            if (!settings.dynamicColor) {
                Text(stringResource(R.string.settings_color), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_color_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENTS.forEach { (value, swatch) ->
                        val on = settings.accent.equals(value, ignoreCase = true)
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(swatch)))
                                .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { vm.setAccent(value) },
                            contentAlignment = Alignment.Center,
                        ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("sm" to R.string.settings_font_sm, "md" to R.string.settings_font_md, "lg" to R.string.settings_font_lg).forEachIndexed { i, (id, res) ->
                    SegmentedButton(settings.fontSize == id, { vm.setFontSize(id) }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(stringResource(res)) }
                }
            }
            TextButton(onClick = { confirmReset = true }) { Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_reset_btn)) }
        }

        Group(stringResource(R.string.reminder_title_setting), Icons.Rounded.Notifications) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_enable), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.reminder_enable_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(settings.reminderEnabled, { on ->
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else { vm.setReminder(on, settings.reminderTime); vm.toast(if (on) R.string.reminder_on else R.string.reminder_off, settings.reminderTime) }
                })
            }
            if (settings.reminderEnabled) {
                OutlinedButton(onClick = { showTime = true }) { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.reminder_time) + ": " + settings.reminderTime) }
                TextButton(onClick = { vm.sendTestReminder() }) { Text(stringResource(R.string.reminder_test)) }
            }
        }

        Group(stringResource(R.string.settings_data), Icons.Rounded.Storage) {
            Text(stringResource(R.string.settings_import_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {
                    if (vm.transactions.value.isEmpty()) vm.toast(R.string.settings_export_empty)
                    else exportLauncher.launch("expenser-${Format.date(System.currentTimeMillis()).replace('/', '-')}.csv")
                }) { Icon(Icons.Rounded.FileUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_export)) }
                FilledTonalButton(onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel", "*/*")) }) {
                    Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_import))
                }
            }
        }

        Group(stringResource(R.string.settings_about), Icons.Rounded.Info) {
            Text("Expenser · ${stringResource(R.string.settings_version)} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.settings_about_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmReset) ConfirmDialog(stringResource(R.string.settings_reset_confirm), stringResource(R.string.settings_reset_btn), onConfirm = { vm.resetAppearance() }, onDismiss = { confirmReset = false })
    if (showTime) {
        val (h, m) = settings.reminderTime.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 6) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        val state = rememberTimePickerState(h, m, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = {
                val t = "%02d:%02d".format(state.hour, state.minute)
                vm.setReminder(true, t); vm.toast(R.string.reminder_time_saved, t); showTime = false
            }) { Text(stringResource(R.string.common_ok)) } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.common_cancel)) } },
            text = { TimePicker(state) },
        )
    }
}
