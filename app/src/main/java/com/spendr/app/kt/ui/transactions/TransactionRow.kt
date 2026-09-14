package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.formatDate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * Full-bleed transaction row, ported from RN `TransactionRow`: 42 dp category
 * circle, note title (w600), "category • merchant" meta, paid amount in
 * `expense` (primary) w700, "saved Rp X" in `savings` (tertiary).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    row: TransactionWithCategoryRow,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showDate: Boolean = true,
    showDayOfWeek: Boolean = false,
    highlight: Float = 0f,
) {
    val tx = row.transaction
    val hasDiscount = (tx.discountAmount ?: 0) > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick ?: {},
                onLongClick = onLongClick,
            )
            .pulseHighlight(highlight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIconBadge(
            icon = row.categoryIcon,
            color = row.categoryColor,
            size = 42.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = tx.note ?: row.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(row.categoryName)
                    tx.merchant?.let { append(" • ").append(it) }
                    if (showDate) {
                        append(" • ")
                        append(if (showDayOfWeek) formatDayShort(tx.date) else formatDate(tx.date))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = formatRupiah(tx.paidAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = SpendrTheme.colors.expense,
            )
            if (hasDiscount) {
                Text(
                    text = "saved " + formatRupiah(tx.discountAmount!!),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpendrTheme.colors.savings,
                )
            }
        }
    }
}

@Composable
private fun Modifier.pulseHighlight(alpha: Float): Modifier {
    if (alpha <= 0f) return this
    val color = MaterialTheme.colorScheme.primary
    return this.then(
        Modifier.drawBehind { drawRect(color.copy(alpha = alpha)) },
    )
}

/** Day-group header: "EEE, d MMM" + group total, ported from RN list sections. */
@Composable
fun DayGroupHeader(label: String, total: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            formatRupiah(total),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
