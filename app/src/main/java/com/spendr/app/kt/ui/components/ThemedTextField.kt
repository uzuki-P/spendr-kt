package com.spendr.app.kt.ui.components

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
 * RN `ThemedTextInput`: labelMedium 600 label above, filled surfaceVariant
 * body with a 1dp border (primary while focused), radius.md, bodyLarge text.
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                if (focused) MaterialTheme.colorScheme.primary else SpendrTheme.colors.border,
            ),
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
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
