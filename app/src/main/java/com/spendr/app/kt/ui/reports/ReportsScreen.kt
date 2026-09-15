package com.spendr.app.kt.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.formatRupiahCompact
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MonthPager
import com.spendr.app.kt.ui.components.SectionHeader
import com.spendr.app.kt.ui.theme.SpendrTheme
import com.spendr.app.kt.ui.components.monthWindow
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/** Reports, ported from RN `ReportsScreen`: hero card, daily trend bars, category share bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onSeeAll: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onBack: () -> Unit = {},
) {
    val report by viewModel.report.collectAsState()
    val cursor by viewModel.cursor.collectAsState()
    val months = remember { monthWindow(System.currentTimeMillis()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                                monthPillLabel(cursor),
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
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
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
                    // Hero card — primaryContainer block: top row (label/amount + See all), stats
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Total spending",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                    )
                                    Text(
                                        formatRupiah(report.total),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                // RN heroButton: icon + "See all", onPrimaryContainer
                                Surface(
                                    onClick = { onSeeAll(cursor) },
                                    shape = MaterialTheme.shapes.small,
                                    color = Color.Transparent,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    ) {
                                        MciIcon(
                                            "format-list-bulleted",
                                            18.dp,
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            "See all",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 12.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f),
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .height(IntrinsicSize.Max),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HeroStat("Transactions", report.count.toString(), Modifier.weight(1f))
                                VerticalHairline()
                                HeroStat("Avg / day", formatRupiahCompact(viewModel.averagePerDay()), Modifier.weight(1f))
                                VerticalHairline()
                                HeroStat(
                                    "Savings",
                                    formatRupiahCompact(report.savings) + " · ${report.savingsCount}x",
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    SectionHeader("Daily trend")
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (report.daily.values.any { it > 0 }) {
                            DailyTrendChart(report = report)
                        } else {
                            // RN: EmptyState "No data yet" when the month has no spending
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                MciIcon(
                                    "chart-line",
                                    28.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "No data yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "No spending this month yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    SectionHeader("Category breakdown")
                    if (report.categories.isEmpty()) {
                        Text(
                            "No categories yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        // RN ListGroup separation="gap": one clipped rounded group,
                        // rows on surfaceContainerHighest separated by 4dp of background
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp)),
                        ) {
                            report.categories.forEachIndexed { index, category ->
                                if (index > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .background(MaterialTheme.colorScheme.background),
                                    )
                                }
                                Surface(
                                    onClick = { onOpenCategory(category.categoryId) },
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = androidx.compose.ui.graphics.RectangleShape,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            CategoryIconBadge(
                                                icon = category.icon,
                                                color = category.color,
                                                size = 34.dp,
                                            )
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
                                                color = SpendrTheme.colors.expense,
                                                maxLines = 1,
                                            )
                                        }
                                        // 6dp track + category-color fill
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                                    RoundedCornerShape(3.dp),
                                                ),
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth(category.percent.coerceIn(0f, 100f) / 100f)
                                                    .height(6.dp)
                                                    .background(
                                                        com.spendr.app.kt.ui.components.categoryColor(category.color),
                                                        RoundedCornerShape(3.dp),
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
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun VerticalHairline() {
    // RN heroSubDivider: 1dp wide, stretches with the row, 8dp margins
    Box(
        Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f)),
    )
}

@Composable
private fun DailyTrendChart(report: ReportsViewModel.MonthReport) {
    // RN DailyTrendChart constants: plot 132dp tall, 64dp y-axis column
    val axisWidthDp = 64.dp
    val plotHeightDp = 132.dp
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val vibrate = LocalVibrate.current

    val primaryBarColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dimBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
    val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val tertiaryLabel = SpendrTheme.colors.textTertiary

    val daily = (1..report.daysInMonth).map { day -> report.daily[day] ?: 0L }
    val latestSpendingIndex = daily.indexOfLast { it > 0 }.coerceAtLeast(0)
    var selected by remember(report) { mutableIntStateOf(latestSpendingIndex) }

    val n = daily.size

    val rawMax = max(1.0, (daily.maxOrNull() ?: 0L).toDouble())
    val magnitude = 10.0.pow(max(0.0, floor(log10(rawMax)) - 1.0))
    val yMax = ceil(rawMax / magnitude) * magnitude

    Column {
        // Selection header: stacked date over amount on surfaceContainerHigh
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                color = SpendrTheme.colors.expense,
            )
        }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row {
                // Y axis: three formatRupiahCompact labels at 0 / mid / max
                Box(Modifier.width(axisWidthDp).height(plotHeightDp)) {
                    listOf(yMax, yMax / 2, 0.0).forEachIndexed { index, value ->
                        Text(
                            formatRupiahCompact(value.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tertiaryLabel,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = plotHeightDp * (index / 2f) - 7.dp),
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(plotHeightDp)
                        .pointerInput(daily) {
                            fun indexAt(x: Float): Int {
                                val plotWidth = size.width.toFloat()
                                val clamped = x.coerceIn(0f, plotWidth)
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
                                val plotWidth = size.width.toFloat()
                                val clamped = change.position.x.coerceIn(0f, plotWidth)
                                val idx = floor(clamped / plotWidth * n).toInt().coerceIn(0, n - 1)
                                if (idx != selected) {
                                    selected = idx
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                ) {
                    val plotWidth = size.width
                    val plotHeight = size.height

                    // gridlines at 0 / 50 / 100%
                    for (fraction in listOf(0f, 0.5f, 1f)) {
                        val y = plotHeight * fraction
                        drawLine(gridColor, Offset(0f, y), Offset(plotWidth, y), 1f)
                    }

                    val slotWidth = plotWidth / n.coerceAtLeast(1)
                    // RN barWidth: max(2, min(8, slot - 2)) in dp
                    val barWidth = with(density) {
                        (slotWidth.toDp() - 2.dp).coerceIn(2.dp, 8.dp).toPx()
                    }
                    val barRadius = with(density) { 3.dp.toPx() }

                    daily.forEachIndexed { index, total ->
                        val x = (index + 0.5f) * slotWidth
                        if (total > 0) {
                            // RN barHeightFor: min 3dp for nonzero days
                            val barHeight = with(density) {
                                (plotHeightDp * ((total / yMax).toFloat())).coerceAtLeast(3.dp).toPx()
                            }
                            drawRoundRect(
                                color = if (index == selected) primaryBarColor else dimBarColor,
                                topLeft = Offset(x - barWidth / 2, plotHeight - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                    minOf(barRadius, barHeight / 2f),
                                ),
                            )
                        }
                        if (index == selected) {
                            drawLine(
                                cursorColor,
                                Offset(x, 0f),
                                Offset(x, plotHeight),
                                with(density) { 1.5.dp.toPx() },
                            )
                        }
                    }
                }
            }

            // X labels: 1, middle (ceil), last
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = axisWidthDp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(1, ceil(n / 2.0).toInt().coerceAtMost(n), n).forEach { day ->
                    Text(
                        day.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tertiaryLabel,
                    )
                }
            }
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { browsedYear-- }, enabled = browsedYear > 1900) {
                    MciIcon("chevron-left", 24.dp, MaterialTheme.colorScheme.primary)
                }
                Text(
                    browsedYear.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                IconButton(onClick = { browsedYear++ }, enabled = browsedYear < nowYear) {
                    MciIcon("chevron-right", 24.dp, MaterialTheme.colorScheme.primary)
                }
                // RN: THIS MONTH filled with primaryContainer, labelMedium 700
                Surface(
                    onClick = { onPick(monthCursor(System.currentTimeMillis())) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "THIS MONTH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            val currentMonth = YearMonth.from(localDate(System.currentTimeMillis()))
            AnimatedContent(
                targetState = browsedYear,
                transitionSpec = {
                    // RN year slide: grid slides 28dp in the travel direction, 220 ms
                    val direction = if (targetState > initialState) 1 else -1
                    val slide = with(density) { 28.dp.roundToPx() }
                    (slideInHorizontally(tween(220)) { direction * slide } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { -direction * slide } + fadeOut(tween(220)))
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
                                    Surface(
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isFuture -> SpendrTheme.colors.textTertiary
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

private fun monthPillLabel(cursor: Long): String {
    val date = localDate(cursor)
    val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
    return "$month ${date.year}"
}

private fun monthShort(index: Int): String =
    java.time.Month.of(index + 1).getDisplayName(
        java.time.format.TextStyle.SHORT,
        java.util.Locale.ENGLISH,
    )
