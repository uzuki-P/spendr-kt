package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptItemDao {
    @Query("SELECT * FROM receipt_items WHERE transaction_id = :transactionId ORDER BY sort_order, id")
    fun observeForTransaction(transactionId: Long): Flow<List<ReceiptItemEntity>>

    @Query("SELECT * FROM receipt_items WHERE transaction_id = :transactionId ORDER BY sort_order, id")
    suspend fun listForTransaction(transactionId: Long): List<ReceiptItemEntity>

    @Query("SELECT * FROM receipt_items ORDER BY transaction_id, sort_order, id")
    suspend fun listAllForBackup(): List<ReceiptItemEntity>

    @Insert
    suspend fun insertAll(items: List<ReceiptItemEntity>)

    @Query("DELETE FROM receipt_items WHERE transaction_id = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)
}
