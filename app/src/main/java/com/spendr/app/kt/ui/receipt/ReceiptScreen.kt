package com.spendr.app.kt.ui.receipt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import com.spendr.app.kt.ui.components.BouncyOutlinedButton
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.BouncyTonalButton
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.segmentCorners
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SpendrTopBar(title = if (transactionId == null) "Add receipt" else "Edit receipt", onBack = onBack)
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                    BouncyButton(
                        onClick = ::saveReceipt,
                        enabled = !busy && amount > 0 && merchant.isNotBlank() && categoryId != null,
                        height = androidx.compose.material3.ButtonDefaults.MediumContainerHeight,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        MciIcon("check", 22.dp, MaterialTheme.colorScheme.onPrimary)
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
            Surface(
                shape = MaterialTheme.shapes.extraLargeIncreased,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total paid", style = MaterialTheme.typography.labelLargeEmphasized)
                    ReceiptField(
                        value = paidAmount,
                        onValueChange = { paidAmount = it.filter(Char::isDigit) },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = "Rp",
                        large = true,
                    )
                    Text(
                        "Enter the amount you paid, including any rounding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
            }
            if (BuildConfig.VISION_API_TOKEN.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MciIcon("line-scan", 24.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Scan receipt", style = MaterialTheme.typography.titleMediumEmphasized)
                        }
                        Text("Fill the merchant, total, and items from a photo. Review them before saving.", style = MaterialTheme.typography.bodyMedium)
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
                            BouncyOutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                Text("Choose image")
                            }
                        }
                        if (busy) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LoadingIndicator(modifier = Modifier.size(40.dp))
                                Text("Reading receipt…", style = MaterialTheme.typography.bodyLargeEmphasized)
                            }
                        }
                        scanError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            ReceiptLabeledField("Merchant", "e.g., Indomaret", merchant, { merchant = it })
            val selectedCategory = categories.firstOrNull { it.id == categoryId }
            SegmentedGroup {
                MorphSurface(
                    onClick = { showDatePicker = true },
                    corners = segmentCorners(0, 2),
                    pressedCorners = Corners(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MciIcon("calendar-blank-outline", 22.dp, MaterialTheme.colorScheme.primary)
                        Text(formatFullDate(date), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        MciIcon("chevron-right", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MorphSurface(
                    onClick = { showCategories = true },
                    corners = segmentCorners(1, 2),
                    pressedCorners = Corners(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (selectedCategory != null) {
                            CategoryIconBadge(selectedCategory.icon, selectedCategory.color, 40.dp)
                        } else {
                            MciIcon("shape-outline", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            selectedCategory?.name ?: "Select category",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedCategory == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        MciIcon("chevron-down", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            ReceiptLabeledField("Note (optional)", "What was this for?", note, { note = it })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Items", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 4.dp))
                BouncyTonalButton(onClick = { items.add(ItemDraft()) }, height = ButtonDefaults.ExtraSmallContainerHeight) {
                    MciIcon("plus", 18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Add item", Modifier.padding(start = 4.dp))
                }
            }
            if (items.isEmpty()) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text("No items yet. Scan a receipt or add them by hand.", Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items.forEachIndexed { index, item ->
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Item ${index + 1}", style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                        BouncyTextButton(
                            onClick = { items.removeAt(index) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Remove") }
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
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceiptSummaryRow("Items total", formatRupiah(itemTotal))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                    ReceiptSummaryRow(if (amount >= itemTotal) "Unallocated" else "Items exceed total", formatRupiah(kotlin.math.abs(amount - itemTotal)))
                    Text("Only total paid counts in reports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f))
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
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
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
        placeholder = { Text(placeholder, style = if (large) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.bodyLarge) },
        prefix = prefix?.let {
            {
                Text(
                    it,
                    style = if (large) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        textStyle = if (large) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.large,
        interactionSource = interaction,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (large) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = if (large) {
                MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth().then(
            if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large) else Modifier,
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
