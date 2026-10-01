package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.spendr.app.kt.data.db.entity.TransactionEntity
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import com.spendr.app.kt.data.db.entity.TransactionWithCategoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Transaction
    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color
        FROM transactions t JOIN categories c ON c.id = t.category_id
        ORDER BY t.date DESC, t.id DESC
        """,
    )
    fun observeTransactionsWithCategory(): Flow<List<TransactionWithCategoryRow>>

    @Transaction
    @RawQuery(observedEntities = [TransactionEntity::class, ReceiptItemEntity::class])
    fun observeTransactionsRaw(query: SupportSQLiteQuery): Flow<List<TransactionWithCategoryRow>>

    @Transaction
    @RawQuery(observedEntities = [TransactionEntity::class])
    suspend fun getTransactionsRaw(query: SupportSQLiteQuery): List<TransactionWithCategoryRow>

    @Transaction
    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color
        FROM transactions t JOIN categories c ON c.id = t.category_id
        WHERE t.id = :id
        """,
    )
    suspend fun getTransactionWithCategory(id: Long): TransactionWithCategoryRow?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun listAllForBackup(): List<TransactionEntity>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query(
        """
        SELECT COALESCE(SUM(paid_amount), 0) AS total, COUNT(*) AS count
        FROM transactions WHERE date >= :start AND date <= :end
        """,
    )
    suspend fun totalInRange(start: Long, end: Long): TotalRow

    @Query(
        """
        SELECT COALESCE(SUM(discount_amount), 0) AS total, COUNT(*) AS count
        FROM transactions
        WHERE discount_amount IS NOT NULL AND discount_amount > 0 AND date >= :start AND date <= :end
        """,
    )
    suspend fun savingsInRange(start: Long, end: Long): TotalRow

    @Query(
        """
        SELECT date(t.date / 1000, 'unixepoch', 'localtime') AS day,
               COALESCE(SUM(t.paid_amount), 0) AS total,
               COUNT(*) AS count
        FROM transactions t
        WHERE t.date >= :start AND t.date <= :end
        GROUP BY day
        ORDER BY day ASC
        """,
    )
    suspend fun dailyTotals(start: Long, end: Long): List<DailyTotalRow>

    @Query(
        """
        SELECT strftime('%Y-%m', t.date / 1000, 'unixepoch', 'localtime') AS month,
               COALESCE(SUM(t.paid_amount), 0) AS total,
               COUNT(*) AS count
        FROM transactions t
        GROUP BY month
        ORDER BY month ASC
        """,
    )
    suspend fun monthlyTotals(): List<MonthlyTotalRow>

    @Query(
        """
        SELECT t.category_id, c.name AS category_name, c.icon AS category_icon, c.color AS category_color,
               COALESCE(SUM(t.paid_amount), 0) AS total, COUNT(*) AS count
        FROM transactions t JOIN categories c ON c.id = t.category_id
        WHERE t.date >= :start AND t.date <= :end
        GROUP BY t.category_id
        ORDER BY total DESC
        """,
    )
    suspend fun categoryTotals(start: Long, end: Long): List<CategoryTotalRow>

    @Query(
        """
        WITH popular_notes AS (
          SELECT note, SUM(use_count) AS usage_count, MAX(latest_date) AS latest_date
          FROM note_stats
          GROUP BY note
          ORDER BY usage_count DESC, latest_date DESC
          LIMIT :limit
        )
        SELECT p.note, t.category_id, c.name AS category_name, c.icon AS category_icon,
               c.color AS category_color, t.merchant, t.paid_amount, t.original_amount,
               t.discount_amount, t.date, p.usage_count
        FROM popular_notes p
        JOIN transactions t ON t.id = (
          SELECT latest.id
          FROM transactions latest
          WHERE latest.note = p.note
          ORDER BY latest.date DESC, latest.id DESC
          LIMIT 1
        )
        JOIN categories c ON c.id = t.category_id
        ORDER BY p.usage_count DESC, t.date DESC
        """,
    )
    suspend fun noteSuggestions(limit: Long): List<SuggestionRow>

    @Query(
        """
        SELECT s.note, t.category_id, c.name AS category_name, c.icon AS category_icon,
               c.color AS category_color, t.merchant, t.paid_amount, t.original_amount,
               t.discount_amount, t.date, s.use_count AS usage_count
        FROM note_stats s
        JOIN transactions t ON t.id = (
          SELECT latest.id
          FROM transactions latest
          WHERE latest.note = s.note AND latest.paid_amount = s.paid_amount
          ORDER BY latest.date DESC, latest.id DESC
          LIMIT 1
        )
        JOIN categories c ON c.id = t.category_id
        WHERE s.paid_amount = :paidAmount
        ORDER BY s.use_count DESC, s.latest_date DESC
        LIMIT :limit
        """,
    )
    suspend fun noteSuggestionsByAmount(paidAmount: Long, limit: Long): List<SuggestionRow>

    @Query("DELETE FROM note_stats WHERE note = :note")
    suspend fun deleteNoteStats(note: String)

    @Query("DELETE FROM note_stats")
    suspend fun deleteAllNoteStats()
}
