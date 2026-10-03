package com.spendr.app.kt.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.formatRupiahCompact
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.ui.components.AnimatedAmount
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.MonthPillButton
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.categoryColor
import com.spendr.app.kt.ui.components.rememberCollapsingBar
import com.spendr.app.kt.ui.components.segmentCorners
import kotlinx.coroutines.delay
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MonthPager
import com.spendr.app.kt.ui.components.MonthYearPickerSheet
import com.spendr.app.kt.ui.components.SectionHeader
import com.spendr.app.kt.ui.theme.SpendrTheme
import com.spendr.app.kt.ui.components.monthWindow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Reports: tonal hero (counting total, stat chips), daily trend bars that grow
 * in on a spring, and the category breakdown as a segmented list whose share
 * bars fill on entry.
 */
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onSeeAll: (Long) -> Unit,
    onOpenCategory: (categoryId: Long, month: Long) -> Unit,
    onBack: () -> Unit = {},
) {
    val report by viewModel.report.collectAsState()
    val cursor by viewModel.cursor.collectAsState()
    val monthlyTotals by viewModel.monthlyTotals.collectAsState()
    // Swiping reaches back to the oldest month with transactions.
    val oldestCursor = monthlyTotals.keys.minOrNull()
        ?.let { it.atDay(1).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val months = remember(cursor, oldestCursor) {
        monthWindow(System.currentTimeMillis(), include = minOf(cursor, oldestCursor ?: cursor))
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    val scrollBehavior = rememberCollapsingBar()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SpendrTopBar(
                title = "Reports",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = { MonthPillButton(cursor) { showMonthPicker = true } },
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
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLargeIncreased,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Total spending",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                    modifier = Modifier.weight(1f),
                                )
                                BouncyButton(
                                    onClick = { onSeeAll(cursor) },
                                    height = ButtonDefaults.ExtraSmallContainerHeight,
                                ) {
                                    MciIcon("format-list-bulleted", 16.dp, MaterialTheme.colorScheme.onPrimary)
                                    Text("See all", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                            AnimatedAmount(
                                amount = report.total,
                                style = MaterialTheme.typography.displaySmallEmphasized,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HeroStat("Transactions", report.count.toString(), Modifier.weight(1f))
                                HeroStat("Avg / day", formatRupiahCompact(viewModel.averagePerDay()), Modifier.weight(1f))
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
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (report.daily.values.any { it > 0 }) {
                            DailyTrendChart(report = report)
                        } else {
                            EmptyState(
                                glyph = "chart-bar",
                                title = "No data yet",
                                message = "No spending this month yet.",
                            )
                        }
                    }

                    SectionHeader("Category breakdown")
                    if (report.categories.isEmpty()) {
                        EmptyState(glyph = "shape-outline", title = "No categories yet")
                    } else {
                        SegmentedGroup {
                            report.categories.forEachIndexed { index, category ->
                                CategoryShareRow(
                                    category = category,
                                    index = index,
                                    corners = segmentCorners(index, report.categories.size),
                                    onClick = { onOpenCategory(category.categoryId, cursor) },
                                )
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
            monthTotals = monthlyTotals,
            onPick = {
                viewModel.setCursor(it)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

/** Category row: badge, name, total, and a share bar that fills in, staggered by [index]. */
@Composable
private fun CategoryShareRow(
    category: ReportsViewModel.CategoryTotalUi,
    index: Int,
    corners: Corners,
    onClick: () -> Unit,
) {
    val fill = remember(category.categoryId, category.percent) { Animatable(0f) }
    LaunchedEffect(category.categoryId, category.percent) {
        delay(60L * index.coerceAtMost(8))
        fill.animateTo(
            category.percent.coerceIn(0f, 100f) / 100f,
            spring(dampingRatio = 0.7f, stiffness = 180f),
        )
    }
    val barColor = categoryColor(category.color)
    MorphSurface(
        onClick = onClick,
        corners = corners,
        pressedCorners = Corners(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIconBadge(icon = category.icon, color = category.color, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        String.format("%.1f%% • %dx", category.percent, category.count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatRupiah(category.total),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = SpendrTheme.colors.expense,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(barColor.copy(alpha = 0.14f), RoundedCornerShape(50)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill.value.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(barColor, RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                MaterialTheme.shapes.large,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            maxLines = 1,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DailyTrendChart(report: ReportsViewModel.MonthReport, modifier: Modifier = Modifier) {
    // RN DailyTrendChart constants: plot 132dp tall, 64dp y-axis column
    val axisWidthDp = 64.dp
    val plotHeightDp = 132.dp
    // Scrubbing also starts this far outside the plot. The layout gives the
    // margin back so the axis labels keep their distance from the bars.
    val touchMargin = 8.dp
    val density = LocalDensity.current
    val vibrate = LocalVibrate.current

    val primaryBarColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dimBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val tertiaryLabel = SpendrTheme.colors.textTertiary

    val reportDate = localDate(report.cursor)
    val daysInMonth = reportDate.lengthOfMonth()
    val daily = (1..daysInMonth).map { day -> report.daily[day] ?: 0L }
    // Bars rise from the baseline with a slight overshoot when the month shows
    val grow = remember(report) { Animatable(0f) }
    LaunchedEffect(report) { grow.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 160f)) }
    val latestSpendingIndex = daily.indexOfLast { it > 0 }.coerceAtLeast(0)
    var selected by remember(report) { mutableIntStateOf(latestSpendingIndex) }

    val n = daily.size

    val rawMax = max(1.0, (daily.maxOrNull() ?: 0L).toDouble())
    val magnitude = 10.0.pow(max(0.0, floor(log10(rawMax)) - 1.0))
    val yMax = ceil(rawMax / magnitude) * magnitude

    val chartGesture = Modifier.pointerInput(report) {
        val plotStart = with(density) { touchMargin.toPx() }
        val plotEndPadding = plotStart
        fun selectAt(x: Float) {
            val plotWidth = (size.width - plotStart - plotEndPadding).coerceAtLeast(1f)
            val idx = floor((x - plotStart).coerceIn(0f, plotWidth) / plotWidth * n)
                .toInt().coerceIn(0, n - 1)
            if (idx != selected) {
                selected = idx
                vibrate()
            }
        }
        // Claim scrubbing from the initial press through release.
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            selectAt(down.position.x)
            do {
                val event = awaitPointerEvent()
                event.changes.firstOrNull { it.id == down.id }?.let {
                    selectAt(it.position.x)
                }
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }

    Column(modifier) {
        // Selection header: stacked date over amount on surfaceContainerHigh
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
        ) {
            Text(
                formatDayShort(
                    reportDate.withDayOfMonth((selected + 1).coerceIn(1, daysInMonth))
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = daily.getOrNull(selected) ?: 0L,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(tween(220)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(220)))
                        .togetherWith(slideOutVertically(tween(180)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(180)))
                },
                label = "selectedDay",
            ) { value ->
                Text(
                    formatRupiah(value),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = SpendrTheme.colors.expense,
                )
            }
        }

        Column(
            Modifier.padding(
                start = 16.dp,
                end = 16.dp - touchMargin,
                top = 16.dp - touchMargin,
                bottom = 16.dp,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Y axis: three formatRupiahCompact labels at 0 / mid / max
                Box(Modifier.width(axisWidthDp - touchMargin).height(plotHeightDp)) {
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
                Box(
                    Modifier.weight(1f).height(plotHeightDp + touchMargin * 2)
                        .then(chartGesture)
                        .padding(touchMargin),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val plotWidth = size.width
                        val plotHeight = size.height

                        // gridlines at 0 / 50 / 100%
                        for (fraction in listOf(0f, 0.5f, 1f)) {
                            val y = plotHeight * fraction
                            drawLine(gridColor, Offset(0f, y), Offset(plotWidth, y), 1f)
                        }

                        val slotWidth = plotWidth / n.coerceAtLeast(1)
                        val barWidth = with(density) {
                            (slotWidth.toDp() - 2.dp).coerceIn(2.dp, 8.dp).toPx()
                        }
                        val barRadius = barWidth / 2f

                        daily.forEachIndexed { index, total ->
                            val x = (index + 0.5f) * slotWidth
                            if (total > 0) {
                                // RN barHeightFor: min 3dp for nonzero days
                                val barHeight = with(density) {
                                    (plotHeightDp * ((total / yMax).toFloat())).coerceAtLeast(3.dp).toPx()
                                } * grow.value
                                drawRoundRect(
                                    color = if (index == selected) primaryBarColor else dimBarColor,
                                    topLeft = Offset(x - barWidth / 2, plotHeight - barHeight),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                        minOf(barRadius, (barHeight / 2f).coerceAtLeast(0f)),
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
            }

            // X labels: 1, middle (ceil), last
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = axisWidthDp, end = touchMargin),
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
