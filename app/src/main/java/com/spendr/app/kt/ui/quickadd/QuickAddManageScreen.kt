package com.spendr.app.kt.ui.quickadd

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentGap
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.rememberCollapsingBar
import com.spendr.app.kt.ui.components.segmentCorners
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.domain.QuickAddSuggestion
import com.spendr.app.kt.domain.formatAmount
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.parseDigits
import com.spendr.app.kt.ui.add.AmountKeypad
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.BouncyTonalButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.CategoryPickerContent
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.ThemedTextField
import com.spendr.app.kt.ui.theme.SpendrTheme
import kotlinx.coroutines.launch

/** QuickAdd create/delete (no edit, no reorder). */
@Composable
fun QuickAddManageScreen(container: AppContainer, onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val quickAdds by container.quickAdds.observeQuickAdds().collectAsState(initial = emptyList())
    val categories by container.categories.observeCategories().collectAsState(initial = emptyList())
    val frequentSpending by produceState(initialValue = emptyList<QuickAddSuggestion>()) {
        value = container.quickAdds.frequentSpending()
    }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow?>(null) }
    val scrollBehavior = rememberCollapsingBar()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SpendrTopBar(title = "Quick add", onBack = onBack, scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                expanded = !listState.canScrollBackward,
                icon = { MciIcon("plus", 24.dp, MaterialTheme.colorScheme.onPrimaryContainer) },
                text = { Text("New quick add") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        ) {
            if (quickAdds.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        glyph = "lightning-bolt",
                        title = "No quick add yet",
                        message = "Create a quick add for fast input on Home.",
                    )
                }
            }
            itemsIndexed(quickAdds, key = { _, qa -> qa.quickAdd.id }) { index, qa ->
                MorphSurface(
                    onClick = null,
                    corners = segmentCorners(index, quickAdds.size),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .padding(bottom = SegmentGap),
                ) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryIconBadge(icon = qa.categoryIcon, color = qa.categoryColor, size = 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(qa.quickAdd.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                qa.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        qa.quickAdd.paidAmount?.let {
                            Text(
                                formatRupiah(it),
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        BouncyIconButton(onClick = { deleting = qa }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove ${qa.quickAdd.label}")
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        // Skip suggestions that already exist as a QuickAdd
        val suggestions = frequentSpending.filter { suggestion ->
            quickAdds.none {
                it.quickAdd.categoryId == suggestion.categoryId &&
                    it.quickAdd.note.equals(suggestion.note, ignoreCase = true)
            }
        }
        QuickAddCreateSheet(
            categories = categories,
            suggestions = suggestions,
            onDismiss = { creating = false },
            onSave = { label, categoryId, amount ->
                scope.launch {
                    container.quickAdds.createQuickAdd(
                        label = label,
                        categoryId = categoryId,
                        note = label,
                        paidAmount = amount.takeIf { it > 0 },
                        merchant = null,
                    )
                    creating = false
                }
            },
        )
    }

    deleting?.let { qa ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${qa.quickAdd.label}\"?") },
            text = { Text("This quick add will be removed.") },
            confirmButton = {
                BouncyTextButton(
                    onClick = {
                        scope.launch {
                            container.quickAdds.deleteQuickAdd(qa.quickAdd.id)
                            deleting = null
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = { BouncyTextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddCreateSheet(
    categories: List<com.spendr.app.kt.data.db.entity.CategoryEntity>,
    suggestions: List<QuickAddSuggestion>,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf("") }
    var showAmountKeypad by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val paidAmount = parseDigits(amountStr)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "New quick add",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(start = 4.dp),
            )
            // Frequent spending fills the form; the user still reviews and saves
            val shown = suggestions.mapNotNull { suggestion ->
                categories.firstOrNull { it.id == suggestion.categoryId }?.let { suggestion to it }
            }
            if (shown.isNotEmpty()) {
                Text(
                    "Often spent in the last 6 months",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                SegmentedGroup {
                    shown.forEachIndexed { index, (suggestion, category) ->
                        FrequentSpendingRow(
                            suggestion = suggestion,
                            category = category,
                            corners = segmentCorners(index, shown.size, outer = 20.dp),
                            onClick = {
                                label = suggestion.note
                                categoryId = suggestion.categoryId
                                amountStr = suggestion.paidAmount.toString()
                            },
                        )
                    }
                }
            }
            ThemedTextField(
                value = label,
                onValueChange = { label = it },
                label = "Label",
                placeholder = "e.g., Coffee",
                modifier = Modifier.fillMaxWidth(),
            )
            // Category + amount as one segmented group; amount opens the keypad sheet
            val selected = categories.firstOrNull { it.id == categoryId }
            SegmentedGroup {
                MorphSurface(
                    onClick = { showCategoryPicker = true },
                    corners = segmentCorners(0, 2),
                    pressedCorners = Corners(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 44.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selected != null) {
                            CategoryIconBadge(icon = selected.icon, color = selected.color, size = 40.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Category",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                selected?.name ?: "Select category",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    Color.Unspecified
                                },
                            )
                        }
                        MciIcon("chevron-down", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MorphSurface(
                    onClick = { showAmountKeypad = true },
                    corners = segmentCorners(1, 2),
                    pressedCorners = Corners(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 44.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Amount (optional)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Rp " + if (paidAmount > 0) formatAmount(paidAmount) else "0",
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = if (paidAmount > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    SpendrTheme.colors.textTertiary
                                },
                            )
                        }
                        MciIcon("dialpad", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            BouncyButton(
                onClick = {
                    val id = categoryId
                    if (label.isNotBlank() && id != null) {
                        onSave(label.trim(), id, paidAmount)
                    }
                },
                enabled = label.isNotBlank() && categoryId != null,
                height = ButtonDefaults.MediumContainerHeight,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Text("Save", Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showAmountKeypad) {
        AmountKeypadSheet(
            paidAmount = paidAmount,
            onDigit = { digits -> amountStr = (amountStr.takeUnless { it == "0" } ?: "") + digits },
            onBackspace = { amountStr = amountStr.dropLast(1) },
            onClear = { amountStr = "" },
            onClose = { showAmountKeypad = false },
        )
    }

    if (showCategoryPicker) {
        ModalBottomSheet(onDismissRequest = { showCategoryPicker = false }) {
            CategoryPickerContent(
                categories = categories,
                selectedId = categoryId,
                onSelect = {
                    categoryId = it
                    showCategoryPicker = false
                },
            )
        }
    }
}

@Composable
private fun FrequentSpendingRow(
    suggestion: QuickAddSuggestion,
    category: com.spendr.app.kt.data.db.entity.CategoryEntity,
    corners: Corners,
    onClick: () -> Unit,
) {
    MorphSurface(
        onClick = onClick,
        corners = corners,
        pressedCorners = Corners(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconBadge(icon = category.icon, color = category.color, size = 36.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    suggestion.note,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${category.name} • ${suggestion.useCount}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatRupiah(suggestion.paidAmount),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = SpendrTheme.colors.expense,
            )
        }
    }
}

/** Keypad sheet for the amount field: live amount display + sheet-layout keypad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountKeypadSheet(
    paidAmount: Long,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberBottomSheetState(
        initialValue = androidx.compose.material3.SheetValue.Hidden,
        enabledValues = setOf(
            androidx.compose.material3.SheetValue.Hidden,
            androidx.compose.material3.SheetValue.Expanded,
        ),
    )
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Live amount so it stays visible while typing
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Amount", style = MaterialTheme.typography.labelLargeEmphasized)
                    Text(
                        "Rp " + if (paidAmount > 0) formatAmount(paidAmount) else "0",
                        style = MaterialTheme.typography.displaySmallEmphasized,
                    )
                }
            }
            AmountKeypad(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onClear = onClear,
                onClose = onClose,
                onNext = null,
            )
        }
    }
}
