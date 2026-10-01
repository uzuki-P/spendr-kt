package com.spendr.app.kt.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.formatDate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.theme.SpendrTheme

/** Peak alpha of the Home "just added" pulse; [TransactionRow] maps it to 0..1. */
private const val PULSE_PEAK = 0.22f

/**
 * Transaction row as a segmented-list item: cookie category badge, note over
 * "category • merchant • date", paidAmount in the expense color with a
 * savings chip when discounted. Pass [corners] from `segmentCorners` so the
 * row joins its group; the corners round out while pressed.
 */
@Composable
fun TransactionRow(
    row: TransactionWithCategoryRow,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showDate: Boolean = true,
    showDayOfWeek: Boolean = false,
    highlight: Float = 0f,
    corners: Corners = Corners(24.dp),
) {
    val tx = row.transaction
    val hasDiscount = (tx.discountAmount ?: 0) > 0
    val container = lerp(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.primaryContainer,
        (highlight / PULSE_PEAK).coerceIn(0f, 1f),
    )
    MorphSurface(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        corners = corners,
        pressedCorners = Corners(28.dp),
        color = container,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CategoryIconBadge(
                icon = row.categoryIcon,
                color = row.categoryColor,
                size = 44.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = tx.note ?: row.categoryName,
                    style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatRupiah(tx.paidAmount),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = SpendrTheme.colors.expense,
                    maxLines = 1,
                )
                if (hasDiscount) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = "saved " + formatRupiah(tx.discountAmount!!),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Day-group header: "EEE, d MMM" with the day's total, above a segmented group. */
@Composable
fun DayGroupHeader(label: String, total: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            formatRupiah(total),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
