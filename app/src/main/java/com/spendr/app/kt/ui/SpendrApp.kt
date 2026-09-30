package com.spendr.app.kt.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
    const val TRANSACTIONS = "transactions?categoryId={categoryId}"
    const val SEARCH = "search"
    const val REPORTS = "reports"
    const val ADD = "add?transactionId={transactionId}&quickAddId={quickAddId}&duplicateFromId={duplicateFromId}"
    val ADD_PATTERN = "${com.spendr.app.kt.BuildConfig.DEEP_LINK_SCHEME}://add"
    const val DETAIL = "transaction/{id}"
    const val RECEIPT = "receipt?transactionId={transactionId}"
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
    fun receipt(id: Long? = null) = if (id == null) "receipt" else "receipt?transactionId=$id"
}

/**
 * OG native-stack motion, ported from React Navigation's `fade_from_bottom` /
 * `fade_to_bottom` (the Android Nougat activity open/close anims that
 * react-native-screens replays; see its res/anim XMLs):
 *
 * - push in:  alpha 0→1 over 210ms + rise from 8% over 350ms, both
 *   decelerate-quint
 * - push out: the covered screen holds still and fully opaque for 350ms
 * - pop out:  sink to 8% over 250ms accelerate-quint + alpha 1→0 over 150ms
 *   after a 100ms delay, linear
 * - pop in:   the revealed screen appears instantly and holds still
 */
private val DecelerateQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val AccelerateQuint = CubicBezierEasing(0.64f, 0f, 0.78f, 0f)

@Composable
fun SpendrApp(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    // Deep links (tile + shortcuts + browser): spendrkt://add[?quickAddId=N]
    LaunchedEffect(Unit) {
        SpendrApplication.pendingDeepLink.collect { uri ->
            if (uri?.startsWith("${com.spendr.app.kt.BuildConfig.DEEP_LINK_SCHEME}://") == true) {
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
        // Fade-and-rise push / fade-and-sink pop, matching the OG's native
        // stack (see the animation docs above).
        enterTransition = {
            fadeIn(tween(210, easing = DecelerateQuint)) +
                slideInVertically(tween(350, easing = DecelerateQuint)) { it * 8 / 100 }
        },
        exitTransition = {
            // Hold the covered screen still and opaque while the new one
            // fades in above it (the native "no animation" pair).
            fadeOut(snap(delayMillis = 350))
        },
        popEnterTransition = {
            fadeIn(snap())
        },
        popExitTransition = {
            fadeOut(tween(150, delayMillis = 100, easing = LinearEasing)) +
                slideOutVertically(tween(250, easing = AccelerateQuint)) { it * 8 / 100 }
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
                        navController.navigate(Routes.TRANSACTIONS) { launchSingleTop = true }
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
                }),
            ) { entry ->
                ReceiptScreen(
                    container = container,
                    transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull(),
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
                )
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
