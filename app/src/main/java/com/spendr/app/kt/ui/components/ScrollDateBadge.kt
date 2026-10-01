package com.spendr.app.kt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
 * Floating scroll-position badge on the Transactions list: primaryContainer
 * pill with the day label, anchored at left 8dp / top 42% of the page. It
 * springs in while scrolling and shrinks away after 1s idle.
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
            enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f) +
                fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.8f) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(50))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(50),
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
