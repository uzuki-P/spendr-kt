package com.spendr.app.kt.ui.receipt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.BuildConfig
import com.spendr.app.kt.data.repo.ReceiptInput
import com.spendr.app.kt.data.repo.ReceiptItemInput
import com.spendr.app.kt.data.vision.ReceiptScanner
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.formatFullDate
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.ui.components.CalendarSheet
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.CategoryPickerContent
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.epochMsToLocalDate
import com.spendr.app.kt.ui.components.toEpochMs
import com.spendr.app.kt.ui.theme.SpendrTheme
import kotlinx.coroutines.launch
import java.io.File

private data class ItemDraft(val name: String = "", val paidAmount: String = "", val quantity: String = "1")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(container: AppContainer, transactionId: Long?, onDone: (Long) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val items = remember { mutableStateListOf<ItemDraft>() }
    var categories by remember { mutableStateOf(emptyList<CategoryEntity>()) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var showCategories by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scanner = remember(context) { ReceiptScanner(context) }
    var cameraPhoto by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun scanImage(uri: Uri, photo: File? = null) {
        scope.launch {
            busy = true
            scanError = null
            try {
                val result = scanner.scan(uri)
                if (merchant.isBlank()) merchant = result.merchant.orEmpty()
                if (paidAmount.isBlank()) paidAmount = result.total?.toString().orEmpty()
                items.clear()
                items.addAll(result.items.map { ItemDraft(it.name, it.paidAmount.toString(), it.quantity) })
            } catch (e: Exception) {
                scanError = e.message ?: "Could not scan the receipt. Add items manually."
            } finally {
                photo?.delete()
                busy = false
            }
        }
    }

    LaunchedEffect(transactionId) {
        categories = container.categories.listCategories()
        categoryId = categories.firstOrNull { it.name.equals("Shopping", true) }?.id ?: categories.firstOrNull()?.id
        if (transactionId != null) {
            val tx = container.transactions.getTransaction(transactionId)
            if (tx?.type == "receipt") {
                paidAmount = tx.paidAmount.toString()
                merchant = tx.merchant.orEmpty()
                note = tx.note.orEmpty()
                categoryId = tx.categoryId
                date = tx.date
                items.clear()
                items.addAll(container.receipts.items(transactionId).map {
                    ItemDraft(it.name, it.paidAmount.toString(), it.quantity)
                })
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scanImage(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        val photo = cameraPhoto
        if (success && uri != null) scanImage(uri, photo) else photo?.delete()
        cameraUri = null
        cameraPhoto = null
    }

    if (showCategories) ModalBottomSheet(onDismissRequest = { showCategories = false }) {
        CategoryPickerContent(
            categories = categories,
            selectedId = categoryId,
            onSelect = { categoryId = it; showCategories = false },
        )
    }
    if (showDatePicker) CalendarSheet(
        selectedDate = epochMsToLocalDate(date),
        onPick = { date = it.toEpochMs(); showDatePicker = false },
        onDismiss = { showDatePicker = false },
    )

    val itemTotal = items.sumOf { it.paidAmount.toLongOrNull() ?: 0L }
    val amount = paidAmount.toLongOrNull() ?: 0L
    fun saveReceipt() {
        val selected = categoryId ?: return
        val parsed = items.mapNotNull { draft ->
            if (draft.name.isBlank() && draft.paidAmount.isBlank()) null else ReceiptItemInput(
                draft.name.trim(), draft.paidAmount.toLongOrNull() ?: -1, draft.quantity,
            )
        }
        if (parsed.any { it.name.isBlank() || it.paidAmount < 0 ||
                (it.quantity.toBigDecimalOrNull()?.signum() ?: 0) <= 0 }) {
            error = "Give every item a name, line total, and quantity."
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val id = container.receipts.save(
                    ReceiptInput(amount, selected, merchant, note, date, parsed), transactionId,
                )
                container.lastAddedTransactionId.value = id
                onDone(id)
            } catch (e: Exception) {
                error = e.message ?: "Could not save receipt."
            } finally {
                busy = false
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add receipt" else "Edit receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = { BouncyIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = SpendrTheme.colors.border)
                    BouncyButton(
                        onClick = ::saveReceipt,
                        enabled = !busy && amount > 0 && merchant.isNotBlank() && categoryId != null,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    ) {
                        MciIcon("check", 18.dp, MaterialTheme.colorScheme.onPrimary)
                        Text(if (transactionId == null) "Save receipt" else "Save changes", Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total paid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ReceiptField(
                        value = paidAmount,
                        onValueChange = { paidAmount = it.filter(Char::isDigit) },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = "Rp",
                        large = true,
                    )
                    Text("Enter the amount you paid, including any rounding.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (BuildConfig.VISION_API_TOKEN.isNotBlank()) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MciIcon("receipt-text-outline", 22.dp, MaterialTheme.colorScheme.primary)
                            Text("Scan receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Fill the merchant, total, and items from a photo. Review them before saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BouncyButton(onClick = {
                        try {
                            val directory = File(context.cacheDir, "receipt_photos").apply { mkdirs() }
                            val photo = File.createTempFile("receipt-", ".jpg", directory)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.receipt-files", photo)
                            cameraPhoto = photo
                            cameraUri = uri
                            camera.launch(uri)
                        } catch (e: Exception) {
                            cameraPhoto?.delete()
                            cameraPhoto = null
                            cameraUri = null
                            scanError = e.message ?: "Could not open the camera."
                        }
                            }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Take photo") }
                            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                Text("Choose image")
                            }
                        }
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Reading receipt…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        scanError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            ReceiptLabeledField("Merchant", "e.g., Indomaret", merchant, { merchant = it })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ReceiptLabel("Date")
                BouncySurface(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, SpendrTheme.colors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MciIcon("calendar-blank-outline", 20.dp, MaterialTheme.colorScheme.primary)
                        Text(formatFullDate(date), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            val selectedCategory = categories.firstOrNull { it.id == categoryId }
            BouncySurface(
                onClick = { showCategories = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, SpendrTheme.colors.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedCategory != null) CategoryIconBadge(selectedCategory.icon, selectedCategory.color, 42.dp)
                    Text(selectedCategory?.name ?: "Select category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    MciIcon("chevron-down", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ReceiptLabeledField("Note (optional)", "What was this for?", note, { note = it })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { items.add(ItemDraft()) }) {
                    MciIcon("plus", 18.dp, MaterialTheme.colorScheme.primary)
                    Text("Add item", Modifier.padding(start = 4.dp))
                }
            }
            if (items.isEmpty()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text("No items yet. Scan a receipt or add them by hand.", Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items.forEachIndexed { index, item ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Item ${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { items.removeAt(index) }) { Text("Remove") }
                    }
                    ReceiptField(item.name, { items[index] = item.copy(name = it) }, "Item name")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(2f)) {
                            ReceiptLabel("Line total")
                            ReceiptField(
                                item.paidAmount,
                                { items[index] = item.copy(paidAmount = it.filter(Char::isDigit)) },
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                prefix = "Rp",
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            ReceiptLabel("Qty")
                            ReceiptField(
                                item.quantity,
                                { items[index] = item.copy(quantity = it.filter { char -> char.isDigit() || char == '.' }) },
                                placeholder = "1",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }
                    }
                }
                }
            }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceiptSummaryRow("Items total", formatRupiah(itemTotal))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ReceiptSummaryRow(if (amount >= itemTotal) "Unallocated" else "Items exceed total", formatRupiah(kotlin.math.abs(amount - itemTotal)))
                    Text("Only total paid counts in reports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ReceiptLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun ReceiptLabeledField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        ReceiptLabel(label)
        ReceiptField(value, onValueChange, placeholder)
    }
}

@Composable
private fun ReceiptField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    prefix: String? = null,
    large: Boolean = false,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        prefix = prefix?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        textStyle = if (large) MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interaction,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth().border(
            1.dp,
            if (focused) MaterialTheme.colorScheme.primary else SpendrTheme.colors.border,
            MaterialTheme.shapes.medium,
        ),
    )
}

@Composable
private fun ReceiptSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
