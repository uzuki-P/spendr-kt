package com.spendr.app.kt.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.spendr.app.kt.data.settings.ColorSource
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.data.settings.ThemeMode

/** MD2 Pink 500 — the RN app's default brand seed. */
const val DEFAULT_SEED = "#E91E63"

/** The 12 preset seeds from the RN settings screen, in order. */
val PRESET_SEEDS = listOf(
    "#E91E63", "#E65100", "#F9A825", "#558B2F", "#2E7D32", "#00838F",
    "#1565C0", "#3F51B5", "#6A1B9A", "#AD1457", "#5D4037", "#424242",
)

/** RN radius tokens: xs 8, sm 12, md 16, lg 20, xl 28. */
private val SpendrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Spendr semantic color aliases (RN `buildTheme`): expense = primary,
 * savings = tertiary, success = custom HCT hue 135 chroma 16 palette.
 */
data class SpendrColors(
    val expense: Color,
    val savings: Color,
    val success: Color,
    val onSuccess: Color,
    val textTertiary: Color,
    val border: Color,
    val overlay: Color,
)

private val lightSuccess = Color(0xFF56624B)
private val darkSuccess = Color(0xFFBECBAE)

private val LocalSpendrColors = staticCompositionLocalOf {
    SpendrColors(
        expense = Color.Unspecified,
        savings = Color.Unspecified,
        success = Color.Unspecified,
        onSuccess = Color.White,
        textTertiary = Color.Unspecified,
        border = Color.Unspecified,
        overlay = Color.Unspecified,
    )
}

object SpendrThemeDefaults {
    val shapes: Shapes = SpendrShapes
}

object SpendrTheme {
    val colors: SpendrColors
        @Composable get() = LocalSpendrColors.current
}

fun resolveSeed(settings: Settings): Color {
    val hex = when (settings.colorSource) {
        ColorSource.USER -> settings.userSeed ?: DEFAULT_SEED
        else -> DEFAULT_SEED
    }
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(Color(0xFFE91E63))
}

@Composable
fun SpendrTheme(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme: ColorScheme = when {
        settings.colorSource == ColorSource.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                dynamicLightColorScheme(LocalContext.current)
            }
        settings.colorSource == ColorSource.WALLPAPER && dark -> darkColorScheme()
        settings.colorSource == ColorSource.WALLPAPER -> lightColorScheme()
        else -> dynamicColorScheme(
            resolveSeed(settings),
            dark,
            false,
            style = PaletteStyle.TonalSpot,
            contrastLevel = 0.0,
        )
    }

    val spendrColors = SpendrColors(
        expense = scheme.primary,
        savings = scheme.tertiary,
        success = if (dark) darkSuccess else lightSuccess,
        onSuccess = if (dark) Color(0xFF29341F) else Color.White,
        textTertiary = scheme.onSurface.copy(alpha = 0.6f),
        border = scheme.outlineVariant,
        overlay = scheme.onSurface.copy(alpha = 0.08f),
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalSpendrColors provides spendrColors) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = SpendrShapes,
            content = content,
        )
    }
}
