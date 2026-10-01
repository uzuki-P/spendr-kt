package com.spendr.app.kt.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiahCompact
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.EmptyState
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.SectionHeader
import com.spendr.app.kt.ui.components.SegmentGap
import com.spendr.app.kt.ui.components.segmentCorners
import com.spendr.app.kt.ui.transactions.TransactionRow

/**
 * Home: large rounded title with a settings button, the pace hero, QuickAdd
 * tiles, and recent transactions as one segmented group. Navigation lives in
 * a floating toolbar at thumb height (Transactions, Search, Reports, Receipt)
 * with Add as its attached FAB; the toolbar tucks away while scrolling down.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAdd: () -> Unit,
    onOpenReceipt: () -> Unit,
    onOpenAddQuickAdd: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit = {},
    onOpenTransactions: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReports: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val quickAdds by viewModel.quickAdds.collectAsState()
    val lastAddedId by viewModel.lastAddedId.collectAsState()
    var pendingHighlightId by remember { mutableStateOf<Long?>(null) }
    var highlightRowId by remember { mutableStateOf<Long?>(null) }
    val pulse = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val toolbarExpanded by rememberExpandedOnScrollUp(listState)

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Buffer the just-added id once it shows up in recent, then consume it so
    // the pulse never re-fires when re-entering Home later.
    LaunchedEffect(lastAddedId, state.recent) {
        val id = lastAddedId ?: return@LaunchedEffect
        if (state.recent.any { it.transaction.id == id }) {
            pendingHighlightId = id
            viewModel.consumeLastAddedId()
        }
    }

    // New-transaction pulse: two tonal flashes on the just-added row
    LaunchedEffect(pendingHighlightId) {
        val id = pendingHighlightId ?: return@LaunchedEffect
        highlightRowId = id
        pulse.snapTo(0f)
        pulse.animateTo(0.22f, tween(300))
        pulse.animateTo(0f, tween(300))
        pulse.animateTo(0.16f, tween(300))
        pulse.animateTo(0f, tween(900))
        highlightRowId = null
        pendingHighlightId = null
    }

    // Surface (not a bare background) so text inherits onSurface in both themes
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 136.dp),
            ) {
                item(key = "header") {
                    HomeHeader(onOpenSettings = onOpenSettings)
                }
                item(key = "pace") {
                    SpendingPaceCard(pace = state.pace, onOpenReport = onOpenReports)
                }
                if (quickAdds.isNotEmpty()) {
                    item(key = "quickadd-header") {
                        SectionHeader(title = "Quick add")
                    }
                    item(key = "quickadd") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickAdds.chunked(3).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (qa in rowItems) {
                                        QuickAddTile(
                                            label = qa.quickAdd.label,
                                            color = qa.categoryColor,
                                            icon = qa.categoryIcon,
                                            paidAmount = qa.quickAdd.paidAmount,
                                            onClick = { onOpenAddQuickAdd(qa.quickAdd.id) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                item(key = "recent-header") {
                    SectionHeader(title = "Recent", action = {
                        BouncyTextButton(onClick = onOpenTransactions) {
                            Text("See all", style = MaterialTheme.typography.labelLargeEmphasized)
                        }
                    })
                }
                if (state.recent.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            glyph = "wallet-outline",
                            title = "No transactions yet",
                            message = "Tap Add to record your first spending.",
                        )
                    }
                } else {
                    itemsIndexed(state.recent, key = { _, row -> row.transaction.id }) { index, row ->
                        TransactionRow(
                            row = row,
                            onClick = { onOpenDetail(row.transaction.id) },
                            showDate = true,
                            showDayOfWeek = true,
                            highlight = if (row.transaction.id == highlightRowId) pulse.value else 0f,
                            corners = segmentCorners(index, state.recent.size),
                            modifier = Modifier
                                .animateItem()
                                .padding(bottom = SegmentGap),
                        )
                    }
                }
            }

            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onOpenAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "Add spending")
                    }
                },
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                // Anchored bottom-end so the FAB keeps the same corner spot
                // whether the toolbar is expanded or collapsed into it
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                ToolbarAction("format-list-bulleted", "Transactions", onOpenTransactions)
                ToolbarAction("magnify", "Search", onOpenSearch)
                ToolbarAction("chart-box-outline", "Reports", onOpenReports)
                ToolbarAction("receipt-text-outline", "Add receipt", onOpenReceipt)
            }
        }
    }
}

/**
 * True while the list rests at the top or the user scrolls back up, so the
 * floating toolbar collapses to its FAB while reading and returns on the way
 * back.
 */
@Composable
private fun rememberExpandedOnScrollUp(state: LazyListState): State<Boolean> {
    val expanded = remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        var lastIndex = state.firstVisibleItemIndex
        var lastOffset = state.firstVisibleItemScrollOffset
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val delta = if (index != lastIndex) (index - lastIndex) * 1000 else offset - lastOffset
                when {
                    index == 0 && offset < 24 -> expanded.value = true
                    delta > 8 -> expanded.value = false
                    delta < -8 -> expanded.value = true
                }
                lastIndex = index
                lastOffset = offset
            }
    }
    return expanded
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Spendr",
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatMonthYear(System.currentTimeMillis()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = onOpenSettings,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.height(48.dp),
        ) {
            MciIcon("cog-outline", 24.dp, MaterialTheme.colorScheme.onSecondaryContainer, contentDescription = "Settings")
        }
    }
}

@Composable
private fun ToolbarAction(glyph: String, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        // The glyph is font text; give TalkBack the action name instead
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        MciIcon(glyph, 24.dp, LocalContentColor.current, Modifier.clearAndSetSemantics { })
    }
}

@Composable
private fun QuickAddTile(
    label: String,
    color: String,
    icon: String,
    paidAmount: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tiles square off while pressed (the M3E button press morph)
    MorphSurface(
        onClick = onClick,
        corners = Corners(28.dp),
        pressedCorners = Corners(14.dp),
        pressedScale = 0.95f,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(120.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            CategoryIconBadge(icon = icon, color = color, size = 48.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                paidAmount?.takeIf { it > 0 }?.let { formatRupiahCompact(it) } ?: "Choose amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
