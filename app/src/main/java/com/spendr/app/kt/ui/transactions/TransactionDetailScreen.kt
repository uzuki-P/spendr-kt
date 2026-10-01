package com.spendr.app.kt.ui.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.spendr.app.kt.ui.components.AnimatedAmount
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.rememberCollapsingBar
import com.spendr.app.kt.ui.components.segmentCorners
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
 * Read-only detail: tonal hero (spinning-in category cookie, counting paid
 * amount, savings chip), receipt items, segmented detail and history rows,
 * the discount breakdown, and a Duplicate / Edit footer.
 */
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val tx by viewModel.transaction.collectAsState()
    val receiptItems by viewModel.receiptItems.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollBehavior = rememberCollapsingBar()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { MciIcon("trash-can-outline", 24.dp, MaterialTheme.colorScheme.error) },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                BouncyTextButton(
                    onClick = { viewModel.delete(onDeleted) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { BouncyTextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SpendrTopBar(
                title = "Transaction",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (tx != null) {
                        BouncyIconButton(onClick = { showDeleteDialog = true }) {
                            MciIcon("trash-can-outline", 24.dp, MaterialTheme.colorScheme.error, contentDescription = "Delete transaction")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val row = tx
                    BouncyTonalButton(
                        onClick = { row?.let { onDuplicate(it.transaction.id) } },
                        enabled = row != null,
                        height = ButtonDefaults.MediumContainerHeight,
                        modifier = Modifier.weight(1f),
                    ) {
                        MciIcon("content-copy", 20.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Duplicate", modifier = Modifier.padding(start = 8.dp))
                    }
                    BouncyButton(
                        onClick = { row?.let { onEdit(it.transaction.id) } },
                        enabled = row != null,
                        height = ButtonDefaults.MediumContainerHeight,
                        modifier = Modifier.weight(1f),
                    ) {
                        MciIcon("pencil-outline", 20.dp, MaterialTheme.colorScheme.onPrimary)
                        Text("Edit", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
    ) { innerPadding ->
        val row = tx
        if (row == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    glyph = "magnify",
                    title = "Transaction not found",
                    message = "It may have been deleted.",
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
                row.transaction.originalAmount.toDouble() * 100).roundToInt()
        } else {
            0
        }
        val isReceipt = row.transaction.type == "receipt"

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero: category cookie spins in, amount counts up
            val entrance = remember { Animatable(0f) }
            LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
            Surface(
                shape = MaterialTheme.shapes.extraLargeIncreased,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = entrance.value
                                    scaleY = entrance.value
                                    rotationZ = (1f - entrance.value) * -120f
                                }
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialShapes.Cookie9Sided.toShape()),
                            contentAlignment = Alignment.Center,
                        ) {
                            CategoryIconBadge(
                                icon = row.categoryIcon,
                                color = row.categoryColor,
                                size = 64.dp,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (isReceipt) "Receipt at" else "Category",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            )
                            Text(
                                if (isReceipt) row.transaction.merchant.orEmpty() else row.categoryName,
                                style = MaterialTheme.typography.titleLargeEmphasized,
                            )
                            if (isReceipt) {
                                Text(
                                    row.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Paid amount",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                        AnimatedAmount(
                            amount = row.transaction.paidAmount,
                            style = MaterialTheme.typography.displayMediumEmphasized,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (hasDiscount) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    MciIcon("sale-outline", 16.dp, MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(
                                        "You saved " + formatRupiah(row.transaction.discountAmount!!),
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isReceipt) {
                ReceiptItemsCard(receiptItems, row.transaction.paidAmount)
            }

            val details = buildList {
                add(Triple("calendar-blank-outline", "Date", formatFullDate(row.transaction.date)))
                add(Triple("note-text-outline", "Note", row.transaction.note))
                if (!isReceipt) add(Triple("storefront-outline", "Merchant", row.transaction.merchant))
            }
            SegmentedGroup {
                details.forEachIndexed { index, (glyph, label, value) ->
                    DetailRow(glyph, label, value, segmentCorners(index, details.size))
                }
            }

            Column {
                SectionTitle("Record history")
                SegmentedGroup {
                    DetailRow("clock-plus-outline", "Created at", formatDateTime(row.transaction.createdAt), segmentCorners(0, 2))
                    DetailRow("clock-edit-outline", "Updated at", formatDateTime(row.transaction.updatedAt), segmentCorners(1, 2))
                }
            }

            // Amount breakdown, only when discounted
            if (hasDiscount) {
                Column {
                    SectionTitle("Amount breakdown")
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
    )
}

@Composable
private fun ReceiptItemsCard(items: List<com.spendr.app.kt.data.db.entity.ReceiptItemEntity>, paidAmount: Long) {
    val itemTotal = items.sumOf { it.paidAmount }
    val difference = paidAmount - itemTotal
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MciIcon("receipt-text-outline", 22.dp, MaterialTheme.colorScheme.primary)
                Text("Receipt items", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
                Text("${items.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (items.isEmpty()) {
                Text("No items recorded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (item.quantity != "1") Text("Qty ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatRupiah(item.paidAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BreakdownRow("Items total", MaterialTheme.colorScheme.onSurfaceVariant, formatRupiah(itemTotal), MaterialTheme.colorScheme.onSurface, valueWeight = FontWeight.SemiBold)
            if (difference != 0L) {
                BreakdownRow(
                    if (difference > 0) "Unallocated" else "Items exceed total",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    formatRupiah(kotlin.math.abs(difference)),
                    MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Detail row in a segmented group: tonal icon chip, label over value, "—" for null. */
@Composable
private fun DetailRow(glyph: String, label: String, value: String?, corners: Corners) {
    MorphSurface(
        onClick = null,
        corners = corners,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                MciIcon(glyph, 20.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value ?: "\u2014",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value != null) MaterialTheme.colorScheme.onSurface else SpendrTheme.colors.textTertiary,
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
