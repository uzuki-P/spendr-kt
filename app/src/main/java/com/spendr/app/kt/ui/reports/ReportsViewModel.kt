package com.spendr.app.kt.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.model.DateRange
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.domain.monthRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class ReportsViewModel(
    private val transactions: TransactionRepository,
) : ViewModel() {

    data class MonthReport(
        val cursor: Long,
        val total: Long = 0,
        val count: Long = 0,
        val savings: Long = 0,
        val savingsCount: Long = 0,
        val daily: Map<Int, Long> = emptyMap(),
        val daysInMonth: Int = 30,
        val categories: List<CategoryTotalUi> = emptyList(),
    )

    data class CategoryTotalUi(
        val categoryId: Long,
        val name: String,
        val icon: String,
        val color: String,
        val total: Long,
        val count: Long,
        val percent: Float,
    )

    private val _cursor = MutableStateFlow(monthCursor(System.currentTimeMillis()))
    val cursor: StateFlow<Long> = _cursor.asStateFlow()

    private val _report = MutableStateFlow(MonthReport(_cursor.value))
    val report: StateFlow<MonthReport> = _report.asStateFlow()

    init {
        refresh()
    }

    fun setCursor(cursor: Long) {
        _cursor.value = cursor
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val range: DateRange = monthRange(_cursor.value)
            val total = transactions.totalInRange(range)
            val savings = transactions.savingsInRange(range)
            val daily = transactions.dailyTotals(range)
            val categories = transactions.categoryTotals(range)
            val daysInMonth = localDate(range.end).dayOfMonth

            _report.value = MonthReport(
                cursor = _cursor.value,
                total = total.total,
                count = total.count,
                savings = savings.total,
                savingsCount = savings.count,
                daily = daily.associate { it.day.takeLast(2).toInt() to it.total },
                daysInMonth = daysInMonth,
                categories = categories.map { c ->
                    CategoryTotalUi(
                        categoryId = c.categoryId,
                        name = c.categoryName,
                        icon = c.categoryIcon,
                        color = c.categoryColor,
                        total = c.total,
                        count = c.count,
                        percent = if (total.total > 0) {
                            (c.total.toFloat() / total.total) * 100f
                        } else {
                            0f
                        },
                    )
                },
            )
        }
    }

    fun averagePerDay(): Long {
        val r = _report.value
        val isCurrentMonth = monthCursor(System.currentTimeMillis()) == monthCursor(r.cursor)
        val calendarDays = if (isCurrentMonth) localDate(System.currentTimeMillis()).dayOfMonth else r.daysInMonth
        return (r.total.toDouble() / calendarDays.coerceAtLeast(1)).roundToLong()
    }
}
