package com.spendr.app.kt.data.repo

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow
import kotlinx.coroutines.flow.Flow

class QuickAddRepository(private val db: SpendrDatabase) {

    fun observeQuickAdds(): Flow<List<QuickAddWithCategoryRow>> = db.quickAddDao().observeQuickAdds()

    suspend fun getQuickAdd(id: Long) = db.quickAddDao().getById(id)

    suspend fun createQuickAdd(
        label: String,
        categoryId: Long,
        note: String?,
        paidAmount: Long?,
        merchant: String?,
    ): Long = db.withTransaction {
        db.quickAddDao().insert(
            com.spendr.app.kt.data.db.entity.QuickAddEntity(
                label = label,
                categoryId = categoryId,
                note = note,
                paidAmount = paidAmount,
                merchant = merchant,
                tags = null,
                sortOrder = db.quickAddDao().maxSortOrder() + 1,
            ),
        )
    }

    suspend fun deleteQuickAdd(id: Long) = db.quickAddDao().deleteById(id)
}
