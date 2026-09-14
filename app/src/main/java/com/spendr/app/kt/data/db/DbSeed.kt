package com.spendr.app.kt.data.db

import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity

/**
 * Debug seed, ported from the RN app (`seedDefaults` + demo rows) so the
 * read-only Transactions screen has real data until ticket 03 brings the
 * backup-archive import path.
 */
object DbSeed {

    private val CATEGORY_PALETTE = listOf(
        "#A86086", "#B05E6B", "#B25F56", "#AF633B", "#A86829", "#9A6F19",
        "#87771C", "#638039", "#328562", "#04857E", "#098396", "#3C7CAB",
        "#5D75B4", "#7A6EB2", "#9366A4", "#A26192", "#AB5F80", "#B16242",
    )

    private val DEFAULT_CATEGORIES = listOf(
        Triple("Food", "silverware-fork-knife", 4),
        Triple("Snack & Coffee", "coffee-outline", 17),
        Triple("Shopping", "shopping-outline", 1),
        Triple("Transport", "car", 11),
        Triple("Game", "gamepad-variant-outline", 14),
        Triple("Bills", "file-document-outline", 2),
        Triple("Health", "heart-pulse", 7),
        Triple("Entertainment", "movie-open-outline", 13),
        Triple("Other", "dots-horizontal-circle-outline", 6),
    )

    private val DEFAULT_QUICK_ADD = listOf(
        Quad("Coffee", "Snack & Coffee", "Coffee", 18_000L),
        Quad("Parking", "Transport", "Parking", 3_000L),
        Quad("Lunch", "Food", "Lunch", 25_000L),
    )

    /** Seeds the default Categories + QuickAdds (used on fresh DBs and after reset). */
    suspend fun seedDefaults(db: SpendrDatabase) {
        seedCategoriesIfEmpty(db)
    }

    suspend fun seedIfEmpty(db: SpendrDatabase) {
        val nameToId = seedCategoriesIfEmpty(db)
        seedDemoTransactionsIfEmpty(db, nameToId)
        com.spendr.app.kt.data.repo.TransactionRepository(db).rebuildNoteStats()
    }

    private suspend fun seedCategoriesIfEmpty(db: SpendrDatabase): Map<String, Long> {
        val existing = db.categoryDao().count()
        if (existing > 0) return emptyMap()

        val now = System.currentTimeMillis()
        val nameToId = mutableMapOf<String, Long>()
        DEFAULT_CATEGORIES.forEachIndexed { index, (name, icon, colorIndex) ->
            val id = db.categoryDao().insert(
                CategoryEntity(
                    name = name,
                    icon = icon,
                    color = CATEGORY_PALETTE[colorIndex],
                    sortOrder = index.toDouble(),
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            nameToId[name] = id
        }
        DEFAULT_QUICK_ADD.forEachIndexed { index, (label, categoryName, note, paidAmount) ->
            val categoryId = nameToId[categoryName] ?: return@forEachIndexed
            db.quickAddDao().insert(
                com.spendr.app.kt.data.db.entity.QuickAddEntity(
                    label = label,
                    categoryId = categoryId,
                    note = note,
                    paidAmount = paidAmount,
                    merchant = null,
                    tags = null,
                    sortOrder = index.toDouble(),
                ),
            )
        }
        return nameToId
    }

    private suspend fun seedDemoTransactionsIfEmpty(db: SpendrDatabase, nameToId: Map<String, Long>) {
        if (nameToId.isEmpty()) return

        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour
        fun tx(
            paidAmount: Long,
            originalAmount: Long?,
            discountAmount: Long?,
            discountType: String?,
            categoryName: String,
            note: String,
            merchant: String?,
            date: Long,
        ) = TransactionEntity(
            paidAmount = paidAmount,
            originalAmount = originalAmount,
            discountAmount = discountAmount,
            discountType = discountType,
            categoryId = nameToId.getValue(categoryName),
            note = note,
            merchant = merchant,
            tags = null,
            date = date,
            createdAt = now,
            updatedAt = now,
        )

        listOf(
            tx(18_000, null, null, null, "Snack & Coffee", "Coffee", "Kopi Kenangan", now - 2 * hour),
            tx(25_000, null, null, null, "Food", "Lunch", "Warung Bu Sari", now - day),
            tx(128_000, 150_000, 22_000, "fixed", "Shopping", "Weekly groceries", "Indomaret", now - 2 * day),
            tx(22_000, null, null, null, "Transport", "Go to office", "Gojek", now - 3 * day),
            tx(80_000, 100_000, 20_000, "percentage", "Game", "Steam sale", null, now - 5 * day),
        ).forEach { db.transactionDao().insert(it) }
    }

    private data class Quad(val label: String, val categoryName: String, val note: String, val paidAmount: Long)
}
