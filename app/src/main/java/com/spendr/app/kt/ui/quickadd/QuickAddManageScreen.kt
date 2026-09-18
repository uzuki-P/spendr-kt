package com.spendr.app.kt.ui.quickadd

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
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

/** QuickAdd create/delete, ported from RN `QuickAddManageScreen` (no edit, no reorder). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddManageScreen(container: AppContainer, onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val quickAdds by container.quickAdds.observeQuickAdds().collectAsState(initial = emptyList())
    val categories by container.categories.observeCategories().collectAsState(initial = emptyList())
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Quick Add", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    BouncyIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BouncyTonalButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                MciIcon("plus", 18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Add quick add", Modifier.padding(start = 8.dp))
            }
                if (quickAdds.isEmpty()) {
                    Text(
                        "No quick add yet. Create a quick add for fast input on Home.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                quickAdds.forEach { qa ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryIconBadge(icon = qa.categoryIcon, color = qa.categoryColor, size = 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    qa.quickAdd.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    qa.categoryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            qa.quickAdd.paidAmount?.let {
                                Text(
                                    formatRupiah(it),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
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
        QuickAddCreateSheet(
            categories = categories,
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            ThemedTextField(
                value = label,
                onValueChange = { label = it },
                label = "Label",
                placeholder = "e.g., Coffee",
                modifier = Modifier.fillMaxWidth(),
            )
            // RN CategorySelectField (no chevron, no placeholder icon)
            BouncySurface(
                onClick = { showCategoryPicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SpendrTheme.colors.border,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val selected = categories.firstOrNull { it.id == categoryId }
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    selected?.let {
                        CategoryIconBadge(icon = it.icon, color = it.color, size = 42.dp)
                    }
                    Text(
                        selected?.name ?: "Select category",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Unspecified
                        },
                    )
                }
            }
            Column {
                Text(
                    "Amount (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                // Tapping the field opens the keypad sheet (add-spending keypad grammar)
                BouncySurface(
                    onClick = { showAmountKeypad = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        SpendrTheme.colors.border,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (paidAmount > 0) formatAmount(paidAmount) else "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (paidAmount > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                SpendrTheme.colors.textTertiary
                            },
                        )
                        Text(
                            "Rp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Live "You pay"-style display so the amount stays visible while typing
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (paidAmount > 0) formatAmount(paidAmount) else "0",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Rp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
