package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendr.app.kt.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort_order, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order, id")
    suspend fun list(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM categories")
    suspend fun maxSortOrder(): Double

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET category_id = :replacementId WHERE category_id = :sourceId")
    suspend fun reassignTransactions(sourceId: Long, replacementId: Long)

    @Query("UPDATE quick_add SET category_id = :replacementId WHERE category_id = :sourceId")
    suspend fun reassignQuickAdds(sourceId: Long, replacementId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :id")
    suspend fun transactionCount(id: Long): Int

    @Query("SELECT COUNT(*) FROM quick_add WHERE category_id = :id")
    suspend fun quickAddCount(id: Long): Int

    @Query("UPDATE categories SET name = :name, icon = :icon, color = :color, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateFields(id: Long, name: String, icon: String, color: String, updatedAt: Long)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}
