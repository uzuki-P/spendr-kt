package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.spendr.app.kt.data.db.entity.QuickAddEntity
import com.spendr.app.kt.data.db.entity.QuickAddSuggestionEntity
import com.spendr.app.kt.domain.NoteAmountCount
import kotlinx.coroutines.flow.Flow

data class QuickAddWithCategoryRow(
    @androidx.room.Embedded val quickAdd: QuickAddEntity,
    @androidx.room.ColumnInfo(name = "category_name") val categoryName: String,
    @androidx.room.ColumnInfo(name = "category_icon") val categoryIcon: String,
    @androidx.room.ColumnInfo(name = "category_color") val categoryColor: String,
)

@Dao
interface QuickAddDao {

    @Transaction
    @Query(
        """
        SELECT q.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color
        FROM quick_add q JOIN categories c ON c.id = q.category_id
        ORDER BY q.sort_order, q.id
        """,
    )
    fun observeQuickAdds(): Flow<List<QuickAddWithCategoryRow>>

    /** Noted Transactions since [since], counted per `(note, category, paidAmount)`. */
    @Query(
        """
        SELECT note, category_id AS categoryId, paid_amount AS paidAmount,
               COUNT(*) AS useCount, MAX(date) AS latestDate
        FROM transactions
        WHERE date >= :since AND note IS NOT NULL AND TRIM(note) != ''
        GROUP BY note, category_id, paid_amount
        """,
    )
    suspend fun noteAmountCountsSince(since: Long): List<NoteAmountCount>

    @Query("SELECT * FROM quick_add_suggestions WHERE computed_on = :day ORDER BY rank")
    suspend fun cachedSuggestions(day: String): List<QuickAddSuggestionEntity>

    @Query("DELETE FROM quick_add_suggestions")
    suspend fun clearSuggestions()

    @Insert
    suspend fun insertSuggestions(suggestions: List<QuickAddSuggestionEntity>)

    /** Replaces the cached suggestions in one transaction. */
    @Transaction
    suspend fun replaceSuggestions(suggestions: List<QuickAddSuggestionEntity>) {
        clearSuggestions()
        insertSuggestions(suggestions)
    }

    @Query("SELECT * FROM quick_add WHERE id = :id")
    suspend fun getById(id: Long): QuickAddEntity?

    @Query("SELECT * FROM quick_add ORDER BY id")
    suspend fun listForBackup(): List<QuickAddEntity>

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM quick_add")
    suspend fun maxSortOrder(): Double

    @Insert
    suspend fun insert(quickAdd: QuickAddEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(quickAdd: QuickAddEntity): Long

    @Query("DELETE FROM quick_add WHERE id = :id")
    suspend fun deleteById(id: Long)
}
