package com.spendr.app.kt.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatDate
import com.spendr.app.kt.domain.formatFullDate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.add.AddSpendingViewModel.SuggestionMode
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CalendarSheet
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.CategoryPickerContent
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.theme.SpendrTheme

// RN fields are borderless rounded surfaces — no focus underline
private val INPUT_COLORS @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

private enum class FocusStage { ORIGINAL, DISCOUNT, PCT, AMOUNT, CATEGORY, NOTE, MERCHANT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpendingScreen(
    viewModel: AddSpendingViewModel,
    onDone: () -> Unit,
    onOpenManageCategories: () -> Unit = {},
    onOpenManageQuickAdd: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recentCategoryIds by viewModel.recentCategoryIds.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val quickAdds by viewModel.quickAdds.collectAsState()
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showQuickAddPicker by remember { mutableStateOf(false) }
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
    // RN back grammar: close the keypad first, then confirm discard.
    // The keypad handler is registered last so it wins while enabled.
    BackHandler(enabled = !editing && state.draft.paidAmount > 0) { showDiscardDialog = true }
    BackHandler(enabled = keypadVisible) { keypadVisible = false }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this entry?") },
            text = { Text("The amount you entered hasn't been saved yet.") },
            confirmButton = { BouncyTextButton(onClick = onDone) { Text("Discard") } },
            dismissButton = { BouncyTextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") } },
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
                BouncyTextButton(onClick = { viewModel.applySuggestion(suggestion, SuggestionMode.FULL) }) {
                    Text("Replace")
                }
            },
            dismissButton = { BouncyTextButton(onClick = viewModel::dismissReplaceDialog) { Text("Cancel") } },
        )
    }

    state.pendingQuickAddReplace?.let { qa ->
        AlertDialog(
            onDismissRequest = viewModel::dismissQuickAddReplace,
            title = { Text("Replace transaction details?") },
            text = {
                Text(
                    "This will overwrite the amount and discount you have entered " +
                        "with the details from this quick add.",
                )
            },
            confirmButton = {
                BouncyTextButton(onClick = viewModel::confirmQuickAddReplace) { Text("Replace") }
            },
            dismissButton = { BouncyTextButton(onClick = viewModel::dismissQuickAddReplace) { Text("Cancel") } },
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
                onManage = {
                    showCategoryPicker = false
                    onOpenManageCategories()
                },
            )
        }
    }

    if (showQuickAddPicker) {
        ModalBottomSheet(onDismissRequest = { showQuickAddPicker = false }) {
            QuickAddPickerContent(
                items = quickAdds,
                onSelect = {
                    viewModel.pickQuickAdd(it)
                    showQuickAddPicker = false
                },
                onManage = {
                    showQuickAddPicker = false
                    onOpenManageQuickAdd()
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
                    BouncyIconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // RN: lightning opens the QuickAddPicker (prefill); shows the
                    // sheet even with no items — empty state + manage CTA there.
                    BouncyIconButton(onClick = { showQuickAddPicker = true }) {
                        com.spendr.app.kt.ui.components.MciIcon(
                            "lightning-bolt",
                            24.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (editing) {
                        BouncyIconButton(onClick = { viewModel.deleteEditing(onDeleted = onDone) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete transaction")
                        }
                    }
                },
            )
        },
        bottomBar = {
            // RN: the keypad is the pinned bottom sibling; the FooterBar (Save)
            // lives at the end of the form column, so Save sits ABOVE the keypad.
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
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // RN contentContainer: padding 16, section gap 12
                    .padding(16.dp),
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

                // Date (RN: label above the field)
                Column {
                    Text(
                        "Date",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                BouncySurface(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpendrTheme.colors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
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
                }

            // Category select field (RN: 42dp icon, 16/8 padding, hairline border)
            BouncySurface(
                onClick = { showCategoryPicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpendrTheme.colors.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val selected = categories.firstOrNull { it.id == state.categoryId }
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected != null) {
                        CategoryIconBadge(icon = selected.icon, color = selected.color, size = 42.dp)
                    } else {
                        com.spendr.app.kt.ui.components.MciIcon(
                            "shape-outline",
                            24.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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

            Column {
                Text(
                    "Note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                // RN ThemedTextInput: rounded 1dp border, primary when focused
                val noteInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val noteFocused by noteInteraction.collectIsFocusedAsState()
                TextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    placeholder = { Text("e.g., Premium coffee") },
                    colors = INPUT_COLORS,
                    shape = MaterialTheme.shapes.medium,
                    interactionSource = noteInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (noteFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                SpendrTheme.colors.border
                            },
                            MaterialTheme.shapes.medium,
                        )
                        .focusRequester(noteFocus),
                    singleLine = true,
                )
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

            Column {
                Text(
                    "Merchant (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                val merchantInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val merchantFocused by merchantInteraction.collectIsFocusedAsState()
                TextField(
                    value = state.merchant,
                    onValueChange = viewModel::setMerchant,
                    placeholder = { Text("e.g., Indomaret") },
                    colors = INPUT_COLORS,
                    shape = MaterialTheme.shapes.medium,
                    interactionSource = merchantInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (merchantFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                SpendrTheme.colors.border
                            },
                            MaterialTheme.shapes.medium,
                        )
                        .focusRequester(merchantFocus),
                    singleLine = true,
                )
            }


                if (state.amountError) {
                    Text(
                        "Amount not entered",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            // RN FooterBar sits between the scroll area and the keypad:
            // hairline top border, filled Save (surfaceVariant/textTertiary disabled)
            Column {
                HorizontalDivider(color = SpendrTheme.colors.border)
                BouncyButton(
                    onClick = { viewModel.save(onSaved = onDone) },
                    enabled = state.draft.saveEnabled,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        if (editing) "Save changes" else "Save",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
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
    BouncySurface(
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(suggestion.categoryName)
                        suggestion.merchant?.let { append(" • ").append(it) }
                        append(" • ").append(formatDate(suggestion.lastDate))
                        if ((suggestion.lastDiscountAmount ?: 0) > 0) {
                            append(" • Save ").append(formatRupiah(suggestion.lastDiscountAmount!!))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
            BouncySurface(
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

/**
 * RN `QuickAddPicker`: one-tap prefill list mirroring the CategoryPicker
 * sheet shape — item rows (badge, label, category, paid amount) plus the
 * "Manage quick adds" link. Empty state keeps the same CTA.
 */
@Composable
private fun QuickAddPickerContent(
    items: List<com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow>,
    onSelect: (com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow) -> Unit,
    onManage: () -> Unit,
) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
        Text(
            "Quick add",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (items.isEmpty()) {
            // RN EmptyState: icon 40, title, message, paddingVertical xxxl
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.spendr.app.kt.ui.components.MciIcon(
                    "lightning-bolt",
                    40.dp,
                    SpendrTheme.colors.textTertiary,
                )
                Text(
                    "No quick add yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Create one to prefill Add Spending in a tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    BouncySurface(
                        onClick = { onSelect(item) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryIconBadge(
                                icon = item.categoryIcon,
                                color = item.categoryColor,
                                size = 36.dp,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.quickAdd.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.categoryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            item.quickAdd.paidAmount?.let { paid ->
                                Text(
                                    formatRupiah(paid),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendrTheme.colors.expense,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Manage quick adds",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(onClick = onManage)
                .padding(top = 8.dp, bottom = 12.dp),
        )
    }
}
