package com.spendr.app.kt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.spendr.app.kt.ui.reports.ReportsScreen
import com.spendr.app.kt.ui.reports.ReportsViewModel
import com.spendr.app.kt.ui.settings.BackupRestoreScreen
import com.spendr.app.kt.ui.settings.DebugScreen
import com.spendr.app.kt.ui.settings.SettingsScreen
import com.spendr.app.kt.ui.transactions.TransactionDetailScreen
import com.spendr.app.kt.ui.transactions.TransactionDetailViewModel
import com.spendr.app.kt.ui.transactions.TransactionsScreen
import com.spendr.app.kt.ui.transactions.TransactionsViewModel

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions?categoryId={categoryId}"
    const val REPORTS = "reports"
    const val ADD = "add?transactionId={transactionId}&quickAddId={quickAddId}&duplicateFromId={duplicateFromId}"
    const val ADD_PATTERN = "spendrkt://add"
    const val DETAIL = "transaction/{id}"
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

    fun detail(id: Long) = "transaction/$id"
}

 


@Composable
fun SpendrApp(container: AppContainer) {
    val navController = rememberNavController()
    // Deep links (tile + shortcuts + browser): spendrkt://add[?quickAddId=N]
    LaunchedEffect(Unit) {
        SpendrApplication.pendingDeepLink.collect { uri ->
            if (uri?.startsWith("spendrkt://") == true) {
                SpendrApplication.pendingDeepLink.value = null
                val quickAddId = uri.substringAfter("quickAddId=", "")
                    .takeWhile { it.isDigit() }.toLongOrNull()
                navController.navigate(Routes.add(quickAddId = quickAddId)) { launchSingleTop = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(250)) { it / 8 }
        },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(250)) { -it / 8 }
        },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) },
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
                    onOpenAddQuickAdd = { navController.navigate(Routes.add(quickAddId = it)) },
                    onOpenTransactions = {
                        navController.navigate(Routes.TRANSACTIONS) { launchSingleTop = true }
                    },
                    onOpenSearch = {
                        navController.navigate(Routes.TRANSACTIONS) { launchSingleTop = true }
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
                    onOpenEdit = { navController.navigate(Routes.add(transactionId = it)) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onDuplicate = transactionsViewModel::duplicate,
                    prefilteredCategoryId = entry.arguments?.getString("categoryId")?.toLongOrNull(),
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
                    onSeeAll = { _ ->
                        navController.navigate(Routes.TRANSACTIONS) { launchSingleTop = true }
                    },
                    onOpenCategory = { categoryId ->
                        navController.navigate("transactions?categoryId=$categoryId") { launchSingleTop = true }
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
                            )
                        }
                    },
                    key = "detail-${entry.arguments?.getLong("id")}",
                )
                TransactionDetailScreen(
                    viewModel = detailViewModel,
                    onEdit = { navController.navigate(Routes.add(transactionId = it)) },
                    onDuplicate = { navController.navigate(Routes.add(duplicateFromId = it)) },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenQuickAdd = { navController.navigate(Routes.QUICK_ADD_MANAGE) },
                    onOpenDebug = { navController.navigate(Routes.DEBUG) },
                )
            }
            composable(Routes.BACKUP) {
                BackupRestoreScreen(container = container)
            }
            composable(Routes.CATEGORIES) {
                CategoryManageScreen(container = container)
            }
            composable(Routes.QUICK_ADD_MANAGE) {
                QuickAddManageScreen(container = container)
            }
            composable(Routes.DEBUG) {
                DebugScreen(container = container)
            }
        }
}
