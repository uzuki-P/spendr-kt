package com.spendr.app.kt.ui.receipt

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.vision.ScannedReceipt
import com.spendr.app.kt.data.vision.VisionApiClient
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.MciIcon
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ReceiptScanPanel(
    container: AppContainer,
    transactionId: Long?,
    activeScanId: String?,
    appliedScanId: String?,
    canApply: Boolean,
    onActiveScan: (String) -> Unit,
    onResult: (String, ScannedReceipt) -> Unit,
    onBusy: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsState(initial = null)
    val scans by container.receiptScans.scans.collectAsState(initial = emptyList())
    val scan = scans.firstOrNull { it.id == activeScanId }
    var checking by remember { mutableStateOf(true) }
    var connected by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val config = settings?.vision
    val busy = preparing || scan?.isPending == true
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = it
    }

    LaunchedEffect(busy) { onBusy(busy) }
    LaunchedEffect(scan?.id, scan?.result, canApply) {
        val result = scan?.result
        if (canApply && result != null && scan.id != appliedScanId && !scan.saved) onResult(scan.id, result)
    }
    LaunchedEffect(config, lifecycle) {
        connected = false
        if (config == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                checking = true
                try {
                    VisionApiClient(config.validated()).connection()
                    connected = true
                    connectionError = null
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    connected = false
                    connectionError = if (config.token.isBlank()) "Add your API token in scanner settings."
                        else e.message ?: "Cannot connect to the scanner."
                } finally { checking = false }
                delay(30_000)
            }
        }
    }
    fun start(uri: Uri, cameraFile: File? = null) {
        val configuration = config ?: return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        scope.launch {
            preparing = true
            inputError = null
            try {
                onActiveScan(container.receiptScans.start(uri, configuration, transactionId))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                connected = false
                inputError = e.message ?: "Could not start the scan."
            } finally {
                cameraFile?.delete()
                preparing = false
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) start(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraPath?.let(::File)
        if (success && file != null) {
            start(FileProvider.getUriForFile(context, "${context.packageName}.receipt-files", file), file)
        } else file?.delete()
        cameraPath = null
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MciIcon("line-scan", 24.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Scan receipt", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Text("Read the merchant, total, and items from an image. Review them before saving.",
                style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BouncyButton(onClick = {
                    try {
                        val directory = File(context.cacheDir, "receipt_photos").apply { mkdirs() }
                        val file = File.createTempFile("receipt-", ".jpg", directory)
                        cameraPath = file.absolutePath
                        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.receipt-files", file))
                    } catch (e: Exception) {
                        cameraPath?.let { File(it).delete() }
                        cameraPath = null
                        inputError = "Could not open the system camera. Choose an image instead."
                    }
                }, enabled = connected && !checking && !busy, modifier = Modifier.weight(1f)) {
                    Text("Take photo")
                }
                BouncyButton(onClick = {
                    try { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    catch (_: Exception) { inputError = "Could not open the image picker." }
                }, enabled = connected && !checking && !busy, modifier = Modifier.weight(1f)) { Text("Choose image") }
            }
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoadingIndicator(Modifier.size(48.dp))
                    Column {
                        Text(when {
                            preparing -> "Preparing image"
                            scan?.status == "uploading" -> "Uploading receipt"
                            scan?.status == "queued" -> "Waiting for the scanner"
                            scan?.status == "reconnecting" -> "Waiting for a connection"
                            scan?.status == "running" -> "Reading receipt"
                            else -> "Starting scan"
                        }, style = MaterialTheme.typography.bodyLargeEmphasized)
                        Text(if (notificationsAllowed) "You can leave the app. We'll notify you when it's ready."
                            else "Your scan continues when you leave the app. Return here to review it.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (scan != null) BouncyTextButton(onClick = { scope.launch { container.receiptScans.cancel(scan.id) } }) {
                    Text("Stop checking")
                }
            } else if (scan?.status == "succeeded" && !scan.saved) {
                Text("Scan ready. Review the fields and items below before saving.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            if (!busy && checking) Text("Checking scanner connection", style = MaterialTheme.typography.bodySmall)
            if (!checking && !connected) Text(connectionError ?: "Scanner unavailable. Check your connection and settings.",
                style = MaterialTheme.typography.bodySmall)
            scan?.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
                BouncyTextButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Enable scan notifications")
                }
            }
            BouncyTextButton(onClick = onOpenSettings) { Text("Scanner settings") }
            scans.filter { it.transactionId == transactionId && !it.saved && it.id != activeScanId }.take(10).forEach { previous ->
                BouncyTextButton(onClick = { onActiveScan(previous.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(when {
                        previous.isPending -> "Open scan in progress"
                        previous.status == "succeeded" -> "Review completed scan"
                        else -> "Review failed scan"
                    })
                }
            }
        }
    }
}
