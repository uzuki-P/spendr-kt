package com.spendr.app.kt.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatDate
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.formatFullDate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.add.AddSpendingViewModel.SuggestionMode
import com.spendr.app.kt.ui.components.CalendarSheet
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.theme.SpendrTheme

private val INPUT_COLORS @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent,
)

private enum class FocusStage { ORIGINAL, DISCOUNT, PCT, AMOUNT, CATEGORY, NOTE, MERCHANT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpendingScreen(
    viewModel: AddSpendingViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recentCategoryIds by viewModel.recentCategoryIds.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var keypadVisible by remember { mutableStateOf(true) }
    var focusStage by remember { mutableStateOf(FocusStage.AMOUNT) }
    val noteFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val merchantFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    fun focusAmountField(field: com.spendr.app.kt.domain.AmountField) {
        focusStage = when (field) {
            com.spendr.app.kt.domain.AmountField.ORIGINAL -> FocusStage.ORIGINAL
            com.spendr.app.kt.domain.AmountField.PCT -> FocusStage.PCT
            com.spendr.app.kt.domain.AmountField.DISCOUNT -> FocusStage.DISCOUNT
            else -> FocusStage.AMOUNT
        }
        viewModel.selectField(field)
        keypadVisible = true
    }

    fun advanceFocus() {
        val pct = state.draft.discountMode == com.spendr.app.kt.domain.DiscountMode.PERCENTAGE
        focusStage = when (focusStage) {
            FocusStage.ORIGINAL -> if (pct) FocusStage.PCT else FocusStage.DISCOUNT
            FocusStage.DISCOUNT, FocusStage.PCT -> FocusStage.AMOUNT
            FocusStage.AMOUNT -> FocusStage.CATEGORY
            FocusStage.CATEGORY -> FocusStage.NOTE
            FocusStage.NOTE -> FocusStage.MERCHANT
            FocusStage.MERCHANT ->
                if (state.draft.hasDiscount) FocusStage.ORIGINAL else FocusStage.AMOUNT
        }
        when (focusStage) {
            FocusStage.ORIGINAL -> focusAmountField(com.spendr.app.kt.domain.AmountField.ORIGINAL)
            FocusStage.DISCOUNT -> focusAmountField(com.spendr.app.kt.domain.AmountField.DISCOUNT)
            FocusStage.PCT -> focusAmountField(com.spendr.app.kt.domain.AmountField.PCT)
            FocusStage.AMOUNT -> focusAmountField(com.spendr.app.kt.domain.AmountField.AMOUNT)
            FocusStage.CATEGORY -> {
                keypadVisible = false
                showCategoryPicker = true
            }
            FocusStage.NOTE -> {
                keypadVisible = false
                noteFocus.requestFocus()
            }
            FocusStage.MERCHANT -> {
                keypadVisible = false
                merchantFocus.requestFocus()
            }
        }
    }


    val editing = state.editing
    BackHandler(enabled = !editing && state.draft.paidAmount > 0) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this entry?") },
            text = { Text("The amount you entered hasn't been saved yet.") },
            confirmButton = { TextButton(onClick = onDone) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") } },
        )
    }

