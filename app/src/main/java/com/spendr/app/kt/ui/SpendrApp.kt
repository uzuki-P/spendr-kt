package com.spendr.app.kt.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.SpendrApplication
import com.spendr.app.kt.ui.add.AddSpendingScreen
import com.spendr.app.kt.ui.add.AddSpendingViewModel
import com.spendr.app.kt.ui.categories.CategoryManageScreen
import com.spendr.app.kt.ui.home.HomeScreen
import com.spendr.app.kt.ui.home.HomeViewModel
import com.spendr.app.kt.ui.quickadd.QuickAddManageScreen
import com.spendr.app.kt.ui.receipt.ReceiptScreen
import com.spendr.app.kt.ui.reports.ReportsScreen
import com.spendr.app.kt.ui.reports.ReportsViewModel
import com.spendr.app.kt.ui.search.TransactionSearchScreen
import com.spendr.app.kt.ui.search.TransactionSearchViewModel
import com.spendr.app.kt.ui.settings.BackupRestoreScreen
import com.spendr.app.kt.ui.settings.DebugScreen
import com.spendr.app.kt.ui.settings.SettingsScreen
import com.spendr.app.kt.ui.transactions.TransactionDetailScreen
import com.spendr.app.kt.ui.transactions.TransactionDetailViewModel
import com.spendr.app.kt.ui.transactions.TransactionsScreen
import com.spendr.app.kt.ui.transactions.TransactionsViewModel
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions?categoryId={categoryId}&month={month}"
    const val SEARCH = "search"
    const val REPORTS = "reports"
    const val ADD = "add?transactionId={transactionId}&quickAddId={quickAddId}&duplicateFromId={duplicateFromId}"
    val ADD_PATTERN = "${com.spendr.app.kt.BuildConfig.DEEP_LINK_SCHEME}://add"
    const val DETAIL = "transaction/{id}"
    const val RECEIPT = "receipt?transactionId={transactionId}&scanId={scanId}"
    const val RECEIPT_SCANNER = "settings/receipt-scanner"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val CATEGORIES = "categories"
    const val QUICK_ADD_MANAGE = "quickadd"
    const val DEBUG = "debug"

    fun add(
        transactionId: Long? = null,
        quickAddId: Long? = null,
        duplicateFromId: Long? = null,
    ): String {
        val query = buildList {
            transactionId?.let { add("transactionId=$it") }
            quickAddId?.let { add("quickAddId=$it") }
            duplicateFromId?.let { add("duplicateFromId=$it") }
        }.joinToString("&")
        return if (query.isEmpty()) "add" else "add?$query"
    }

    fun transactions(categoryId: Long? = null, month: Long? = null): String {
        val query = buildList {
            categoryId?.let { add("categoryId=$it") }
            month?.let { add("month=$it") }
        }.joinToString("&")
        return if (query.isEmpty()) "transactions" else "transactions?$query"
    }

    fun detail(id: Long) = "transaction/$id"
    fun receipt(id: Long? = null) = if (id == null) "receipt" else "receipt?transactionId=$id"
}

/**
 * M3 Expressive navigation motion: a shared-axis X transition driven by the
 * theme's expressive springs. The incoming screen slides a quarter width and
 * fades in; the covered screen drifts back and dims. Creation flows (Add,
 * Receipt) rise from the bottom instead, like a sheet. Pops play the same
 * choreography in reverse, which also drives the predictive-back preview.
 */
private const val SLIDE_DIVISOR = 4
private const val DRIFT_DIVISOR = 10

