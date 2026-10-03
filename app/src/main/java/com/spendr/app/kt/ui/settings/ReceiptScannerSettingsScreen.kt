package com.spendr.app.kt.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.vision.VisionApiClient
import com.spendr.app.kt.data.vision.VisionConfiguration
import com.spendr.app.kt.data.vision.VisionDefaults
import com.spendr.app.kt.data.vision.VisionModel
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.SpendrTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ReceiptScannerSettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(VisionConfiguration().url) }
    var token by rememberSaveable { mutableStateOf("") }
    var provider by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var effort by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var defaults by remember { mutableStateOf<VisionDefaults?>(null) }
    var models by remember { mutableStateOf(emptyList<VisionModel>()) }
    var showModels by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!loaded) {
            val config = container.settings.settings.first().vision
            url = config.url
            token = config.token
            provider = config.provider
            model = config.model
            effort = config.reasoningEffort
            loaded = true
        }
    }
    fun configuration(requireToken: Boolean = true) = VisionConfiguration(url, token, provider, model, effort)
        .validated(requireToken)
    fun checkConnection() {
        scope.launch {
            busy = true
            message = null
            error = null
            try {
                val client = VisionApiClient(configuration())
                defaults = client.connection()
                message = "Connected. Image scanning is available."
                models = try { client.models(provider.ifBlank { defaults!!.provider }) }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { emptyList() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not connect to the scanner." }
            finally { busy = false }
        }
    }
    if (showModels) ModalBottomSheet(onDismissRequest = { showModels = false }) {
        Text("Choose model", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
        LazyColumn {
            items(models, key = { it.id }) { option ->
                BouncyTextButton(onClick = { model = option.id; effort = ""; showModels = false },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text(option.label) }
            }
        }
    }
    Scaffold(topBar = { SpendrTopBar(title = "Receipt scanner", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A photo becomes a receipt", style = MaterialTheme.typography.titleLargeEmphasized)
                    Text("Choose a receipt image or use your phone's camera. The scanner reads the merchant, total, and items. Review the result before saving.")
                    Text("Scans continue when you leave the app. Allow notifications to know when a result is ready.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!loaded) { LoadingIndicator(); return@Column }
            OutlinedTextField(url, { url = it; message = null; error = null }, label = { Text("API address") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(token, { token = it; message = null; error = null }, label = { Text("API token") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Text("Default scan configuration", style = MaterialTheme.typography.titleMediumEmphasized)
            Text("Leave a field empty to use the API's configured default.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to "API default", "opencode" to "OpenCode", "codex" to "Codex").forEach { (value, label) ->
                    FilterChip(selected = provider == value, onClick = {
                        provider = value
                        model = ""
                        effort = ""
                        models = emptyList()
                        message = null
                    }, label = { Text(label) })
                }
            }
            OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true,
                placeholder = { Text(defaults?.model ?: "API default") }, modifier = Modifier.fillMaxWidth())
            if (models.isNotEmpty()) {
                BouncyTextButton(onClick = { showModels = true }) { Text("Choose available model") }
            }
            OutlinedTextField(effort, { effort = it }, label = { Text("Reasoning effort") }, singleLine = true,
                placeholder = { Text(defaults?.reasoningEffort ?: "API default") }, modifier = Modifier.fillMaxWidth())
            models.firstOrNull { it.id == model }?.reasoningEfforts?.takeIf { it.isNotEmpty() }?.let {
                Text("Supported values: ${it.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            if (busy) LoadingIndicator()
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BouncyTextButton(onClick = ::checkConnection, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("Test connection")
                }
                BouncyButton(onClick = {
                    scope.launch {
                        busy = true
                        try {
                            container.settings.setVision(configuration(requireToken = false))
                            onBack()
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.message ?: "Could not save scanner settings." }
                        finally { busy = false }
                    }
                }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Save settings") }
            }
        }
    }
}
