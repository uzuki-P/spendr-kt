package com.spendr.app.kt.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CalendarSheet
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.CategoryMultiSelectSheet
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.ThemedTextField
import com.spendr.app.kt.ui.components.epochMsToLocalDate
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentGap
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.segmentCorners
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.spendr.app.kt.ui.components.toEpochMs
import com.spendr.app.kt.ui.transactions.DayGroupHeader
import com.spendr.app.kt.ui.transactions.TransactionRow
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * Global transaction search, ported from RN `TransactionSearchScreen`: whole
 * history (preset/custom range) with note/merchant/category text search,
 * category multi-select, and near-amount matching. Distinct from the monthly
 * Transactions page — no month tabs, results group by day across the range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchScreen(
    viewModel: TransactionSearchViewModel,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    val filters by viewModel.filterState.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val committedQuery by viewModel.committedSearch.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val matchingItems by viewModel.matchingItems.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    val isSearching = searchText.trim() != committedQuery

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SpendrTopBar(title = "Search", onBack = onBack) },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchBar(
                    query = searchText,
                    isSearching = isSearching,
                    onQueryChange = viewModel::setSearch,
                )

                // Filter summary: range chip, category chip(s), amount chip, tune button
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchChip(
                        label = filters.rangeKey.label,
                        selected = filters.rangeKey != SearchRangeKey.PAST_3_MONTHS,
                        showSelectedIcon = false,
                        onClick = { showFilters = true },
                    )
                    val selectedCategories = categories.filter { it.id in filters.categoryIds }
                    when {
                        selectedCategories.size == 1 -> SearchChip(
                            label = selectedCategories[0].name,
                            selected = true,
                            showSelectedIcon = false,
                            color = selectedCategories[0].color,
                            onClick = { showFilters = true },
                        )
                        filters.categoryIds.size > 1 -> SearchChip(
                            label = "${filters.categoryIds.size} categories",
                            selected = true,
                            showSelectedIcon = false,
                            onClick = { showFilters = true },
                        )
                    }
                    filters.approximatePaidAmount?.takeIf { it > 0 }?.let { amount ->
                        SearchChip(
                            label = "Near " + com.spendr.app.kt.domain.formatRupiah(amount),
                            selected = true,
                            showSelectedIcon = false,
                            onClick = { showFilters = true },
                        )
                    }
                    FilledTonalIconToggleButton(
                        checked = filters.activeCount > 0,
                        onCheckedChange = { showFilters = true },
                        shapes = IconButtonDefaults.toggleableShapes(),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .semantics { contentDescription = "Search filters" },
                    ) {
                        MciIcon(
                            "tune-variant",
                            20.dp,
                            if (filters.activeCount > 0) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                }

                Text(
                    "${items.size} transaction" + if (items.size == 1) "" else "s",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (items.isEmpty()) {
                EmptyState(
                    glyph = "text-box-search-outline",
                    title = "No transactions found",
                    message = "Try another search or adjust the filters.",
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                val grouped = remember(items) { groupByDay(items) }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    grouped.forEachIndexed { groupIndex, group ->
                        item(key = "header-$groupIndex") {
                            DayGroupHeader(group.label, group.total, Modifier.animateItem())
                        }
                        itemsIndexed(group.rows, key = { _, row -> row.transaction.id }) { index, row ->
                            Column(
                                Modifier
                                    .animateItem()
                                    .padding(start = 16.dp, end = 16.dp, bottom = SegmentGap),
                            ) {
                                matchingItems[row.transaction.id]?.let { name ->
                                    Text(
                                        "Item: $name",
                                        modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
                                        style = MaterialTheme.typography.labelMediumEmphasized,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                TransactionRow(
                                    row = row,
                                    onClick = { onOpenDetail(row.transaction.id) },
                                    showDate = false,
                                    corners = segmentCorners(index, group.rows.size),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        SearchFilterDialog(
            current = filters,
            categories = categories,
            onDismiss = { showFilters = false },
            onApply = { next ->
                viewModel.setFilters { next }
                showFilters = false
            },
        )
    }
}

/** RN searchBar pill: surfaceContainerHigh, radius full, 56dp, autofocus. */
@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search note, merchant, or category",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SpendrTheme.colors.textTertiary,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (isSearching) {
                LoadingIndicator(modifier = Modifier.size(32.dp))
            } else if (query.isNotEmpty()) {
                com.spendr.app.kt.ui.components.BouncyIconButton(onClick = { onQueryChange("") }) {
                    MciIcon("close-circle", 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** RN Chip: 36dp pill, radius 12, selected = secondaryContainer + check, optional accent dot. */
@Composable
private fun SearchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showSelectedIcon: Boolean = true,
    color: String? = null,
) {
    // Selected chips round into pills; all chips square off while pressed
    MorphSurface(
        onClick = onClick,
        corners = if (selected) Corners(18.dp) else Corners(10.dp),
        pressedCorners = Corners(6.dp),
        pressedScale = 0.95f,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (color != null) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            com.spendr.app.kt.ui.components.categoryColor(color)
                                .copy(alpha = if (selected) 1f else 0.7f),
                            CircleShape,
                        ),
                )
            } else if (selected && showSelectedIcon) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterDialog(
    current: TransactionSearchViewModel.SearchFilters,
    categories: List<com.spendr.app.kt.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: (TransactionSearchViewModel.SearchFilters) -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    var amountText by remember {
        mutableStateOf(current.approximatePaidAmount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf<DateFieldTarget?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
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
                    "Search filters",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Date range",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SearchRangeKey.entries.forEach { key ->
                                SearchChip(
                                    label = key.label,
                                    selected = draft.rangeKey == key,
                                    onClick = { draft = draft.copy(rangeKey = key) },
                                )
                            }
                        }
                        if (draft.rangeKey == SearchRangeKey.CUSTOM) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DateField(
                                    label = "From",
                                    value = draft.customStart,
                                    onClick = { pickingDate = DateFieldTarget.FROM },
                                    modifier = Modifier.weight(1f),
                                )
                                DateField(
                                    label = "To",
                                    value = draft.customEnd,
                                    onClick = { pickingDate = DateFieldTarget.TO },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val selectedCategories = categories.filter { it.id in draft.categoryIds }
                        BouncySurface(
                            onClick = { showCategoryPicker = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                when {
                                    selectedCategories.size == 1 -> CategoryIconBadge(
                                        icon = selectedCategories[0].icon,
                                        color = selectedCategories[0].color,
                                        size = 40.dp,
                                    )
                                    else -> MciIcon(
                                        "shape-outline",
                                        24.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when {
                                            draft.categoryIds.isEmpty() -> "All categories"
                                            selectedCategories.size == 1 -> selectedCategories[0].name
                                            else -> "${draft.categoryIds.size} categories"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                    )
                                    Text(
                                        "Select one or more categories",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                MciIcon(
                                    "chevron-right",
                                    24.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    ThemedTextField(
                        value = amountText,
                        onValueChange = { value -> amountText = value.filter { it.isDigit() } },
                        label = "Paid amount near (optional)",
                        placeholder = "e.g., 50000",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Matches within \u00B110% (at least Rp1.000).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BouncyTextButton(onClick = {
                        draft = TransactionSearchViewModel.defaultFilters()
                        amountText = ""
                    }) { Text("Reset") }
                    BouncyTextButton(onClick = onDismiss) { Text("Cancel") }
                    BouncyButton(onClick = {
                        onApply(
                            draft.copy(
                                approximatePaidAmount = amountText.toLongOrNull()?.takeIf { it > 0 },
                            ),
                        )
                    }) { Text("Apply") }
                }
            }
        }
    }

    pickingDate?.let { target ->
        CalendarSheet(
            selectedDate = epochMsToLocalDate(
                if (target == DateFieldTarget.FROM) draft.customStart else draft.customEnd,
            ),
            onPick = { picked ->
                val ms = picked.toEpochMs()
                draft = when (target) {
                    DateFieldTarget.FROM -> draft.copy(customStart = ms)
                    DateFieldTarget.TO -> draft.copy(customEnd = ms)
                }
                pickingDate = null
            },
            onDismiss = { pickingDate = null },
        )
    }

    if (showCategoryPicker) {
        CategoryMultiSelectSheet(
            categories = categories,
            selectedIds = draft.categoryIds,
            onDone = {
                draft = draft.copy(categoryIds = it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

private enum class DateFieldTarget { FROM, TO }

/** RN DateField: label above a bordered field showing "d MMM yyyy". */
@Composable
private fun DateField(
    label: String,
    value: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        BouncySurface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, SpendrTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(onClick = onClick),
        ) {
            Text(
                com.spendr.app.kt.domain.formatDate(value),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}
