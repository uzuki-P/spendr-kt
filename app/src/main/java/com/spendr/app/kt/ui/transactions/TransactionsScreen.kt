package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.domain.formatDate
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.domain.monthRange
import com.spendr.app.kt.domain.model.DateRange
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MonthPager
import com.spendr.app.kt.ui.components.monthWindow
import com.spendr.app.kt.ui.components.ScrollDateBadge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onOpenAdd: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenEdit: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onDuplicate: (Long) -> Unit,
    prefilteredCategoryId: Long? = null,
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    var monthCursorState by remember { mutableStateOf(monthCursor(System.currentTimeMillis())) }
    val months = remember { monthWindow(System.currentTimeMillis()) }
    var actionForRow by remember { mutableStateOf<TransactionWithCategoryRow?>(null) }
    var deleteForRow by remember { mutableStateOf<TransactionWithCategoryRow?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(prefilteredCategoryId) {
        prefilteredCategoryId?.let(viewModel::setPrefilteredCategory)
    }
    LaunchedEffect(monthCursorState) {
        viewModel.setFilters { it.copy(range = monthRange(monthCursorState)) }
    }

    if (deleteForRow != null) {
        AlertDialog(
            onDismissRequest = { deleteForRow = null },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(deleteForRow!!.transaction.id)
                        deleteForRow = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteForRow = null }) { Text("Cancel") } },
        )
    }

    actionForRow?.let { row ->
        ModalBottomSheet(onDismissRequest = { actionForRow = null }) {
            Text(
                row.transaction.note ?: row.categoryName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ListItem(
                headlineContent = { Text("Edit") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable {
                        actionForRow = null
                        onOpenEdit(row.transaction.id)
                    },
            )
            ListItem(
                headlineContent = { Text("Duplicate") },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable {
                        actionForRow = null
                        onDuplicate(row.transaction.id)
                    },
            )
            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 24.dp)
                    .clickable {
                        actionForRow = null
                        deleteForRow = row
                    },
            )
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        MonthPager(
            selectedCursor = monthCursorState,
            months = months,
            onMonthChange = { monthCursorState = it },
            modifier = Modifier.fillMaxSize(),
        ) { _ ->
            Column(Modifier.fillMaxSize()) {
                // Summary card (elevated, radius 28)
                val monthTotal = transactions.sumOf { it.transaction.paidAmount }
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            "Total spending",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatRupiah(monthTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendrTheme.colors.expense,
                                maxLines = 1,
                            )
                            TextButton(onClick = onOpenReports) {
                                MciIcon("chart-line", 18.dp, MaterialTheme.colorScheme.primary)
                                Text(
                                    "Full report",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                formatMonthYear(monthCursorState) + " · " + transactions.size + " transaction(s)",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Search pill with filter count (RN SearchBar)
                Surface(
                    onClick = { showFilters = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = filterState.search.ifEmpty {
                                if (filterState.activeCount > 0) {
                                    filterState.activeCount.toString() + " filter(s) active"
                                } else {
                                    "Search note, merchant, or category"
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (filterState.search.isEmpty() && filterState.activeCount == 0) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        if (filterState.activeCount > 0) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        filterState.activeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                        MciIcon(
                            "tune-variant",
                            20.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

                val grouped = remember(transactions, filterState.discountOnly, filterState.sortMode) {
                    buildGroups(transactions, filterState.discountOnly, filterState.sortMode)
                }

                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No transactions match",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        var headerIndex = 0
                        var current: String? = null
                        for (group in grouped) {
                            if (listState.firstVisibleItemIndex >= headerIndex) current = group.label
                            headerIndex += 1 + group.rows.size
                        }
                        current
                    }
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                        ScrollDateBadge(
                            label = badgeLabel,
                            visible = badgeVisible,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp),
                        )
                        LazyColumn(Modifier.fillMaxSize(), state = listState) {
                            grouped.forEach { group ->
                                item(key = "header-${group.key}") {
                                    DayGroupHeader(group.label, group.total)
                                }
                                items(group.rows, key = { it.transaction.id }) { row ->
                                    TransactionRow(
                                        row = row,
                                        onClick = { onOpenDetail(row.transaction.id) },
                                        showDate = false,
                                        onLongClick = { actionForRow = row },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onOpenAdd,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun FilterDialog(
    current: TransactionsViewModel.Filters,
    categories: List<com.spendr.app.kt.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: (TransactionsViewModel.Filters) -> Unit,
) {
    var search by remember { mutableStateOf(current.search) }
    var categoryIds by remember { mutableStateOf(current.categoryIds) }
    var discountOnly by remember { mutableStateOf(current.discountOnly) }
    var sortMode by remember { mutableStateOf(current.sortMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort & filter") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search note, merchant, or category") },
                    singleLine = true,
                )

                Text("Sort", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SortMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortMode = mode },
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = { sortMode = mode })
                        MciIcon(mode.glyph, 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            mode.label,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Text("Categories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                categories.forEach { category ->
                    val checked = category.id in categoryIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryIds = if (checked) {
                                    categoryIds - category.id
                                } else {
                                    categoryIds + category.id
                                }
                            },
                    ) {
                        Checkbox(checked = checked, onCheckedChange = {
                            categoryIds = if (it) categoryIds + category.id else categoryIds - category.id
                        })
                        Text(category.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = discountOnly, onCheckedChange = { discountOnly = it })
                    Text("Discount only", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(current.copy(search = search, categoryIds = categoryIds, discountOnly = discountOnly, sortMode = sortMode))
                },
            ) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onApply(
                            TransactionsViewModel.Filters(
                                search = "",
                                categoryIds = emptyList(),
                                discountOnly = false,
                                sortMode = SortMode.DATE_DESC,
                                range = current.range,
                            ),
                        )
                    },
                ) { Text("Clear all") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
