package com.spendr.app.kt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.ui.theme.SpendrTheme

/** Tonal month button ("Sep 2026" with calendar and chevron) that opens the month/year picker. */
@Composable
fun MonthPillButton(cursor: Long, onClick: () -> Unit) {
    BouncyTonalButton(
        onClick = onClick,
        height = ButtonDefaults.ExtraSmallContainerHeight,
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(monthPillLabel(cursor), modifier = Modifier.padding(horizontal = 6.dp))
        Icon(Icons.Outlined.ExpandMore, contentDescription = "Pick month", modifier = Modifier.size(18.dp))
    }
}

/**
 * Full-width pill search field: leading magnifier, inline text, clear button
 * while non-empty, and an optional [trailing] slot (e.g. a filter button).
 */
@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSearch: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MciIcon("magnify", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = SpendrTheme.colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (value.isNotEmpty()) {
                BouncyIconButton(onClick = { onValueChange("") }) {
                    MciIcon("close-circle", 20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke(this)
        }
    }
}

/**
 * One row of a segmented action list (bottom-sheet menus): glyph, label,
 * error tint when [destructive].
 */
@Composable
fun ActionRow(
    glyph: String,
    label: String,
    corners: Corners,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    MorphSurface(
        onClick = onClick,
        corners = corners,
        pressedCorners = Corners(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MciIcon(glyph, 24.dp, tint)
            Text(label, style = MaterialTheme.typography.titleMedium, color = tint)
        }
    }
}
