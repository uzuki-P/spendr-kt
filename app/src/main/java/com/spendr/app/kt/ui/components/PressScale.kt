package com.spendr.app.kt.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * RN `PressableSurface` press feel: spring scale to 0.98
 * (damping 22 / stiffness 420 / mass 0.7 → dampingRatio ≈ 0.64).
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.98f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    pressedScaleModifier(interactionSource, pressedScale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 * Scale-only variant for components that own their click
 * (`Surface(onClick)`, M3 buttons, `clickable` rows): pass the same
 * [MutableInteractionSource] to both this and the click handler.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.98f,
): Modifier = composed { pressedScaleModifier(interactionSource, pressedScale) }

private fun Modifier.pressedScaleModifier(
    interactionSource: InteractionSource,
    pressedScale: Float,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.64f, stiffness = 420f),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Keypad press: 0.98 spring scale plus the RN opacity 0.55 fallback. */
@Composable
fun Modifier.keypadPress(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.64f, stiffness = 420f),
        label = "keypadScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.55f else 1f,
        animationSpec = tween(90),
        label = "keypadAlpha",
    )
    return composed {
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    }
}
