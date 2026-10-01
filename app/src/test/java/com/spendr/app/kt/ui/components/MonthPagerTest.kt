package com.spendr.app.kt.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
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
class MonthPagerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun cursor(year: Int, month: Int) =
        LocalDate.of(year, month, 1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun extendingWindowBackwardsKeepsSelectionAndPagesIntoOlderMonths() {
        val short = listOf(cursor(2026, 2), cursor(2026, 3))
        val extended = listOf(cursor(2025, 11), cursor(2025, 12), cursor(2026, 1)) + short
        var months by mutableStateOf(short)
        var selected = cursor(2026, 3)
        compose.setContent {
            var current by remember { mutableLongStateOf(selected) }
            MaterialTheme {
                MonthPager(
                    selectedCursor = current,
                    months = months,
                    onMonthChange = {
                        current = it
                        selected = it
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { month -> Box(Modifier.fillMaxSize().testTag("page-$month")) }
            }
        }

        // Older months arrive after the first frame, shifting every page index.
        months = extended
        compose.waitForIdle()
        assertEquals("The selected month must survive the window growing", cursor(2026, 3), selected)

        repeat(4) {
            compose.onNodeWithTag("page-$selected").performTouchInput { swipeRight() }
            compose.waitForIdle()
        }
        assertEquals("Swiping must reach the oldest month", cursor(2025, 11), selected)
    }
}
