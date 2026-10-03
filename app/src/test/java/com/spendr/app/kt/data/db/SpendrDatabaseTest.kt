package com.spendr.app.kt.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SpendrDatabaseTest {

    private lateinit var db: SpendrDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpendrDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun transactionRoundTripPersistsPaidAndDiscountFields() = runBlocking {
        val categoryId = db.categoryDao().insert(category())
        db.transactionDao().insert(
            TransactionEntity(
                paidAmount = 40_000,
                originalAmount = 50_000,
                discountAmount = 10_000,
                discountType = "fixed",
                categoryId = categoryId,
                note = "Kopi premium",
                merchant = "Kopi Kenangan",
                tags = null,
                date = 1_000,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        db.transactionDao().insert(
            TransactionEntity(
                paidAmount = 18_000,
                originalAmount = null,
                discountAmount = null,
                discountType = null,
                categoryId = categoryId,
                note = "Coffee",
                merchant = null,
                tags = null,
                date = 2_000,
                createdAt = 0,
                updatedAt = 0,
            ),
        )

        val rows = db.transactionDao().observeTransactionsWithCategory().first()

        assertEquals(2, rows.size)
        assertEquals("newest transaction first", 18_000L, rows[0].transaction.paidAmount)
        val discounted = rows.first { it.transaction.paidAmount == 40_000L }
        assertEquals(50_000L, discounted.transaction.originalAmount)
        assertEquals(10_000L, discounted.transaction.discountAmount)
        assertEquals("fixed", discounted.transaction.discountType)
        assertEquals(
            "paidAmount = originalAmount - discountAmount",
            discounted.transaction.originalAmount!! - discounted.transaction.discountAmount!!,
            discounted.transaction.paidAmount,
        )
        assertEquals("Food", discounted.categoryName)
        assertEquals("Kopi Kenangan", discounted.transaction.merchant)
    }

    @Test
    fun schemaCoversAllFiveTables() {
        db.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        ).use { cursor ->
            val tables = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            assertTrue(
                "missing tables: ${listOf("transactions", "categories", "merchants", "quick_add", "note_stats") - tables}",
                setOf("transactions", "categories", "merchants", "quick_add", "note_stats").all { it in tables },
            )
        }
    }

    @Test
    fun seedPopulatesCategoriesQuickAddsAndTransactions() = runBlocking {
        DbSeed.seedIfEmpty(db)

        val categories = db.categoryDao().observeCategories().first()
        val quickAdds = db.quickAddDao().observeQuickAdds().first()
        val transactions = db.transactionDao().observeTransactionsWithCategory().first()

        assertEquals(9, categories.size)
        assertEquals(3, quickAdds.size)
        assertEquals(5, transactions.size)
        assertTrue(categories.all { it.isDefault })
    }

    private fun category() = CategoryEntity(
        name = "Food",
        icon = "silverware-fork-knife",
        color = "#A86829",
        sortOrder = 0.0,
        isDefault = true,
        createdAt = 0,
        updatedAt = 0,
    )
}
