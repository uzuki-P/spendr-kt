package com.spendr.app.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.localDate
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.YearMonth

/** Month identity token for a cursor: noon of the 1st, ascending index helper. */
fun monthWindow(nowCursor: Long, monthsBack: Int = 12): List<Long> =
    (monthsBack downTo 0).map { offset ->
        YearMonth.from(localDate(nowCursor)).minusMonths(offset.toLong())
            .atDay(1).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
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

/**
 * Month pager with scrollable tabs + sliding 3 dp primary indicator, ported
 * from RN `MonthPager` (tabs "This Month"/"Last Month"/"MMMM yyyy", no future
 * months). Static window; the picker covers longer jumps.
 */
@Composable
fun MonthPager(
    selectedCursor: Long,
    months: List<Long>,
    onMonthChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Long) -> Unit,
) {
    val nowCursor = months.lastOrNull() ?: selectedCursor
    val initialIndex = months.indexOf(selectedCursor).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { months.size }

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

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val index = position.toInt().coerceIn(0, tabPositions.size - 1)
                    val next = (index + 1).coerceAtMost(tabPositions.size - 1)
                    val fraction = (position - index).coerceIn(0f, 1f)
                    val left = tabPositions[index].left +
                        (tabPositions[next].left - tabPositions[index].left) * fraction
                    val width = tabPositions[index].width +
                        (tabPositions[next].width - tabPositions[index].width) * fraction
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = left)
                            .width(width)
                            .padding(horizontal = 12.dp)
                            .height(3.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                    )
                }
            },
            divider = {},
        ) {
            months.forEachIndexed { index, cursor ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { onMonthChange(cursor) },
                    modifier = Modifier.width(132.dp).height(48.dp),
                ) {
                    Text(
                        monthTabLabel(cursor, nowCursor).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        HorizontalPager(state = pagerState) { page ->
            months.getOrNull(page)?.let { cursor ->
                Box(Modifier.fillMaxWidth()) { pageContent(cursor) }
            }
        }
    }
}