@Composable
fun SpendrApp(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    // Deep links (tile + shortcuts + browser): spendrkt://add[?quickAddId=N]
    LaunchedEffect(Unit) {
        SpendrApplication.pendingDeepLink.collect { uri ->
            if (uri?.startsWith("${com.spendr.app.kt.BuildConfig.DEEP_LINK_SCHEME}://") == true) {
                SpendrApplication.pendingDeepLink.value = null
                val link = android.net.Uri.parse(uri)
                if (link.host == "receipt") {
                    val scanId = link.getQueryParameter("scanId")
                    val scan = scanId?.let { container.receiptScans.get(it) }
                    if (scan != null) {
                        val route = Routes.receipt(scan.transactionId) +
                            (if (scan.transactionId == null) "?" else "&") + "scanId=${scan.id}"
                        navController.navigate(route) { launchSingleTop = true }
                    }
                } else if (link.host == "add") {
                    val quickAddId = link.getQueryParameter("quickAddId")?.toLongOrNull()
                    navController.navigate(Routes.add(quickAddId = quickAddId)) { launchSingleTop = true }
                }
            }
        }
    }

    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<IntOffset>()
    val fastEffects = motion.fastEffectsSpec<Float>()
    val effects = motion.defaultEffectsSpec<Float>()
    val sheetRoutes = setOf(Routes.ADD, Routes.RECEIPT)
    fun AnimatedContentTransitionScope<NavBackStackEntry>.isSheet(entry: NavBackStackEntry) =
        entry.destination.route in sheetRoutes

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        // Opaque theme surface behind every screen: while two screens
        // cross-fade, this shows through instead of the window's splash color.
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        enterTransition = {
            if (isSheet(targetState)) {
                slideInVertically(spatial) { it / SLIDE_DIVISOR } + fadeIn(effects)
            } else {
                slideInHorizontally(spatial) { it / SLIDE_DIVISOR } + fadeIn(effects)
            }
        },
        exitTransition = {
            if (isSheet(targetState)) {
                fadeOut(fastEffects, targetAlpha = 0.6f)
            } else {
                slideOutHorizontally(spatial) { -it / DRIFT_DIVISOR } + fadeOut(fastEffects)
            }
        },
        popEnterTransition = {
            if (isSheet(initialState)) {
                fadeIn(fastEffects, initialAlpha = 0.6f)
            } else {
                slideInHorizontally(spatial) { -it / DRIFT_DIVISOR } + fadeIn(effects)
            }
        },
        popExitTransition = {
            if (isSheet(initialState)) {
                slideOutVertically(spatial) { it / SLIDE_DIVISOR } + fadeOut(fastEffects)
            } else {
                slideOutHorizontally(spatial) { it / SLIDE_DIVISOR } +
                    scaleOut(motion.defaultSpatialSpec(), targetScale = 0.94f) + fadeOut(fastEffects)
            }
        },
    ) {
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            HomeViewModel(container.transactions, container.quickAdds, container.lastAddedTransactionId)
                        }
                    },
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenAdd = { navController.navigate(Routes.add()) },
                    onOpenReceipt = { navController.navigate(Routes.receipt()) },
                    onOpenAddQuickAdd = { navController.navigate(Routes.add(quickAddId = it)) },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onOpenTransactions = {
                        navController.navigate(Routes.transactions()) { launchSingleTop = true }
                    },
                    onOpenSearch = {
                        navController.navigate(Routes.SEARCH) { launchSingleTop = true }
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                )
            }
            composable(
                route = Routes.TRANSACTIONS,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("month") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val transactionsViewModel: TransactionsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TransactionsViewModel(container.transactions, container.categories)
                        }
                    },
                )
                TransactionsScreen(
                    viewModel = transactionsViewModel,
                    onOpenAdd = { navController.navigate(Routes.add()) },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onOpenEdit = { id -> scope.launch {
                        val route = if (container.transactions.getTransaction(id)?.type == "receipt") {
                            Routes.receipt(id)
                        } else Routes.add(transactionId = id)
                        navController.navigate(route)
                    } },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onDuplicate = transactionsViewModel::duplicate,
                    prefilteredCategoryId = entry.arguments?.getString("categoryId")?.toLongOrNull(),
                    initialMonthCursor = entry.arguments?.getString("month")?.toLongOrNull(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SEARCH) {
                val searchViewModel: TransactionSearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TransactionSearchViewModel(container.transactions, container.categories, container.receipts)
                        }
                    },
                )
                TransactionSearchScreen(
                    viewModel = searchViewModel,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.REPORTS) {
                val reportsViewModel: ReportsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ReportsViewModel(container.transactions) }
                    },
                )
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onSeeAll = { month ->
                        navController.navigate(Routes.transactions(month = month)) { launchSingleTop = true }
                    },
                    onOpenCategory = { categoryId, month ->
                        navController.navigate(Routes.transactions(categoryId, month)) { launchSingleTop = true }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ADD,
                deepLinks = listOf(navDeepLink { uriPattern = Routes.ADD_PATTERN }),
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("quickAddId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("duplicateFromId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val addViewModel: AddSpendingViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            AddSpendingViewModel(
                                transactions = container.transactions,
                                categoriesRepository = container.categories,
                                quickAddsRepository = container.quickAdds,
                                transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull(),
                                quickAddId = entry.arguments?.getString("quickAddId")?.toLongOrNull(),
                                duplicateFromId = entry.arguments?.getString("duplicateFromId")?.toLongOrNull(),
                                onTransactionAdded = { container.lastAddedTransactionId.value = it },
                            )
                        }
                    },
                    key = "add-${entry.arguments?.getString("transactionId")}" +
                        "-${entry.arguments?.getString("quickAddId")}" +
                        "-${entry.arguments?.getString("duplicateFromId")}",
                )
                AddSpendingScreen(
                    viewModel = addViewModel,
                    onDone = { navController.popBackStack() },
                    onOpenManageCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenManageQuickAdd = { navController.navigate(Routes.QUICK_ADD_MANAGE) },
                )
            }
            composable(
                route = Routes.RECEIPT,
                arguments = listOf(navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }, navArgument("scanId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                ReceiptScreen(
                    container = container,
                    transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull(),
                    initialScanId = entry.arguments?.getString("scanId"),
                    onOpenScannerSettings = { navController.navigate(Routes.RECEIPT_SCANNER) },
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val detailViewModel: TransactionDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            TransactionDetailViewModel(
                                container.transactions,
                                entry.arguments?.getLong("id") ?: 0L,
                                container.receipts,
                            )
                        }
                    },
                    key = "detail-${entry.arguments?.getLong("id")}",
                )
                TransactionDetailScreen(
                    viewModel = detailViewModel,
                    onEdit = { id ->
                        if (detailViewModel.transaction.value?.transaction?.type == "receipt") {
                            navController.navigate(Routes.receipt(id))
                        } else navController.navigate(Routes.add(transactionId = id))
                    },
                    onDuplicate = { id ->
                        if (detailViewModel.transaction.value?.transaction?.type == "receipt") {
                            detailViewModel.duplicateReceipt { navController.navigate(Routes.detail(it)) }
                        } else navController.navigate(Routes.add(duplicateFromId = id))
                    },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenQuickAdd = { navController.navigate(Routes.QUICK_ADD_MANAGE) },
                    onOpenDebug = { navController.navigate(Routes.DEBUG) },
                    onOpenReceiptScanner = { navController.navigate(Routes.RECEIPT_SCANNER) },
                )
            }
            composable(Routes.RECEIPT_SCANNER) {
                com.spendr.app.kt.ui.settings.ReceiptScannerSettingsScreen(container) { navController.popBackStack() }
            }
            composable(Routes.BACKUP) {
                BackupRestoreScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryManageScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.QUICK_ADD_MANAGE) {
                QuickAddManageScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.DEBUG) {
                DebugScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
        }
}
