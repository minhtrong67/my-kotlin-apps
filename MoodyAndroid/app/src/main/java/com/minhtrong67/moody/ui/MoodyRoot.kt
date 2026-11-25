@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.ActivitiesState
import com.minhtrong67.moody.data.AppUser
import com.minhtrong67.moody.data.local.Settings
import com.minhtrong67.moody.ui.components.UserAvatar
import com.minhtrong67.moody.ui.screens.CheckInScreen
import com.minhtrong67.moody.ui.screens.HistoryScreen
import com.minhtrong67.moody.ui.screens.LoginScreen
import com.minhtrong67.moody.ui.screens.ManageActivitiesScreen
import com.minhtrong67.moody.ui.screens.ManageMoodsScreen
import com.minhtrong67.moody.ui.screens.SettingsScreen
import com.minhtrong67.moody.ui.screens.StatsScreen
import com.minhtrong67.moody.ui.screens.TodayScreen
import com.minhtrong67.moody.util.effectiveLanguage
import kotlinx.coroutines.launch

private const val R_TABS = "tabs"
private const val R_TODAY = "today"
private const val R_HISTORY = "history"
private const val R_STATS = "stats"
private const val R_SETTINGS = "settings"
private const val R_ACTIVITIES = "activities"
private const val R_MOODS = "moods"

private class Tab(val route: String, val label: Int, val icon: ImageVector, val activeIcon: ImageVector)

private val TABS = listOf(
    Tab(R_TODAY, R.string.nav_today, Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(R_HISTORY, R.string.nav_history, Icons.Outlined.History, Icons.Rounded.History),
    Tab(R_STATS, R.string.nav_stats, Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    Tab(R_SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings),
)

private fun NavHostController.goManage(route: String) = navigate(route) { launchSingleTop = true }

@Composable
fun MoodyRoot(vm: MainViewModel, settings: Settings, pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val user by vm.user.collectAsStateWithLifecycle()
    val initializing by vm.authInitializing.collectAsStateWithLifecycle()
    val activitiesState by vm.activitiesState.collectAsStateWithLifecycle()
    val moodOverrides by vm.moodOverrides.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]

    // Remembered: these feed static composition locals, a new list instance would recompose the whole tree.
    val moods = resolveMoods(moodOverrides ?: emptyMap()).let { r -> remember(r) { r } }
    val activities = resolveActivities(activitiesState ?: ActivitiesState()).let { r -> remember(r) { r } }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toaster = remember { Toaster(snackbar, scope) }
    val authed = user != null || settings.guest

    CompositionLocalProvider(
        LocalMoods provides moods,
        LocalActivities provides activities,
        LocalLocale provides locale,
        LocalToaster provides toaster,
    ) {
        when {
            initializing -> Splash()
            !authed -> LoginScreen(vm)
            else -> MainShell(vm, settings, snackbar, pendingRoute, onRouteConsumed)
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.mipmap.ic_logo), null, Modifier.size(64.dp))
    }
}

