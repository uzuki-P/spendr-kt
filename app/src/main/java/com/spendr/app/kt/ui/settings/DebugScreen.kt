package com.spendr.app.kt.ui.settings

import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.SpendrTopBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.pressScale
import kotlinx.coroutines.launch

/** Debug screen, ported from RN `DebugScreen`: seed realistic sample data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(container: AppContainer, onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var confirmSeed by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SpendrTopBar(title = "Debug", onBack = onBack) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(16.dp)) {
            SettingsRow(
                corners = Corners(24.dp),
                icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("database-plus-outline", "#9366A4", 40.dp) },
                title = "Seed debug data",
                subtitle = "Add realistic Transactions over the last three months.",
                onClick = { confirmSeed = true },
            )
        }
    }

    if (confirmSeed) {
        AlertDialog(
            onDismissRequest = { confirmSeed = false },
            title = { Text("Seed debug data") },
            text = { Text("This adds realistic Transactions to your current data. Continue?") },
            confirmButton = {
                BouncyTextButton(
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
            dismissButton = { BouncyTextButton(onClick = { confirmSeed = false }) { Text("Cancel") } },
        )
    }

    result?.let { message ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text("Debug") },
            text = { Text(message) },
            confirmButton = { BouncyTextButton(onClick = { result = null }) { Text("OK") } },
        )
    }
}
