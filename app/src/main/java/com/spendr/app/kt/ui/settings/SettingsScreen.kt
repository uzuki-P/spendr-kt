package com.spendr.app.kt.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spendr.app.kt.AppContainer
import com.spendr.app.kt.data.settings.ColorSource
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.data.settings.ThemeMode
import com.spendr.app.kt.data.settings.VibrationStrength
import com.spendr.app.kt.platform.vibrationMs
import com.spendr.app.kt.ui.components.BouncyTextButton
import com.spendr.app.kt.ui.components.CategoryIconBadge
import com.spendr.app.kt.ui.components.Corners
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.components.MorphSurface
import com.spendr.app.kt.ui.components.MorphingBadge
import com.spendr.app.kt.ui.components.SegmentedGroup
import com.spendr.app.kt.ui.components.SpendrTopBar
import com.spendr.app.kt.ui.components.pressScale
import com.spendr.app.kt.ui.components.rememberCollapsingBar
import com.spendr.app.kt.ui.components.segmentCorners
import com.spendr.app.kt.ui.theme.PRESET_SEEDS
import kotlinx.coroutines.launch

/** Settings: Appearance, Spending, Data, Haptics, Developer. */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit = {},
    onOpenBackup: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenQuickAdd: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    // Null until DataStore emits: the body waits for the stored values so the
    // toggles and swatch panel don't animate over from defaults on every open
    val loaded by container.settings.settings.collectAsState(initial = null)
    val settings = loaded ?: Settings()
    val scope = rememberCoroutineScope()
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scrollBehavior = rememberCollapsingBar()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SpendrTopBar(title = "Settings", onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        if (loaded == null) return@Scaffold
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GroupLabel("Appearance")
            ConnectedChoices(
                options = listOf(
                    Choice(ThemeMode.SYSTEM, "System", "theme-light-dark"),
                    Choice(ThemeMode.LIGHT, "Light", "white-balance-sunny"),
                    Choice(ThemeMode.DARK, "Dark", "moon-waning-crescent"),
                ),
                selected = settings.themeMode,
                onSelect = { scope.launch { container.settings.setThemeMode(it) } },
            )

            GroupLabel("Theme color")
            ConnectedChoices(
                options = listOf(
                    Choice(ColorSource.DEFAULT, "Default", "format-color-fill"),
                    Choice(ColorSource.WALLPAPER, "Wallpaper", "wallpaper"),
                    Choice(ColorSource.USER, "Custom", "palette-outline"),
                ),
                selected = settings.colorSource,
                onSelect = { scope.launch { container.settings.setColorSource(it) } },
            )
            AnimatedVisibility(
                visible = settings.colorSource == ColorSource.WALLPAPER &&
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S,
            ) {
                Text(
                    "Wallpaper colors are only available on Android 12 and above. " +
                        "Using the default brand color instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            AnimatedVisibility(
                visible = settings.colorSource == ColorSource.USER,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    SwatchGrid(
                        selectedHex = settings.userSeed ?: com.spendr.app.kt.ui.theme.DEFAULT_SEED,
                        onPick = { hex -> scope.launch { container.settings.setUserSeed(hex) } },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            GroupLabel("Spending")
            SegmentedGroup {
                SettingsRow(
                    corners = segmentCorners(0, 3),
                    icon = { CategoryIconBadge("shape-outline", "#A86086", 40.dp) },
                    title = "Manage categories",
                    subtitle = "Add, edit, reorder categories",
                    onClick = onOpenCategories,
                )
                SettingsRow(
                    corners = segmentCorners(1, 3),
                    icon = { CategoryIconBadge("lightning-bolt", "#A86829", 40.dp) },
                    title = "Manage quick add",
                    subtitle = "Fast input shortcuts",
                    onClick = onOpenQuickAdd,
                )
                SettingsRow(
                    corners = segmentCorners(2, 3),
                    icon = { CategoryIconBadge("currency-usd", "#3C7CAB", 40.dp) },
                    title = "Currency",
                    subtitle = "Rupiah (IDR)",
                )
            }

            GroupLabel("Data")
            SegmentedGroup {
                SettingsRow(
                    corners = segmentCorners(0, 2),
                    icon = { CategoryIconBadge("backup-restore", "#098396", 40.dp) },
                    title = "Backup & restore",
                    subtitle = "Archives, restore, CSV import and export",
                    onClick = onOpenBackup,
                )
                SettingsRow(
                    corners = segmentCorners(1, 2),
                    icon = {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialShapes.Cookie9Sided.toShape(),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            MciIcon("database-remove-outline", 20.dp, MaterialTheme.colorScheme.onErrorContainer)
                        }
                    },
                    title = "Clear database",
                    subtitle = "Erase local data and restore defaults",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true },
                )
            }

            GroupLabel("Haptics")
            SegmentedGroup {
                SettingsRow(
                    corners = segmentCorners(0, 1),
                    icon = { CategoryIconBadge("vibrate", "#7B6A9E", 40.dp) },
                    title = "Vibration strength",
                    subtitle = describeVibration(settings),
                    onClick = { showVibrationDialog = true },
                )
            }

            GroupLabel("Developer")
            SegmentedGroup {
                SettingsRow(
                    corners = segmentCorners(0, 1),
                    icon = { CategoryIconBadge("bug-outline", "#9366A4", 40.dp) },
                    title = "Debug",
                    subtitle = "Developer-only tools and sample data",
                    onClick = onOpenDebug,
                )
            }

            Text(
                "spendr-kt v${com.spendr.app.kt.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
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
            icon = { MciIcon("database-remove-outline", 24.dp, MaterialTheme.colorScheme.error) },
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
                BouncyTextButton(
                    onClick = {
                        scope.launch {
                            com.spendr.app.kt.data.db.resetDomainDatabase(container.database)
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Clear database") }
            },
            dismissButton = { BouncyTextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}

private data class Choice<T>(val value: T, val label: String, val glyph: String)

/** Full-width connected toggle buttons; the checked one rounds fully. */
@Composable
private fun <T> ConnectedChoices(
    options: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = option.value == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(option.value) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MciIcon(option.glyph, 22.dp, LocalContentColor.current)
                    Text(
                        option.label,
                        style = if (checked) {
                            MaterialTheme.typography.labelLargeEmphasized
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Seed swatches; the picked one morphs from a circle into a scalloped cookie. */
@Composable
private fun SwatchGrid(selectedHex: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val normalized = selectedHex.uppercase()
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESET_SEEDS.forEach { hex ->
            val isSelected = hex.uppercase() == normalized
            MorphingBadge(
                selected = isSelected,
                color = com.spendr.app.kt.ui.theme.parseSeedColor(hex),
                size = 48.dp,
                modifier = Modifier
                    .pressScale(pressedScale = 0.88f, onClick = { onPick(hex) })
                    .semantics { contentDescription = "Seed color $hex" },
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                    exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
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
                    val sliderState = rememberSliderState(
                        value = customMs.toFloat(),
                        trackRange = 1f..200f,
                    )
                    Slider(
                        state = sliderState,
                        onValueChange = {
                            sliderState.value = it
                            customMs = it.toInt().coerceIn(1, 200)
                            preview(VibrationStrength.CUSTOM, customMs)
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Barely felt", style = MaterialTheme.typography.labelSmall)
                        Text("Strong", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            BouncyTextButton(onClick = { onSave(draft, if (draft == VibrationStrength.CUSTOM) customMs else null) }) {
                Text("Save")
            }
        },
        dismissButton = { BouncyTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val VibrationStrength.ms: Int
    get() = when (this) {
        VibrationStrength.LIGHT -> 12
        VibrationStrength.DEFAULT -> 25
        VibrationStrength.STRONG -> 50
        else -> 0
    }
