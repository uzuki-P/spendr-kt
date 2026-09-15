package com.spendr.app.kt.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.spendr.app.kt.data.settings.ColorSource
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.data.settings.ThemeMode
import com.spendr.app.kt.data.settings.VibrationStrength
import com.spendr.app.kt.platform.vibrationMs
import com.spendr.app.kt.ui.theme.PRESET_SEEDS
import com.spendr.app.kt.ui.theme.resolveSeed
import kotlinx.coroutines.launch

/** Settings, ported from RN `SettingsScreen` (Appearance, Spending, Data, Haptics, Developer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenBackup: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenQuickAdd: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    val settings by container.settings.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            GroupLabel("Appearance")
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceTiles(
                            label = "Theme mode",
                            options = listOf(
                                Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.Info),
                                Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
                                Triple(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
                            ),
                            selected = settings.themeMode,
                            labelOf = { it.name },
                            onSelect = { scope.launch { container.settings.setThemeMode(it) } },
                        )
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Theme color source", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ColorSourceTile(
                                    label = "Default",
                                    icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                                    selected = settings.colorSource == ColorSource.DEFAULT,
                                    onClick = { scope.launch { container.settings.setColorSource(ColorSource.DEFAULT) } },
                                    modifier = Modifier.weight(1f),
                                )
                                ColorSourceTile(
                                    label = "Wallpaper",
                                    icon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) },
                                    selected = settings.colorSource == ColorSource.WALLPAPER,
                                    onClick = { scope.launch { container.settings.setColorSource(ColorSource.WALLPAPER) } },
                                    modifier = Modifier.weight(1f),
                                )
                                ColorSourceTile(
                                    label = "Custom",
                                    icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                                    selected = settings.colorSource == ColorSource.USER,
                                    onClick = { scope.launch { container.settings.setColorSource(ColorSource.USER) } },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (settings.colorSource == ColorSource.WALLPAPER &&
                                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
                            ) {
                                Text(
                                    "Wallpaper colors are only available on Android 12 and above. " +
                                        "Using the default brand color instead.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (settings.colorSource == ColorSource.USER) {
                                SwatchGrid(
                                    selectedHex = settings.userSeed ?: com.spendr.app.kt.ui.theme.DEFAULT_SEED,
                                    onPick = { hex -> scope.launch { container.settings.setUserSeed(hex) } },
                                )
                            }
                        }
                    }
                }

                GroupLabel("Spending")
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Column {
                        SettingsRow(
                            icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("shape-outline", "#A86086", 38.dp) },
                            title = "Manage categories",
                            subtitle = "Add, edit, reorder categories",
                            onClick = onOpenCategories,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("lightning-bolt", "#A86829", 38.dp) },
                            title = "Manage quick add",
                            subtitle = "Fast input shortcuts",
                            onClick = onOpenQuickAdd,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("currency-usd", "#3C7CAB", 38.dp) },
                            title = "Currency",
                            subtitle = "Rupiah (IDR)",
                        )
                    }
                }

                GroupLabel("Data")
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Column {
                        SettingsRow(
                            icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("backup-restore", "#098396", 38.dp) },
                            title = "Backup & restore",
                            subtitle = "Archives, restore, CSV import and export",
                            onClick = onOpenBackup,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    com.spendr.app.kt.ui.components.MciIcon(
                                        "database-remove-outline",
                                        20.dp,
                                        MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                            title = "Clear database",
                            subtitle = "Erase local data and restore defaults",
                            onClick = { showClearDialog = true },
                        )
                    }
                }

                GroupLabel("Haptics")
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    SettingsRow(
                        icon = { Icon(Icons.Outlined.Vibration, contentDescription = null) },
                        title = "Vibration strength",
                        subtitle = describeVibration(settings),
                        onClick = { showVibrationDialog = true },
                    )
                }

                GroupLabel("Developer")
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    SettingsRow(
                        icon = { com.spendr.app.kt.ui.components.CategoryIconBadge("bug-outline", "#9366A4", 38.dp) },
                        title = "Debug",
                        subtitle = "Developer-only tools and sample data",
                        onClick = onOpenDebug,
                    )
                }

                Text(
                    "spendr-kt v0.4.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
    }

    if (showVibrationDialog) {
        VibrationStrengthDialog(
            current = settings,
            vibrateMs = { ms -> container.vibrator.vibrate(ms) },
            onSave = { strength, ms ->
                scope.launch { container.settings.setVibration(strength, ms) }
                showVibrationDialog = false
            },
            onDismiss = { showVibrationDialog = false },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear database") },
            text = {
                Text(
                    "This permanently deletes all transactions, merchants, categories, quick add " +
                        "shortcuts, and note suggestions from this device. Default categories and " +
                        "quick add shortcuts will be restored. Export a backup first if you need " +
                        "this data. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            com.spendr.app.kt.data.db.resetDomainDatabase(container.database)
                            showClearDialog = false
                        }
                    },
                ) { Text("Clear database") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}

/** RN GroupLabel: small uppercase secondary label, 4dp inset. */
@Composable
private fun GroupLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** 1dp divider inset 16dp, between grouped rows. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * RN ListItem row: 38dp leading badge, titleMedium 600 + bodySmall subtitle,
 * 16/12 padding, chevron when pressable — tighter than M3 ListItem.
 */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) icon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            com.spendr.app.kt.ui.components.MciIcon(
                "chevron-right",
                22.dp,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun describeVibration(settings: Settings): String =
    when (settings.vibrationStrength) {
        VibrationStrength.OFF -> "Off"
        VibrationStrength.CUSTOM -> "Custom · ${settings.customVibrationMs} ms"
        else -> {
            val label = settings.vibrationStrength.name.lowercase().replaceFirstChar { it.uppercase() }
            val ms = settings.vibrationMs
            "$label · $ms ms"
        }
    }

@Composable
private fun <T> ChoiceTiles(
    label: String,
    options: List<Triple<T, String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text, icon) ->
                val isSelected = value == selected
                Surface(
                    onClick = { onSelect(value) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSourceTile(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            icon()
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SwatchGrid(selectedHex: String, onPick: (String) -> Unit) {
    val normalized = selectedHex.uppercase()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESET_SEEDS.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hex ->
                    val isSelected = hex.uppercase() == normalized
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(resolveSeed(com.spendr.app.kt.data.settings.Settings(userSeed = hex)), CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { onPick(hex) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                repeat(6 - row.size) { Box(Modifier.size(40.dp)) }
            }
        }
    }
}

@Composable
private fun VibrationStrengthDialog(
    current: Settings,
    vibrateMs: (Int) -> Unit,
    onSave: (VibrationStrength, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current.vibrationStrength) }
    var customMs by remember { mutableIntStateOf(current.customVibrationMs) }
    var lastPreviewAt by remember { mutableLongStateOf(0L) }

    // RN useVibrationPreview: feel out the value being configured (not the
    // stored one), throttled so a slider drag reads as separate taps.
    fun preview(strength: VibrationStrength, ms: Int) {
        val resolved = when (strength) {
            VibrationStrength.OFF -> 0
            VibrationStrength.LIGHT -> 12
            VibrationStrength.DEFAULT -> 25
            VibrationStrength.STRONG -> 50
            VibrationStrength.CUSTOM -> ms
        }
        if (resolved <= 0) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPreviewAt < resolved + 60L) return
        lastPreviewAt = now
        vibrateMs(resolved)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vibration strength") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Haptic feedback while you drag across charts. Tap an option to feel it. Android only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(
                    VibrationStrength.OFF to "Off",
                    VibrationStrength.LIGHT to "Light",
                    VibrationStrength.DEFAULT to "Default",
                    VibrationStrength.STRONG to "Strong",
                    VibrationStrength.CUSTOM to "Custom",
                ).forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                draft = value
                                preview(value, customMs)
                            },
                    ) {
                        RadioButton(
                            selected = draft == value,
                            onClick = {
                                draft = value
                                preview(value, customMs)
                            },
                        )
                        Text(label, modifier = Modifier.weight(1f))
                        if (value != VibrationStrength.OFF) {
                            Text(
                                "${if (value == VibrationStrength.CUSTOM) customMs else value.ms} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (draft == VibrationStrength.CUSTOM) {
                    Slider(
                        value = customMs.toFloat(),
                        onValueChange = {
                            customMs = it.toInt().coerceIn(1, 200)
                            preview(VibrationStrength.CUSTOM, customMs)
                        },
                        valueRange = 1f..200f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Barely felt", style = MaterialTheme.typography.labelSmall)
                        Text("Strong", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft, if (draft == VibrationStrength.CUSTOM) customMs else null) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val VibrationStrength.ms: Int
    get() = when (this) {
        VibrationStrength.LIGHT -> 12
        VibrationStrength.DEFAULT -> 25
        VibrationStrength.STRONG -> 50
        else -> 0
    }
