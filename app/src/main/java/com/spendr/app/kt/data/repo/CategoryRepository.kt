package com.spendr.app.kt.data.repo

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** Rotating palette for auto-created categories, ported from RN `categoryPalette.ts`. */
val CATEGORY_PALETTE = listOf(
    "#A86086", "#B05E6B", "#B25F56", "#AF633B", "#A86829", "#9A6F19",
    "#87771C", "#638039", "#328562", "#04857E", "#098396", "#3C7CAB",
    "#5D75B4", "#7A6EB2", "#9366A4", "#A26192", "#AB5F80", "#B16242",
)

const val AUTO_CATEGORY_ICON = "tag"

class CategoryRepository(private val db: SpendrDatabase) {

    fun observeCategories(): Flow<List<CategoryEntity>> = db.categoryDao().observeCategories()

    suspend fun listCategories(): List<CategoryEntity> = db.categoryDao().list()

    suspend fun getCategory(id: Long): CategoryEntity? =
        db.categoryDao().list().firstOrNull { it.id == id }

    suspend fun createCategory(name: String, icon: String, color: String): Long =
        db.withTransaction {
            val now = System.currentTimeMillis()
            db.categoryDao().insert(
                CategoryEntity(
                    name = name.trim(),
                    icon = icon,
                    color = color,
                    sortOrder = db.categoryDao().maxSortOrder() + 1,
                    isDefault = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

    suspend fun updateCategory(id: Long, name: String, icon: String, color: String) =
        db.withTransaction {
            db.categoryDao().updateFields(id, name.trim(), icon, color, System.currentTimeMillis())
        }

    data class CategoryUsage(val transactionCount: Int, val quickAddCount: Int)

    suspend fun getCategoryUsage(id: Long): CategoryUsage = db.withTransaction {
        CategoryUsage(db.categoryDao().transactionCount(id), db.categoryDao().quickAddCount(id))
    }

    /** Rewrites sort_order = index for every id, port of `reorderCategories`. */
    suspend fun reorderCategories(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id ->
            db.categoryDao().getById(id)?.let {
                db.categoryDao().update(it.copy(sortOrder = index.toDouble(), updatedAt = System.currentTimeMillis()))
            }
        }
    }

    /** Atomically reassigns Transactions and QuickAdds, then deletes the source Category. */
    suspend fun replaceCategoryAndDelete(sourceId: Long, replacementId: Long) = db.withTransaction {
        val now = System.currentTimeMillis()
        db.categoryDao().reassignTransactions(sourceId, replacementId)
        db.categoryDao().reassignQuickAdds(sourceId, replacementId)
        db.categoryDao().deleteById(sourceId)
    }

    suspend fun deleteCategory(id: Long) = db.withTransaction {
        db.categoryDao().deleteById(id)
    }
}
