package com.spendr.app.kt.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.domain.formatMonthYear
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.domain.formatRupiahCompact
import com.spendr.app.kt.ui.components.BouncySurface
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.components.SectionHeader
import com.spendr.app.kt.ui.transactions.TransactionRow

/**
 * Home, ported from RN `HomeScreen`: custom header with search/settings
 * circles, pace card, QuickAdd panel, recent transactions, extended FAB.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAdd: () -> Unit,
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
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }

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

    // RN new-transaction pulse: 0 → 0.22 → 0 → 0.16 → 0 on the just-added row
    LaunchedEffect(pendingHighlightId) {
        val id = pendingHighlightId ?: return@LaunchedEffect
        highlightRowId = id
        pulse.snapTo(0f)
        pulse.animateTo(0.22f, androidx.compose.animation.core.tween(300))
        pulse.animateTo(0f, androidx.compose.animation.core.tween(300))
        pulse.animateTo(0.16f, androidx.compose.animation.core.tween(300))
        pulse.animateTo(0f, androidx.compose.animation.core.tween(900))
        highlightRowId = null
        pendingHighlightId = null
    }

    androidx.compose.material3.Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAdd,
                // RN FAB order: label first, then the icon ("Add ➕")
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Add")
                        MciIcon("plus", 24.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.pressScale(onClick = onOpenAdd),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Spendr",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${formatMonthYear(System.currentTimeMillis())} overview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderCircleButton(icon = { MciIcon("magnify", 24.dp, MaterialTheme.colorScheme.onSurface) }, onClick = onOpenSearch)
                        HeaderCircleButton(icon = { MciIcon("cog-outline", 24.dp, MaterialTheme.colorScheme.onSurface) }, onClick = onOpenSettings)
                    }
                }
            }
            item {
                SpendingPaceCard(pace = state.pace, onOpenReport = onOpenReports)
            }
            if (quickAdds.isNotEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MciIcon(
                                        "lightning-bolt",
                                        20.dp,
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                Column {
                                    Text("QuickAdd", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Prefilled and ready to save",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            quickAdds.chunked(3).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 8.dp),
                                ) {
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
            }
            item {
                SectionHeader(title = "Recent transactions", action = {
                    BouncyTextButton(onClick = onOpenTransactions) {
                        Text("See all", fontWeight = FontWeight.Bold)
                    }
                })
            }
            if (state.recent.isEmpty()) {
                item {
                    Text(
                        "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.recent, key = { it.transaction.id }) { row ->
                    TransactionRow(
                        row = row,
                        onClick = { onOpenDetail(row.transaction.id) },
                        showDate = true,
                        showDayOfWeek = true,
                        highlight = if (row.transaction.id == highlightRowId) pulse.value else 0f,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(icon: @Composable () -> Unit, onClick: () -> Unit) {
    BouncySurface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
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
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .height(116.dp)
            .pressScale(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            CategoryIconBadge(icon = icon, color = color, size = 44.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
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
