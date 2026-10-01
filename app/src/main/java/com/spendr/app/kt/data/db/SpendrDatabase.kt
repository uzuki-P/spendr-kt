package com.spendr.app.kt.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spendr.app.kt.data.db.dao.CategoryDao
import com.spendr.app.kt.data.db.dao.MerchantDao
import com.spendr.app.kt.data.db.dao.NoteStatDao
import com.spendr.app.kt.data.db.dao.QuickAddDao
import com.spendr.app.kt.data.db.dao.TransactionDao
import com.spendr.app.kt.data.db.dao.ReceiptItemDao
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.MerchantEntity
import com.spendr.app.kt.data.db.entity.NoteStatEntity
import com.spendr.app.kt.data.db.entity.QuickAddEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity
import com.spendr.app.kt.data.db.entity.ReceiptItemEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Baseline schema = the RN app's final schema (user_version 4), per ADR-0004.
 * Schema evolution restarts at Room version 1; no RN migration history is replayed.
 */
@Database(
    entities = [
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class,
        QuickAddEntity::class,
        NoteStatEntity::class,
        ReceiptItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class SpendrDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun quickAddDao(): QuickAddDao
    abstract fun noteStatDao(): NoteStatDao
    abstract fun receiptItemDao(): ReceiptItemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN type TEXT NOT NULL DEFAULT 'standard'")
                db.execSQL("""CREATE TABLE IF NOT EXISTS receipt_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transaction_id INTEGER NOT NULL, name TEXT NOT NULL, paid_amount INTEGER NOT NULL, quantity INTEGER NOT NULL, sort_order INTEGER NOT NULL, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_items_transaction_id ON receipt_items(transaction_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_items_name ON receipt_items(name)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE receipt_items_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transaction_id INTEGER NOT NULL, name TEXT NOT NULL, paid_amount INTEGER NOT NULL, quantity TEXT NOT NULL, sort_order INTEGER NOT NULL, FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("INSERT INTO receipt_items_new (id, transaction_id, name, paid_amount, quantity, sort_order) SELECT id, transaction_id, name, paid_amount, CAST(quantity AS TEXT), sort_order FROM receipt_items")
                db.execSQL("DROP TABLE receipt_items")
                db.execSQL("ALTER TABLE receipt_items_new RENAME TO receipt_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_items_transaction_id ON receipt_items(transaction_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_items_name ON receipt_items(name)")
            }
        }

        fun build(context: Context): SpendrDatabase =
            Room.databaseBuilder(context, SpendrDatabase::class.java, "spendr.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
