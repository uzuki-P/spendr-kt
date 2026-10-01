package com.spendr.app.kt.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.data.repo.CategoryRepository
import com.spendr.app.kt.data.repo.TransactionFilters
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.data.repo.ReceiptRepository
import com.spendr.app.kt.domain.formatDayShort
import com.spendr.app.kt.domain.localDate
import com.spendr.app.kt.domain.model.DateRange
import com.spendr.app.kt.domain.localDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.ZoneId

/** Preset ranges for the global search, ported from RN `TransactionSearchScreen`. */
enum class SearchRangeKey(val label: String, val months: Int?) {
    PAST_MONTH("Past month", 1),
    PAST_3_MONTHS("Past 3 months", 3),
    PAST_6_MONTHS("Past 6 months", 6),
    ALL_TIME("All time", null),
    CUSTOM("Custom", null),
}

class TransactionSearchViewModel(
    private val repository: TransactionRepository,
    categoriesRepository: CategoryRepository? = null,
    private val receipts: ReceiptRepository? = null,
) : ViewModel() {

    data class SearchFilters(
        val rangeKey: SearchRangeKey = SearchRangeKey.PAST_3_MONTHS,
        val customStart: Long = 0L,
        val customEnd: Long = 0L,
        val categoryIds: List<Long> = emptyList(),
        val approximatePaidAmount: Long? = null,
    ) {
        /** Optional filters diverging from the default: range chip + categories + amount. */
        val activeCount: Int
            get() = (if (rangeKey != SearchRangeKey.PAST_3_MONTHS) 1 else 0) +
                (if (categoryIds.isNotEmpty()) 1 else 0) +
                (if (approximatePaidAmount != null && approximatePaidAmount > 0) 1 else 0)
    }

    private val searchInput = MutableStateFlow("")
    private val committedQuery = MutableStateFlow("")
    private val filters = MutableStateFlow(defaultFilters())

    val filterState: StateFlow<SearchFilters> = filters

    val categories: StateFlow<List<CategoryEntity>> =
        (categoriesRepository?.observeCategories()
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Raw input as typed; [items] waits for the 500ms debounce (RN parity). */
    val searchText: StateFlow<String> = searchInput
    val committedSearch: StateFlow<String> = committedQuery
    private val _matchingItems = MutableStateFlow<Map<Long, String>>(emptyMap())
    val matchingItems: StateFlow<Map<Long, String>> = _matchingItems

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<TransactionWithCategoryRow>> =
        combine(committedQuery, filters) { query, f -> query to f }
            .debounce(50)
            .flatMapLatest { (query, f) ->
                repository.observeTransactions(
                    TransactionFilters(
                        range = rangeFor(f),
                        search = query.takeIf { it.isNotEmpty() },
                        categoryIds = f.categoryIds.takeIf { it.isNotEmpty() },
                        approximatePaidAmount = f.approximatePaidAmount,
                        approximatePaidAmountTolerance = f.approximatePaidAmount
                            ?.let { maxOf(1000L, Math.round(it * 0.1)) },
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        searchInput
            .debounce(500)
            .onEach { committedQuery.value = it.trim() }
            .launchIn(viewModelScope)
        combine(items, committedQuery) { rows, query -> rows to query }
            .onEach { (rows, query) ->
                _matchingItems.value = if (query.isBlank() || receipts == null) emptyMap() else {
                    val matches = mutableMapOf<Long, String>()
                    for (row in rows.filter { it.transaction.type == "receipt" }) {
                        val match = receipts.items(row.transaction.id)
                            .firstOrNull { it.name.contains(query, ignoreCase = true) }
                        if (match != null) matches[row.transaction.id] = match.name
                    }
                    matches
                }
            }
            .launchIn(viewModelScope)
    }

    fun setSearch(value: String) {
        searchInput.value = value
    }

    fun setFilters(transform: (SearchFilters) -> SearchFilters) {
        filters.value = transform(filters.value)
    }

    companion object {
        /** RN defaultFilters: past 3 months, no categories, no amount. */
        fun defaultFilters(now: Long = System.currentTimeMillis()): SearchFilters {
            val end = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(now),
                ZoneId.systemDefault(),
            )
            val start = end.minusMonths(3)
            return SearchFilters(
                rangeKey = SearchRangeKey.PAST_3_MONTHS,
                customStart = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                customEnd = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
        }

        /** RN rangeFor: presets end "now"; custom normalizes start/end to day bounds. */
        fun rangeFor(filters: SearchFilters, now: Long = System.currentTimeMillis()): DateRange? =
            when (filters.rangeKey) {
                SearchRangeKey.ALL_TIME -> null
                SearchRangeKey.CUSTOM -> {
                    val zone = ZoneId.systemDefault()
                    val start = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(minOf(filters.customStart, filters.customEnd)),
                        zone,
                    ).`with`(java.time.LocalTime.MIN)
                    val end = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(maxOf(filters.customStart, filters.customEnd)),
                        zone,
                    ).`with`(java.time.LocalTime.MAX)
                    DateRange(
                        start.atZone(zone).toInstant().toEpochMilli(),
                        end.atZone(zone).toInstant().toEpochMilli(),
                    )
                }
                else -> {
                    val months = filters.rangeKey.months ?: return null
                    val zone = ZoneId.systemDefault()
                    val end = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
                    val start = end.minusMonths(months.toLong())
                        .`with`(java.time.LocalTime.MIN)
                    DateRange(
                        start.atZone(zone).toInstant().toEpochMilli(),
                        end.atZone(zone).toInstant().toEpochMilli(),
                    )
                }
            }
    }
}

/** Day grouping for the search results, ported from RN `groupTransactions`. */
data class DaySearchGroup(
    val rows: List<TransactionWithCategoryRow>,
) {
    val label: String = formatDayShort(rows.first().transaction.date)
    val total: Long = rows.sumOf { it.transaction.paidAmount }
}

fun groupByDay(
    items: List<TransactionWithCategoryRow>,
): List<DaySearchGroup> {
    val groups = LinkedHashMap<Int, MutableList<TransactionWithCategoryRow>>()
    for (item in items) {
        val key = localDate(item.transaction.date).toEpochDay().toInt()
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }
    return groups.values.map { DaySearchGroup(it) }
}
