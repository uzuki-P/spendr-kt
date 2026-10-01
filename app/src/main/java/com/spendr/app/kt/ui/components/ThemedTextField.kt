package com.spendr.app.kt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * Filled rounded text field: label above, a tonal body with a hairline
 * outline (so it reads on any container, dialogs included) that grows into
 * an animated 2dp primary outline while focused.
 */
@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    if (onFocusChange != null) {
        androidx.compose.runtime.LaunchedEffect(focused) { onFocusChange(focused) }
    }
    Column(modifier) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            )
        }
        val borderWidth by animateDpAsState(
            if (focused) 2.dp else 0.dp,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "fieldBorder",
        )
        val container by animateColorAsState(
            if (focused) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
            MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "fieldColor",
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = container,
            border = if (borderWidth > 0.dp) {
                BorderStroke(borderWidth, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = singleLine,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                interactionSource = source,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
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
        }
    }
}
