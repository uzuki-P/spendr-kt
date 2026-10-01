package com.spendr.app.kt.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.data.repo.CategoryRepository
import com.spendr.app.kt.data.repo.TransactionFilters
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.model.DateRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String, val glyph: String) {
    DATE_DESC("Newest first", "sort-calendar-descending"),
    DATE_ASC("Oldest first", "sort-calendar-ascending"),
    AMOUNT_DESC("Highest amount first", "sort-numeric-descending"),
    AMOUNT_ASC("Lowest amount first", "sort-numeric-ascending"),
}

class TransactionsViewModel(
    private val repository: TransactionRepository,
    categoriesRepository: CategoryRepository? = null,
) : ViewModel() {

    data class Filters(
        val search: String = "",
        val categoryIds: List<Long> = emptyList(),
        val range: DateRange? = null,
        val discountOnly: Boolean = false,
        val sortMode: SortMode = SortMode.DATE_DESC,
    ) {
        val activeCount: Int
            get() = (if (categoryIds.isNotEmpty()) 1 else 0) +
                (if (discountOnly) 1 else 0) +
                (if (sortMode != SortMode.DATE_DESC) 1 else 0)
    }

    private val filters = MutableStateFlow(Filters())

    /** Typed search; committed to the query 250ms after the last keystroke (RN parity). */
    private val searchInput = MutableStateFlow("")
    private val committedSearch = MutableStateFlow("")

    val filterState: StateFlow<Filters> = filters

    val categories: StateFlow<List<CategoryEntity>> =
        (categoriesRepository?.observeCategories()
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionWithCategoryRow>> =
        combine(filters, committedSearch) { f, search -> f to search }
            .debounce(50)
            .flatMapLatest { (f, search) ->
                repository.observeTransactions(
                    TransactionFilters(
                        range = f.range,
                        search = search.trim().takeIf { it.isNotEmpty() },
                        categoryIds = f.categoryIds.takeIf { it.isNotEmpty() },
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
            .debounce(250)
            .onEach { committedSearch.value = it.trim() }
            .launchIn(viewModelScope)
    }

    fun setSearch(value: String) {
        filters.value = filters.value.copy(search = value)
        searchInput.value = value
    }

    fun setFilters(transform: (Filters) -> Filters) {
        filters.value = transform(filters.value)
    }

    fun setPrefilteredCategory(categoryId: Long) {
        filters.value = filters.value.copy(categoryIds = listOf(categoryId))
    }

    fun duplicate(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.duplicateTransaction(id)
            onDone()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}
