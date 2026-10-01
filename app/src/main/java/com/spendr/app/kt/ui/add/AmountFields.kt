package com.spendr.app.kt.ui.add

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.AmountDraft
import com.spendr.app.kt.domain.AmountField
import com.spendr.app.kt.domain.DiscountMode
import com.spendr.app.kt.domain.formatAmount
import com.spendr.app.kt.domain.parseDigits
import kotlinx.coroutines.delay
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentGap
import com.spendr.app.kt.ui.components.segmentCorners
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * The three interdependent amount fields on a primaryContainer hero. Without
 * a discount it is one large "You pay" figure; with one, Original / Discount /
 * You pay stack as a segmented group. The active field lifts onto the surface
 * color and shows a blinking caret; digits roll in as they are typed.
 */
@Composable
fun AmountFields(
    draft: AmountDraft,
    onSelectField: (AmountField) -> Unit,
    editing: Boolean,
    onToggleDiscount: () -> Unit,
    onToggleDiscountType: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (!draft.hasDiscount) {
                FieldBox(
                    label = "You pay",
                    value = displayAmount(draft.amountStr).ifEmpty { "0" },
                    active = editing && (draft.activeField == AmountField.AMOUNT),
                    emphasized = true,
                    corners = Corners(24.dp),
                    onClick = { onSelectField(AmountField.AMOUNT) },
                )
                // Full-width, tall target: the whole strip under the amount taps
                BouncyTextButton(
                    onClick = onToggleDiscount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(top = 4.dp),
                ) {
                    MciIcon("sale-outline", 18.dp, MaterialTheme.colorScheme.primary)
                    Text("Add discount", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Amount and discount",
                        style = MaterialTheme.typography.titleSmallEmphasized,
                    )
                    BouncyTextButton(
                        onClick = onToggleDiscount,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Remove") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(SegmentGap)) {
                    FieldBox(
                        label = "Original",
                        value = displayAmount(draft.originalStr).ifEmpty { "0" },
                        active = editing && (draft.activeField == AmountField.ORIGINAL),
                        corners = segmentCorners(0, 3),
                        onClick = { onSelectField(AmountField.ORIGINAL) },
                    )
                    val percentage = draft.discountMode == DiscountMode.PERCENTAGE
                    FieldBox(
                        label = "Discount",
                        value = if (percentage) {
                            displayAmount(draft.pctStr).ifEmpty { "0" }
                        } else {
                            displayAmount(draft.discountStr).ifEmpty { "0" }
                        },
                        prefix = if (percentage) null else "Rp",
                        suffix = if (percentage) "%" else null,
                        active = editing && (draft.activeField == AmountField.DISCOUNT || draft.activeField == AmountField.PCT),
                        corners = segmentCorners(1, 3),
                        onClick = {
                            onSelectField(if (percentage) AmountField.PCT else AmountField.DISCOUNT)
                        },
                        trailing = {
                            ModeToggle(
                                percentage = percentage,
                                onFixed = { if (percentage) onToggleDiscountType() },
                                onPercentage = { if (!percentage) onToggleDiscountType() },
                            )
                        },
                    )
                    FieldBox(
                        label = "You pay",
                        value = displayAmount(draft.amountStr).ifEmpty { "0" },
                        active = editing && (draft.activeField == AmountField.AMOUNT),
                        emphasized = true,
                        savedAmount = draft.discountAmount,
                        corners = segmentCorners(2, 3),
                        onClick = { onSelectField(AmountField.AMOUNT) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldBox(
    label: String,
    value: String,
    active: Boolean,
    corners: Corners,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = "Rp",
    suffix: String? = null,
    emphasized: Boolean = false,
    savedAmount: Long = 0,
    trailing: (@Composable () -> Unit)? = null,
) {
    val valueStyle = if (emphasized) {
        MaterialTheme.typography.displaySmallEmphasized
    } else {
        MaterialTheme.typography.headlineSmallEmphasized
    }
    val valueColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    MorphSurface(
        onClick = onClick,
        corners = corners,
        pressedCorners = Corners(28.dp),
        color = if (active) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f)
        },
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = if (emphasized) 14.dp else 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (prefix != null) {
                    Text(
                        prefix,
                        style = valueStyle.copy(fontSize = valueStyle.fontSize * 0.6f),
                        color = valueColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                RollingValue(value, valueStyle, valueColor)
                if (suffix != null) {
                    Text(suffix, style = valueStyle, color = valueColor)
                }
                if (active) Caret(valueStyle, restartKey = value)
                Box(Modifier.weight(1f))
                if (savedAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SpendrTheme.colors.success.copy(alpha = 0.16f),
                        contentColor = SpendrTheme.colors.success,
                    ) {
                        Text(
                            "Save " + formatAmount(savedAmount),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (trailing != null) trailing()
            }
        }
    }
}

/** Digits slide up as the number grows and down as it shrinks. */
@Composable
private fun RollingValue(value: String, style: TextStyle, color: androidx.compose.ui.graphics.Color) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val grew = targetState.length >= initialState.length
            (slideInVertically(tween(180)) { if (grew) it / 2 else -it / 2 } + fadeIn(tween(180)))
                .togetherWith(slideOutVertically(tween(140)) { if (grew) -it / 2 else it / 2 } + fadeOut(tween(140)))
        },
        label = "amount",
    ) { shown ->
        Text(shown, style = style, color = color, maxLines = 1)
    }
}

/**
 * Insertion caret after the active figure. Holds solid while typing (each
 * [restartKey] change restarts it visible), then blinks with a soft fade on a
 * slow 1.2 s cycle.
 */
@Composable
private fun Caret(style: TextStyle, restartKey: Any) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(restartKey) {
        alpha.snapTo(1f)
        delay(600)
        while (true) {
            alpha.animateTo(0f, tween(150))
            delay(450)
            alpha.animateTo(1f, tween(150))
            delay(450)
        }
    }
    Box(
        Modifier
            .padding(start = 3.dp)
            .width(2.5.dp)
            .height(with(androidx.compose.ui.platform.LocalDensity.current) { style.fontSize.toDp() })
            .graphicsLayer { this.alpha = alpha.value }
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
    )
}

/** Rp / % connected toggle pair. */
@Composable
private fun ModeToggle(
    percentage: Boolean,
    onFixed: () -> Unit,
    onPercentage: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        ToggleButton(
            checked = !percentage,
            onCheckedChange = { onFixed() },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            buttonSize = ToggleButtonSize.ExtraSmall,
        ) { Text("Rp", style = MaterialTheme.typography.labelLargeEmphasized) }
        ToggleButton(
            checked = percentage,
            onCheckedChange = { onPercentage() },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            buttonSize = ToggleButtonSize.ExtraSmall,
        ) { Text("%", style = MaterialTheme.typography.labelLargeEmphasized) }
    }
}

private fun displayAmount(digits: String): String {
    val amount = parseDigits(digits)
    return if (amount > 0) formatAmount(amount) else if (digits.isNotEmpty()) "0" else ""
}
