package com.spendr.app.kt.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import com.spendr.app.kt.data.repo.ReceiptRepository
import com.spendr.app.kt.data.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionDetailViewModel(
    private val repository: TransactionRepository,
    private val id: Long,
    receipts: ReceiptRepository,
) : ViewModel() {

    val receiptItems = receipts.observeItems(id)
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _transaction = MutableStateFlow<TransactionWithCategoryRow?>(null)
    val transaction: StateFlow<TransactionWithCategoryRow?> = _transaction.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _transaction.value = repository.getTransactionWithCategory(id)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            onDeleted()
        }
    }

    fun duplicateReceipt(onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            repository.duplicateTransaction(id)?.let(onDuplicated)
        }
    }
}