@Composable
private fun MainShell(
    vm: MainViewModel,
    settings: Settings,
    snackbar: SnackbarHostState,
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val toaster = LocalToaster.current
    val cs = MaterialTheme.colorScheme
    val user by vm.user.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val moodOverrides by vm.moodOverrides.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // The four tabs live in ONE pager (swipe left/right to switch); Manage pages are regular destinations on top.
    val startPage = remember {
        when (settings.startPage) { "history" -> 1; "stats" -> 2; "settings" -> 3; else -> 0 }
    }
    val pager = rememberPagerState(initialPage = startPage) { TABS.size }
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: R_TABS
    val onTabs = route == R_TABS
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val page = pager.currentPage
    LaunchedEffect(route, page) { scrollBehavior.state.contentOffset = 0f }   // a new page starts un-scrolled

    fun goPage(i: Int) {
        if (onTabs) scope.launch { pager.animateScrollToPage(i) }
        else { nav.popBackStack(R_TABS, false); scope.launch { pager.scrollToPage(i) } }
    }
    BackHandler(enabled = onTabs && page != startPage && drawer.isClosed) { goPage(startPage) }

    // ---- check-in is a full-screen overlay (no Scaffold padding jumps, one smooth slide) ----
    var checkInKey by rememberSaveable { mutableStateOf<String?>(null) }
    val keyHolder = remember { arrayOfNulls<String>(1) }
    if (checkInKey != null) keyHolder[0] = checkInKey
    fun openCheckIn(id: String? = null, date: String? = null) { checkInKey = "${id.orEmpty()}\n${date.orEmpty()}" }

    var startHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!startHandled) {
            startHandled = true
            when (settings.startPage) {
                "checkin" -> if (pendingRoute == null) openCheckIn()
                "activities" -> nav.goManage(R_ACTIVITIES)
                "moods" -> nav.goManage(R_MOODS)
            }
        }
    }
    LaunchedEffect(pendingRoute) {   // taps on a reminder notification
        when (pendingRoute) {
            "checkin" -> openCheckIn()
            "stats" -> goPage(2)
            "today" -> goPage(0)
        }
        if (pendingRoute != null) onRouteConsumed()
    }

    val title = if (onTabs) TABS[page].label else if (route == R_ACTIVITIES) R.string.nav_activities else R.string.nav_moods
    val lang = effectiveLanguage(settings.language)

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = drawer.isOpen,   // opened by the menu button only (edge swipes clash with the pager / swipe rows)
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = cs.surfaceContainerLow,
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth(0.82f),
                ) {
                    DrawerBody(
                        selectedKey = if (onTabs) TABS[page].route else route, user = user,
                        onNavigate = { r ->
                            scope.launch { drawer.close() }
                            val i = TABS.indexOfFirst { it.route == r }
                            if (i >= 0) goPage(i) else nav.goManage(r)
                        },
                        onLogout = { scope.launch { drawer.close() }; vm.logout() },
                    )
                }
            },
        ) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = cs.surface,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    MoodyTopBar(
                        title = stringResource(title), lang = lang, themeMode = settings.themeMode, scrollBehavior = scrollBehavior,
                        onMenu = { scope.launch { drawer.open() } },
                        onLang = { vm.setLanguage(if (lang == "vi") "en" else "vi") },
                        onTheme = { vm.setThemeMode(when (settings.themeMode) { "light" -> "dark"; "dark" -> "system"; else -> "light" }) },
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = cs.surfaceContainer) {
                        TABS.forEachIndexed { i, t ->
                            val sel = onTabs && page == i
                            NavigationBarItem(
                                selected = sel,
                                onClick = { if (!sel) goPage(i) },
                                icon = { Icon(if (sel) t.activeIcon else t.icon, null) },
                                label = { Text(stringResource(t.label), fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = onTabs && (page == 0 || page == 1),
                        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(),
                        exit = scaleOut(tween(120)) + fadeOut(tween(120)),
                    ) {
                        FloatingActionButton(
                            onClick = { openCheckIn() },
                            containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        ) { Icon(Icons.Rounded.Add, stringResource(R.string.nav_newCheckin)) }
                    }
                },
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = R_TABS,
                    modifier = Modifier.fillMaxSize().padding(inner),
                    // Material "fade through": outgoing page fades quickly, incoming fades + settles from 96%.
                    enterTransition = { fadeIn(tween(230, delayMillis = 70)) + scaleIn(tween(230, delayMillis = 70, easing = FastOutSlowInEasing), initialScale = 0.96f) },
                    exitTransition = { fadeOut(tween(70)) },
                    popEnterTransition = { fadeIn(tween(230, delayMillis = 70)) + scaleIn(tween(230, delayMillis = 70, easing = FastOutSlowInEasing), initialScale = 0.96f) },
                    popExitTransition = { fadeOut(tween(70)) },
                ) {
                    composable(R_TABS) {
                        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { p ->
                            when (p) {
                                0 -> TodayScreen(
                                    entries = entries, user = user,
                                    onCheckIn = { id -> openCheckIn(id) },
                                    onViewAll = { goPage(1) },
                                    onAvatar = { goPage(3) },
                                )
                                1 -> {
                                    val deleted = stringResource(R.string.history_deleted)
                                    HistoryScreen(
                                        entries = entries,
                                        onEdit = { id -> openCheckIn(id) },
                                        onNewForDate = { date -> openCheckIn(date = date) },
                                        onDelete = { id -> vm.deleteEntry(id); toaster.show(deleted) },
                                    )
                                }
                                2 -> StatsScreen(entries)
                                else -> SettingsScreen(
                                    vm = vm, settings = settings, user = user, entries = entries,
                                    onOpenActivities = { nav.goManage(R_ACTIVITIES) },
                                    onOpenMoods = { nav.goManage(R_MOODS) },
                                )
                            }
                        }
                    }
                    composable(R_ACTIVITIES) {
                        ManageActivitiesScreen(
                            onAdd = { e, l -> vm.addActivity(e, l) },
                            onUpdate = { id, e, l -> vm.updateActivity(id, e, l) },
                            onRemove = { vm.removeActivity(it) },
                            onReorder = { vm.reorderActivities(it) },
                        )
                    }
                    composable(R_MOODS) {
                        ManageMoodsScreen(
                            overridden = moodOverrides?.keys ?: emptySet(),
                            onUpdate = { k, e, l -> vm.updateMood(k, e, l) },
                            onReset = { vm.resetMood(it) },
                        )
                    }
                }
            }
        }

        // Check-in overlay
        val ctxStrings = stringResource(R.string.checkin_saved) to stringResource(R.string.checkin_updated)
        AnimatedVisibility(
            visible = checkInKey != null,
            enter = slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(200)),
        ) {
            val key = keyHolder[0] ?: return@AnimatedVisibility
            val parts = key.split("\n")
            val id = parts.getOrNull(0)?.ifBlank { null }
            val date = parts.getOrNull(1)?.ifBlank { null }
            Surface(Modifier.fillMaxSize(), color = cs.surface) {
                CheckInScreen(
                    entries = entries, id = id, dateArg = date,
                    onSave = { existingId, data, isUpdate ->
                        if (existingId != null) vm.updateEntry(existingId, data) else vm.addEntry(data)
                        toaster.show(if (isUpdate) ctxStrings.second else ctxStrings.first)
                        checkInKey = null
                    },
                    onAddActivity = { e, l -> vm.addActivity(e, l) },
                    onClose = { checkInKey = null },
                    onManageActivities = { checkInKey = null; nav.goManage(R_ACTIVITIES) },
                )
            }
        }
    }
}

