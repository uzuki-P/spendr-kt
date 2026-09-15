package com.spendr.app.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Floating scroll-position badge from the RN Transactions list: primaryContainer
 * pill with the day label, anchored at left 8dp / top 42% of the page, appears
 * instantly while scrolling and fades out over 180 ms after 1s idle.
 */
@Composable
fun ScrollDateBadge(
    label: String?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.zIndex(2f)) {
        AnimatedVisibility(
            visible = visible && label != null,
            // RN sets opacity to 1 instantly on scroll
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(180)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                MciIcon(
                    "calendar-blank-outline",
                    16.dp,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    label.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}
