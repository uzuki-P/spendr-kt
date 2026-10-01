package com.spendr.app.kt.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendr.app.kt.data.backup.createBackup
import com.spendr.app.kt.data.backup.restoreBackup
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.csv.CsvFormat
import com.spendr.app.kt.data.csv.ImportExportRepository
import com.spendr.app.kt.domain.model.DateRange
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
@Config(sdk = [34])
class ReceiptRepositoryTest {
    private lateinit var db: SpendrDatabase
    private lateinit var transactions: TransactionRepository
    private lateinit var receipts: ReceiptRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SpendrDatabase::class.java,
        ).allowMainThreadQueries().build()
        transactions = TransactionRepository(db)
        receipts = ReceiptRepository(db, transactions)
    }

    @After fun tearDown() = db.close()

    @Test fun receiptCountsOnceAndItemsAreSearchable() = runBlocking {
        val category = CategoryRepository(db).createCategory("Shopping", "cart", "#A86086")
        val id = receipts.save(ReceiptInput(
            paidAmount = 100_000,
            categoryId = category,
            merchant = "Supermarket",
            note = null,
            date = 1_000,
            items = listOf(ReceiptItemInput("Milk", 18_000), ReceiptItemInput("Rice", 79_350, "0.5")),
        ))
        assertEquals("receipt", transactions.getTransaction(id)?.type)
        assertEquals(100_000L, transactions.totalInRange(DateRange(0, 2_000)).total)
        assertEquals(listOf(id), transactions.listTransactions(TransactionFilters(search = "milk"))
            .map { it.transaction.id })
        assertEquals(2_650L, 100_000 - receipts.items(id).sumOf { it.paidAmount })
        assertEquals("0.5", receipts.items(id).last().quantity)

        val duplicate = transactions.duplicateTransaction(id)!!
        assertEquals(2, receipts.items(duplicate).size)
        transactions.deleteTransaction(id)
        assertTrue(receipts.items(id).isEmpty())
    }

    @Test fun backupPreservesReceiptTypeAndItems() = runBlocking {
        val category = CategoryRepository(db).createCategory("Shopping", "cart", "#A86086")
        val id = receipts.save(ReceiptInput(50_000, category, "Market", null, 1_000,
            listOf(ReceiptItemInput("Eggs", 46_000, "2"))))
        val bytes = createBackup(db).second
        restoreBackup(db, bytes)
        assertEquals("receipt", transactions.getTransaction(id)?.type)
        assertEquals("Eggs", receipts.items(id).single().name)
        assertEquals("2", receipts.items(id).single().quantity)
    }

    @Test fun spendrCsvPreservesReceiptItems() = runBlocking {
        val categories = CategoryRepository(db)
        val category = categories.createCategory("Shopping", "cart", "#A86086")
        val id = receipts.save(ReceiptInput(50_000, category, "Market", "Groceries", 1_000,
            listOf(ReceiptItemInput("Eggs", 46_000, "2"))))
        val csv = ImportExportRepository(db, transactions, categories)
            .exportTransactionsToCsv(CsvFormat.SPENDR).csv
        transactions.deleteTransaction(id)
        val imported = ImportExportRepository(db, transactions, categories).importCsv(csv, CsvFormat.SPENDR)
        assertEquals(1, imported.added)
        val restored = transactions.listTransactions().single().transaction
        assertEquals("receipt", restored.type)
        assertEquals("Eggs", receipts.items(restored.id).single().name)
    }
}
