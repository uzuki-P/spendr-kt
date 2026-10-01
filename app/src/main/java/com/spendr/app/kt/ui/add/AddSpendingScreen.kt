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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.spendr.app.kt.ui.components.EmphasizedAccelerate
import com.spendr.app.kt.ui.components.EmphasizedDecelerate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.segmentCorners
import com.spendr.app.kt.ui.theme.SpendrTheme

// Filled rounded fields without the underline; focus shows as a tonal lift
private val INPUT_COLORS @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
    val focusManager = LocalFocusManager.current

    // The custom keypad and the text fields never edit at the same time:
    // opening the keypad drops text focus (closing the system keyboard), and
    // focusing a text field closes the keypad (see onFocusChanged below).
    fun openKeypad() {
        focusManager.clearFocus()
        keypadVisible = true
    }

    fun focusAmountField(field: com.spendr.app.kt.domain.AmountField) {
        focusStage = when (field) {
            com.spendr.app.kt.domain.AmountField.ORIGINAL -> FocusStage.ORIGINAL
            com.spendr.app.kt.domain.AmountField.PCT -> FocusStage.PCT
            com.spendr.app.kt.domain.AmountField.DISCOUNT -> FocusStage.DISCOUNT
            else -> FocusStage.AMOUNT
        }
        viewModel.selectField(field)
        openKeypad()
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SpendrTopBar(
                title = if (editing) "Edit spending" else "Add spending",
                onBack = onDone,
                actions = {
                    // Lightning opens the QuickAddPicker (prefill); shows the
                    // sheet even with no items — empty state + manage CTA there.
                    BouncyIconButton(onClick = { showQuickAddPicker = true }) {
                        com.spendr.app.kt.ui.components.MciIcon(
                            "lightning-bolt",
                            24.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "Quick add",
                        )
                    }
                    if (editing) {
                        BouncyIconButton(onClick = { viewModel.deleteEditing(onDeleted = onDone) }) {
                            com.spendr.app.kt.ui.components.MciIcon(
                                "trash-can-outline",
                                24.dp,
                                MaterialTheme.colorScheme.error,
                                contentDescription = "Delete transaction",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // RN: the keypad is the pinned bottom sibling; the FooterBar (Save)
            // lives at the end of the form column, so Save sits ABOVE the keypad.
            // The bar is bottom-anchored, so growing it from its top edge reads
            // as a slide-up while the form resizes in the same motion. Non-
            // bouncing M3 emphasized curves: a spring exit kept settling
            // offscreen and delayed the form's jump into the freed space.
            AnimatedVisibility(
                visible = keypadVisible,
                enter = expandVertically(tween(400, easing = EmphasizedDecelerate), expandFrom = Alignment.Top),
                exit = shrinkVertically(tween(200, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Top),
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
                        openKeypad()
                    },
                    editing = keypadVisible,
                    onToggleDiscount = viewModel::toggleDiscount,
                    onToggleDiscountType = viewModel::toggleDiscountType,
                )

                // Date + category as one segmented group
                val selectedCategory = categories.firstOrNull { it.id == state.categoryId }
                SegmentedGroup {
                    FormRow(
                        corners = segmentCorners(0, 2),
                        onClick = { showDatePicker = true },
                        leading = {
                            com.spendr.app.kt.ui.components.MciIcon(
                                "calendar-blank-outline",
                                22.dp,
                                MaterialTheme.colorScheme.primary,
                            )
                        },
                        label = "Date",
                        value = formatFullDate(state.dateMs),
                        trailingGlyph = "chevron-right",
                    )
                    FormRow(
                        corners = segmentCorners(1, 2),
                        onClick = { showCategoryPicker = true },
                        leading = {
                            if (selectedCategory != null) {
                                CategoryIconBadge(icon = selectedCategory.icon, color = selectedCategory.color, size = 40.dp)
                            } else {
                                com.spendr.app.kt.ui.components.MciIcon(
                                    "shape-outline",
                                    22.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        label = "Category",
                        value = selectedCategory?.name ?: "Select category",
                        valueMuted = selectedCategory == null,
                        trailingGlyph = "chevron-down",
                    )
                }

                TextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = { Text("Note") },
                    placeholder = { Text("e.g., Premium coffee") },
                    leadingIcon = {
                        com.spendr.app.kt.ui.components.MciIcon(
                            "note-text-outline",
                            22.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = INPUT_COLORS,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(noteFocus)
                        .onFocusChanged { if (it.isFocused) keypadVisible = false },
                    singleLine = true,
                )

                // Note suggestions, springing in as the list changes
                AnimatedVisibility(
                    visible = suggestions.isNotEmpty(),
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                ) {
                    val shown = suggestions.take(6)
                    SegmentedGroup {
                        shown.forEachIndexed { index, suggestion ->
                            SuggestionRow(
                                suggestion = suggestion,
                                corners = segmentCorners(index, shown.size, outer = 20.dp),
                                onFull = { viewModel.applySuggestion(suggestion, SuggestionMode.FULL) },
                                onNoteOnly = { viewModel.applySuggestion(suggestion, SuggestionMode.NOTE_ONLY) },
                            )
                        }
                    }
                }

                TextField(
                    value = state.merchant,
                    onValueChange = viewModel::setMerchant,
                    label = { Text("Merchant (optional)") },
                    placeholder = { Text("e.g., Indomaret") },
                    leadingIcon = {
                        com.spendr.app.kt.ui.components.MciIcon(
                            "storefront-outline",
                            22.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = INPUT_COLORS,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(merchantFocus)
                        .onFocusChanged { if (it.isFocused) keypadVisible = false },
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

            // Save sits between the scroll area and the keypad
            BouncyButton(
                onClick = { viewModel.save(onSaved = onDone) },
                enabled = state.draft.saveEnabled,
                height = ButtonDefaults.MediumContainerHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(
                    if (editing) "Save changes" else "Save",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Tappable label/value row inside the form's segmented group. */
@Composable
private fun FormRow(
    corners: com.spendr.app.kt.ui.components.Corners,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    label: String,
    value: String,
    trailingGlyph: String,
    valueMuted: Boolean = false,
) {
    MorphSurface(
        onClick = onClick,
        corners = corners,
        pressedCorners = com.spendr.app.kt.ui.components.Corners(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { leading() }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (valueMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            com.spendr.app.kt.ui.components.MciIcon(
                trailingGlyph,
                22.dp,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: com.spendr.app.kt.domain.model.NoteSuggestion,
    corners: com.spendr.app.kt.ui.components.Corners,
    onFull: () -> Unit,
    onNoteOnly: () -> Unit,
) {
    MorphSurface(
        onClick = onFull,
        corners = corners,
        pressedCorners = com.spendr.app.kt.ui.components.Corners(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
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
                    style = MaterialTheme.typography.bodyLargeEmphasized,
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
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = SpendrTheme.colors.expense,
                )
            }
            // Fill only the note, keeping the typed amount
            FilledTonalIconButton(
                onClick = onNoteOnly,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Use note only" },
            ) {
                com.spendr.app.kt.ui.components.MciIcon(
                    "text-short",
                    18.dp,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Modifier.clearAndSetSemantics { },
                )
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
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
        )
        if (items.isEmpty()) {
            com.spendr.app.kt.ui.components.EmptyState(
                glyph = "lightning-bolt",
                title = "No quick add yet",
                message = "Create one to prefill Add Spending in a tap.",
            )
        } else {
            SegmentedGroup {
                items.forEachIndexed { index, item ->
                    MorphSurface(
                        onClick = { onSelect(item) },
                        corners = segmentCorners(index, items.size),
                        pressedCorners = com.spendr.app.kt.ui.components.Corners(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                size = 44.dp,
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
        BouncyTextButton(
            onClick = onManage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            com.spendr.app.kt.ui.components.MciIcon("cog-outline", 18.dp, MaterialTheme.colorScheme.primary)
            Text("Manage quick adds", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
