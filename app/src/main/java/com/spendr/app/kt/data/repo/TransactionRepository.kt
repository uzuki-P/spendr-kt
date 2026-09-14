package com.spendr.app.kt.data.repo

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.dao.SuggestionRow
import com.spendr.app.kt.data.db.dao.TotalRow
import com.spendr.app.kt.data.db.entity.TransactionEntity
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import com.spendr.app.kt.domain.model.DailyTotal
import com.spendr.app.kt.domain.model.DateRange
import com.spendr.app.kt.domain.model.NoteSuggestion
import com.spendr.app.kt.domain.model.TransactionInput
import kotlinx.coroutines.flow.Flow

data class TransactionFilters(
    val range: DateRange? = null,
    val categoryIds: List<Long>? = null,
    val search: String? = null,
    val approximatePaidAmount: Long? = null,
    val approximatePaidAmountTolerance: Long? = null,
    val limit: Int? = null,
)

class TransactionRepository(private val db: SpendrDatabase) {

    private val dao get() = db.transactionDao()

    fun observeTransactions(filters: TransactionFilters = TransactionFilters()): Flow<List<TransactionWithCategoryRow>> =
        dao.observeTransactionsRaw(buildQuery(filters))

    suspend fun listTransactions(filters: TransactionFilters = TransactionFilters()): List<TransactionWithCategoryRow> =
        dao.getTransactionsRaw(buildQuery(filters))

    suspend fun getTransaction(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun getTransactionWithCategory(id: Long): TransactionWithCategoryRow? =
        dao.getTransactionWithCategory(id)

    suspend fun insertTransaction(input: TransactionInput): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            TransactionEntity(
                paidAmount = input.paidAmount,
                originalAmount = input.originalAmount,
                discountAmount = input.discountAmount,
                discountType = input.discountType,
                categoryId = input.categoryId,
                note = input.note,
                merchant = input.merchant,
                tags = input.tags,
                date = input.date,
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (!input.note.isNullOrBlank()) recomputeNoteStats(input.note)
        if (!input.merchant.isNullOrBlank()) upsertMerchant(input.merchant)
        id
    }

    suspend fun updateTransaction(id: Long, input: TransactionInput) = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = dao.getById(id)
        if (existing != null) {
            dao.update(
                existing.copy(
                    paidAmount = input.paidAmount,
                    originalAmount = input.originalAmount,
                    discountAmount = input.discountAmount,
                    discountType = input.discountType,
                    categoryId = input.categoryId,
                    note = input.note,
                    merchant = input.merchant,
                    tags = input.tags,
                    date = input.date,
                    updatedAt = now,
                ),
            )
        }
        val affectedNotes = buildSet {
            val oldNote = existing?.note
            if (!oldNote.isNullOrBlank()) add(oldNote)
            if (!input.note.isNullOrBlank()) add(input.note)
        }
        for (note in affectedNotes) recomputeNoteStats(note)
        if (!input.merchant.isNullOrBlank()) upsertMerchant(input.merchant)
    }

    suspend fun deleteTransaction(id: Long) = db.withTransaction {
        val existing = dao.getById(id)
        if (existing != null) {
            dao.delete(existing)
            if (!existing.note.isNullOrBlank()) recomputeNoteStats(existing.note)
        }
    }

    /** Copies the Transaction with date = now. */
    suspend fun duplicateTransaction(id: Long): Long? = db.withTransaction {
        val source = dao.getById(id) ?: return@withTransaction null
        val now = System.currentTimeMillis()
        val newId = dao.insert(
            source.copy(id = 0, date = now, createdAt = now, updatedAt = now),
        )
        if (!source.note.isNullOrBlank()) recomputeNoteStats(source.note)
        newId
    }

    suspend fun totalInRange(range: DateRange): TotalRow =
        dao.totalInRange(range.start, range.end)

    suspend fun savingsInRange(range: DateRange): TotalRow =
        dao.savingsInRange(range.start, range.end)

    suspend fun categoryTotals(range: DateRange): List<com.spendr.app.kt.data.db.dao.CategoryTotalRow> =
        dao.categoryTotals(range.start, range.end)

    suspend fun dailyTotals(range: DateRange): List<DailyTotal> =
        dao.dailyTotals(range.start, range.end).map { DailyTotal(it.day, it.total, it.count) }

