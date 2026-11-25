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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.expenser.app.data.*
import com.expenser.app.data.TxType
import com.expenser.app.ui.*
import com.expenser.app.ui.components.*
import com.expenser.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private val ACCENTS = listOf(
    "" to "#0061A4", "#F44336" to "#F44336", "#E91E63" to "#E91E63", "#9C27B0" to "#9C27B0", "#673AB7" to "#673AB7",
    "#3F51B5" to "#3F51B5", "#2196F3" to "#2196F3", "#03A9F4" to "#03A9F4", "#00BCD4" to "#00BCD4", "#009688" to "#009688",
    "#8BC34A" to "#8BC34A", "#CDDC39" to "#CDDC39", "#FFEB3B" to "#FFEB3B", "#FFC107" to "#FFC107", "#FF9800" to "#FF9800",
    "#FF5722" to "#FF5722", "#795548" to "#795548", "#607D8B" to "#607D8B",
)

@Composable
private fun SettingCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

private class EntityInfo(val entity: Entity, val label: Int, val icon: ImageVector)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val wals by vm.wallets.collectAsStateWithLifecycle()
    val shops by vm.shops.collectAsStateWithLifecycle()
    val drinks by vm.drinks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var dataOpen by rememberSaveable { mutableStateOf(false) }

    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }?.substringBefore('-')
        ?: context.resources.configuration.locales[0].language.let { if (it == "en") "en" else "vi" }

    // ---- export / import ----
    val labels = TxLabels(
        listOf(R.string.export_col_date, R.string.export_col_time, R.string.export_col_type, R.string.export_col_category, R.string.export_col_wallet, R.string.export_col_amount, R.string.export_col_note).map { stringResource(it) },
        mapOf(TxType.INCOME to stringResource(R.string.export_type_income), TxType.EXPENSE to stringResource(R.string.export_type_expense), TxType.TRANSFER to stringResource(R.string.tx_transfer)),
    )
    var pendingExport by remember { mutableStateOf<ExportFile?>(null) }
    var pendingImport by remember { mutableStateOf<Entity?>(null) } // null + importAll flag below = everything
    var importAll by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val file = pendingExport; pendingExport = null
        if (uri == null || file == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(file.bytes) } }
                vm.toast(R.string.settings_exported)
            } catch (e: Exception) { vm.toast(R.string.settings_export_error) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: "" }
                if (importAll) vm.importAll(text) else pendingImport?.let { vm.importEntity(it, text) }
            } catch (e: Exception) { vm.toast(R.string.import_error) }
        }
    }
    val today = Format.date(System.currentTimeMillis()).replace('/', '-')
    fun export(entity: Entity?, json: Boolean) {
        val snap = vm.snapshot()
        val file = when {
            entity == null && json -> ExportFile("expenser-all-$today.json", DataIo.jsonAll(snap).toByteArray())
            entity == null -> ExportFile("expenser-all-$today.zip", DataIo.zipAllCsv(snap, labels))
            json -> ExportFile("expenser-${entity!!.key}-$today.json", DataIo.json(entity, snap).toByteArray())
            else -> ExportFile("expenser-${entity!!.key}-$today.csv", DataIo.csv(entity, snap, labels).toByteArray())
        }
        pendingExport = file
        exportLauncher.launch(file.name)
    }
    fun startImport(entity: Entity?) { importAll = entity == null; pendingImport = entity; importLauncher.launch(arrayOf("*/*")) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminder(true, settings.reminderTime) else vm.toast(R.string.reminder_denied)
    }

    val entities = listOf(
        EntityInfo(Entity.TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Rounded.ReceiptLong) to txs.size,
        EntityInfo(Entity.CATEGORIES, R.string.nav_categories, Icons.Rounded.Category) to cats.size,
        EntityInfo(Entity.WALLETS, R.string.nav_wallets, Icons.Rounded.AccountBalanceWallet) to wals.size,
        EntityInfo(Entity.SHOPS, R.string.nav_cafe_shops, Icons.Rounded.Storefront) to shops.size,
        EntityInfo(Entity.DRINKS, R.string.nav_cafe_drinks, Icons.Rounded.Coffee) to drinks.size,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- account ----
        SettingCard(stringResource(R.string.settings_account), Icons.Rounded.Person) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    if (user?.photoUrl != null) coil.compose.AsyncImage(user?.photoUrl, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else Text((user?.name?.firstOrNull() ?: user?.email?.firstOrNull() ?: '?').uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name?.ifBlank { null } ?: stringResource(R.string.settings_no_name), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { confirmSignOut = true }) { Icon(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.common_logout), tint = MaterialTheme.colorScheme.error) }
            }
        }

        // ---- language ----
        SettingCard(stringResource(R.string.settings_language), Icons.Rounded.Language) {
            PillSegmented(listOf(Seg("vi", "Tiếng Việt"), Seg("en", "English")), currentLang, { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) })
        }

        // ---- appearance ----
        SettingCard(stringResource(R.string.settings_appearance), Icons.Rounded.Palette) {
            Label(stringResource(R.string.settings_mode))
            PillSegmented(listOf(Seg("light", stringResource(R.string.settings_mode_light), Icons.Rounded.LightMode), Seg("dark", stringResource(R.string.settings_mode_dark), Icons.Rounded.DarkMode), Seg("system", stringResource(R.string.settings_mode_system), Icons.Rounded.BrightnessAuto)), settings.themeMode, { vm.setThemeMode(it) })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(stringResource(R.string.settings_dynamic), stringResource(R.string.settings_dynamic_hint), settings.dynamicColor) { vm.setDynamicColor(it) }
            }
            if (!settings.dynamicColor) {
                Label(stringResource(R.string.settings_color))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENTS.forEach { (value, swatch) ->
                        val on = settings.accent.equals(value, ignoreCase = true)
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(swatch)))
                                .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { vm.setAccent(value) },
                            contentAlignment = Alignment.Center,
                        ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Label(stringResource(R.string.settings_font_size))
            PillSegmented(listOf(Seg("sm", stringResource(R.string.settings_font_sm)), Seg("md", stringResource(R.string.settings_font_md)), Seg("lg", stringResource(R.string.settings_font_lg))), settings.fontSize, { vm.setFontSize(it) })
            TextButton(onClick = { confirmReset = true }, Modifier.align(Alignment.End)) { Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.settings_reset_btn)) }
        }

        // ---- reminder ----
        SettingCard(stringResource(R.string.reminder_title_setting), Icons.Rounded.Notifications) {
            SwitchRow(stringResource(R.string.reminder_enable), stringResource(R.string.reminder_enable_hint), settings.reminderEnabled) { on ->
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { vm.setReminder(on, settings.reminderTime); vm.toast(if (on) R.string.reminder_on else R.string.reminder_off, settings.reminderTime) }
            }
            if (settings.reminderEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTime = true }, Modifier.weight(1f)) { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(settings.reminderTime) }
                    FilledTonalButton(onClick = { vm.sendTestReminder() }, Modifier.weight(1f)) { Text(stringResource(R.string.reminder_test)) }
                }
            }
        }

        // ---- data (the only accordion) ----
        AccordionCard(stringResource(R.string.settings_data), Icons.Rounded.Storage, dataOpen, { dataOpen = !dataOpen }, subtitle = stringResource(R.string.data_subtitle)) {
            entities.forEach { (info, count) ->
                var menu by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(info.icon, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(info.label), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.data_count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        FilledTonalButton(onClick = { menu = true }, contentPadding = PaddingValues(horizontal = 12.dp)) { Icon(Icons.Rounded.FileUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.settings_export)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("CSV") }, { menu = false; export(info.entity, false) })
                            DropdownMenuItem({ Text("JSON") }, { menu = false; export(info.entity, true) })
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { startImport(info.entity) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.settings_import)) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
            Label(stringResource(R.string.data_all))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export(null, true) }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(stringResource(R.string.data_export_all_json), maxLines = 1) }
                Button(onClick = { export(null, false) }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(stringResource(R.string.data_export_all_csv), maxLines = 1) }
            }
            OutlinedButton(onClick = { startImport(null) }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.data_import_all)) }
            Text(stringResource(R.string.settings_import_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ---- about ----
        SettingCard(stringResource(R.string.settings_about), Icons.Rounded.Info) {
            Text("Expenser · ${stringResource(R.string.settings_version)} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.settings_about_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmSignOut) ConfirmDialog(stringResource(R.string.settings_logout_confirm), stringResource(R.string.common_logout), onConfirm = { vm.signOut(context) }, onDismiss = { confirmSignOut = false })
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
