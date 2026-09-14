package com.spendr.app.kt.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.formatRupiahCompact
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MonthPager
import com.spendr.app.kt.ui.components.SectionHeader
import com.spendr.app.kt.ui.components.monthTabLabel
import com.spendr.app.kt.ui.components.monthWindow
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** Reports, ported from RN `ReportsScreen`: hero card, daily trend bars, category share bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onSeeAll: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
) {
    val report by viewModel.report.collectAsState()
    val cursor by viewModel.cursor.collectAsState()
    val months = remember { monthWindow(System.currentTimeMillis()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold) },
                actions = {
                    Surface(
                        onClick = { showMonthPicker = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                formatMonthYear(cursor),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    IconButton(onClick = { onSeeAll(cursor) }) {
                        Icon(Icons.Outlined.FormatListBulleted, contentDescription = "See all transactions")
                    }
                },
            )

            MonthPager(
                selectedCursor = cursor,
                months = months,
                onMonthChange = viewModel::setCursor,
                modifier = Modifier.fillMaxSize(),
            ) { _ ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Hero card
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Total spending",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    formatRupiah(report.total),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { onSeeAll(cursor) }) {
                                    Text("See all", fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f),
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                HeroStat("Transactions", report.count.toString())
                                HeroStat("Avg / day", formatRupiahCompact(viewModel.averagePerDay()))
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            formatRupiahCompact(report.savings),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            " · ${report.savingsCount}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                        )
                                    }
                                    Text(
                                        "Savings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                    )
                                }
                            }
                        }
                    }

                    SectionHeader("Daily trend")
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DailyTrendChart(report = report)
                    }

                    SectionHeader("Category breakdown")
                    if (report.categories.isEmpty()) {
                        Text(
                            "No categories yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.categories.forEach { category ->
                                Surface(
                                    onClick = { onOpenCategory(category.categoryId) },
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CategoryIconBadge(
                                            icon = category.icon,
                                            color = category.color,
                                            size = 34.dp,
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    category.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                Text(
                                                    formatRupiah(category.total),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                                        androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                                                    ),
                                            ) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth(category.percent.coerceIn(0f, 100f) / 100f)
                                                        .height(6.dp)
                                                        .background(
                                                            com.spendr.app.kt.ui.components.categoryColor(category.color),
                                                            androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
                                                        ),
                                                )
                                            }
                                            Text(
                                                String.format("%.1f%% • %dx", category.percent, category.count),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    if (showMonthPicker) {
        MonthYearPickerSheet(
            selectedCursor = cursor,
            onPick = {
                viewModel.setCursor(it)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun DailyTrendChart(report: ReportsViewModel.MonthReport) {
    val axisWidthDp = 48.dp
    val plotHeightDp = 132.dp
    val density = LocalDensity.current
    val labelPx = with(density) { 10.sp.toPx() }
    val haptics = LocalHapticFeedback.current
    val vibrate = LocalVibrate.current

    val primaryBarColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dimBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
    val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

    val daily = (1..report.daysInMonth).map { day -> report.daily[day] ?: 0L }
    val latestSpendingIndex = daily.indexOfLast { it > 0 }.coerceAtLeast(0)
    var selected by remember(report) { mutableIntStateOf(latestSpendingIndex) }

    val hasData = daily.any { it > 0 }
    val n = daily.size

    val rawMax = max(1.0, (daily.maxOrNull() ?: 0L).toDouble())
    val magnitude = 10.0.pow(max(0.0, floor(log10(rawMax)) - 1.0))
    val yMax = ceil(rawMax / magnitude) * magnitude

    Column(Modifier.padding(12.dp)) {
        // Selection header
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatDayShort(
                    localDate(report.cursor).withDayOfMonth((selected + 1).coerceAtMost(report.daysInMonth))
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatRupiah(daily.getOrNull(selected) ?: 0L),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeightDp)
                .padding(top = 8.dp)
                .pointerInput(daily) {
                    fun indexAt(x: Float): Int {
                        val plotWidth = (size.width - axisWidthDp.toPx()).coerceAtLeast(1f)
                        val clamped = (x - axisWidthDp.toPx()).coerceIn(0f, plotWidth)
                        return floor(clamped / plotWidth * n).toInt().coerceIn(0, n - 1)
                    }
                    detectTapGestures { offset ->
                        selected = indexAt(offset.x)
                        vibrate()
                    }
                }
                .pointerInput(daily) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val plotWidth = (size.width - axisWidthDp.toPx()).coerceAtLeast(1f)
                        val clamped = (change.position.x - axisWidthDp.toPx()).coerceIn(0f, plotWidth)
                        val idx = floor(clamped / plotWidth * n).toInt().coerceIn(0, n - 1)
                        if (idx != selected) {
                            selected = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                },
        ) {
            val axisWidth = axisWidthDp.toPx()
            val plotWidth = size.width - axisWidth
            val plotHeight = size.height

            // gridlines at 0 / 50 / 100%
            for (fraction in listOf(0f, 0.5f, 1f)) {
                val y = plotHeight * fraction
                drawLine(gridColor, Offset(axisWidth, y), Offset(size.width, y), 1f)
            }

            fun yFor(value: Long) = (plotHeight * (1f - (value / yMax).toFloat())).toFloat()
            val slotWidth = plotWidth / n.coerceAtLeast(1)
            val barWidth = (slotWidth - 2f).coerceIn(2f, 8f)

            daily.forEachIndexed { index, total ->
                val x = axisWidth + (index + 0.5f) * slotWidth
                if (total > 0) {
                    val barHeight = max(3f, (total / yMax).toFloat() * plotHeight)
                    drawRoundRect(
                        color = if (index == selected) primaryBarColor else dimBarColor,
                        topLeft = Offset(x - barWidth / 2, plotHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(minOf(3f, barHeight / 2)),
                    )
                }
                if (index == selected) {
                    drawLine(cursorColor, Offset(x, 0f), Offset(x, plotHeight), 1.5f)
                }
            }
        }

        // X labels: 1, middle, last
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = axisWidthDp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                (n / 2).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                n.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthYearPickerSheet(
    selectedCursor: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val nowYear = YearMonth.from(localDate(System.currentTimeMillis())).year
    val selectedMonth = YearMonth.from(localDate(selectedCursor))
    var browsedYear by remember { mutableIntStateOf(selectedMonth.year) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { browsedYear-- },
                    enabled = browsedYear > 1900,
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    browsedYear.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { browsedYear++ },
                    enabled = browsedYear < nowYear,
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
                Button(
                    onClick = { onPick(monthCursor(System.currentTimeMillis())) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("THIS MONTH", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            val currentMonth = YearMonth.from(localDate(System.currentTimeMillis()))
            androidx.compose.animation.AnimatedContent(
                targetState = browsedYear,
                transitionSpec = {
                    val forward = targetState > initialState
                    (androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(220)) { if (forward) -56 else 56 } +
                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))) togetherWith
                        (androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { if (forward) 56 else -56 } +
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)))
                },
                label = "yearSlide",
            ) { browsedYear ->
            (0 until 12).chunked(3).forEach { monthsRow ->
                Row(Modifier.fillMaxWidth()) {
                    for (monthIndex in monthsRow) {
                        val isSelected = monthIndex + 1 == selectedMonth.monthValue && browsedYear == selectedMonth.year
                        val isCurrent = browsedYear == nowYear && monthIndex + 1 == currentMonth.monthValue
                        val isFuture = browsedYear > nowYear ||
                            (browsedYear == nowYear && monthIndex + 1 > currentMonth.monthValue)
                        Box(Modifier.weight(1f).padding(4.dp)) {
                            Surface(
                                onClick = { onPick(monthCursor(java.time.LocalDateTime.of(browsedYear, monthIndex + 1, 1, 12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())) },
                                enabled = !isFuture,
                                shape = MaterialTheme.shapes.medium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                monthShort(monthIndex),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(start = 4.dp),
                                            )
                                        }
                                    } else {
                                        Text(
                                            monthShort(monthIndex),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isFuture) {
                                                com.spendr.app.kt.ui.theme.SpendrTheme.colors.textTertiary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
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

private fun monthShort(index: Int): String =
    java.time.Month.of(index + 1).getDisplayName(
        java.time.format.TextStyle.SHORT,
        java.util.Locale.ENGLISH,
    )
