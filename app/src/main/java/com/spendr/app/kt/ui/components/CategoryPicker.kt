package com.spendr.app.kt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.data.db.entity.CategoryEntity
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.theme.SpendrTheme

/**
 * RN `CategoryPicker`: handle + titleLarge, pill search, 3-column grid,
 * optional "Manage categories" link pinned at the bottom. Shared by the
 * Add Spending sheet and the Quick Add sheet.
 */
@Composable
fun CategoryPickerContent(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    recentCategoryIds: List<Long> = emptyList(),
    onManage: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val ordered = remember(categories, recentCategoryIds, query) {
        val matching = categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
        val recents = recentCategoryIds.mapNotNull { id -> matching.firstOrNull { it.id == id } }
        (recents + matching.filterNot { c -> recents.any { it.id == c.id } }).distinct()
    }
    Column(modifier.padding(horizontal = 16.dp)) {
        Text(
            "Select category",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
        )
        SearchPill(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search categories",
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            items(ordered, key = { it.id }) { category ->
                CategoryTile(
                    label = category.name,
                    selected = category.id == selectedId,
                    onClick = { onSelect(category.id) },
                    modifier = Modifier.animateItem(),
                ) {
                    CategoryIconBadge(icon = category.icon, color = category.color, size = 44.dp)
                }
            }
        }
        if (onManage != null) {
            BouncyTextButton(
                onClick = onManage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
            ) {
                MciIcon("cog-outline", 18.dp, MaterialTheme.colorScheme.primary)
                Text("Manage categories", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * RN CategoryPicker in multiple mode: search pill + 3-column grid + count/Done
 * footer. Shared by the Transactions filter dialog and the Search screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMultiSelectSheet(
    categories: List<CategoryEntity>,
    selectedIds: List<Long>,
    onDone: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(selectedIds) }

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
            Text(
                "Select categories",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
            )
            SearchPill(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search categories",
            )
            val filtered = remember(categories, query) {
                categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                if (query.isEmpty()) {
                    item(key = "all") {
                        MultiSelectCategoryTile(
                            label = "All categories",
                            icon = null,
                            color = null,
                            selected = draft.isEmpty(),
                            onClick = { draft = emptyList() },
                        )
                    }
                }
                items(filtered, key = { it.id }) { category ->
                    MultiSelectCategoryTile(
                        label = category.name,
                        icon = category.icon,
                        color = category.color,
                        selected = category.id in draft,
                        onClick = {
                            draft = if (category.id in draft) draft - category.id else draft + category.id
                        },
                    )
                }
                if (filtered.isEmpty() && query.isNotEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No categories match \u201C${query.trim()}\u201D.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (draft.isEmpty()) "All categories" else "${draft.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                BouncyTextButton(onClick = onDismiss) { Text("Cancel") }
                BouncyButton(onClick = { onDone(draft) }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun MultiSelectCategoryTile(
    label: String,
    icon: String?,
    color: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CategoryTile(label = label, selected = selected, onClick = onClick, modifier = modifier) {
        if (icon != null && color != null) {
            CategoryIconBadge(icon = icon, color = color, size = 44.dp)
        } else {
            // "All categories": neutral badge that morphs to a cookie when picked
            MorphingBadge(
                selected = selected,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                size = 44.dp,
            ) {
                MciIcon("shape-outline", 22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Picker tile: badge over the name. Selected tiles fill with primaryContainer
 * and round out; every tile squares off slightly while pressed.
 */
@Composable
private fun CategoryTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tileColor",
    )
    MorphSurface(
        onClick = onClick,
        corners = if (selected) Corners(32.dp) else Corners(16.dp),
        pressedCorners = Corners(10.dp),
        pressedScale = 0.94f,
        color = container,
        modifier = modifier.padding(4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
        ) {
            badge()
            Text(
                label,
                style = if (selected) {
                    MaterialTheme.typography.labelLargeEmphasized
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
