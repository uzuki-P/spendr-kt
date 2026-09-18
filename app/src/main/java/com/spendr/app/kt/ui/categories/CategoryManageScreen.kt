package com.spendr.app.kt.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.data.repo.CATEGORY_PALETTE
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.BouncyIconButton
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.BouncyTonalButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.ThemedTextField
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.theme.SpendrTheme
import kotlinx.coroutines.launch

private val ICON_CHOICES = listOf(
    "silverware-fork-knife", "coffee-outline", "shopping-outline", "cart-outline", "car",
    "bus", "train", "airplane", "gamepad-variant-outline", "file-document-outline",
    "heart-pulse", "movie-open-outline", "music-note", "book-open-variant", "dumbbell",
    "gift-outline", "paw", "home-outline", "devices", "flask-outline",
    "wrench", "phone", "tshirt-crew-outline", "baby-carriage", "dots-horizontal-circle-outline",
)

/** Category CRUD with reorder + delete-with-reassignment, ported from RN `CategoryManageScreen`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(container: AppContainer, onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val categories by container.categories.observeCategories()
        .collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    BouncyIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BouncyTonalButton(
                onClick = { creating = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                MciIcon("plus", 18.dp, MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Add category", Modifier.padding(start = 8.dp))
            }
            if (categories.isEmpty()) {
                Text(
                    "No categories yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            categories.forEachIndexed { index, category ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryIconBadge(icon = category.icon, color = category.color, size = 42.dp)
                        Text(
                            category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        // RN styles.actions: trailing icon cluster with 2dp gap
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BouncyIconButton(
                                onClick = {
                                    val ids = categories.map { it.id }.toMutableList()
                                    ids.removeAt(index)
                                    ids.add(index - 1, category.id)
                                    scope.launch { container.categories.reorderCategories(ids) }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp),
                            ) {
                                MciIcon(
                                    "chevron-up",
                                    24.dp,
                                    if (index > 0) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                )
                            }
                            BouncyIconButton(
                                onClick = {
                                    val ids = categories.map { it.id }.toMutableList()
                                    ids.removeAt(index)
                                    ids.add(index + 1, category.id)
                                    scope.launch { container.categories.reorderCategories(ids) }
                                },
                                enabled = index < categories.size - 1,
                                modifier = Modifier.size(36.dp),
                            ) {
                                MciIcon(
                                    "chevron-down",
                                    24.dp,
                                    if (index < categories.size - 1) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                )
                            }
                            BouncyIconButton(
                                onClick = { editing = category },
                                modifier = Modifier.size(36.dp),
                            ) {
                                MciIcon("pencil-outline", 24.dp, MaterialTheme.colorScheme.primary)
                            }
                            BouncyIconButton(
                                onClick = { deleting = category },
                                modifier = Modifier.size(36.dp),
                            ) {
                                MciIcon("trash-can-outline", 24.dp, MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryEditSheet(
            existing = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, icon, color ->
                scope.launch {
                    val target = editing
                    if (target != null) {
                        container.categories.updateCategory(target.id, name, icon, color)
                    } else {
                        container.categories.createCategory(name, icon, color)
                    }
                    creating = false
                    editing = null
                }
            },
        )
    }

    deleting?.let { target ->
        DeleteCategoryFlow(
            container = container,
            target = target,
            categories = categories,
            onDone = { deleting = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditSheet(
    existing: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: ICON_CHOICES.first()) }
    var color by remember { mutableStateOf(existing?.color ?: CATEGORY_PALETTE.first()) }
    var showNameError by remember { mutableStateOf(false) }

    // Open fully expanded: name, icons, colors, and Save are all on screen at
    // once (the inner column still scrolls on short screens, Save stays pinned)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Scrollable content + pinned Save: the button stays visible even when
        // the name field, icon grid, and palette overflow the sheet height.
        Column(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (existing == null) "New category" else "Edit category",
                    style = MaterialTheme.typography.titleLarge,
                )
                ThemedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = "Name",
                    placeholder = "e.g., Pets",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showNameError) {
                    Text(
                        "Name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                // RN ChoiceTiles labels: labelLarge 600, textSecondary
                Text(
                    "Icon",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                // RN SelectableTile 48dp rounded: surfaceContainerHigh + outlineVariant
                // border idle, primaryContainer + primary border selected, plain glyph
                // 5 fixed columns: the height formula below must match the column count
                val iconRows = (ICON_CHOICES.size + 4) / 5
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((iconRows * 56 - 8).dp),
                ) {
                    items(ICON_CHOICES) { iconName ->
                        val selected = iconName == icon
                        BouncySurface(
                            onClick = { icon = iconName },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MciIcon(
                                    iconName,
                                    22.dp,
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                // RN swatch SelectableTile: 36dp circle, 3dp border, check when picked
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    COLUMN_PALETTE.forEach { hex ->
                        val selected = hex == color
                        BouncySurface(
                            onClick = { color = hex },
                            shape = CircleShape,
                            color = com.spendr.app.kt.ui.components.categoryColor(hex),
                            border = BorderStroke(
                                3.dp,
                                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            ),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Pinned below the scroll area: always on screen
            BouncyButton(
                onClick = {
                    if (name.trim().isEmpty()) {
                        showNameError = true
                    } else {
                        onSave(name.trim(), icon, color)
                    }
                },
                enabled = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("Save", Modifier.padding(start = 8.dp))
            }
        }
    }
}

private val COLUMN_PALETTE = CATEGORY_PALETTE

@Composable
private fun DeleteCategoryFlow(
    container: AppContainer,
    target: CategoryEntity,
    categories: List<CategoryEntity>,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var replacementId by remember { mutableStateOf<Long?>(null) }
    var stage by remember { mutableStateOf(Stage.PICK) }

    when (stage) {
        Stage.PICK -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Move and delete \"${target.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Transactions and QuickAdds referencing this Category must move somewhere else first.",
                    )
                    if (categories.size <= 1) {
                        Text("Create another Category first so these items have somewhere to move.")
                    } else {
                        categories.filter { it.id != target.id }.forEach { candidate ->
                            val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(source)
                                    .clickable(
                                        interactionSource = source,
                                        indication = androidx.compose.material3.ripple(),
                                        onClick = { replacementId = candidate.id },
                                    ),
                            ) {
                                RadioButton(
                                    selected = replacementId == candidate.id,
                                    onClick = { replacementId = candidate.id },
                                )
                                CategoryIconBadge(icon = candidate.icon, color = candidate.color, size = 28.dp)
                                Text(
                                    candidate.name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                BouncyTextButton(
                    enabled = replacementId != null,
                    onClick = { stage = Stage.CONFIRM },
                ) { Text("Continue") }
            },
            dismissButton = { BouncyTextButton(onClick = onDone) { Text("Cancel") } },
        )

        Stage.CONFIRM -> {
            val replacement = categories.firstOrNull { it.id == replacementId }
            AlertDialog(
                onDismissRequest = onDone,
                title = { Text("Confirm Category migration") },
                text = {
                    Text(
                        "They will be moved from \"${target.name}\" to \"${replacement?.name}\".\n\n" +
                            "\"${target.name}\" will then be permanently deleted. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    BouncyTextButton(
                        onClick = {
                            scope.launch {
                                container.categories.replaceCategoryAndDelete(target.id, replacementId!!)
                                onDone()
                            }
                        },
                    ) { Text("Move and delete") }
                },
                dismissButton = { BouncyTextButton(onClick = { stage = Stage.PICK }) { Text("Go back") } },
            )
        }
    }
}

private enum class Stage { PICK, CONFIRM }
