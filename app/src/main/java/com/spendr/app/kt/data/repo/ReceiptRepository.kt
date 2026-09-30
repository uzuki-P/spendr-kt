package com.spendr.app.kt.data.repo

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class ReceiptItemInput(val name: String, val paidAmount: Long, val quantity: String = "1")

data class ReceiptInput(
    val paidAmount: Long,
    val categoryId: Long,
    val merchant: String,
    val note: String?,
    val date: Long,
    val items: List<ReceiptItemInput>,
)

class ReceiptRepository(private val db: SpendrDatabase, private val transactions: TransactionRepository) {
    fun observeItems(transactionId: Long): Flow<List<ReceiptItemEntity>> =
        db.receiptItemDao().observeForTransaction(transactionId)

    suspend fun items(transactionId: Long): List<ReceiptItemEntity> =
        db.receiptItemDao().listForTransaction(transactionId)

    suspend fun save(input: ReceiptInput, id: Long? = null): Long = db.withTransaction {
        require(input.paidAmount > 0)
        require(input.merchant.isNotBlank())
        require(input.items.all { it.name.isNotBlank() && it.paidAmount >= 0 && (it.quantity.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO })
        val now = System.currentTimeMillis()
        val existing = id?.let { db.transactionDao().getById(it) }
        require(existing == null || existing.type == "receipt")
        val transaction = TransactionEntity(
            id = existing?.id ?: 0,
            paidAmount = input.paidAmount,
            originalAmount = null,
            discountAmount = null,
            discountType = null,
            categoryId = input.categoryId,
            note = input.note?.trim()?.takeIf { it.isNotEmpty() },
            merchant = input.merchant.trim(),
            tags = null,
            date = input.date,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            type = "receipt",
        )
        val savedId = if (existing == null) db.transactionDao().insert(transaction) else {
            db.transactionDao().update(transaction)
            existing.id
        }
        db.receiptItemDao().deleteForTransaction(savedId)
        db.receiptItemDao().insertAll(input.items.mapIndexed { index, item ->
            ReceiptItemEntity(
                transactionId = savedId,
                name = item.name.trim(),
                paidAmount = item.paidAmount,
                quantity = item.quantity,
                sortOrder = index,
            )
        })
        buildSet {
            existing?.note?.let(::add)
            transaction.note?.let(::add)
        }.forEach { transactions.recomputeNoteStats(it) }
        transactions.upsertMerchant(transaction.merchant!!)
        savedId
    }
}
