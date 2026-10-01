package com.spendr.app.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.localDate
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.YearMonth

/** Month identity token for a cursor: noon of the 1st, ascending index helper. */
fun monthWindow(nowCursor: Long, monthsBack: Int = 12, include: Long? = null): List<Long> {
    val now = YearMonth.from(localDate(nowCursor))
    // The window always ends at the current month; an out-of-window selected
    // month (e.g. picked from the month/year sheet) extends it backwards.
    val oldest = minOf(
        now.minusMonths(monthsBack.toLong()),
        include?.let { YearMonth.from(localDate(it)) } ?: now,
    )
    val count = java.time.temporal.ChronoUnit.MONTHS.between(oldest, now).toInt()
    return (0..count).map { offset ->
        oldest.plusMonths(offset.toLong())
            .atDay(1).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

fun monthTabLabel(cursor: Long, nowCursor: Long): String {
    val selected = YearMonth.from(localDate(cursor))
    val current = YearMonth.from(localDate(nowCursor))
    return when {
        selected == current -> "This Month"
        selected == current.minusMonths(1) -> "Last Month"
        else -> formatMonthYear(cursor)
    }
}

private val TAB_WIDTH = 132.dp

/**
 * Month pager with scrolling month tabs + a sliding tonal pill indicator,
 * ported from RN `MonthPager` (tabs "This Month"/"Last Month"/"MMMM yyyy", no
 * future months). The selected tab is auto-centered like the RN `scrollTo`
 * behavior; static window — the picker covers longer jumps. [topContent] sits
 * between the tabs and the pager: pinned UI that never swipes with months.
 */
@Composable
fun MonthPager(
    selectedCursor: Long,
    months: List<Long>,
    onMonthChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    topContent: @Composable () -> Unit = {},
    pageContent: @Composable (Long) -> Unit,
) {
    val nowCursor = months.lastOrNull() ?: selectedCursor
    val initialIndex = months.indexOf(selectedCursor).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { months.size }
    val density = LocalDensity.current
    val tabScroll = rememberScrollState()
    var tabRowWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState, months) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                months.getOrNull(page)?.let { if (it != selectedCursor) onMonthChange(it) }
            }
    }
    LaunchedEffect(selectedCursor, months) {
        val target = months.indexOf(selectedCursor)
        if (target >= 0 && pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }
    // RN scrollTabsToIndex: keep the selected tab centered in the bar
    LaunchedEffect(pagerState.currentPage, tabRowWidth, months.size) {
        if (tabRowWidth == 0) return@LaunchedEffect
        val tabWidthPx = with(density) { TAB_WIDTH.toPx() }
        val target = (pagerState.currentPage + 0.5f) * tabWidthPx - tabRowWidth / 2f
        tabScroll.animateScrollTo(target.toInt().coerceIn(0, tabScroll.maxValue))
    }

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                // size must be read OUTSIDE the scroll modifier to get the
                // viewport width (the centering math depends on it)
                .onSizeChanged { tabRowWidth = it.width }
                .horizontalScroll(tabScroll)
                .padding(vertical = 8.dp),
        ) {
            // Indicator: a tonal pill behind the active tab that follows the
            // pager's fractional position, so it glides while swiping. Lives
            // INSIDE the scroll container so it moves with the tabs.
            if (months.isNotEmpty()) {
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val index = position.toInt().coerceIn(0, months.size - 1)
                val fraction = (position - index).coerceIn(0f, 1f)
                val leftDp = TAB_WIDTH * (index + fraction)
                Box(Modifier.matchParentSize()) {
                    Box(
                        Modifier
                            .offset(x = leftDp)
                            .width(TAB_WIDTH)
                            .padding(horizontal = 6.dp)
                            .height(40.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(50),
                            ),
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                months.forEachIndexed { index, cursor ->
                    val selected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .width(TAB_WIDTH)
                            .height(40.dp)
                            .pressScale { onMonthChange(cursor) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            monthTabLabel(cursor, nowCursor),
                            style = if (selected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            },
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        topContent()

        HorizontalPager(state = pagerState) { page ->
            months.getOrNull(page)?.let { cursor ->
                Box(Modifier.fillMaxWidth()) { pageContent(cursor) }
            }
        }
    }
}
