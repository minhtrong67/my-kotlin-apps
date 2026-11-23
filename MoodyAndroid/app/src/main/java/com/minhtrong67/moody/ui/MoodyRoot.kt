@file:OptIn(ExperimentalMaterial3Api::class)

package com.minhtrong67.moody.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minhtrong67.moody.R
import com.minhtrong67.moody.data.local.Settings
import com.minhtrong67.moody.ui.screens.CheckInScreen
import com.minhtrong67.moody.ui.screens.HistoryScreen
import com.minhtrong67.moody.ui.screens.LoginScreen
import com.minhtrong67.moody.ui.screens.ManageActivitiesScreen
import com.minhtrong67.moody.ui.screens.ManageMoodsScreen
import com.minhtrong67.moody.ui.screens.SettingsScreen
import com.minhtrong67.moody.ui.screens.StatsScreen
import com.minhtrong67.moody.ui.screens.TodayScreen

private const val R_TODAY = "today"
private const val R_HISTORY = "history"
private const val R_STATS = "stats"
private const val R_SETTINGS = "settings"
private const val R_ACTIVITIES = "activities"
private const val R_MOODS = "moods"
private const val R_CHECKIN = "checkin?id={id}&date={date}"

private fun checkInRoute(id: String? = null, date: String? = null) =
    "checkin?id=${Uri.encode(id.orEmpty())}&date=${Uri.encode(date.orEmpty())}"

private class Tab(val route: String, val label: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(R_TODAY, R.string.nav_today, Icons.Rounded.Home),
    Tab(R_HISTORY, R.string.nav_history, Icons.Rounded.CalendarMonth),
    Tab(R_STATS, R.string.nav_stats, Icons.Rounded.BarChart),
    Tab(R_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun MoodyRoot(vm: MainViewModel, settings: Settings, pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val user by vm.user.collectAsStateWithLifecycle()
    val initializing by vm.authInitializing.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val activitiesState by vm.activitiesState.collectAsStateWithLifecycle()
    val moodOverrides by vm.moodOverrides.collectAsStateWithLifecycle()

    val moods = resolveMoods(moodOverrides ?: emptyMap())
    val activities = resolveActivities(activitiesState ?: com.minhtrong67.moody.data.ActivitiesState())
    val locale = LocalConfiguration.current.locales[0]

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
            else -> MainNav(vm, settings, snackbar, pendingRoute, onRouteConsumed)
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
private fun MainNav(
    vm: MainViewModel,
    settings: Settings,
    snackbar: SnackbarHostState,
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val ctx = LocalContext.current
    val toaster = LocalToaster.current
    val user by vm.user.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val moodOverrides by vm.moodOverrides.collectAsStateWithLifecycle()

    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route

    // Start page chosen in Settings (opened once per launch).
    val startRoute = remember {
        when (settings.startPage) {
            "history" -> R_HISTORY; "stats" -> R_STATS; "settings" -> R_SETTINGS
            "activities" -> R_ACTIVITIES; "moods" -> R_MOODS
            else -> R_TODAY
        }
    }
    var startHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!startHandled) {
            startHandled = true
            if (settings.startPage == "checkin" && pendingRoute == null) nav.navigate(checkInRoute())
        }
    }
    // Notification taps.
    LaunchedEffect(pendingRoute) {
        when (pendingRoute) {
            "checkin" -> nav.navigate(checkInRoute())
            "stats" -> nav.goTab(R_STATS)
            "today" -> nav.goTab(R_TODAY)
        }
        if (pendingRoute != null) onRouteConsumed()
    }

    val isTab = route in TABS.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTab) NavigationBar {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { if (route != t.route) nav.goTab(t.route) },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.label)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (route == R_TODAY || route == R_HISTORY) {
                FloatingActionButton(
                    onClick = { nav.navigate(checkInRoute()) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.nav_newCheckin)) }
            }
        },
    ) { inner ->
        // Tabs use the scaffold insets; the full-screen routes (check-in, manage) own their insets.
        val tabPadding = if (isTab || route == null) inner else PaddingValues(0.dp)
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(R_TODAY) {
                TodayScreen(
                    entries = entries, user = user,
                    onCheckIn = { id -> nav.navigate(checkInRoute(id)) },
                    onViewAll = { nav.goTab(R_HISTORY) },
                    onAvatar = { nav.goTab(R_SETTINGS) },
                    modifier = Modifier.padding(tabPadding),
                )
            }
            composable(R_HISTORY) {
                val deleted = stringResource(R.string.history_deleted)
                HistoryScreen(
                    entries = entries,
                    onEdit = { id -> nav.navigate(checkInRoute(id)) },
                    onNewForDate = { date -> nav.navigate(checkInRoute(date = date)) },
                    onDelete = { id -> vm.deleteEntry(id); toaster.show(deleted) },
                    modifier = Modifier.padding(tabPadding),
                )
            }
            composable(R_STATS) { StatsScreen(entries, Modifier.padding(tabPadding)) }
            composable(R_SETTINGS) {
                SettingsScreen(
                    vm = vm, settings = settings, user = user, entries = entries,
                    onOpenActivities = { nav.navigate(R_ACTIVITIES) },
                    onOpenMoods = { nav.navigate(R_MOODS) },
                    modifier = Modifier.padding(tabPadding),
                )
            }
            composable(R_ACTIVITIES) {
                SubPage(stringResource(R.string.nav_activities), onBack = { if (!nav.popBackStack()) nav.goTab(R_TODAY) }) { pad ->
                    ManageActivitiesScreen(
                        onAdd = { e, l -> vm.addActivity(e, l) },
                        onUpdate = { id, e, l -> vm.updateActivity(id, e, l) },
                        onRemove = { vm.removeActivity(it) },
                        onReorder = { vm.reorderActivities(it) },
                        modifier = Modifier.padding(pad),
                    )
                }
            }
            composable(R_MOODS) {
                SubPage(stringResource(R.string.nav_moods), onBack = { if (!nav.popBackStack()) nav.goTab(R_TODAY) }) { pad ->
                    ManageMoodsScreen(
                        overridden = moodOverrides?.keys ?: emptySet(),
                        onUpdate = { k, e, l -> vm.updateMood(k, e, l) },
                        onReset = { vm.resetMood(it) },
                        modifier = Modifier.padding(pad),
                    )
                }
            }
            composable(
                route = R_CHECKIN,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                ),
                enterTransition = { slideInVertically(tween(320)) { it / 6 } + fadeIn(tween(250)) },
                popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(200)) },
            ) { be ->
                val id = be.arguments?.getString("id")?.takeIf { it.isNotBlank() }
                val date = be.arguments?.getString("date")?.takeIf { it.isNotBlank() }
                CheckInScreen(
                    entries = entries, id = id, dateArg = date,
                    onSave = { existingId, data, isUpdate ->
                        if (existingId != null) vm.updateEntry(existingId, data) else vm.addEntry(data)
                        toaster.show(ctx.getString(if (isUpdate) R.string.checkin_updated else R.string.checkin_saved))
                        nav.popBackStack()
                    },
                    onClose = { nav.popBackStack() },
                    onManageActivities = { nav.popBackStack(); nav.navigate(R_ACTIVITIES) },
                )
            }
        }
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
        content = content,
    )
}
