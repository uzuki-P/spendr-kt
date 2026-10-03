package com.spendr.app.kt.data.db

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReceiptMigrationTest {
    @Test fun migrationKeepsExistingTransactionAndAddsReceiptTables() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "receipt-migration-test.db"
        createOldDatabase(context, name, 1).use { old ->
            old.execSQL("INSERT INTO categories (id, name, icon, color, sort_order, is_default, created_at, updated_at) VALUES (1, 'Food', 'tag', '#A86086', 0, 1, 0, 0)")
            old.execSQL("INSERT INTO transactions (id, paid_amount, category_id, date, created_at, updated_at) VALUES (1, 12000, 1, 1000, 0, 0)")
        }
        val db = Room.databaseBuilder(context, SpendrDatabase::class.java, name)
            .addMigrations(SpendrDatabase.MIGRATION_1_2, SpendrDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("standard", db.transactionDao().getById(1)?.type)
            assertTrue(db.receiptItemDao().listForTransaction(1).isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migrationConvertsExistingWholeQuantitiesToText() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "receipt-quantity-migration-test.db"
        createOldDatabase(context, name, 2).use { old ->
            old.execSQL("INSERT INTO categories (id, name, icon, color, sort_order, is_default, created_at, updated_at) VALUES (1, 'Shopping', 'cart', '#A86086', 0, 1, 0, 0)")
            old.execSQL("INSERT INTO transactions (id, paid_amount, category_id, date, created_at, updated_at, type) VALUES (1, 12000, 1, 1000, 0, 0, 'receipt')")
            old.execSQL("INSERT INTO receipt_items (id, transaction_id, name, paid_amount, quantity, sort_order) VALUES (1, 1, 'Eggs', 12000, 2, 0)")
        }
        val db = Room.databaseBuilder(context, SpendrDatabase::class.java, name)
            .addMigrations(SpendrDatabase.MIGRATION_1_2, SpendrDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("2", db.receiptItemDao().listForTransaction(1).single().quantity)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun createOldDatabase(context: Context, name: String, version: Int): SQLiteDatabase {
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.spendr.app.kt.data.db.SpendrDatabase/$version.json").readText())
            .getJSONObject("database")
        val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices")
            for (j in 0 until (indices?.length() ?: 0)) {
                old.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
        old.version = version
        return old
    }
}
