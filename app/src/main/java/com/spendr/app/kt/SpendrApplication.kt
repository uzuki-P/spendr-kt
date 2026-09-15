package com.spendr.app.kt

import android.app.Application
import com.spendr.app.kt.data.settings.BackupFrequency
import com.spendr.app.kt.platform.AutomaticBackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpendrApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Re-sync the automatic backup task with the stored frequency; the
        // WorkManager job itself survives restarts, this only reconciles.
        CoroutineScope(Dispatchers.Default).launch {
            val settings = container.settings.settings.first()
            AutomaticBackupWorker.sync(this@SpendrApplication, settings.backupFrequency != BackupFrequency.OFF)
        }
    }

    companion object {
        /** Latest cold/warm deep link URI, consumed by the nav host. */
        val pendingDeepLink = MutableStateFlow<String?>(null)
    }
}
