package com.expenser.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expenser.app.R
import com.expenser.app.data.TxType
import com.expenser.app.ui.screens.*

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_STATS = "statistics"
const val ROUTE_WALLETS = "wallets"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_CAFE = "cafe"
const val ROUTE_CAFE_SHOPS = "cafe_shops"
const val ROUTE_CAFE_DRINKS = "cafe_drinks"

private data class NavTab(val route: String, val label: Int, val icon: ImageVector)

private val TABS = listOf(
    NavTab(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Rounded.SpaceDashboard),
    NavTab(ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Rounded.ReceiptLong),
    NavTab(ROUTE_STATS, R.string.nav_statistics, Icons.Rounded.BarChart),
    NavTab(ROUTE_WALLETS, R.string.nav_wallets, Icons.Rounded.AccountBalanceWallet),
    NavTab(ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, launchAdd: String?, onLaunchAddHandled: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: ROUTE_DASHBOARD
    val isTab = TABS.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var form by remember { mutableStateOf<TxFormRequest?>(null) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val cafeId = cafeCategoryId(categories)

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(context.getString(it.res, *it.args.toTypedArray())) } }
    // Opened from the daily-reminder notification.
    LaunchedEffect(launchAdd) {
        if (launchAdd != null) { form = TxFormRequest(initialType = if (launchAdd == TxType.INCOME) TxType.INCOME else TxType.EXPENSE); onLaunchAddHandled() }
    }

    val showFab = route == ROUTE_DASHBOARD || route == ROUTE_TRANSACTIONS || (route == ROUTE_CAFE && cafeId != null)
    fun go(r: String) = nav.navigate(r) { launchSingleTop = true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (isTab) TopAppBar(title = { Text(stringResource(TABS.first { it.route == route }.label)) })
        },
        bottomBar = {
            if (isTab) NavigationBar {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { nav.navigate(t.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.label)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (showFab) ExtendedFloatingActionButton(
                onClick = { form = TxFormRequest(initialCategoryId = if (route == ROUTE_CAFE) cafeId else null) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(if (route == ROUTE_CAFE) R.string.cafe_add_tx else R.string.tx_add_title)) },
            )
        },
    ) { padding ->
        NavHost(nav, startDestination = ROUTE_DASHBOARD, modifier = Modifier.padding(if (isTab) padding else androidx.compose.foundation.layout.PaddingValues())) {
            composable(ROUTE_DASHBOARD) { DashboardScreen(vm, onAdd = { form = TxFormRequest() }, onEdit = { form = TxFormRequest(editing = it) }, onViewAll = { nav.navigate(ROUTE_TRANSACTIONS) { launchSingleTop = true } }) }
            composable(ROUTE_TRANSACTIONS) { TransactionsScreen(vm, onEdit = { form = TxFormRequest(editing = it) }) }
            composable(ROUTE_STATS) { StatisticsScreen(vm, onEdit = { form = TxFormRequest(editing = it) }) }
            composable(ROUTE_WALLETS) { WalletsScreen(vm) }
            composable(ROUTE_SETTINGS) { SettingsScreen(vm, onNavigate = ::go) }
            composable(ROUTE_CATEGORIES) { CategoriesScreen(vm, onBack = { nav.popBackStack() }) }
            composable(ROUTE_CAFE) {
                Scaffold(
                    topBar = { com.expenser.app.ui.components.BackTopBar(stringResource(R.string.nav_cafe), { nav.popBackStack() }) },
                    snackbarHost = {}, floatingActionButton = {}, contentWindowInsets = WindowInsets0,
                ) { p -> androidx.compose.foundation.layout.Box(Modifier.padding(p)) { CafeScreen(vm, onEdit = { form = TxFormRequest(editing = it) }, onBack = { nav.popBackStack() }) } }
            }
            composable(ROUTE_CAFE_SHOPS) { CafeShopsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(ROUTE_CAFE_DRINKS) { CafeDrinksScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }

    form?.let { TransactionFormDialog(it, vm, onDismiss = { form = null }) }
}

private val WindowInsets0 = androidx.compose.foundation.layout.WindowInsets(0)
