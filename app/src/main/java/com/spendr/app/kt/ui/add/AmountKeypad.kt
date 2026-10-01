package com.spendr.app.kt.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MorphSurface

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("000", "0"),
)

private val KEY_HEIGHT = 56.dp
private val KEY_REST = Corners(28.dp)
private val KEY_PRESSED = Corners(12.dp)

/**
 * Digits-only keypad on a rounded sheet: pill keys that square off while
 * pressed (M3 Expressive press morph), tonal action row [Clear | Close |
 * Backspace], and a primary "Next" in the grid's bottom-right. Haptic per
 * press.
 *
 * With `onNext = null` (sheets): the backspace stays in the grid's
 * bottom-right and the action row reads [Clear | Close].
 */
@Composable
fun AmountKeypad(
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onNext: (() -> Unit)? = null,
) {
    val vibrate = LocalVibrate.current
    fun press(action: () -> Unit) {
        vibrate()
        action()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLargeIncreased.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KeypadKey(
                    glyph = "eraser",
                    label = "Clear",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                    onClick = { press(onClear) },
                )
                KeypadKey(
                    glyph = "chevron-down",
                    label = "Close",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                    onClick = { press(onClose) },
                )
                if (onNext != null) {
                    KeypadKey(
                        glyph = "backspace-outline",
                        label = null,
                        description = "Backspace",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { press(onBackspace) },
                    )
                } else {
                    Box(Modifier.weight(1f))
                }
            }
            for ((rowIndex, row) in KEYPAD_ROWS.withIndex()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (key in row) {
                        DigitKey(
                            label = key,
                            modifier = Modifier.weight(1f),
                            onClick = { press { onDigit(key) } },
                        )
                    }
                    if (rowIndex == KEYPAD_ROWS.size - 1) {
                        if (onNext != null) {
                            KeypadKey(
                                glyph = "arrow-right",
                                label = "Next",
                                container = MaterialTheme.colorScheme.primary,
                                content = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = { press(onNext) },
                            )
                        } else {
                            KeypadKey(
                                glyph = "backspace-outline",
                                label = null,
                                description = "Backspace",
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f),
                                onClick = { press(onBackspace) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MorphSurface(
        onClick = onClick,
        corners = KEY_REST,
        pressedCorners = KEY_PRESSED,
        pressedScale = 0.94f,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.height(KEY_HEIGHT),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun KeypadKey(
    glyph: String,
    label: String?,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    height: Dp = KEY_HEIGHT,
) {
    MorphSurface(
        onClick = onClick,
        corners = KEY_REST,
        pressedCorners = KEY_PRESSED,
        pressedScale = 0.94f,
        color = container,
        contentColor = content,
        modifier = modifier
            .height(height)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            MciIcon(glyph, 20.dp, content, Modifier.clearAndSetSemantics { })
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = content,
                )
            }
        }
    }
}
