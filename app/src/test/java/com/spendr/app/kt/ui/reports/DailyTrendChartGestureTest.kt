package com.spendr.app.kt.ui.reports

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.components.MonthPager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DailyTrendChartGestureTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun chartScrubbingDoesNotMoveMonthPager() = assertScrubbingStaysOnMonth(0.65f)

    @Test
    fun scrubbingFromGraphPaddingDoesNotMoveMonthPager() = assertScrubbingStaysOnMonth(0.26f)

    @Test
    fun swipingDayLabelsStillMovesMonthPager() = assertScrubbingStaysOnMonth(0.65f, 0.95f, expectPaging = true)

    @Test
    fun diagonalScrubbingDoesNotMoveMonthPager() = assertScrubbingStaysOnMonth(0.65f, diagonal = true)

    @Test
    fun olderFebruaryReportDoesNotCrashWithStaleDayCount() {
        val february = LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        compose.setContent {
            MaterialTheme {
                DailyTrendChart(ReportsViewModel.MonthReport(
                    cursor = february,
                    daysInMonth = 31,
                    daily = mapOf(31 to 1_000L),
                ))
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun swipingCardHeaderStillMovesMonthPager() = assertScrubbingStaysOnMonth(0.65f, 0.1f, expectPaging = true)

    @Test
    fun swipingAxisTextStillMovesMonthPager() = assertScrubbingStaysOnMonth(0.2f, expectPaging = true)

    private fun assertScrubbingStaysOnMonth(
        startFraction: Float,
        heightFraction: Float = 0.65f,
        diagonal: Boolean = false,
        expectPaging: Boolean = false,
    ) {
        val months = (1..3).map { month ->
            LocalDate.of(2026, month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        var selectedMonth = months[1]
        compose.setContent {
            var cursor by remember { mutableLongStateOf(months[1]) }
            MaterialTheme {
                MonthPager(
                    selectedCursor = cursor,
                    months = months,
                    onMonthChange = {
                        cursor = it
                        selectedMonth = it
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { month ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        DailyTrendChart(
                            report = ReportsViewModel.MonthReport(
                                cursor = month,
                                daily = (1..28).associateWith { it * 1_000L },
                                daysInMonth = 28,
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("chart-$month"),
                        )
                        Box(Modifier.fillMaxWidth().height(80.dp).testTag("pager-area-$month"))
                        Spacer(Modifier.height(800.dp))
                    }
                }
            }
        }

        val chart = compose.onNodeWithTag("chart-${months[1]}")
        val originalLeft = chart.fetchSemanticsNode().boundsInRoot.left
        if (expectPaging) {
            chart.performTouchInput {
                val start = Offset(width * startFraction, height * heightFraction)
                swipe(start, Offset(if (startFraction < 0.3f) width.toFloat() else 0f, start.y), durationMillis = 300)
            }
            compose.waitForIdle()
            assertEquals("The card outside the plot must allow paging", months[if (startFraction < 0.3f) 0 else 2], selectedMonth)
            return
        }
        // Include a diagonal start, reversal, and movement outside the plot.
        chart.performTouchInput {
            val start = Offset(width * startFraction, height * heightFraction)
            down(start)
            moveTo(start + if (diagonal) Offset(1f, 40f) else Offset(40f, 1f), delayMillis = 80)
            moveTo(Offset(width * 0.95f, start.y), delayMillis = 80)
        }
        compose.waitForIdle()
        assertEquals("Scrubbing must not shift the page", originalLeft,
            chart.fetchSemanticsNode().boundsInRoot.left, 0.5f)
        chart.performTouchInput {
            moveTo(Offset(width * 0.3f, height * 0.65f), delayMillis = 80)
            moveTo(Offset(0f, height * 0.65f), delayMillis = 80)
        }
        compose.waitForIdle()
        assertEquals("Reversing out of the plot must not shift the page", originalLeft,
            chart.fetchSemanticsNode().boundsInRoot.left, 0.5f)
        chart.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals("Scrubbing must not change the month", months[1], selectedMonth)
        compose.onNodeWithText(formatRupiah(1_000L)).assertExists()

        // The pager must still respond after the chart releases the gesture.
        compose.onNodeWithTag("pager-area-${months[1]}").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals("Swiping outside the chart must still change the month", months[2], selectedMonth)
    }
}
