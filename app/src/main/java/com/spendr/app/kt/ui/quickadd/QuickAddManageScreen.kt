package com.spendr.app.kt.ui.quickadd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.add.AmountKeypad
import kotlinx.coroutines.launch

private val INPUT_COLORS @Composable get() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent,
)

/** QuickAdd create/delete, ported from RN `QuickAddManageScreen` (no edit, no reorder). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddManageScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val quickAdds by container.quickAdds.observeQuickAdds().collectAsState(initial = emptyList())
    val categories by container.categories.observeCategories().collectAsState(initial = emptyList())
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TopAppBar(title = { Text("Quick Add", fontWeight = FontWeight.Bold) })
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
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
                            IconButton(onClick = { deleting = qa }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove ${qa.quickAdd.label}")
                            }
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
                TextButton(
                    onClick = {
                        scope.launch {
                            container.quickAdds.deleteQuickAdd(qa.quickAdd.id)
                            deleting = null
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
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
    var showCategoryPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New quick add", style = MaterialTheme.typography.titleLarge)
            TextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                placeholder = { Text("e.g., Coffee") },
                colors = INPUT_COLORS,
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                onClick = { showCategoryPicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val selected = categories.firstOrNull { it.id == categoryId }
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    selected?.let {
                        CategoryIconBadge(icon = it.icon, color = it.color, size = 36.dp)
                    }
                    Text(
                        selected?.name ?: "Select category",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Amount (optional)", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Rp " + (parseDigits(amountStr).takeIf { it > 0 }?.let { formatAmount(it) } ?: "0"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            AmountKeypad(
                onDigit = { digits -> amountStr = (amountStr.takeUnless { it == "0" } ?: "") + digits },
                onBackspace = { amountStr = amountStr.dropLast(1) },
                onClear = { amountStr = "" },
                onClose = {},
                onNext = {},
            )
            Button(
                onClick = {
                    val id = categoryId
                    if (label.isNotBlank() && id != null) {
                        onSave(label.trim(), id, parseDigits(amountStr))
                    }
                },
                enabled = label.isNotBlank() && categoryId != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }

    if (showCategoryPicker) {
        ModalBottomSheet(onDismissRequest = { showCategoryPicker = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                categories.forEach { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        leadingContent = {
                            CategoryIconBadge(icon = category.icon, color = category.color, size = 36.dp)
                        },
                        modifier = Modifier.clickable {
                            categoryId = category.id
                            showCategoryPicker = false
                        },
                    )
                }
            }
        }
    }
}