/** Top app bar: menu · page title (primary, animated) · language · theme — turns surfaceContainer once content scrolls under it. */
@Composable
private fun MoodyTopBar(
    title: String,
    lang: String,
    themeMode: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onMenu: () -> Unit,
    onLang: () -> Unit,
    onTheme: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = title,
                transitionSpec = { (fadeIn(tween(220, 60)) + slideInVertically(tween(260)) { it / 3 }) togetherWith fadeOut(tween(90)) },
                label = "title",
            ) { t ->
                Text(t, style = MaterialTheme.typography.headlineSmall, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Rounded.Menu, stringResource(R.string.nav_openMenu)) } },
        actions = {
            TextButton(onClick = onLang) {
                Text(lang.uppercase(), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            }
            IconButton(onClick = onTheme) {
                AnimatedContent(
                    targetState = themeMode,
                    transitionSpec = { (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.6f)) togetherWith (fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.6f)) },
                    label = "themeIcon",
                ) { m ->
                    Icon(
                        when (m) { "light" -> Icons.Rounded.LightMode; "dark" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.DesktopWindows },
                        stringResource(R.string.common_themeCycle), tint = cs.onSurfaceVariant,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, scrolledContainerColor = cs.surfaceContainer),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun DrawerBody(selectedKey: String, user: AppUser?, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.mipmap.ic_logo), null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
            Text("Moody", style = MaterialTheme.typography.headlineSmall, color = cs.primary)
        }
        TABS.forEach { t -> DrawerItem(t.activeIcon, stringResource(t.label), selectedKey == t.route) { onNavigate(t.route) } }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = cs.outlineVariant)
        Text(stringResource(R.string.nav_manage), style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        DrawerItem(Icons.Rounded.Checklist, stringResource(R.string.nav_activities), selectedKey == R_ACTIVITIES) { onNavigate(R_ACTIVITIES) }
        DrawerItem(Icons.Rounded.EmojiEmotions, stringResource(R.string.nav_moods), selectedKey == R_MOODS) { onNavigate(R_MOODS) }
        Spacer(Modifier.weight(1f))
        if (user != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerHigh).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UserAvatar(user, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(user.displayName ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user.email ?: "", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.common_logout), tint = cs.onSurfaceVariant) }
            }
        } else {
            Text(stringResource(R.string.nav_tagline), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by androidx.compose.animation.animateColorAsState(if (selected) cs.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent, tween(200), label = "drawerBg")
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant)
    }
}
