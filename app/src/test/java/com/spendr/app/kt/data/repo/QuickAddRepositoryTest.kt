package com.spendr.app.kt.data.repo

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.domain.model.TransactionInput
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuickAddRepositoryTest {

    private val day = 86_400_000L
    private var now = 1_783_560_600_000L

    private lateinit var db: SpendrDatabase
    private lateinit var transactions: TransactionRepository
    private lateinit var quickAdds: QuickAddRepository
    private var categoryId = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpendrDatabase::class.java,
        ).allowMainThreadQueries().build()
        transactions = TransactionRepository(db)
        quickAdds = QuickAddRepository(db) { now }
        categoryId = CategoryRepository(db).createCategory("Food", "tag", "#A86086")
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun spend(note: String, date: Long, paid: Long = 20_000) {
        transactions.insertTransaction(
            TransactionInput(
                paidAmount = paid,
                originalAmount = null,
                discountAmount = null,
                discountType = null,
                categoryId = categoryId,
                note = note,
                merchant = null,
                tags = null,
                date = date,
            ),
        )
    }

    @Test
    fun `suggests notes repeated in the last 6 months only`() = runBlocking {
        spend("Coffee", now - day)
        spend("Coffee", now - 2 * day)
        spend("Old habit", now - 200 * day)
        spend("Old habit", now - 210 * day)

        val suggestions = quickAdds.frequentSpending()

        assertEquals(listOf("Coffee"), suggestions.map { it.note })
        assertEquals(2L, suggestions[0].useCount)
    }

    @Test
    fun `caches suggestions in the database until the day changes or the cache is cleared`() = runBlocking {
        spend("Coffee", now - day)
        spend("Coffee", now - 2 * day)
        assertEquals(1, quickAdds.frequentSpending().size)
        assertEquals(listOf("Coffee"), db.quickAddDao().cachedSuggestions("2026-07-09").map { it.note })

        // A fresh repository on the same database reads the stored cache
        quickAdds = QuickAddRepository(db) { now }

        spend("Lunch", now - day)
        spend("Lunch", now - 2 * day)
        assertEquals(1, quickAdds.frequentSpending().size)

        db.quickAddDao().clearSuggestions()
        assertEquals(2, quickAdds.frequentSpending().size)

        spend("Bus", now - day)
        spend("Bus", now - 2 * day)
        now += day
        assertEquals(3, quickAdds.frequentSpending().size)
    }
}
