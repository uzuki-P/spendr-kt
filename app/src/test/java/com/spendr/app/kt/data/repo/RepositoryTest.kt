package com.spendr.app.kt.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendr.app.kt.data.csv.CsvFormat
import com.spendr.app.kt.data.csv.ImportExportRepository
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.domain.model.TransactionInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var db: SpendrDatabase
    private lateinit var transactions: TransactionRepository
    private lateinit var categories: CategoryRepository
    private lateinit var importExport: ImportExportRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpendrDatabase::class.java,
        ).allowMainThreadQueries().build()
        transactions = TransactionRepository(db)
        categories = CategoryRepository(db)
        importExport = ImportExportRepository(db, transactions, categories)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun input(
        paid: Long = 20_000,
        note: String? = "Coffee",
        merchant: String? = "Kopi Kenangan",
        categoryId: Long = 1,
        discount: Triple<Long, Long, String>? = null,
    ) = TransactionInput(
        paidAmount = paid,
        originalAmount = discount?.first,
        discountAmount = discount?.second,
        discountType = discount?.third,
        categoryId = categoryId,
        note = note,
        merchant = merchant,
        tags = null,
        date = 1_783_560_600_000,
    )

    @Test
    fun `insert recomputes note stats and upserts merchant`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val id = transactions.insertTransaction(input(categoryId = categoryId))

        val stats = db.noteStatDao().list()
        assertEquals(1, stats.size)
        assertEquals("Coffee", stats[0].note)
        assertEquals(20_000L, stats[0].paidAmount)
        assertEquals(1L, stats[0].useCount)

        val merchants = mutableListOf<String>()
        db.openHelper.writableDatabase.query("SELECT name FROM merchants").use { c ->
            while (c.moveToNext()) merchants.add(c.getString(0))
        }
        assertEquals(listOf("Kopi Kenangan"), merchants)
    }

    @Test
    fun `update recomputes stats for old and new note`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val id = transactions.insertTransaction(input(note = "Kopi"))
        transactions.insertTransaction(input(note = "Kopi"))
        transactions.updateTransaction(id, input(note = "Coffee", categoryId = categoryId))

        val stats = db.noteStatDao().list().sortedBy { it.note }
        assertEquals(2, stats.size)
        assertEquals(1L, stats.first { it.note == "Coffee" }.useCount)
        assertEquals(1L, stats.first { it.note == "Kopi" }.useCount)
    }

    @Test
    fun `delete removes the note stat for a single-occurrence note`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val id = transactions.insertTransaction(input(note = "Lunch"))
        transactions.deleteTransaction(id)
        assertEquals(0, db.noteStatDao().list().size)
    }

    @Test
    fun `duplicate copies with date now`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val id = transactions.insertTransaction(input())
        val before = System.currentTimeMillis()
        val newId = transactions.duplicateTransaction(id)!!
        val copy = transactions.getTransaction(newId)!!
        org.junit.Assert.assertNotEquals(id, newId)
        org.junit.Assert.assertTrue(copy.date >= before)
        assertEquals(20_000L, copy.paidAmount)
    }

    @Test
    fun `totals and daily cumulative respect the range`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val hour = 60 * 60 * 1000L
        val monthStart = com.spendr.app.kt.domain.monthRange(1_783_560_600_000L).start
        transactions.insertTransaction(
            input(paid = 10_000, categoryId = categoryId, note = null).copy(date = monthStart + hour),
        )
        transactions.insertTransaction(
            input(paid = 5_000, categoryId = categoryId, note = null).copy(
                date = monthStart + 24 * hour + hour,
            ),
        )
        val range = com.spendr.app.kt.domain.monthRange(monthStart)
        val total = transactions.totalInRange(range)
        assertEquals(15_000L, total.total)
        assertEquals(2L, total.count)

        val cumulative = transactions.cumulativeDailyTotals(range, 3)
        assertEquals(listOf(10_000L, 15_000L, 15_000L), cumulative)
    }

    @Test
    fun `spendr csv import creates categories and dedupes`() = runBlocking {
        val csv = """
            Spendr Version,Transaction Date,Paid Amount,Original Amount,Discount Amount,Discount Type,Category,Category Icon,Category Color,Note,Merchant,Tags,Created At,Updated At
            1,1783560600000,18000,20000,2000,fixed,Snacks,tag,#B05E6B,Coffee,Kopi Place,,10,20
            1,1783560600000,18000,20000,2000,fixed,Snacks,tag,#B05E6B,Coffee,Kopi Place,,10,20
            1,1783560700000,25000,,,,Groceries,,,Weekly shop,Indomaret,,10,20
        """.trimIndent().replace("\n", "\r\n")

        val result = importExport.importCsv(csv, CsvFormat.SPENDR)
        assertEquals(2, result.added)
        assertEquals(1, result.skipped)
        assertEquals(2, result.categoriesCreated)

        val all = transactions.listTransactions()
        assertEquals(2, all.size)
        val coffee = all.first { it.transaction.note == "Coffee" }
        assertEquals(20_000L, coffee.transaction.originalAmount)
        assertEquals(2_000L, coffee.transaction.discountAmount)
        assertEquals("fixed", coffee.transaction.discountType)
        assertEquals("Snacks", coffee.categoryName)
        assertEquals("Kopi Place", coffee.transaction.merchant)
        assertNotNull(db.merchantDao().list().firstOrNull { it.name == "Kopi Place" })
    }

    @Test
    fun `moneylover import maps negative amounts and never sets merchant`() = runBlocking {
        val csv = "ID,Note,Amount,Category,Account,Currency,Date,Event,Exclude Report\r\n" +
            "1,Jajan susu,-107000,Shopping,Eceknya Bank,IDR,09/07/2026,,False"
        val result = importExport.importCsv(csv, CsvFormat.MONEY_LOVER)
        assertEquals(1, result.added)
        val tx = transactions.listTransactions().single()
        assertEquals(107_000L, tx.transaction.paidAmount)
        assertEquals("Shopping", tx.categoryName)
        assertEquals(null, tx.transaction.merchant)
        // local noon
        assertEquals(12, java.time.Instant.ofEpochMilli(tx.transaction.date)
            .atZone(java.time.ZoneId.systemDefault()).hour)
    }

    @Test
    fun `export and import round-trip spendr csv`() = runBlocking {
        val categoryId = categories.createCategory("Snack & Coffee", "coffee-outline", "#A86086")
        transactions.insertTransaction(
            input(paid = 18_000, categoryId = categoryId).copy(
                originalAmount = 20_000,
                discountAmount = 2_000,
                discountType = "fixed",
            ),
        )
        val export = importExport.exportTransactionsToCsv(CsvFormat.SPENDR)
        assertEquals(1, export.count)

        val result = importExport.importCsv(export.csv, CsvFormat.SPENDR)
        // dedupe (local day + amount + note + category) catches the re-import
        assertEquals(0, result.added)
        assertEquals(1, result.skipped)
        assertEquals(0, result.categoriesCreated)
        val rows = transactions.listTransactions()
        assertEquals(1, rows.size)
        assertEquals("Snack & Coffee", rows.single().categoryName)
    }

    @Test
    fun `recent list is newest-first with limit`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        val day = 24 * 60 * 60 * 1000L
        val base = 1_783_560_600_000L
        transactions.insertTransaction(input(categoryId = categoryId).copy(date = base))
        transactions.insertTransaction(input(categoryId = categoryId).copy(date = base + day))
        transactions.insertTransaction(input(categoryId = categoryId).copy(date = base + 2 * day))
        val recent = transactions.listTransactions(TransactionFilters(limit = 2))
        assertEquals(2, recent.size)
        org.junit.Assert.assertTrue(recent[0].transaction.date > recent[1].transaction.date)
    }

    @Test
    fun `search filters by note and merchant`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        transactions.insertTransaction(input(note = "Kopi premium", merchant = "Kopi Kenangan", categoryId = categoryId))
        transactions.insertTransaction(input(note = "Lunch", merchant = "Warung", categoryId = categoryId))

        val byNote = transactions.listTransactions(TransactionFilters(search = "premium"))
        assertEquals(1, byNote.size)
        val byMerchant = transactions.listTransactions(TransactionFilters(search = "warung"))
        assertEquals(1, byMerchant.size)
        assertEquals(0, transactions.listTransactions(TransactionFilters(search = "bazaar")).size)
    }

    @Test
    fun `suggestions by amount return ranked candidates`() = runBlocking {
        val categoryId = categories.createCategory("Food", "tag", "#A86086")
        transactions.insertTransaction(input(paid = 18_000, note = "Coffee", categoryId = categoryId))
        transactions.insertTransaction(input(paid = 18_000, note = "Coffee", categoryId = categoryId))
        transactions.insertTransaction(input(paid = 25_000, note = "Lunch", categoryId = categoryId))

        val byAmount = transactions.noteSuggestionsByAmount(18_000)
        assertEquals(1, byAmount.size)
        assertEquals("Coffee", byAmount[0].note)
        assertEquals(2L, byAmount[0].usageCount)

        val popular = transactions.noteSuggestions()
        assertEquals(2, popular.size)
        assertEquals("Coffee", popular[0].note)
    }
}
