package com.spendr.app.kt.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.spendr.app.kt.data.backup.createBackup
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.settings.BackupFrequency
import com.spendr.app.kt.data.settings.Settings

/**
 * SAF archive storage, ported from RN `backup/backupStorage.ts`: write a
 * Spendr ZIP into the chosen backup folder, prune older archives beyond the
 * rotation, and decide when an automatic backup is due.
 */
object BackupStorage {

    /** { daily: 1d, weekly: 7d, monthly: 30d } — same constants as the OG. */
    fun dueForBackup(settings: Settings, now: Long = System.currentTimeMillis()): Boolean {
        if (settings.backupFrequency == BackupFrequency.OFF || settings.backupDirectoryUri == null) return false
        if (settings.lastBackupAt == null) return true
        val milliseconds = when (settings.backupFrequency) {
            BackupFrequency.DAILY -> 86_400_000L
            BackupFrequency.WEEKLY -> 604_800_000L
            BackupFrequency.MONTHLY -> 2_592_000_000L
            BackupFrequency.OFF -> return false
        }
        return now - settings.lastBackupAt >= milliseconds
    }

    /**
     * `content://…/tree/primary%3ADocuments%2Fspendr_backups` →
     * `Documents/spendr_backups`; falls back to the raw URI.
     */
    fun describeBackupDirectory(uri: String?): String {
        if (uri == null) return DEFAULT_BACKUP_FOLDER_LABEL
        val treeSegment = Uri.decode(uri).split("/tree/").getOrNull(1) ?: return uri
        val colon = treeSegment.indexOf(':')
        return if (colon >= 0) treeSegment.substring(colon + 1) else treeSegment
    }

    const val DEFAULT_BACKUP_FOLDER_LABEL = "Documents/spendr_backups"

    /** Writes `spendr_backup_<stamp>.zip` into the folder, then applies rotation. */
    suspend fun writeBackupToDirectory(
        context: Context,
        db: SpendrDatabase,
        directoryUri: String,
        rotation: Int,
    ): Triple<String, Long, Int> {
        val resolver = context.contentResolver
        val treeUri = Uri.parse(directoryUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val (fileName, bytes, count) = createBackup(db)
        val fileUri = DocumentsContract.createDocument(resolver, docUri, "application/zip", fileName)
            ?: throw IllegalStateException("Could not create the archive in the backup folder.")
        resolver.openOutputStream(fileUri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("Could not open the archive for writing.")
        pruneBackups(context, directoryUri, rotation)
        return Triple(fileName, System.currentTimeMillis(), count)
    }

    /** Deletes older `spendr_backup_*.zip` archives beyond [rotation]. */
    fun pruneBackups(context: Context, directoryUri: String, rotation: Int) {
        val resolver = context.contentResolver
        val treeUri = Uri.parse(directoryUri)
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        data class Entry(val documentId: String, val name: String)

        val backups = mutableListOf<Entry>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (name.contains("spendr_backup_") && name.endsWith(".zip")) {
                    backups += Entry(cursor.getString(0), name)
                }
            }
        }
        val keep = maxOf(1, rotation)
        backups.sortBy { it.name }
        backups.dropLast(keep).forEach { entry ->
            runCatching {
                DocumentsContract.deleteDocument(
                    resolver,
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.documentId),
                )
            }
        }
    }
}