    /** Month-to-date cumulative paidAmount series, length = min(upToDay, daysInMonth). */
    suspend fun cumulativeDailyTotals(range: DateRange, upToDay: Int): List<Long> {
        val daily = dao.dailyTotals(range.start, range.end)
        val dailyMap = daily.associate { it.day.takeLast(2).toInt() to it.total }
        val daysInMonth = com.spendr.app.kt.domain.localDate(range.end).dayOfMonth
        val limit = maxOf(0, minOf(upToDay, daysInMonth))
        val series = ArrayList<Long>(limit)
        var cum = 0L
        for (d in 1..limit) {
            cum += dailyMap[d] ?: 0L
            series.add(cum)
        }
        return series
    }

    suspend fun noteSuggestions(limit: Long = 100): List<NoteSuggestion> =
        dao.noteSuggestions(limit).map { it.toSuggestion() }

    suspend fun noteSuggestionsByAmount(paidAmount: Long, limit: Long = 100): List<NoteSuggestion> {
        if (paidAmount <= 0) return emptyList()
        return dao.noteSuggestionsByAmount(paidAmount, limit).map { it.toSuggestion() }
    }

    suspend fun upsertMerchant(name: String) = db.withTransaction {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val touched = db.merchantDao().touch(trimmed, now)
            if (touched == 0) {
                db.merchantDao().insertOrIgnore(
                    com.spendr.app.kt.data.db.entity.MerchantEntity(
                        name = trimmed,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    /** Rebuilds one note's counters from the raw transactions table (delete-then-insert). */
    suspend fun recomputeNoteStats(note: String) {
        val trimmed = note.trim()
        if (trimmed.isEmpty()) return
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM note_stats WHERE note = ?", arrayOf(trimmed))
        sql.execSQL(
            """
            INSERT INTO note_stats (note, paid_amount, use_count, latest_date)
            SELECT note, paid_amount, COUNT(*) AS use_count, MAX(date) AS latest_date
            FROM transactions
            WHERE note = ? AND paid_amount > 0
            GROUP BY note, paid_amount
            """.trimIndent(),
            arrayOf(trimmed),
        )
    }

    suspend fun rebuildNoteStats() {
        val sql = db.openHelper.writableDatabase
        sql.execSQL("DELETE FROM note_stats")
        sql.execSQL(
            """
            INSERT INTO note_stats (note, paid_amount, use_count, latest_date)
            SELECT note, paid_amount, COUNT(*) AS use_count, MAX(date) AS latest_date
            FROM transactions
            WHERE note IS NOT NULL AND note != '' AND paid_amount > 0
            GROUP BY note, paid_amount
            """.trimIndent(),
        )
    }

    private fun buildQuery(filters: TransactionFilters): SimpleSQLiteQuery {
        val sql = StringBuilder(
            """
            SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color
            FROM transactions t JOIN categories c ON c.id = t.category_id
            """.trimIndent(),
        )
        val args = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        filters.range?.let {
            conditions.add("t.date >= ? AND t.date <= ?")
            args.add(it.start)
            args.add(it.end)
        }
        filters.categoryIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            conditions.add("t.category_id IN (${ids.joinToString(", ") { "?" }})")
            args.addAll(ids)
        }
        val search = filters.search?.trim()
        if (!search.isNullOrEmpty()) {
            // LOWER(...): search also matches the Category name, per RN listTransactions
            conditions.add("(LOWER(t.note) LIKE ? OR LOWER(t.merchant) LIKE ? OR LOWER(c.name) LIKE ?)")
            val needle = "%" + search.lowercase() + "%"
            args.add(needle)
            args.add(needle)
            args.add(needle)
        }
        filters.approximatePaidAmount?.takeIf { it > 0 }?.let { amount ->
            val tolerance = maxOf(0, filters.approximatePaidAmountTolerance ?: 0)
            conditions.add("t.paid_amount BETWEEN ? AND ?")
            args.add(maxOf(0, amount - tolerance))
            args.add(amount + tolerance)
        }
        if (conditions.isNotEmpty()) sql.append(" WHERE ").append(conditions.joinToString(" AND "))
        sql.append(" ORDER BY t.date DESC, t.id DESC")
        filters.limit?.let {
            sql.append(" LIMIT ?")
            args.add(it)
        }
        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
    }
}

private fun SuggestionRow.toSuggestion() = NoteSuggestion(
    note = note,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    merchant = merchant,
    lastPaidAmount = paidAmount,
    lastOriginalAmount = originalAmount,
    lastDiscountAmount = discountAmount,
    lastDate = date,
    usageCount = usageCount,
)
