package com.spendr.app.kt.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spendr.app.kt.SpendrApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Automatic backup, ported from RN `backup/backgroundTask.ts`: Android runs
 * background work when the system allows it, so the worker checks `dueForBackup`
 * (frequency vs. lastBackupAt) before writing — a backup may run later than its
 * due time.
 */
class AutomaticBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SpendrApplication).container
        val settings = container.settings.settings.first()
        if (!BackupStorage.dueForBackup(settings)) return Result.success()
        val folder = settings.backupDirectoryUri ?: return Result.success()
        return try {
            val (_, createdAt, _) = BackupStorage.writeBackupToDirectory(
                applicationContext,
                container.database,
                folder,
                settings.backupRotation,
            )
            container.settings.setBackup(lastBackupAt = createdAt)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "spendr_automatic_backup"

        /** Registers/cancels the periodic 12-hour check, like `syncAutomaticBackupTask`. */
        fun sync(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(12, TimeUnit.HOURS).build()
                workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            } else {
                workManager.cancelUniqueWork(UNIQUE_NAME)
            }
        }
    }
}
