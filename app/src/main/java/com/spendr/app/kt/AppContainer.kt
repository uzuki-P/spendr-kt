package com.spendr.app.kt

import android.app.Application
import com.spendr.app.kt.data.backup.createBackup
import com.spendr.app.kt.data.csv.ImportExportRepository
import com.spendr.app.kt.data.db.DbSeed
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.repo.CategoryRepository
import com.spendr.app.kt.data.repo.QuickAddRepository
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.data.repo.ReceiptRepository
import com.spendr.app.kt.data.settings.SettingsRepository
import com.spendr.app.kt.platform.SpendrVibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Application) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: SpendrDatabase = SpendrDatabase.build(context)

    val transactions = TransactionRepository(database)
    val receipts = ReceiptRepository(database, transactions)
    val categories = CategoryRepository(database)
    val quickAdds = QuickAddRepository(database)
    val importExport = ImportExportRepository(database, transactions, categories)
    val settings = SettingsRepository(context)
    val vibrator = SpendrVibrator(context)

    /** Id of the most recently saved Transaction; drives the Home row pulse. */
    val lastAddedTransactionId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    init {
        appScope.launch {
            DbSeed.seedIfEmpty(database)
        }
    }
}
