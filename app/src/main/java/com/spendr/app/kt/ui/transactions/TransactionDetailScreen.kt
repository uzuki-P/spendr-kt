package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.BouncyTonalButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.theme.SpendrTheme
import kotlin.math.roundToInt

/**
 * Read-only detail, ported from RN `TransactionDetailScreen`: hero card
 * (category + paid amount), details card (date/note/merchant), record history,
 * amount breakdown when discounted, Duplicate/Edit footer.
 */
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
                BouncyTextButton(onClick = { viewModel.delete(onDeleted) }) { Text("Delete") }
            },
            dismissButton = { BouncyTextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    BouncyIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tx != null) {
                        BouncyIconButton(onClick = { showDeleteDialog = true }) {
                            MciIcon(
                                "trash-can-outline",
                                24.dp,
                                MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // RN FooterBar: hairline top border, 16/12 padding, two flex buttons
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = SpendrTheme.colors.border)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val row = tx
                        BouncyTonalButton(
                            onClick = { row?.let { onDuplicate(it.transaction.id) } },
                            enabled = row != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            MciIcon("content-copy", 18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                "Duplicate",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        BouncyButton(
                            onClick = { row?.let { onEdit(it.transaction.id) } },
                            enabled = row != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            MciIcon("pencil-outline", 18.dp, MaterialTheme.colorScheme.onPrimary)
                            Text(
                                "Edit",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val row = tx
        if (row == null) {
            // RN EmptyState "Transaction not found"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MciIcon("magnify", 40.dp, SpendrTheme.colors.textTertiary)
                Text(
                    "Transaction not found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "It may have been deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        val hasDiscount = (row.transaction.discountAmount ?: 0) > 0 &&
            (row.transaction.originalAmount ?: 0) > 0
        val discountPercent = if (hasDiscount && row.transaction.originalAmount != null &&
            row.transaction.originalAmount > 0
        ) {
            (row.transaction.discountAmount!!.toDouble() /
                row.transaction.originalAmount!!.toDouble() * 100).roundToInt()
        } else {
            0
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero card: category, then paid amount below a hairline
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryIconBadge(
                            icon = row.categoryIcon,
                            color = row.categoryColor,
                            size = 56.dp,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Category",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                row.categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // RN heroAmount: 1dp top border, paddingTop 16, gap 2
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Paid amount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatRupiah(row.transaction.paidAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendrTheme.colors.expense,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (hasDiscount) {
                            Text(
                                "You saved " + formatRupiah(row.transaction.discountAmount!!),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SpendrTheme.colors.savings,
                            )
                        }
                    }
                }
            }

            // Details card: date / note / merchant
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("calendar-blank-outline", "Date", formatFullDate(row.transaction.date))
                    Hairline()
                    DetailRow("note-text-outline", "Note", row.transaction.note)
                    Hairline()
                    DetailRow("storefront-outline", "Merchant", row.transaction.merchant)
                }
            }

            // Record history card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Record history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    DetailRow("clock-plus-outline", "Created at", formatDateTime(row.transaction.createdAt))
                    Hairline()
                    DetailRow("clock-edit-outline", "Updated at", formatDateTime(row.transaction.updatedAt))
                }
            }

            // Amount breakdown card, only when discounted
            if (hasDiscount) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Amount breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        BreakdownRow(
                            label = "Original",
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            value = formatRupiah(row.transaction.originalAmount!!),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                        )
                        BreakdownRow(
                            label = if (discountPercent > 0) "Discount ($discountPercent%)" else "Discount",
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            value = "\u2212" + formatRupiah(row.transaction.discountAmount!!),
                            valueColor = SpendrTheme.colors.savings,
                            valueWeight = FontWeight.SemiBold,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        BreakdownRow(
                            label = "Paid",
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            value = formatRupiah(row.transaction.paidAmount),
                            valueColor = SpendrTheme.colors.expense,
                            weight = FontWeight.Bold,
                            valueWeight = FontWeight.Bold,
                            titleStyle = true,
                        )
                    }
                }
            }
        }
    }
}

/** RN DetailRow: 36dp circular icon chip, label over value, "—" for null. */
@Composable
private fun DetailRow(glyph: String, label: String, value: String?) {
    Row(
        Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MciIcon(glyph, 18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    "\u2014",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpendrTheme.colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun Hairline() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** RN breakdownRow: space-between label/value pair. */
@Composable
private fun BreakdownRow(
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    weight: FontWeight = FontWeight.Normal,
    valueWeight: FontWeight = FontWeight.Normal,
    titleStyle: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = if (titleStyle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = weight,
            color = labelColor,
        )
        Text(
            value,
            style = if (titleStyle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = valueWeight,
            color = valueColor,
        )
    }
}
