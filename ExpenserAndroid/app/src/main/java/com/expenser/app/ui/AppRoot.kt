package com.expenser.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.expenser.app.R
import com.expenser.app.data.TxType
import com.expenser.app.ui.components.*
import com.expenser.app.ui.screens.*
import kotlinx.coroutines.launch

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_STATS = "statistics"
const val ROUTE_WALLETS = "wallets"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_CAFE = "cafe"
const val ROUTE_CAFE_SHOPS = "cafe_shops"
const val ROUTE_CAFE_DRINKS = "cafe_drinks"

private data class Dest(val route: String, val label: Int, val icon: ImageVector)

// Same order as the web sidebar.
private val DRAWER = listOf(
    Dest(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Rounded.SpaceDashboard),
    Dest(ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Rounded.ReceiptLong),
    Dest(ROUTE_CATEGORIES, R.string.nav_categories, Icons.Rounded.Category),
    Dest(ROUTE_CAFE, R.string.nav_cafe, Icons.Rounded.LocalCafe),
    Dest(ROUTE_CAFE_SHOPS, R.string.nav_cafe_shops, Icons.Rounded.Storefront),
    Dest(ROUTE_CAFE_DRINKS, R.string.nav_cafe_drinks, Icons.Rounded.Coffee),
    Dest(ROUTE_STATS, R.string.nav_statistics, Icons.Rounded.BarChart),
    Dest(ROUTE_WALLETS, R.string.nav_wallets, Icons.Rounded.AccountBalanceWallet),
    Dest(ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)
private val BOTTOM = listOf(ROUTE_DASHBOARD, ROUTE_TRANSACTIONS, ROUTE_STATS, ROUTE_SETTINGS).map { r -> DRAWER.first { it.route == r } }

@Composable
fun AppRoot(vm: MainViewModel, launchAdd: String?, onLaunchAddHandled: () -> Unit) {
    val configured = vm.configured
    val user by vm.user.collectAsStateWithLifecycle()
    when {
        !configured -> SetupScreen()
        user == null -> LoginScreen(vm)
        else -> MainShell(vm, launchAdd, onLaunchAddHandled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: MainViewModel, launchAdd: String?, onLaunchAddHandled: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: ROUTE_DASHBOARD
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    var form by remember { mutableStateOf<TxFormRequest?>(null) }
    var palette by remember { mutableStateOf(false) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val cafeId = cafeCategoryId(categories)

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(context.getString(it.res, *it.args.toTypedArray())) } }
    LaunchedEffect(launchAdd) {
        if (launchAdd != null) { form = TxFormRequest(initialType = if (launchAdd == TxType.INCOME) TxType.INCOME else TxType.EXPENSE); onLaunchAddHandled() }
    }
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }

    fun go(r: String) {
        scope.launch { drawer.close() }
        if (r == route) return
        nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
    }

    val title = DRAWER.firstOrNull { it.route == route }?.label ?: R.string.app_name
    val showFab = route == ROUTE_DASHBOARD || route == ROUTE_TRANSACTIONS || (route == ROUTE_CAFE && cafeId != null)

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp), drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxHeight().statusBarsPadding()) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 16.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DRAWER.forEach { d ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(d.label)) }, icon = { Icon(d.icon, null) },
                                selected = route == d.route, onClick = { go(d.route) },
                                shape = CircleShape,
                            )
                        }
                    }
                    Row(Modifier.padding(16.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            val photo = user?.photoUrl
                            if (photo != null) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text((user?.name?.firstOrNull() ?: user?.email?.firstOrNull() ?: '?').uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user?.name?.ifBlank { null } ?: stringResource(R.string.settings_no_name), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = { vm.signOut(context) }) { Icon(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.common_logout), tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(title), fontWeight = FontWeight.Medium) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, stringResource(R.string.nav_menu)) } },
                    actions = {
                        IconButton(onClick = { palette = true }) { Icon(Icons.Rounded.Search, stringResource(R.string.common_search)) }
                        val (icon, next) = when (settings.themeMode) {
                            "light" -> Icons.Rounded.LightMode to "dark"
                            "dark" -> Icons.Rounded.DarkMode to "system"
                            else -> Icons.Rounded.BrightnessAuto to "light"
                        }
                        IconButton(onClick = { vm.setThemeMode(next) }) {
                            AnimatedContent(icon, transitionSpec = { (fadeIn(tween(200)) + scaleIn(initialScale = 0.7f)) togetherWith fadeOut(tween(120)) }, label = "themeIcon") { Icon(it, stringResource(R.string.topbar_theme)) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    BOTTOM.forEach { t ->
                        NavigationBarItem(selected = route == t.route, onClick = { go(t.route) }, icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.label)) })
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                NavHost(
                    nav, startDestination = ROUTE_DASHBOARD,
                    enterTransition = { fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(260, delayMillis = 60), initialScale = 0.94f) },
                    exitTransition = { fadeOut(tween(90)) },
                    popEnterTransition = { fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(260, delayMillis = 60), initialScale = 0.94f) },
                    popExitTransition = { fadeOut(tween(90)) },
                ) {
                    composable(ROUTE_DASHBOARD) { DashboardScreen(vm, onAdd = { form = TxFormRequest() }, onEdit = { form = TxFormRequest(editing = it) }, onViewAll = { go(ROUTE_TRANSACTIONS) }) }
                    composable(ROUTE_TRANSACTIONS) { TransactionsScreen(vm, onEdit = { form = TxFormRequest(editing = it) }) }
                    composable(ROUTE_STATS) { StatisticsScreen(vm, onEdit = { form = TxFormRequest(editing = it) }) }
                    composable(ROUTE_WALLETS) { WalletsScreen(vm) }
                    composable(ROUTE_SETTINGS) { SettingsScreen(vm) }
                    composable(ROUTE_CATEGORIES) { CategoriesScreen(vm) }
                    composable(ROUTE_CAFE) { CafeScreen(vm, onEdit = { form = TxFormRequest(editing = it) }) }
                    composable(ROUTE_CAFE_SHOPS) { CafeShopsScreen(vm) }
                    composable(ROUTE_CAFE_DRINKS) { CafeDrinksScreen(vm) }
                }
                if (showFab) key(route) {
                    ScreenFab(onClick = { form = TxFormRequest(initialCategoryId = if (route == ROUTE_CAFE) cafeId else null) }, modifier = Modifier.align(Alignment.BottomEnd))
                }
            }
        }
    }

    if (palette) CommandPalette(onDismiss = { palette = false }, onPick = { palette = false; go(it) })
    form?.let { TransactionFormDialog(it, vm, onDismiss = { form = null }) }
}

@Composable
private fun CommandPalette(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val labels = DRAWER.map { it to stringResource(it.label) }
    val list = labels.filter { q.isBlank() || it.second.contains(q, ignoreCase = true) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(16.dp)) {
                SearchField(q, { q = it }, stringResource(R.string.palette_hint))
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(list, key = { it.first.route }) { (d, label) ->
                        Row(Modifier.fillMaxWidth().clip(CircleShape).clickable { onPick(d.route) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(d.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(16.dp)); Text(label)
                        }
                    }
                }
            }
        }
    }
}
