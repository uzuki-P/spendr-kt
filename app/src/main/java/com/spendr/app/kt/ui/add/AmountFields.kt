package com.spendr.app.kt.ui.add

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.AmountDraft
import com.spendr.app.kt.domain.AmountField
import com.spendr.app.kt.domain.DiscountMode
import com.spendr.app.kt.domain.formatAmount
import com.spendr.app.kt.domain.parseDigits
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * The three interdependent amount fields, ported from RN `AmountFields.tsx`:
 * container surfaceContainerHighest radius 20, 1.5 dp active borders, Rp/%
 * slider toggle, success-green "Save Rp" block on the pay row.
 */
@Composable
fun AmountFields(
    draft: AmountDraft,
    onSelectField: (AmountField) -> Unit,
    onToggleDiscount: () -> Unit,
    onToggleDiscountType: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .surfaceHighest()
            .animateContentSize(tween(280))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!draft.hasDiscount) {
            FieldBox(
                label = "You pay",
                value = displayAmount(draft.amountStr).ifEmpty { "0" },
                suffix = "Rp",
                active = draft.activeField == AmountField.AMOUNT,
                emphasized = true,
                onClick = { onSelectField(AmountField.AMOUNT) },
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(onClick = onToggleDiscount),
            ) {
                com.spendr.app.kt.ui.components.MciIcon(
                    "sale-outline",
                    18.dp,
                    MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Add discount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Amount and discount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Remove discount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = onToggleDiscount),
                )
            }
            FieldBox(
                label = "Original",
                value = displayAmount(draft.originalStr).ifEmpty { "0" },
                suffix = "Rp",
                active = draft.activeField == AmountField.ORIGINAL,
                onClick = { onSelectField(AmountField.ORIGINAL) },
            )
            FieldBox(
                label = "Discount",
                value = if (draft.discountMode == DiscountMode.PERCENTAGE) {
                    displayAmount(draft.pctStr).ifEmpty { "0" }
                } else {
                    displayAmount(draft.discountStr).ifEmpty { "0" }
                },
                suffix = null,
                active = draft.activeField == AmountField.DISCOUNT || draft.activeField == AmountField.PCT,
                onClick = {
                    onSelectField(
                        if (draft.discountMode == DiscountMode.PERCENTAGE) AmountField.PCT else AmountField.DISCOUNT,
                    )
                },
                trailing = {
                    ModeToggle(
                        percentage = draft.discountMode == DiscountMode.PERCENTAGE,
                        onFixed = { if (draft.discountMode != DiscountMode.FIXED) onToggleDiscountType() },
                        onPercentage = { if (draft.discountMode != DiscountMode.PERCENTAGE) onToggleDiscountType() },
                    )
                },
            )
            FieldBox(
                label = "You pay",
                value = displayAmount(draft.amountStr).ifEmpty { "0" },
                suffix = "Rp",
                active = draft.activeField == AmountField.AMOUNT,
                emphasized = true,
                showSavings = draft.discountAmount > 0,
                savedAmount = draft.discountAmount,
                onClick = { onSelectField(AmountField.AMOUNT) },
            )
        }
    }
}

@Composable
private fun Modifier.surfaceHighest(): Modifier = this.background(
    MaterialTheme.colorScheme.surfaceContainerHighest,
    RoundedCornerShape(20.dp),
)

@Composable
private fun FieldBox(
    label: String,
    value: String,
    suffix: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    showSavings: Boolean = false,
    savedAmount: Long = 0,
    trailing: (@Composable () -> Unit)? = null,
) {
    val borderColor = if (active) MaterialTheme.colorScheme.primary else SpendrTheme.colors.border
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                value,
                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showSavings && savedAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Save Rp",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SpendrTheme.colors.success,
                        )
                        Text(
                            formatAmount(savedAmount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SpendrTheme.colors.success,
                        )
                    }
                }
                if (trailing != null) trailing()
                if (suffix != null) {
                    Text(
                        suffix,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 64x28 Rp/% toggle with sliding primary knob. */
@Composable
private fun ModeToggle(
    percentage: Boolean,
    onFixed: () -> Unit,
    onPercentage: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val knobColor = MaterialTheme.colorScheme.primaryContainer
    val knobOffset by animateDpAsState(if (percentage) 32.dp else 0.dp, label = "modeKnob")
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(28.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, SpendrTheme.colors.border, RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .width(32.dp)
                .height(28.dp)
                .padding(1.dp)
                .background(knobColor, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxWidth().height(28.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onFixed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Rp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!percentage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onPercentage),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun displayAmount(digits: String): String {
    val amount = parseDigits(digits)
    return if (amount > 0) formatAmount(amount) else if (digits.isNotEmpty()) "0" else ""
}