    state.pendingReplace?.let { suggestion ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReplaceDialog,
            title = { Text("Replace transaction details?") },
            text = {
                Text(
                    "This will overwrite the amount and discount you have entered " +
                        "with the details from this note.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applySuggestion(suggestion, SuggestionMode.FULL) }) {
                    Text("Replace")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissReplaceDialog) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        CalendarSheet(
            selectedDate = com.spendr.app.kt.ui.components.epochMsToLocalDate(state.dateMs),
            onPick = { picked: java.time.LocalDate ->
                val previous = com.spendr.app.kt.ui.components.epochMsToLocalDate(state.dateMs)
                viewModel.setDate(
                    state.dateMs - previous.toEpochDay() * 86_400_000L +
                        picked.toEpochDay() * 86_400_000L,
                )
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showCategoryPicker) {
        ModalBottomSheet(onDismissRequest = { showCategoryPicker = false }) {
            CategoryPickerContent(
                categories = categories,
                recentCategoryIds = recentCategoryIds,
                selectedId = state.categoryId,
                onSelect = {
                    viewModel.setCategory(it)
                    showCategoryPicker = false
                },
            )
        }
    }

    androidx.compose.material3.Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit Spending" else "Add Spending", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Icon(
                        Icons.Outlined.Bolt,
                        contentDescription = "Quick add prefill available on Home tiles",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    if (editing) {
                        IconButton(onClick = { viewModel.deleteEditing(onDeleted = onDone) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete transaction")
                        }
                    }
                },
            )
        },
        bottomBar = {
            // RN: keypad pinned above the FooterBar
            Column {
                AnimatedVisibility(
                    visible = keypadVisible,
                    enter = slideInVertically(animationSpec = tween(240), initialOffsetY = { it }) +
                        fadeIn(tween(240)),
                    exit = slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }) +
                        fadeOut(tween(200)),
                ) {
                    AmountKeypad(
                        onDigit = viewModel::append,
                        onBackspace = viewModel::backspace,
                        onClear = viewModel::clearActive,
                        onClose = { keypadVisible = false },
                        onNext = { advanceFocus() },
                    )
                }
                // FooterBar: surface bg, hairline top border, Save with check icon
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        HorizontalDivider(color = SpendrTheme.colors.border)
                        Button(
                            onClick = { viewModel.save(onSaved = onDone) },
                            enabled = state.draft.saveEnabled,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text(
                                if (editing) "Save changes" else "Save",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AmountFields(
                draft = state.draft,
                onSelectField = { field ->
                    viewModel.selectField(field)
                    focusStage = when (field) {
                        com.spendr.app.kt.domain.AmountField.ORIGINAL -> FocusStage.ORIGINAL
                        com.spendr.app.kt.domain.AmountField.DISCOUNT -> FocusStage.DISCOUNT
                        com.spendr.app.kt.domain.AmountField.PCT -> FocusStage.PCT
                        else -> FocusStage.AMOUNT
                    }
                    keypadVisible = true
                },
                onToggleDiscount = viewModel::toggleDiscount,
                onToggleDiscountType = viewModel::toggleDiscountType,
            )

            // Date
            Surface(
                onClick = { showDatePicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.spendr.app.kt.ui.components.MciIcon(
                        "calendar-blank-outline",
                        20.dp,
                        MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        formatFullDate(state.dateMs),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Category select field
            Surface(
                onClick = { showCategoryPicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val selected = categories.firstOrNull { it.id == state.categoryId }
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected != null) {
                        CategoryIconBadge(icon = selected.icon, color = selected.color, size = 42.dp)
                    }
                    Text(
                        text = selected?.name ?: "Select category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    com.spendr.app.kt.ui.components.MciIcon(
                        "chevron-down",
                        22.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Note suggestions (RN NoteSuggestions style)
            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.take(6).forEach { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            onFull = { viewModel.applySuggestion(suggestion, SuggestionMode.FULL) },
                            onNoteOnly = { viewModel.applySuggestion(suggestion, SuggestionMode.NOTE_ONLY) },
                        )
                    }
                }
            }

            TextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note") },
                colors = INPUT_COLORS,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(noteFocus),
                singleLine = true,
            )

            TextField(
                value = state.merchant,
                onValueChange = viewModel::setMerchant,
                label = { Text("Merchant (optional)") },
                placeholder = { Text("e.g., Indomaret") },
                colors = INPUT_COLORS,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(merchantFocus),
                singleLine = true,
            )


            if (state.amountError) {
                Text(
                    "Amount not entered",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: com.spendr.app.kt.domain.model.NoteSuggestion,
    onFull: () -> Unit,
    onNoteOnly: () -> Unit,
) {
    Surface(
        onClick = onFull,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpendrTheme.colors.border),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconBadge(
                icon = suggestion.categoryIcon,
                color = suggestion.categoryColor,
                size = 36.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    suggestion.note,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(suggestion.categoryName)
                        suggestion.merchant?.let { append(" • ").append(it) }
                        append(" • ").append(formatDayShort(suggestion.lastDate))
                        append(" • Save ").append(formatRupiah(suggestion.lastPaidAmount))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if ((suggestion.lastOriginalAmount ?: 0) > 0 && suggestion.lastDiscountAmount != null) {
                    Text(
                        formatRupiah(suggestion.lastOriginalAmount!!),
                        style = MaterialTheme.typography.labelSmall,
                        color = SpendrTheme.colors.textTertiary,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
                Text(
                    formatRupiah(suggestion.lastPaidAmount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendrTheme.colors.expense,
                )
            }
            Surface(
                onClick = onNoteOnly,
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    com.spendr.app.kt.ui.components.MciIcon(
                        "text-short",
                        18.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerContent(
    categories: List<com.spendr.app.kt.data.db.entity.CategoryEntity>,
    recentCategoryIds: List<Long>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val ordered = remember(categories, recentCategoryIds, query) {
        val matching = categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
        val recents = recentCategoryIds.mapNotNull { id -> matching.firstOrNull { it.id == id } }
        (recents + matching.filterNot { c -> recents.any { it.id == c.id } }).distinct()
    }
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            "Select category",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search categories") },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            colors = INPUT_COLORS,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        HorizontalDivider(Modifier.padding(top = 8.dp))
        if (ordered.isEmpty()) {
            Text(
                "No categories match “$query”.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            items(ordered, key = { it.id }) { category ->
                val selected = category.id == selectedId
                Surface(
                    onClick = { onSelect(category.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .pressScale(onClick = { onSelect(category.id) }),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    ) {
                        CategoryIconBadge(icon = category.icon, color = category.color, size = 40.dp)
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
