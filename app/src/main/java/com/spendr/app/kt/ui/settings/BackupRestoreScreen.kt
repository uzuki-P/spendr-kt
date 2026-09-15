package com.spendr.app.kt.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.backup.BackupException
import com.spendr.app.kt.data.backup.restoreBackup
import com.spendr.app.kt.data.csv.CsvFormat
import com.spendr.app.kt.data.settings.BackupFrequency
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.platform.AutomaticBackupWorker
import com.spendr.app.kt.platform.BackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backup & restore + CSV transfer, ported from RN `BackupRestoreScreen`:
 * archive folder, automatic backup, rotation, manual backup/restore, CSV in
 * Spendr or Money Lover format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsState(initial = Settings())

    var confirmExport by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var frequencyOpen by remember { mutableStateOf(false) }
    var rotationOpen by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf(CsvFormat.SPENDR) }
    var pendingFrequency by remember { mutableStateOf<BackupFrequency?>(null) }

    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            scope.launch {
                container.settings.setBackup(directoryUri = uri.toString())
                val frequency = pendingFrequency
                if (frequency != null) {
                    container.settings.setBackup(frequency = frequency)
                    AutomaticBackupWorker.sync(context, frequency != BackupFrequency.OFF)
                    pendingFrequency = null
                }
            }
        }
    }

    fun chooseFrequency(frequency: BackupFrequency) {
        frequencyOpen = false
        if (frequency != BackupFrequency.OFF && settings.backupDirectoryUri == null) {
            // Enabling automatic backup requires a folder first (RN setFrequency)
            pendingFrequency = frequency
            folderLauncher.launch(null)
            return
        }
        scope.launch {
            container.settings.setBackup(frequency = frequency)
            AutomaticBackupWorker.sync(context, frequency != BackupFrequency.OFF)
        }
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

    fun backupNow() {
        val folder = settings.backupDirectoryUri
        if (folder == null) {
            toast("Choose a backup folder first.")
            return
        }
        scope.launch {
            try {
                val (fileName, createdAt, count) = withContext(Dispatchers.IO) {
                    BackupStorage.writeBackupToDirectory(context, container.database, folder, settings.backupRotation)
                }
                container.settings.setBackup(lastBackupAt = createdAt)
                toast("Backup saved as $fileName ($count transactions).")
            } catch (e: Exception) {
                toast("Backup failed: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("Backup & restore", fontWeight = FontWeight.Bold) })
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            GroupLabel("Archive backup")
            GroupCard {
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("folder-outline", "#2E7D32", 38.dp) },
                    title = "Backup folder",
                    subtitle = settings.backupDirectoryUri?.let { BackupStorage.describeBackupDirectory(it) }
                        ?: "Choose ${BackupStorage.DEFAULT_BACKUP_FOLDER_LABEL}",
                    onClick = { folderLauncher.launch(null) },
                )
                RowDivider()
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("backup-restore", "#1565C0", 38.dp) },
                    title = "Automatic backup",
                    subtitle = frequencyLabel(settings),
                    onClick = { frequencyOpen = true },
                )
                RowDivider()
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("history", "#6A1B9A", 38.dp) },
                    title = "Backup rotation",
                    subtitle = "Keep newest ${settings.backupRotation} archive" +
                        if (settings.backupRotation == 1) "" else "s",
                    onClick = { rotationOpen = true },
                )
                RowDivider()
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("content-save-outline", "#455A64", 38.dp) },
                    title = "Back up now",
                    subtitle = settings.lastBackupAt?.let {
                        "Last backup " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
                    } ?: "Create a complete Spendr archive",
                    onClick = { backupNow() },
                )
                RowDivider()
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("database-import-outline", "#37474F", 38.dp) },
                    title = "Restore backup",
                    subtitle = "Replace local data from a Spendr ZIP archive",
                    onClick = { confirmRestore = true },
                )
            }

            GroupLabel("CSV transfer")
            GroupCard {
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("upload", "#E65100", 38.dp) },
                    title = "Export CSV",
                    subtitle = "Spendr or Money Lover format",
                    onClick = { confirmExport = true },
                )
                RowDivider()
                SettingsRow(
                    icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("download", "#00838F", 38.dp) },
                    title = "Import CSV",
                    subtitle = "Merge Spendr or Money Lover transactions",
                    onClick = { confirmImport = true },
                )
            }

            Text(
                "Import merges new Transactions; existing matching rows " +
                    "(same day, amount, note, and category) are skipped.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (frequencyOpen) {
        RadioDialog(
            title = "Automatic backup",
            body = "Choose how often Spendr creates a backup. Android runs background work when the " +
                "system allows it, so a backup may run later than its due time.",
            options = listOf(
                BackupFrequency.OFF to "Off",
                BackupFrequency.DAILY to "Daily",
                BackupFrequency.WEEKLY to "Weekly",
                BackupFrequency.MONTHLY to "Monthly",
            ),
            selected = settings.backupFrequency,
            onSelect = { chooseFrequency(it) },
            onDismiss = { frequencyOpen = false },
        )
    }
    if (rotationOpen) {
        val rotations = listOf(3, 7, 14, 30)
        RadioDialog(
            title = "Backup rotation",
            body = "When a new backup succeeds, Spendr deletes older archives beyond this number.",
            options = rotations.map { it to "Keep newest $it archives" },
            selected = settings.backupRotation,
            onSelect = { rotation ->
                scope.launch { container.settings.setBackup(rotation = rotation) }
                rotationOpen = false
            },
            onDismiss = { rotationOpen = false },
        )
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

private fun frequencyLabel(settings: Settings): String =
    when (settings.backupFrequency) {
        BackupFrequency.OFF -> "Off"
        else -> {
            val name = settings.backupFrequency.name.lowercase().replaceFirstChar { it.uppercase() }
            "$name · keep ${settings.backupRotation}"
        }
    }

/** Card container for grouped rows (RN ListGroup: radius 20, clipped). */
@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

/** RN GroupLabel. */
@Composable
private fun GroupLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** 1dp divider inset 16dp, between grouped rows. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** RN ListItem row: 38dp badge, titleMedium 600 + bodySmall subtitle, chevron when pressable. */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) icon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            com.spendr.app.kt.ui.components.MciIcon(
                "chevron-right",
                22.dp,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Radio option list dialog (RN OptionRow dialogs: frequency, rotation). */
@Composable
private fun <T> RadioDialog(
    title: String,
    body: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) },
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
