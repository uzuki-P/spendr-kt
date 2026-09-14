package com.spendr.app.kt.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.backup.BackupArchive
import com.spendr.app.kt.data.backup.BackupException
import com.spendr.app.kt.data.backup.createBackup
import com.spendr.app.kt.data.backup.restoreBackup
import com.spendr.app.kt.data.csv.CsvFormat
import com.spendr.app.kt.data.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backup & restore + CSV transfer, ported from RN `BackupRestoreScreen`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsState(initial = Settings())

    var confirmExport by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf(CsvFormat.SPENDR) }

    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val format = pendingExportFormat
                val result = container.importExport.exportTransactionsToCsv(format)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(result.csv.toByteArray()) }
                }
                toast("Exported ${result.count} transaction(s).")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }
                if (content == null) {
                    toast("Could not read the selected file.")
                    return@launch
                }
                val format = if (content.trimStart('\uFEFF').startsWith("Spendr Version", ignoreCase = true)) {
                    CsvFormat.SPENDR
                } else {
                    CsvFormat.MONEY_LOVER
                }
                try {
                    val result = container.importExport.importCsv(content, format)
                    toast(
                        "Imported ${result.added}, skipped ${result.skipped}, " +
                            "${result.categoriesCreated} new category/categories.",
                    )
                } catch (e: Exception) {
                    toast("Import failed: ${e.message}")
                }
            }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val (fileName, bytes, count) = createBackup(container.database)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                    container.settings.setBackup(lastBackupAt = System.currentTimeMillis())
                    toast("Backup saved as $fileName ($count transactions).")
                } catch (e: Exception) {
                    toast("Backup failed: ${e.message}")
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    if (bytes == null) {
                        toast("Could not read the selected file.")
                        return@launch
                    }
                    val count = restoreBackup(container.database, bytes)
                    toast("Restored $count transaction(s).")
                } catch (e: BackupException) {
                    toast("Restore failed: ${e.message}")
                } catch (e: Exception) {
                    toast("Restore failed: ${e.message}")
                }
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TopAppBar(title = { Text("Backup & restore", fontWeight = FontWeight.Bold) })
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Archive backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Back up now", fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text(
                                    settings.lastBackupAt?.let {
                                        "Last backup " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
                                    } ?: "Create a complete Spendr archive",
                                )
                            },
                            modifier = Modifier.clickable { backupLauncher.launch(BackupArchive.archiveFileName()) },
                        )
                        ListItem(
                            headlineContent = { Text("Restore backup", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Replace local data from a Spendr ZIP archive") },
                            modifier = Modifier.clickable { confirmRestore = true },
                        )
                    }
                }

                Text("CSV transfer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Export CSV", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Spendr or Money Lover format") },
                            modifier = Modifier.clickable { confirmExport = true },
                        )
                        ListItem(
                            headlineContent = { Text("Import CSV", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Merge Spendr or Money Lover transactions") },
                            modifier = Modifier.clickable { confirmImport = true },
                        )
                    }
                }

                Text(
                    "Import merges new Transactions; existing matching rows " +
                        "(same day, amount, note, and category) are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmExport) {
        FormatDialog(
            title = "Export CSV",
            body = "Spendr CSV preserves Spendr fields. Money Lover CSV is for interoperability.",
            onCancel = { confirmExport = false },
            onSpendr = {
                confirmExport = false
                pendingExportFormat = CsvFormat.SPENDR
                exportLauncher.launch(suggestedCsvName("spendr"))
            },
            onMoneyLover = {
                confirmExport = false
                pendingExportFormat = CsvFormat.MONEY_LOVER
                exportLauncher.launch(suggestedCsvName("spendr_money_lover"))
            },
        )
    }
    if (confirmImport) {
        FormatDialog(
            title = "Import CSV",
            body = "Import merges new Transactions; existing matching rows are skipped.",
            onCancel = { confirmImport = false },
            onSpendr = {
                confirmImport = false
                importLauncher.launch(arrayOf("text/csv", "text/plain", "*/*"))
            },
            onMoneyLover = {
                confirmImport = false
                importLauncher.launch(arrayOf("text/csv", "text/plain", "*/*"))
            },
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore backup?") },
            text = {
                Text("This replaces all local Transactions, Categories, and QuickAdd shortcuts. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restoreLauncher.launch(arrayOf("application/zip", "*/*"))
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FormatDialog(
    title: String,
    body: String,
    onCancel: () -> Unit,
    onSpendr: () -> Unit,
    onMoneyLover: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                TextButton(onClick = onSpendr) { Text("Spendr") }
                TextButton(onClick = onMoneyLover) { Text("Money Lover") }
            }
        },
    )
}

private fun suggestedCsvName(prefix: String): String {
    val stamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .format(java.time.LocalDate.now())
    return "${prefix}_$stamp.csv"
}
