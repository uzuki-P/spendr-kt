package com.spendr.app.kt.data.repo

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.dao.QuickAddWithCategoryRow
import com.spendr.app.kt.data.db.entity.QuickAddSuggestionEntity
import com.spendr.app.kt.domain.QuickAddSuggestion
import com.spendr.app.kt.domain.rankQuickAddSuggestions
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class QuickAddRepository(
    private val db: SpendrDatabase,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

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

    /**
     * QuickAdd suggestions from the last 6 months of Transactions, cached in the
     * `quick_add_suggestions` table for the rest of the local day. One new
     * Transaction barely moves a 6-month frequency count, so the grouping query
     * runs at most once a day while suggestions exist.
     */
    suspend fun frequentSpending(): List<QuickAddSuggestion> {
        val dao = db.quickAddDao()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMs()).atZone(zone).toLocalDate()
        val day = today.toString()
        val cached = dao.cachedSuggestions(day)
        if (cached.isNotEmpty()) {
            return cached.map {
                QuickAddSuggestion(it.note, it.categoryId, it.paidAmount, it.useCount, it.latestDate)
            }
        }
        val since = today.minusMonths(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val suggestions = rankQuickAddSuggestions(dao.noteAmountCountsSince(since))
        dao.replaceSuggestions(
            suggestions.mapIndexed { rank, it ->
                QuickAddSuggestionEntity(rank, it.note, it.categoryId, it.paidAmount, it.useCount, it.latestDate, day)
            },
        )
        return suggestions
    }
}
