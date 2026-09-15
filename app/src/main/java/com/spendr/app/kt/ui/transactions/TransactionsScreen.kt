package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

import androidx.compose.ui.graphics.SolidColor
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
import com.spendr.app.kt.ui.components.CategoryIconBadge
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
    onBack: () -> Unit = {},
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

    androidx.compose.material3.Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
        MonthPager(
            selectedCursor = monthCursorState,
            months = months,
            onMonthChange = { monthCursorState = it },
            modifier = Modifier.fillMaxSize(),
        ) { _ ->
            Column(Modifier.fillMaxSize()) {
                // Summary card (elevated, radius 28) — label, amount, footer row
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
                    Column(
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Total spending",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatRupiah(monthTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendrTheme.colors.expense,
                            maxLines = 1,
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                MciIcon(
                                    "receipt-text-outline",
                                    18.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    transactions.size.toString() + " transaction" +
                                        if (transactions.size == 1) "" else "s",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            TextButton(onClick = onOpenReports) {
                                MciIcon("chart-line", 18.dp, MaterialTheme.colorScheme.primary)
                                Text(
                                    "Full report",
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }

                // Search pill with inline query + filter count (RN SearchBar)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        BasicTextField(
                            value = filterState.search,
                            onValueChange = { value -> viewModel.setFilters { it.copy(search = value) } },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (filterState.search.isEmpty()) {
                                        Text(
                                            "Search transactions",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        if (filterState.search.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFilters { it.copy(search = "") } }) {
                                MciIcon("close-circle", 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box {
                            Surface(
                                onClick = { showFilters = true },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = if (filterState.activeCount > 0) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    MciIcon(
                                        "tune-variant",
                                        24.dp,
                                        if (filterState.activeCount > 0) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            if (filterState.activeCount > 0) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            filterState.activeCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
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
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                        // RN scrollDayBadge: absolute, left 8dp, top 42% of the page
                        ScrollDateBadge(
                            label = badgeLabel,
                            visible = badgeVisible,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = maxHeight * 0.42f)
                                .padding(start = 8.dp),
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
    }
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
    var showCategoryPicker by remember { mutableStateOf(false) }

    val stagedChanged = categoryIds.isNotEmpty() || discountOnly || sortMode != SortMode.DATE_DESC

    // RN Sort & filter: centered dialog, surfaceContainerHigh, radius 28
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Sort & filter",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Sort by: 4 pill tiles, selected label caption below
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Sort by",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SortTile(SortMode.DATE_DESC == sortMode, "sort-calendar-descending", Modifier.weight(1f)) { sortMode = SortMode.DATE_DESC }
                            SortTile(SortMode.DATE_ASC == sortMode, "sort-calendar-ascending", Modifier.weight(1f)) { sortMode = SortMode.DATE_ASC }
                            SortTile(SortMode.AMOUNT_DESC == sortMode, "sort-numeric-descending", Modifier.weight(1f)) { sortMode = SortMode.AMOUNT_DESC }
                            SortTile(SortMode.AMOUNT_ASC == sortMode, "sort-numeric-ascending", Modifier.weight(1f)) { sortMode = SortMode.AMOUNT_ASC }
                        }
                        Text(
                            sortMode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-8).dp),
                        )
                    }

                    // Category: tappable field row opening the multi-select sheet
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            onClick = { showCategoryPicker = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                when {
                                    categoryIds.isEmpty() -> MciIcon(
                                        "shape-outline", 24.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    categoryIds.size == 1 -> CategoryIconBadge(
                                        icon = categories.firstOrNull { it.id == categoryIds[0] }?.icon ?: "shape-outline",
                                        color = categories.firstOrNull { it.id == categoryIds[0] }?.color ?: "#888888",
                                        size = 40.dp,
                                    )
                                    else -> MciIcon(
                                        "shape-plus-outline", 24.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when {
                                            categoryIds.isEmpty() -> "All categories"
                                            categoryIds.size == 1 ->
                                                categories.firstOrNull { it.id == categoryIds[0] }?.name
                                                    ?: "1 category"
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
                    }

                    // Discount only: checkbox option row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { discountOnly = !discountOnly }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        MciIcon(
                            if (discountOnly) "checkbox-marked" else "checkbox-blank-outline",
                            24.dp,
                            if (discountOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text("Discount only", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Actions: [Clear all] Cancel, Apply
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (stagedChanged) {
                        TextButton(onClick = {
                            categoryIds = emptyList()
                            discountOnly = false
                            sortMode = SortMode.DATE_DESC
                        }) { Text("Clear all") }
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
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

/** RN sort pill tile: icon-only pill, selected = primaryContainer. */
@Composable
private fun SortTile(
    selected: Boolean,
    glyph: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = modifier.height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            MciIcon(
                glyph,
                24.dp,
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** RN CategoryPicker in multiple mode: search pill + 3-column grid + count/Done footer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMultiSelectSheet(
    categories: List<com.spendr.app.kt.data.db.entity.CategoryEntity>,
    selectedIds: List<Long>,
    onDone: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(selectedIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
            Text(
                "Select categories",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            // Search pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MciIcon("magnify", 20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 14.dp),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search categories",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = SpendrTheme.colors.textTertiary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                            MciIcon("close-circle", 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            val filtered = remember(categories, query) {
                categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
            ) {
                if (query.isEmpty()) {
                    CategoryTile(
                        label = "All categories",
                        icon = null,
                        color = null,
                        selected = draft.isEmpty(),
                        onClick = { draft = emptyList() },
                        modifier = Modifier.fillMaxWidth(1f / 3f),
                    )
                }
                filtered.chunked(3).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (category in rowCategories) {
                            CategoryTile(
                                label = category.name,
                                icon = category.icon,
                                color = category.color,
                                selected = category.id in draft,
                                onClick = {
                                    draft = if (category.id in draft) draft - category.id else draft + category.id
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (draft.isEmpty()) "All categories" else "${draft.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = { onDone(draft) }) { Text("Done") }
            }
        }
    }
}

/** RN CategoryPicker tile: bordered rounded square, icon circle over the name. */
@Composable
private fun CategoryTile(
    label: String,
    icon: String?,
    color: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            if (icon != null && color != null) {
                CategoryIconBadge(icon = icon, color = color, size = 40.dp)
            } else {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    MciIcon("shape-outline", 20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
