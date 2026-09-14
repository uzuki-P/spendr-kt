package com.spendr.app.kt.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.db.DebugSeeder
import kotlinx.coroutines.launch

/** Debug screen, ported from RN `DebugScreen`: seed realistic sample data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var confirmSeed by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TopAppBar(title = { Text("Debug", fontWeight = FontWeight.Bold) })
            Column(Modifier.padding(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ) {
                    ListItem(
                        headlineContent = { Text("Seed debug data", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Add realistic Transactions over the last three months.") },
                        modifier = Modifier.clickable { confirmSeed = true },
                    )
                }
            }
        }
    }

    if (confirmSeed) {
        AlertDialog(
            onDismissRequest = { confirmSeed = false },
            title = { Text("Seed debug data") },
            text = { Text("This adds realistic Transactions to your current data. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSeed = false
                        scope.launch {
                            result = try {
                                DebugSeeder.seed(container.database)
                                "Debug Transactions added successfully."
                            } catch (e: Exception) {
                                "Could not seed debug data: ${e.message}"
                            }
                        }
                    },
                ) { Text("Seed") }
            },
            dismissButton = { TextButton(onClick = { confirmSeed = false }) { Text("Cancel") } },
        )
    }

    result?.let { message ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text("Debug") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}
