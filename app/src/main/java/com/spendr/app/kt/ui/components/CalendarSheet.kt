package com.spendr.app.kt.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.ui.theme.SpendrTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val WeekdayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun LocalDate.isWeekend(): Boolean =
    dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY

/**
 * In-app calendar sheet, ported from RN `Calendar`: Mon-first grid, weekend
 * danger color, today ring, selected fill, month chevrons, Today pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSheet(
    selectedDate: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    var displayed by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var derivedDirection by remember { mutableStateOf(0) }
    val danger = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary
    val advance: (Int) -> Unit = { months: Int ->
        derivedDirection = months
        displayed = displayed.plusMonths(months.toLong())
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            // Header: chevrons, month title, Today pill
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BouncySurface(
                    onClick = { advance(-1) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MciIcon("chevron-left", 20.dp, MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text(
                    formatMonthYear(
                        displayed.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                BouncySurface(
                    onClick = {
                        displayed = YearMonth.from(today)
                        onPick(today)
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                BouncySurface(
                    onClick = { advance(1) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MciIcon("chevron-right", 20.dp, MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Weekday headers (Mon-first), Sat + Sun in danger — centered over
            // the date columns like the date cells themselves
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                WeekdayHeaders.forEach { label ->
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (label == "Sat" || label == "Sun") {
                            danger
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Day grid; month changes slide 28dp from the travel direction (RN
            // 220ms). Horizontal swipe also advances/rewinds a month.
            val direction = remember(displayed) { derivedDirection }
            Box(
                Modifier.pointerInput(Unit) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            when {
                                dragTotal < -80f -> advance(1)
                                dragTotal > 80f -> advance(-1)
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragTotal += dragAmount
                    }
                },
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = displayed,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (androidx.compose.animation.slideInHorizontally(tween(220)) { if (forward) 96 else -96 } +
                            androidx.compose.animation.fadeIn(tween(220))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(tween(220)) { if (forward) -96 else 96 } +
                            androidx.compose.animation.fadeOut(tween(220)))
                    },
                    label = "monthSlide",
                ) { month ->
                    MonthGrid(
                        month = month,
                        selectedDate = selectedDate,
                        onPick = onPick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val danger = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(tween(220))
            .padding(top = 4.dp),
    ) {
            val firstDayOffset = (month.atDay(1).dayOfWeek.value + 6) % 7
            val daysInMonth = month.lengthOfMonth()
            val cells = firstDayOffset + daysInMonth
            val rows = (cells + 6) / 7
            Column(Modifier.fillMaxWidth()) {
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val dayIndex = row * 7 + col - firstDayOffset + 1
                            val valid = dayIndex in 1..daysInMonth
                            val date = if (valid) month.atDay(dayIndex) else null
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .then(
                                        if (date != null) {
                                            Modifier.pressScale { onPick(date) }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (date != null) {
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val weekend = date.isWeekend()
                                    val contentColor = when {
                                        isSelected && weekend -> MaterialTheme.colorScheme.onError
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        weekend -> danger
                                        !valid -> SpendrTheme.colors.textTertiary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    val background = when {
                                        isSelected && weekend -> danger
                                        isSelected -> primary
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                    val border = when {
                                        isToday && weekend -> 2.dp
                                        isToday -> 2.dp
                                        else -> 0.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(background, CircleShape)
                                            .border(
                                                border,
                                                if (isToday) {
                                                    if (weekend) danger else primary
                                                } else {
                                                    androidx.compose.ui.graphics.Color.Transparent
                                                },
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = when {
                                                isSelected || isToday -> FontWeight.Bold
                                                else -> FontWeight.Normal
                                            },
                                            color = contentColor,
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

/** Convenience: epoch ms ↔ LocalDate in the system zone. */
fun LocalDate.toEpochMs(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun epochMsToLocalDate(ms: Long): LocalDate =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
