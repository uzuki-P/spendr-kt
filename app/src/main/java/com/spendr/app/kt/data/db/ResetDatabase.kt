package com.spendr.app.kt.data.db

import com.spendr.app.kt.data.repo.TransactionRepository

/**
 * Wipes all domain tables and restores defaults, port of RN `resetDatabase.ts`.
 */
suspend fun resetDomainDatabase(db: SpendrDatabase) {
    val sql = db.openHelper.writableDatabase
    sql.execSQL("DELETE FROM quick_add")
    sql.execSQL("DELETE FROM transactions")
    sql.execSQL("DELETE FROM note_stats")
    sql.execSQL("DELETE FROM merchants")
    sql.execSQL("DELETE FROM categories")
    sql.execSQL(
        "DELETE FROM sqlite_sequence WHERE name IN ('transactions', 'note_stats', 'merchants', 'quick_add', 'categories')",
    )
    DbSeed.seedDefaults(db)
    TransactionRepository(db).rebuildNoteStats()
}
