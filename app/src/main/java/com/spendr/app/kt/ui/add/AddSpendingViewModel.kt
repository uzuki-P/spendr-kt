package com.spendr.app.kt.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.data.repo.CategoryRepository
import com.spendr.app.kt.data.repo.QuickAddRepository
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.AmountDraft
import com.spendr.app.kt.domain.AmountField
import com.spendr.app.kt.domain.DiscountMode
import com.spendr.app.kt.domain.appendDigits
import com.spendr.app.kt.domain.backspace
import com.spendr.app.kt.domain.buildInput
import com.spendr.app.kt.domain.clearField
import com.spendr.app.kt.domain.discountedAmountDraft
import com.spendr.app.kt.domain.editField
import com.spendr.app.kt.domain.model.NoteSuggestion
import com.spendr.app.kt.domain.plainAmountDraft
import com.spendr.app.kt.domain.rankSuggestions
import com.spendr.app.kt.domain.selectField
import com.spendr.app.kt.domain.toggleDiscount
import com.spendr.app.kt.domain.toggleDiscountType
import com.spendr.app.kt.domain.topSuggestionsForCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddSpendingViewModel(
    private val transactions: TransactionRepository,
    categoriesRepository: CategoryRepository,
    quickAddsRepository: QuickAddRepository,
    private val transactionId: Long?,
    quickAddId: Long?,
    duplicateFromId: Long?,
    private val onTransactionAdded: (Long) -> Unit = {},
) : ViewModel() {

    data class UiState(
        val draft: AmountDraft = AmountDraft(),
        val categoryId: Long? = null,
        val note: String = "",
        val merchant: String = "",
        val dateMs: Long = System.currentTimeMillis(),
        val editing: Boolean = false,
        val amountError: Boolean = false,
        val pendingReplace: NoteSuggestion? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val categories: StateFlow<List<com.spendr.app.kt.data.db.entity.CategoryEntity>> =
        categoriesRepository.observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quickAdds: StateFlow<List<com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow>> =
        quickAddsRepository.observeQuickAdds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Category ids of the last 12 Transactions, for recents-first picker order. */
    val recentCategoryIds: StateFlow<List<Long>> =
        kotlinx.coroutines.flow.flow {
            emit(
                transactions.listTransactions(
                    com.spendr.app.kt.data.repo.TransactionFilters(limit = 12),
                ).mapNotNull { row ->
                    row.transaction.let { it.categoryId }
                }.distinct(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Suggestion candidates, re-queried whenever the paid amount changes (RN parity). */
    private val candidates = state
        .flatMapLatest { s ->
            val paid = s.draft.paidAmount
            kotlinx.coroutines.flow.flow {
                emit(
                    if (paid > 0) {
                        transactions.noteSuggestionsByAmount(paid)
                    } else {
                        transactions.noteSuggestions()
                    },
                )
            }
        }

    val suggestions: StateFlow<List<NoteSuggestion>> =
        combine(state, candidates) { s, list ->
            val paid = s.draft.paidAmount
            when {
                s.note.isNotBlank() -> rankSuggestions(s.note, list, s.categoryId, limit = 6)
                s.categoryId != null -> topSuggestionsForCategory(s.categoryId, list, limit = 6)
                paid > 0 || list.isNotEmpty() -> rankSuggestions("", list, s.categoryId, limit = 6)
                else -> emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            when {
                transactionId != null -> transactions.getTransaction(transactionId)?.let { tx ->
                    _state.value = _state.value.copy(
                        draft = if (tx.discountAmount != null && tx.discountAmount > 0) {
                            discountedAmountDraft(
                                tx.paidAmount,
                                tx.originalAmount,
                                tx.discountAmount,
                                mode = if (tx.discountType == "percentage") DiscountMode.PERCENTAGE else DiscountMode.FIXED,
                            )
                        } else {
                            plainAmountDraft(tx.paidAmount)
                        },
                        categoryId = tx.categoryId,
                        note = tx.note.orEmpty(),
                        merchant = tx.merchant.orEmpty(),
                        dateMs = tx.date,
                        editing = true,
                    )
                }

                duplicateFromId != null -> transactions.getTransaction(duplicateFromId)?.let { tx ->
                    _state.value = _state.value.copy(
                        draft = if (tx.discountAmount != null && tx.discountAmount > 0) {
                            discountedAmountDraft(
                                tx.paidAmount,
                                tx.originalAmount,
                                tx.discountAmount,
                                mode = if (tx.discountType == "percentage") DiscountMode.PERCENTAGE else DiscountMode.FIXED,
                            )
                        } else {
                            plainAmountDraft(tx.paidAmount)
                        },
                        categoryId = tx.categoryId,
                        note = tx.note.orEmpty(),
                        merchant = tx.merchant.orEmpty(),
                        dateMs = System.currentTimeMillis(),
                    )
                }

                quickAddId != null -> quickAddsRepository.getQuickAdd(quickAddId)?.let { qa ->
                    _state.value = _state.value.copy(
                        draft = plainAmountDraft(qa.paidAmount ?: 0),
                        categoryId = qa.categoryId,
                        note = qa.note.orEmpty(),
                        merchant = qa.merchant.orEmpty(),
                    )
                }
            }
        }
    }

    fun selectField(field: AmountField) = update { selectField(it, field) }

    fun append(digits: String) = update { appendDigits(it, it.activeField, digits) }

    fun backspace() = update { backspace(it, it.activeField) }

    fun clearActive() = update { clearField(it, it.activeField) }

    fun toggleDiscount() = update { toggleDiscount(it) }

    fun toggleDiscountType() = update { toggleDiscountType(it) }

    fun applyDiscountMode(mode: DiscountMode) = update {
        if (it.discountMode == mode) it else toggleDiscountType(it)
    }

    fun setNote(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun setMerchant(value: String) {
        _state.value = _state.value.copy(merchant = value)
    }

    fun setDate(ms: Long) {
        _state.value = _state.value.copy(dateMs = ms)
    }

    fun setCategory(id: Long) {
        _state.value = _state.value.copy(categoryId = id)
    }

    /** Full suggestion prefill; asks before overwriting entered amounts. */
    fun applySuggestion(suggestion: NoteSuggestion, mode: SuggestionMode) {
        if (mode == SuggestionMode.NOTE_ONLY) {
            _state.value = _state.value.copy(note = suggestion.note)
            return
        }
        val s = _state.value
        val wouldOverwrite = s.draft.paidAmount > 0 || s.draft.originalStr.isNotBlank() || s.draft.discountStr.isNotBlank()
        if (wouldOverwrite && s.pendingReplace == null) {
            _state.value = s.copy(pendingReplace = suggestion)
            return
        }
        _state.value = s.copy(
            pendingReplace = null,
            note = suggestion.note,
            categoryId = suggestion.categoryId,
            merchant = suggestion.merchant ?: s.merchant,
            draft = if ((suggestion.lastOriginalAmount ?: 0) > 0 && (suggestion.lastDiscountAmount ?: 0) > 0) {
                discountedAmountDraft(
                    suggestion.lastPaidAmount,
                    suggestion.lastOriginalAmount,
                    suggestion.lastDiscountAmount,
                    DiscountMode.FIXED,
                )
            } else {
                plainAmountDraft(suggestion.lastPaidAmount)
            },
        )
    }

    fun dismissReplaceDialog() {
        _state.value = _state.value.copy(pendingReplace = null)
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        val input = buildInput(s.draft, s.categoryId, s.note, s.merchant, s.dateMs)
        if (input == null) {
            if (s.categoryId == null) return
            _state.value = s.copy(amountError = true)
            return
        }
        viewModelScope.launch {
            if (s.editing && transactionId != null) {
                transactions.updateTransaction(transactionId, input)
            } else {
                val newId = transactions.insertTransaction(input)
                onTransactionAdded(newId)
            }
            onSaved()
        }
    }

    fun deleteEditing(onDeleted: () -> Unit) {
        val id = transactionId ?: return
        viewModelScope.launch {
            transactions.deleteTransaction(id)
            onDeleted()
        }
    }

    private fun update(transform: (AmountDraft) -> AmountDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft))
    }

    enum class SuggestionMode { FULL, NOTE_ONLY }
}
