package com.spendr.app.kt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * RN `PressableSurface` equivalents: the M3 control plus the shared press
 * spring (scale 0.98, damping 22 / stiffness 420). Every tappable surface in
 * the app goes through these so the bounce feel matches the OG everywhere.
 */
@Composable
fun BouncySurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        shape = shape,
        color = color,
        contentColor = contentColor,
        border = border,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun BouncyTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun BouncyTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        colors = colors,
        interactionSource = source,
        content = content,
    )
}
