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
            AmountRow(
                label = "You pay",
                value = displayAmount(draft.amountStr).ifEmpty { "0" },
                active = draft.activeField == AmountField.AMOUNT,
                emphasized = true,
                onClick = { onSelectField(AmountField.AMOUNT) },
            )
            Surface(
                onClick = onToggleDiscount,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Sell,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Add discount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Text(
                "Amount and discount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FieldBox(
                label = "Original price",
                value = displayAmount(draft.originalStr).ifEmpty { "0" },
                suffix = "Rp",
                active = draft.activeField == AmountField.ORIGINAL,
                onClick = { onSelectField(AmountField.ORIGINAL) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FieldBox(
                    label = "Discount",
                    value = if (draft.discountMode == DiscountMode.PERCENTAGE) {
                        displayAmount(draft.pctStr).ifEmpty { "0" }
                    } else {
                        displayAmount(draft.discountStr).ifEmpty { "0" }
                    },
                    suffix = if (draft.discountMode == DiscountMode.PERCENTAGE) "%" else "Rp",
                    active = draft.activeField == AmountField.DISCOUNT || draft.activeField == AmountField.PCT,
                    onClick = {
                        onSelectField(
                            if (draft.discountMode == DiscountMode.PERCENTAGE) AmountField.PCT else AmountField.DISCOUNT,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                ModeToggle(
                    percentage = draft.discountMode == DiscountMode.PERCENTAGE,
                    onFixed = { if (draft.discountMode != DiscountMode.FIXED) onToggleDiscountType() },
                    onPercentage = { if (draft.discountMode != DiscountMode.PERCENTAGE) onToggleDiscountType() },
                )
            }
            AmountRow(
                label = "You pay",
                value = displayAmount(draft.amountStr).ifEmpty { "0" },
                active = draft.activeField == AmountField.AMOUNT,
                emphasized = true,
                onClick = { onSelectField(AmountField.AMOUNT) },
                showSavings = draft.discountAmount > 0,
                savedAmount = draft.discountAmount,
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
    suffix: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                suffix,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    showSavings: Boolean = false,
    savedAmount: Long = 0,
) {
    FieldBox(
        label = label,
        value = value,
        suffix = "Rp",
        active = active,
        onClick = onClick,
    )
    if (showSavings && savedAmount > 0) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
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
