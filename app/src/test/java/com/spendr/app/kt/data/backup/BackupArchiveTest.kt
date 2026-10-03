package com.spendr.app.kt.data.backup

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupArchiveTest {

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

    private fun sampleBackupJson(): org.json.JSONObject {
        val backup = org.json.JSONObject()
            .put(
                "manifest",
                org.json.JSONObject()
                    .put("format", "spendr_backup")
                    .put("version", 1)
                    .put("createdAt", 1_700_000_000_000)
                    .put(
                        "counts",
                        org.json.JSONObject()
                            .put("categories", 1)
                            .put("transactions", 1)
                            .put("quickAdd", 0),
                    ),
            )
            .put(
                "categories",
                org.json.JSONArray()
                    .put(
                        org.json.JSONObject()
                            .put("id", 1)
                            .put("name", "Food")
                            .put("icon", "food")
                            .put("color", "#A86086")
                            .put("sortOrder", 0.0)
                            .put("isDefault", true)
                            .put("createdAt", 1)
                            .put("updatedAt", 2),
                    ),
            )
            .put(
                "transactions",
                org.json.JSONArray()
                    .put(
                        org.json.JSONObject()
                            .put("id", 1)
                            .put("paidAmount", 5000)
                            .put("originalAmount", org.json.JSONObject.NULL)
                            .put("discountAmount", org.json.JSONObject.NULL)
                            .put("discountType", org.json.JSONObject.NULL)
                            .put("categoryId", 1)
                            .put("note", "Coffee")
                            .put("merchant", org.json.JSONObject.NULL)
                            .put("tags", org.json.JSONObject.NULL)
                            .put("date", 3)
                            .put("createdAt", 3)
                            .put("updatedAt", 3),
                    ),
            )
            .put("quickAdd", org.json.JSONArray())
        return backup
    }

    @Test
    fun `round-trips a complete backup through a zip archive`() {
        val backup = sampleBackupJson()
        val bytes = BackupArchive.encode(
            backup.getJSONObject("manifest"),
            backup.getJSONArray("categories"),
            backup.getJSONArray("transactions"),
            backup.getJSONArray("quickAdd"),
        )
        val decoded = BackupArchive.decode(bytes)

        assertEquals(
            "spendr_backup",
            decoded.getJSONObject("manifest").getString("format"),
        )
        assertEquals(1, decoded.getJSONObject("manifest").getInt("version"))
        assertEquals(1, decoded.getJSONArray("categories").length())
        val category = decoded.getJSONArray("categories").getJSONObject(0)
        assertEquals("Food", category.getString("name"))
        assertEquals(true, category.getBoolean("isDefault"))
        val transaction = decoded.getJSONArray("transactions").getJSONObject(0)
        assertEquals(5000L, transaction.getLong("paidAmount"))
        assertEquals("Coffee", transaction.getString("note"))
        assertEquals(true, transaction.isNull("merchant"))
    }

    @Test
    fun `rejects junk bytes as unsupported`() {
        assertThrows(BackupException::class.java) {
            BackupArchive.decode("not a zip".toByteArray())
        }
    }

    @Test
    fun `uses the snake_case filename pattern`() {
        assertEquals(
            "spendr_backup_2026_07_10_143005.zip",
            BackupArchive.archiveFileName(
                java.time.LocalDateTime.of(2026, 7, 10, 14, 30, 5)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
    }

    @Test
    fun `restore replays ids and rebuilds derived tables`() = runBlocking {
        val backup = sampleBackupJson()
        val bytes = BackupArchive.encode(
            backup.getJSONObject("manifest"),
            backup.getJSONArray("categories"),
            backup.getJSONArray("transactions"),
            backup.getJSONArray("quickAdd"),
        )

        val count = restoreBackup(db, bytes)
        assertEquals(1, count)

        val tx = db.transactionDao().getById(1)!!
        assertEquals("Coffee", tx.note)
        assertEquals(5000L, tx.paidAmount)
        val category = db.categoryDao().list().single()
        assertEquals("Food", category.name)

        // Derived tables rebuilt
        val stats = mutableListOf<Array<Any>>()
        db.openHelper.writableDatabase.query("SELECT note, paid_amount, use_count FROM note_stats").use { cursor ->
            while (cursor.moveToNext()) {
                stats.add(arrayOf(cursor.getString(0), cursor.getLong(1), cursor.getLong(2)))
            }
        }
        assertEquals(1, stats.size)
        assertEquals("Coffee", stats[0][0])
        assertEquals(5000L, stats[0][1])
        assertEquals(1L, stats[0][2])

        val merchants = mutableListOf<String>()
        db.openHelper.writableDatabase.query("SELECT name FROM merchants").use { cursor ->
            while (cursor.moveToNext()) merchants.add(cursor.getString(0))
        }
        assertEquals(0, merchants.size)
    }

    @Test
    fun `create produces an archive the RN decoder rules accept`() = runBlocking {
        db.categoryDao().insertWithId(
            CategoryEntity(
                id = 1,
                name = "Food",
                icon = "food",
                color = "#A86086",
                sortOrder = 0.0,
                isDefault = true,
                createdAt = 1,
                updatedAt = 2,
            ),
        )
        db.transactionDao().insert(
            TransactionEntity(
                paidAmount = 5000,
                originalAmount = null,
                discountAmount = null,
                discountType = null,
                categoryId = 1,
                note = "Coffee",
                merchant = null,
                tags = null,
                date = 3,
                createdAt = 3,
                updatedAt = 3,
            ),
        )
        val (fileName, bytes, txCount) = createBackup(db)
        assertEquals(1, txCount)
        assertTrue(fileName.startsWith("spendr_backup_"))
        // RN decoder requires: >=30 bytes, local header signature, stored method
        val signature = bytes.sliceArray(0..3)
        assertEquals(0x50, signature[0].toInt() and 0xFF)
        assertEquals(0x4B, signature[1].toInt() and 0xFF)
        assertEquals(0x03, signature[2].toInt() and 0xFF)
        assertEquals(0x04, signature[3].toInt() and 0xFF)
        assertEquals(0, bytes[8].toInt() and 0xFF)

        val restored = restoreBackup(db, bytes)
        assertEquals(1, restored)
    }

    @Test
    fun `restore tolerates orphan transactions like real RN data`() = runBlocking {
        // Real RN databases can contain transactions whose categoryId points at
        // a deleted category (FK enforcement was off on the RN connection).
        val backup = org.json.JSONObject()
            .put(
                "manifest",
                org.json.JSONObject()
                    .put("format", "spendr_backup")
                    .put("version", 1)
                    .put("createdAt", 1L)
                    .put("counts", org.json.JSONObject().put("categories", 1).put("transactions", 1).put("quickAdd", 0)),
            )
            .put(
                "categories",
                org.json.JSONArray().put(
                    org.json.JSONObject()
                        .put("id", 1).put("name", "Food").put("icon", "food").put("color", "#A86086")
                        .put("sortOrder", 0).put("isDefault", true).put("createdAt", 1L).put("updatedAt", 2L),
                ),
            )
            .put(
                "transactions",
                org.json.JSONArray()
                    .put(
                        org.json.JSONObject()
                            .put("id", 1).put("paidAmount", 5000)
                            .put("originalAmount", org.json.JSONObject.NULL)
                            .put("discountAmount", org.json.JSONObject.NULL)
                            .put("discountType", org.json.JSONObject.NULL)
                            .put("categoryId", 1).put("note", "Coffee")
                            .put("merchant", org.json.JSONObject.NULL)
                            .put("tags", org.json.JSONObject.NULL)
                            .put("date", 3L).put("createdAt", 3L).put("updatedAt", 3L),
                    )
                    .put(
                        org.json.JSONObject()
                            .put("id", 2).put("paidAmount", 7000)
                            .put("originalAmount", org.json.JSONObject.NULL)
                            .put("discountAmount", org.json.JSONObject.NULL)
                            .put("discountType", org.json.JSONObject.NULL)
                            .put("categoryId", 99) // dangling
                            .put("note", "Orphan")
                            .put("merchant", org.json.JSONObject.NULL)
                            .put("tags", org.json.JSONObject.NULL)
                            .put("date", 4L).put("createdAt", 4L).put("updatedAt", 4L),
                    ),
            )
            .put("quickAdd", org.json.JSONArray())

        val bytes = BackupArchive.encode(
            backup.getJSONObject("manifest"),
            backup.getJSONArray("categories"),
            backup.getJSONArray("transactions"),
            backup.getJSONArray("quickAdd"),
        )
        val count = restoreBackup(db, bytes)
        assertEquals(2, count)
        assertEquals(2, db.transactionDao().listAllForBackup().size)
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
