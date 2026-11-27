package com.gnort67.calendarpro.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.gnort67.calendarpro.MainViewModel
import com.gnort67.calendarpro.R
import com.gnort67.calendarpro.core.Holiday
import com.gnort67.calendarpro.core.PersonalEvent
import com.gnort67.calendarpro.data.AppSettings
import com.gnort67.calendarpro.ui.components.AppIcon
import com.gnort67.calendarpro.ui.components.pressable
import com.gnort67.calendarpro.ui.screens.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private class Tab(val title: Int, val label: Int, val icon: String)

private val TABS = listOf(
    Tab(R.string.nav_home, R.string.nav_home, "home"),
    Tab(R.string.nav_calendar_full, R.string.nav_calendar, "calendar_month"),
    Tab(R.string.nav_van_nien_full, R.string.nav_van_nien, "auto_stories"),
    Tab(R.string.nav_countdown_full, R.string.nav_countdown, "hourglass_top")
)

private class EditorState(val event: PersonalEvent?)

@Composable
private fun rememberToday(): State<LocalDate> {
    val s = remember { mutableStateOf(LocalDate.now()) }
    @Suppress("DEPRECATION")
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { s.value = LocalDate.now(); delay(30_000) }
        }
    }
    return s
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, s: AppSettings) {
    val events by vm.events.collectAsState()
    val lang = s.language
    val today by rememberToday()
    val pager = rememberPagerState(pageCount = { TABS.size })
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Holiday?>(null) }
    var daySheet by remember { mutableStateOf<LocalDate?>(null) }
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var deleting by remember { mutableStateOf<PersonalEvent?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val refreshed = stringResource(R.string.home_refresh_success)

    // Áp dụng ngôn ngữ cho toàn app (Android 13+ dùng LocaleManager, bản cũ dùng AppCompat).
    LaunchedEffect(lang) {
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!cur.startsWith(lang)) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    fun message(text: String) {
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(text) }
    }
    fun goTab(i: Int) {
        showSettings = false
        scope.launch { pager.animateScrollToPage(i, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) }
    }

    // Back: đóng Cài đặt (có hiệu ứng thu nhỏ theo cử chỉ back dự đoán), hoặc về tab Trang chủ.
    PredictiveBackHandler(enabled = showSettings && !drawer.isOpen) { progress ->
        try {
            progress.collect { e: BackEventCompat -> backProgress = e.progress }
            showSettings = false
            backProgress = 0f
        } catch (c: CancellationException) {
            backProgress = 0f
        }
    }
    BackHandler(enabled = !showSettings && pager.currentPage != 0 && !drawer.isOpen) { goTab(0) }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = navBottom + 16.dp + 76.dp

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = (pager.currentPage == 0 && !showSettings) || drawer.isOpen,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp)) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(stringResource(R.string.app_name), Modifier.padding(start = 16.dp, top = 24.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.app_tagline), Modifier.padding(start = 16.dp, bottom = 20.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    TABS.forEachIndexed { i, t ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(t.title)) },
                            selected = !showSettings && pager.currentPage == i,
                            icon = { AppIcon(t.icon, filled = !showSettings && pager.currentPage == i) },
                            onClick = { scope.launch { drawer.close() }; goTab(i) },
                            shape = CircleShape
                        )
                    }
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_settings)) }, selected = showSettings,
                        icon = { AppIcon("settings", filled = showSettings) },
                        onClick = { scope.launch { drawer.close() }; showSettings = true }, shape = CircleShape
                    )
                    Text(stringResource(R.string.app_tip_text), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }
    ) {
        Box(Modifier.fillMaxSize().background(cs.surface)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Header(
                    title = stringResource(if (showSettings) R.string.nav_settings else TABS[pager.currentPage].title),
                    themeMode = s.themeMode, settingsOpen = showSettings,
                    onMenu = { scope.launch { drawer.open() } },
                    onTheme = {
                        val next = when (s.themeMode) { "light" -> "dark"; "dark" -> "system"; else -> "light" }
                        vm.update { it.copy(themeMode = next) }
                    },
                    onSettings = { showSettings = !showSettings }
                )
                Box(Modifier.weight(1f)) {
                    HorizontalPager(pager, Modifier.fillMaxSize(), key = { it }) { page ->
                        when (page) {
                            0 -> HomeScreen(vm.catalog, events, lang, today, bottomInset, { daySheet = it }, { detail = it }, { goTab(3) }, { message(refreshed) })
                            1 -> CalendarScreen(vm.catalog, events, lang, today, bottomInset) { daySheet = it }
                            2 -> VanNienScreen(vm.catalog, events, lang, today, bottomInset, { detail = it }, ::message)
                            else -> CountdownScreen(
                                vm.catalog, events, lang, today, bottomInset, { detail = it },
                                { editor = EditorState(null) }, { editor = EditorState(it) }, { deleting = it }
                            )
                        }
                    }
                    // Cài đặt trượt vào từ phải; khi back dự đoán sẽ thu nhỏ và dịch theo cử chỉ.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } + fadeIn(),
                        exit = slideOutHorizontally { it / 3 } + fadeOut()
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(cs.surface).graphicsLayer {
                                val sc = 1f - 0.12f * backProgress
                                scaleX = sc; scaleY = sc; translationX = backProgress * 48.dp.toPx(); alpha = 1f - 0.2f * backProgress
                            }
                        ) {
                            SettingsScreen(s, { block -> vm.update(block) }, { vm.resetSettings() }, bottomInset, ::message)
                        }
                    }
                }
            }

            BottomBar(
                selected = if (showSettings) -1 else pager.currentPage,
                onSelect = { if (it != pager.currentPage || showSettings) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); goTab(it) } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navBottom + 12.dp)
            )
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 4.dp, start = 16.dp, end = 16.dp))
        }
    }

    // ---- Sheets & dialogs ----
    daySheet?.let { d ->
        val info = remember(d, events, lang) { buildDayInfo(d, vm.catalog, events, lang) }
        ModalBottomSheet(
            onDismissRequest = { daySheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cs.surfaceContainerLow, shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
        ) {
            DayDetailContent(
                info, vm.catalog, lang, { h -> daySheet = null; detail = h },
                Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp)
            )
        }
    }
    detail?.let { h ->
        HolidayDetailSheet(
            h, vm.catalog, events, lang, onDismiss = { detail = null },
            onEdit = { detail = null; editor = EditorState(it) }, onDelete = { deleting = it }, onMessage = ::message
        )
    }
    editor?.let { e ->
        val limit = stringResource(R.string.events_limit)
        val saved = stringResource(R.string.events_saved)
        EventFormSheet(e.event, onDismiss = { editor = null }, onMessage = ::message, onSave = { ev ->
            if (vm.saveEvent(ev)) { editor = null; message(saved) } else message(limit)
        })
    }
    deleting?.let { ev ->
        val done = stringResource(R.string.events_deleted)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.events_delete_title)) },
            text = { Text(stringResource(R.string.events_delete_body, ev.name)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteEvent(ev.id); deleting = null; detail = null; message(done) }) {
                    Text(stringResource(R.string.holiday_detail_remove), color = cs.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun Header(
    title: String, themeMode: String, settingsOpen: Boolean,
    onMenu: () -> Unit, onTheme: () -> Unit, onSettings: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).pressable(CircleShape, onClick = onMenu).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            AppIcon("menu", tint = cs.onSurface)
        }
        AnimatedContent(
            title, Modifier.weight(1f).padding(horizontal = 14.dp), label = "title",
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) }
        ) { t -> Text(t, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
        Box(Modifier.size(48.dp).pressable(CircleShape, onClick = onTheme), contentAlignment = Alignment.Center) {
            AppIcon(when (themeMode) { "light" -> "light_mode"; "dark" -> "dark_mode"; else -> "brightness_auto" }, tint = cs.onSurface)
        }
        Spacer(Modifier.width(6.dp))
        val bg by animateColorAsState(if (settingsOpen) cs.primary else cs.primaryContainer, label = "settings-bg")
        Box(Modifier.size(56.dp).pressable(CircleShape, onClick = onSettings).background(bg), contentAlignment = Alignment.Center) {
            AppIcon("settings", tint = if (settingsOpen) cs.onPrimary else cs.onPrimaryContainer, filled = settingsOpen)
        }
    }
}

/** Thanh điều hướng nổi dạng viên thuốc với chỉ báo lò xo. */
@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = cs.surfaceContainerHigh, shadowElevation = 6.dp, tonalElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TABS.forEachIndexed { i, t ->
                val on = i == selected
                val ind by animateColorAsState(if (on) cs.primaryContainer else cs.primaryContainer.copy(alpha = 0f), spring(stiffness = Spring.StiffnessMedium), label = "ind")
                Column(
                    Modifier.weight(1f).pressable(RoundedCornerShape(24.dp)) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(64.dp).height(32.dp).clip(CircleShape).background(ind), contentAlignment = Alignment.Center) {
                        AppIcon(t.icon, filled = on, tint = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant)
                    }
                    Text(
                        stringResource(t.label), style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (on) cs.onSurface else cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
