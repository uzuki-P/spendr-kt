package com.spendr.app.kt.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.monthCursor
import java.time.YearMonth

/**
 * RN MonthYearPicker: year header (chevrons + THIS MONTH pill), year-swipe
 * grid of 12 month cells, future months disabled. Shared by Reports and
 * Transactions for out-of-window month jumps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPickerSheet(
    selectedCursor: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val nowYear = YearMonth.from(localDate(System.currentTimeMillis())).year
    val selectedMonth = YearMonth.from(localDate(selectedCursor))
    var browsedYear by remember { mutableIntStateOf(selectedMonth.year) }
    val density = LocalDensity.current

    // RN MonthYearPicker: plain slide-up sheet, surfaceContainerHigh, no handle
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BouncyIconButton(onClick = { browsedYear-- }, enabled = browsedYear > 1900) {
                    MciIcon("chevron-left", 24.dp, MaterialTheme.colorScheme.primary)
                }
                Text(
                    browsedYear.toString(),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                BouncyIconButton(onClick = { browsedYear++ }, enabled = browsedYear < nowYear) {
                    MciIcon("chevron-right", 24.dp, MaterialTheme.colorScheme.primary)
                }
                BouncyButton(
                    onClick = { onPick(monthCursor(System.currentTimeMillis())) },
                    height = androidx.compose.material3.ButtonDefaults.ExtraSmallContainerHeight,
                ) { Text("This month") }
            }
            val currentMonth = YearMonth.from(localDate(System.currentTimeMillis()))
            // RN yearSwipe: fling left → next year, right → previous; next is
            // blocked at the current year (tracker only reports recorded months)
            Box(
                Modifier.pointerInput(Unit) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            when {
                                dragTotal < -80f -> if (browsedYear < nowYear) browsedYear++
                                dragTotal > 80f -> browsedYear--
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragTotal += dragAmount
                    }
                },
            ) {
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = browsedYear,
                    transitionSpec = {
                        // Year slide: the grid travels with an expressive spring
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(spatial) { direction * it / 3 } + fadeIn(effects)) togetherWith
                            (slideOutHorizontally(spatial) { -direction * it / 3 } + fadeOut(effects))
                    },
                    label = "yearSlide",
                ) { year ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        (0 until 12).chunked(3).forEach { monthsRow ->
                            Row(Modifier.fillMaxWidth()) {
                                for (monthIndex in monthsRow) {
                                    val isSelected = year == selectedMonth.year &&
                                        monthIndex + 1 == selectedMonth.monthValue
                                    val isCurrent = year == nowYear &&
                                        monthIndex + 1 == currentMonth.monthValue
                                    val isFuture = year > nowYear ||
                                        (year == nowYear && monthIndex + 1 > currentMonth.monthValue)
                                    Box(Modifier.weight(1f).padding(4.dp)) {
                                        MorphSurface(
                                            onClick = {
                                                onPick(
                                                    monthCursor(
                                                        java.time.LocalDateTime.of(year, monthIndex + 1, 1, 12, 0)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .toInstant().toEpochMilli(),
                                                    ),
                                                )
                                            },
                                            enabled = !isFuture,
                                            // Picked month rounds into a pill; presses square off
                                            corners = if (isSelected) Corners(28.dp) else Corners(16.dp),
                                            pressedCorners = Corners(10.dp),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                        ) {
                                            // RN: selection is the filled pill alone, no checkmark
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    monthShort(monthIndex),
                                                    style = if (isSelected) {
                                                        MaterialTheme.typography.titleSmallEmphasized
                                                    } else {
                                                        MaterialTheme.typography.titleSmall
                                                    },
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isFuture -> com.spendr.app.kt.ui.theme.SpendrTheme.colors.textTertiary
                                                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** App-bar pill label: short English month + year, e.g. "Sep 2026". */
fun monthPillLabel(cursor: Long): String {
    val date = localDate(cursor)
    val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
    return "$month ${date.year}"
}

private fun monthShort(index: Int): String =
    java.time.Month.of(index + 1).getDisplayName(
        java.time.format.TextStyle.SHORT,
        java.util.Locale.ENGLISH,
    )
