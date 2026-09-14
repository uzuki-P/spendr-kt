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
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.db.entity.MerchantEntity
import com.spendr.app.kt.data.db.entity.NoteStatEntity
import com.spendr.app.kt.data.db.entity.QuickAddEntity
import com.spendr.app.kt.data.db.entity.TransactionEntity

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
    ],
    version = 1,
    exportSchema = true,
)
abstract class SpendrDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun quickAddDao(): QuickAddDao
    abstract fun noteStatDao(): NoteStatDao

    companion object {
        fun build(context: Context): SpendrDatabase =
            Room.databaseBuilder(context, SpendrDatabase::class.java, "spendr.db")
                .build()
    }
}
