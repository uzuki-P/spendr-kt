package com.spendr.app.kt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.spendr.app.kt.data.settings.ColorSource
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.data.settings.ThemeMode

/** MD2 Pink 500 — the RN app's default brand seed. */
const val DEFAULT_SEED = "#E91E63"

/**
 * Preset seeds for the custom theme picker, in order: the original 12 from the
 * RN settings screen first, then a full hue-wheel spread (pinks → reds →
 * oranges → greens → cyans → blues → purples → browns → greys).
 */
val PRESET_SEEDS = listOf(
    // Original 12 (RN settings screen)
    "#E91E63", "#E65100", "#F9A825", "#558B2F", "#2E7D32", "#00838F",
    "#1565C0", "#3F51B5", "#6A1B9A", "#AD1457", "#5D4037", "#424242",
    // Extended palette (kept at 30 total: 6 full rows of 5)
    "#D81B60", "#C62828", "#F4511E", "#FB8C00", "#9E9D24",
    "#1B5E20", "#00695C", "#00ACC1", "#039BE5", "#1E88E5",
    "#3949AB", "#5E35B1", "#8E24AA", "#FF6F00", "#6D4C41",
    "#546E7A", "#757575", "#212121",
)

/**
 * M3 Expressive corner scale. The base steps keep the RN radius tokens
 * (8/12/16/20/28); the expressive steps add the larger 24/32/48 corners used
 * by hero cards and sheets.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SpendrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
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

/** Parses a `#RRGGBB` seed hex, falling back to the brand pink. */
fun parseSeedColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFFE91E63))

fun resolveSeed(settings: Settings): Color {
    val hex = when (settings.colorSource) {
        ColorSource.USER -> settings.userSeed ?: DEFAULT_SEED
        else -> DEFAULT_SEED
    }
    return parseSeedColor(hex)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // Composable-call structure must not depend on `settings`: the scheme is
    // computed BEFORE anything user-facing in the tree (nav state included),
    // and a settings-driven branch flip here would re-key the whole subtree
    // below and reset rememberSaveable state (nav back stack → Home). Hoist
    // locals so every path performs the same composable calls.
    val context = LocalContext.current
    val scheme: ColorScheme = if (settings.colorSource == ColorSource.WALLPAPER) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
    } else {
        remember(resolveSeed(settings), dark) {
            dynamicColorScheme(
                resolveSeed(settings),
                dark,
                false,
                style = PaletteStyle.TonalSpot,
                contrastLevel = 0.0,
            )
        }
    }

    // System-bar icons follow the app's theme, not the system's, so a forced
    // Light/Dark choice keeps the status bar readable. Called on every path.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
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
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = SpendrShapes,
            typography = SpendrTypography,
            content = content,
        )
    }
}
