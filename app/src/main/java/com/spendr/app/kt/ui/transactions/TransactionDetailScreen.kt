package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatDateTime
import com.spendr.app.kt.domain.formatFullDate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.theme.SpendrTheme

/** Read-only detail: every stored field of a Transaction, RN `TransactionDetailScreen` styling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val tx by viewModel.transaction.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDeleted) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = SpendrTheme.colors.border)
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val row = tx
                        androidx.compose.material3.Button(
                            onClick = { row?.let { onEdit(it.transaction.id) } },
                            enabled = row != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("Edit") }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { row?.let { onDuplicate(it.transaction.id) } },
                            enabled = row != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("Duplicate") }
                    }
                }
            }
        },
    ) { innerPadding ->
        val row = tx ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CategoryIconBadge(
                    icon = row.categoryIcon,
                    color = row.categoryColor,
                    size = 56.dp,
                )
                Column {
                    Text(
                        row.categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatFullDate(row.transaction.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatRupiah(row.transaction.paidAmount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SpendrTheme.colors.expense,
            )
            if ((row.transaction.discountAmount ?: 0) > 0 && row.transaction.originalAmount != null) {
                Text(
                    text = "from " + formatRupiah(row.transaction.originalAmount!!) +
                        " · saved " + formatRupiah(row.transaction.discountAmount!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpendrTheme.colors.savings,
                )
            }

            DetailField(Icons.Outlined.Description, "Note", row.transaction.note ?: "—")
            DetailField(Icons.Outlined.Storefront, "Merchant", row.transaction.merchant ?: "—")
            DetailField(Icons.Outlined.Sell, "Paid amount", formatRupiah(row.transaction.paidAmount))
            DetailField(
                Icons.Outlined.Sell,
                "Original amount",
                row.transaction.originalAmount?.let { formatRupiah(it) } ?: "—",
            )
            DetailField(
                Icons.Outlined.Sell,
                "Discount amount",
                row.transaction.discountAmount?.let { formatRupiah(it) } ?: "—",
            )
            DetailField(
                Icons.Outlined.Sell,
                "Discount type",
                row.transaction.discountType ?: "—",
            )
            DetailField(Icons.Outlined.LocalOffer, "Tags", row.transaction.tags ?: "—")
            DetailField(Icons.Outlined.AccessTime, "Created at", formatDateTime(row.transaction.createdAt))
            DetailField(Icons.Outlined.AccessTime, "Updated at", formatDateTime(row.transaction.updatedAt))
        }
    }
}

@Composable
private fun DetailField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.labelMedium) },
        supportingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}
