package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.domain.monthRange
import com.spendr.app.kt.ui.components.ActionRow
import com.spendr.app.kt.ui.components.AnimatedAmount
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.CategoryMultiSelectSheet
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MonthPager
import com.spendr.app.kt.ui.components.MonthPillButton
import com.spendr.app.kt.ui.components.MonthYearPickerSheet
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.ScrollDateBadge
import com.spendr.app.kt.ui.components.SearchPill
import com.spendr.app.kt.ui.components.SegmentGap
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.monthWindow
import com.spendr.app.kt.ui.components.rememberCollapsingBar
import com.spendr.app.kt.ui.components.segmentCorners
import com.spendr.app.kt.ui.theme.SpendrTheme

private data class DayGroup(
    val key: String,
    val label: String,
    val rows: MutableList<TransactionWithCategoryRow>,
    val total: Long,
)

/** Client-side filter + sort + grouping, port of RN `buildGroups`. */
private fun buildGroups(
    items: List<TransactionWithCategoryRow>,
    discountOnly: Boolean,
    sortMode: SortMode,
): List<DayGroup> {
    val rows = if (discountOnly) {
        items.filter { (it.transaction.discountAmount ?: 0) > 0 }
    } else {
        items
    }
    val sorted = when (sortMode) {
        SortMode.AMOUNT_DESC -> rows.sortedByDescending { it.transaction.paidAmount }
        SortMode.AMOUNT_ASC -> rows.sortedBy { it.transaction.paidAmount }
        SortMode.DATE_ASC -> rows.sortedWith(
            compareBy<TransactionWithCategoryRow> { it.transaction.date }.thenBy { it.transaction.id },
        )
        SortMode.DATE_DESC -> rows.sortedWith(
            compareByDescending<TransactionWithCategoryRow> { it.transaction.date }
                .thenByDescending { it.transaction.id },
        )
    }

    if (sortMode == SortMode.AMOUNT_DESC || sortMode == SortMode.AMOUNT_ASC) {
        return listOf(
            DayGroup(
                key = "all",
                label = sorted.size.toString() + " transaction" + if (sorted.size == 1) "" else "s",
                rows = sorted.toMutableList(),
                total = sorted.sumOf { it.transaction.paidAmount },
            ),
        )
    }

    val groups = LinkedHashMap<String, DayGroup>()
    for (item in sorted) {
        val key = formatDayShort(item.transaction.date)
        val group = groups.getOrPut(key) {
            DayGroup(key, key, mutableListOf(), 0)
        }
        group.rows.add(item)
    }
    return groups.values.map { group ->
        group.copy(total = group.rows.sumOf { it.transaction.paidAmount })
    }
}

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenAdd: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenEdit: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onDuplicate: (Long) -> Unit,
    prefilteredCategoryId: Long? = null,
    onBack: () -> Unit = {},
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val scrollBehavior = rememberCollapsingBar()

    var monthCursorState by remember { mutableStateOf(monthCursor(System.currentTimeMillis())) }
    val months = remember(monthCursorState) {
        monthWindow(System.currentTimeMillis(), include = monthCursorState)
    }
    var actionForRow by remember { mutableStateOf<TransactionWithCategoryRow?>(null) }
    var deleteForRow by remember { mutableStateOf<TransactionWithCategoryRow?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(prefilteredCategoryId) {
        prefilteredCategoryId?.let(viewModel::setPrefilteredCategory)
    }
    LaunchedEffect(monthCursorState) {
        viewModel.setFilters { it.copy(range = monthRange(monthCursorState)) }
    }

    if (deleteForRow != null) {
        AlertDialog(
            onDismissRequest = { deleteForRow = null },
            icon = { MciIcon("trash-can-outline", 24.dp, MaterialTheme.colorScheme.error) },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                BouncyTextButton(
                    onClick = {
                        viewModel.delete(deleteForRow!!.transaction.id)
                        deleteForRow = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { BouncyTextButton(onClick = { deleteForRow = null }) { Text("Cancel") } },
        )
    }

    actionForRow?.let { row ->
        ModalBottomSheet(onDismissRequest = { actionForRow = null }) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                Row(
                    Modifier.padding(start = 4.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryIconBadge(icon = row.categoryIcon, color = row.categoryColor, size = 44.dp)
                    Text(
                        row.transaction.note ?: row.categoryName,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SegmentedGroup {
                    ActionRow("pencil-outline", "Edit", segmentCorners(0, 3), onClick = {
                        actionForRow = null
                        onOpenEdit(row.transaction.id)
                    })
                    ActionRow("content-copy", "Duplicate", segmentCorners(1, 3), onClick = {
                        actionForRow = null
                        onDuplicate(row.transaction.id)
                    })
                    ActionRow("trash-can-outline", "Delete", segmentCorners(2, 3), destructive = true, onClick = {
                        actionForRow = null
                        deleteForRow = row
                    })
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SpendrTopBar(
                title = "Transactions",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                // Month/year pill (same as Reports): jumps outside the tab window
                actions = { MonthPillButton(monthCursorState) { showMonthPicker = true } },
            )
        },
    ) { innerPadding ->
        MonthPager(
            selectedCursor = monthCursorState,
            months = months,
            onMonthChange = { monthCursorState = it },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            topContent = {
                // Pinned: search pill with inline query + filter toggle
                SearchPill(
                    value = filterState.search,
                    onValueChange = { value -> viewModel.setFilters { it.copy(search = value) } },
                    placeholder = "Search transactions",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    val active = filterState.activeCount > 0
                    BadgedBox(
                        badge = {
                            if (active) Badge { Text(filterState.activeCount.toString()) }
                        },
                    ) {
                        FilledTonalIconToggleButton(
                            checked = active,
                            onCheckedChange = { showFilters = true },
                            shapes = IconButtonDefaults.toggleableShapes(),
                            modifier = Modifier.semantics { contentDescription = "Sort and filter" },
                        ) {
                            MciIcon(
                                "tune-variant",
                                22.dp,
                                if (active) {
                                    MaterialTheme.colorScheme.onSecondary
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            )
                        }
                    }
                }
            },
        ) { _ ->
            val grouped = remember(transactions, filterState.discountOnly, filterState.sortMode) {
                buildGroups(transactions, filterState.discountOnly, filterState.sortMode)
            }

            if (showFilters) {
                FilterDialog(
                    current = filterState,
                    categories = viewModel.categories.collectAsState().value,
                    onDismiss = { showFilters = false },
                    onApply = { filters ->
                        viewModel.setFilters { filters }
                        showFilters = false
                    },
                )
            }

            if (transactions.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TransactionsSummaryCard(
                        monthTotal = 0L,
                        count = 0,
                        onOpenReports = onOpenReports,
                    )
                    EmptyState(
                        glyph = "text-box-search-outline",
                        title = "Nothing here",
                        message = "No transactions match this month and filter.",
                    )
                }
            } else {
                val listState = rememberLazyListState()
                var badgeVisible by remember { mutableStateOf(false) }
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) {
                        badgeVisible = true
                    } else {
                        kotlinx.coroutines.delay(1000)
                        badgeVisible = false
                    }
                }
                val badgeLabel = remember(listState.firstVisibleItemIndex, grouped) {
                    var current: String? = null
                    var headerIndex = 1 // after the summary item
                    for (group in grouped) {
                        if (listState.firstVisibleItemIndex >= headerIndex) current = group.label
                        headerIndex += 1 + group.rows.size
                    }
                    current
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    ScrollDateBadge(
                        label = badgeLabel,
                        visible = badgeVisible,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = maxHeight * 0.42f)
                            .padding(start = 8.dp),
                    )
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        // Summary card scrolls away with the list (pinned above:
                        // app bar, month tabs, search + filter only)
                        item(key = "summary") {
                            TransactionsSummaryCard(
                                monthTotal = transactions.sumOf { it.transaction.paidAmount },
                                count = transactions.size,
                                onOpenReports = onOpenReports,
                            )
                        }
                        grouped.forEach { group ->
                            item(key = "header-${group.key}") {
                                DayGroupHeader(group.label, group.total, Modifier.animateItem())
                            }
                            itemsIndexed(group.rows, key = { _, row -> row.transaction.id }) { index, row ->
                                TransactionRow(
                                    row = row,
                                    onClick = { onOpenDetail(row.transaction.id) },
                                    showDate = false,
                                    onLongClick = { actionForRow = row },
                                    corners = segmentCorners(index, group.rows.size),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(start = 16.dp, end = 16.dp, bottom = SegmentGap),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerSheet(
            selectedCursor = monthCursorState,
            onPick = {
                monthCursorState = it
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

/** Month summary: counting total, transaction count chip, Report button. */
@Composable
private fun TransactionsSummaryCard(
    monthTotal: Long,
    count: Int,
    onOpenReports: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            Modifier.padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Total spending",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            AnimatedAmount(
                amount = monthTotal,
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MciIcon("receipt-text-outline", 16.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            count.toString() + " transaction" + if (count == 1) "" else "s",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                BouncyButton(
                    onClick = onOpenReports,
                    height = androidx.compose.material3.ButtonDefaults.ExtraSmallContainerHeight,
                ) {
                    MciIcon("chart-line", 16.dp, MaterialTheme.colorScheme.onPrimary)
                    Text("Report", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    current: TransactionsViewModel.Filters,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: (TransactionsViewModel.Filters) -> Unit,
) {
    val search = current.search
    var categoryIds by remember { mutableStateOf(current.categoryIds) }
    var discountOnly by remember { mutableStateOf(current.discountOnly) }
    var sortMode by remember { mutableStateOf(current.sortMode) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val stagedChanged = categoryIds.isNotEmpty() || discountOnly || sortMode != SortMode.DATE_DESC

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Sort & filter",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Column(
                    Modifier
                        .padding(top = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Sort by: connected button group, label of the pick below
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Sort by",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            SortMode.entries.forEachIndexed { index, mode ->
                                ToggleButton(
                                    checked = sortMode == mode,
                                    onCheckedChange = { sortMode = mode },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        SortMode.entries.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = mode.label },
                                ) {
                                    MciIcon(
                                        mode.glyph,
                                        22.dp,
                                        androidx.compose.material3.LocalContentColor.current,
                                    )
                                }
                            }
                        }
                        Text(
                            sortMode.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Category + discount-only as a two-row segmented group
                    SegmentedGroup {
                        MorphSurface(
                            onClick = { showCategoryPicker = true },
                            corners = segmentCorners(0, 2, outer = 20.dp),
                            pressedCorners = Corners(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val single = categories.firstOrNull { it.id == categoryIds.singleOrNull() }
                                if (single != null) {
                                    CategoryIconBadge(icon = single.icon, color = single.color, size = 40.dp)
                                } else {
                                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                        MciIcon(
                                            if (categoryIds.isEmpty()) "shape-outline" else "shape-plus-outline",
                                            24.dp,
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when {
                                            categoryIds.isEmpty() -> "All categories"
                                            single != null -> single.name
                                            categoryIds.size == 1 -> "1 category"
                                            else -> "${categoryIds.size} categories"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "Tap to search and select multiple",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                MciIcon("chevron-right", 24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        MorphSurface(
                            onClick = { discountOnly = !discountOnly },
                            corners = segmentCorners(1, 2, outer = 20.dp),
                            pressedCorners = Corners(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    MciIcon("sale-outline", 24.dp, SpendrTheme.colors.savings)
                                }
                                Text(
                                    "Discount only",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(checked = discountOnly, onCheckedChange = { discountOnly = it })
                            }
                        }
                    }
                }

                // Actions: [Clear all] Cancel, Apply
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (stagedChanged) {
                        BouncyTextButton(onClick = {
                            categoryIds = emptyList()
                            discountOnly = false
                            sortMode = SortMode.DATE_DESC
                        }) { Text("Clear all") }
                    }
                    BouncyTextButton(onClick = onDismiss) { Text("Cancel") }
                    BouncyButton(onClick = {
                        onApply(current.copy(search = search, categoryIds = categoryIds, discountOnly = discountOnly, sortMode = sortMode))
                    }) { Text("Apply") }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryMultiSelectSheet(
            categories = categories,
            selectedIds = categoryIds,
            onDone = {
                categoryIds = it
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}
