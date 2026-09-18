package com.spendr.app.kt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.data.repo.QuickAddRepository
import com.spendr.app.kt.data.repo.TransactionFilters
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.currentMonthRange
import com.spendr.app.kt.domain.daysInMonth
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.model.DateRange
import com.spendr.app.kt.domain.monthRange
import com.spendr.app.kt.domain.shiftMonth
import com.spendr.app.kt.domain.monthCursor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class HomeViewModel(
    private val transactions: TransactionRepository,
    quickAddsRepository: QuickAddRepository,
    private val lastAddedTransactionId: kotlinx.coroutines.flow.MutableStateFlow<Long?>,
) : ViewModel() {

    data class PaceData(
        val thisMonth: List<Long> = emptyList(),
        val average: List<Long> = emptyList(),
        val daysInMonth: Int = 30,
        val monthLabel: String = "",
        val hasRealAverage: Boolean = false,
    )

    data class UiState(
        val loaded: Boolean = false,
        val monthTotal: Long = 0,
        val monthCount: Long = 0,
        val pace: PaceData = PaceData(),
        val recent: List<TransactionWithCategoryRow> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val quickAdds: StateFlow<List<QuickAddWithCategoryRow>> =
        quickAddsRepository.observeQuickAdds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastAddedId: StateFlow<Long?> = lastAddedTransactionId.asStateFlow()

    /** One-shot: clears the pulse id so it never re-fires on later Home visits. */
    fun consumeLastAddedId() {
        lastAddedTransactionId.value = null
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val range = currentMonthRange()
            val total = transactions.totalInRange(range)
            val thisMonth = transactions.cumulativeDailyTotals(range, localDate(System.currentTimeMillis()).dayOfMonth)

            // 3 previous months, each cumulative series clamped to the current
            // month's length (a shorter February holds its final value flat).
            val cursor = monthCursor(System.currentTimeMillis())
            val currentLength = daysInMonth(range)
            val prevSeries = (1..3).map { offset ->
                transactions.cumulativeDailyTotals(monthRange(shiftMonth(cursor, -offset)), currentLength)
            }
            val avg = (0 until currentLength).map { i ->
                val nonEmpty = prevSeries.count { it.isNotEmpty() }
                if (nonEmpty == 0) {
                    0L
                } else {
                    val sum = prevSeries.sumOf { it.getOrElse(i) { _ -> 0L } }
                    (sum.toDouble() / nonEmpty).roundToLong()
                }
            }
            val recent = transactions.listTransactions(TransactionFilters(limit = 6))

            _state.value = UiState(
                loaded = true,
                monthTotal = total.total,
                monthCount = total.count,
                pace = PaceData(
                    thisMonth = thisMonth,
                    average = avg,
                    daysInMonth = currentLength,
                    monthLabel = com.spendr.app.kt.domain.formatMonthYear(System.currentTimeMillis()),
                    hasRealAverage = avg.any { it > 0 },
                ),
                recent = recent,
            )
        }
    }
}
