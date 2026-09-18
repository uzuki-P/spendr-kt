package com.spendr.app.kt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.spendr.app.kt.data.settings.Settings
import com.spendr.app.kt.platform.vibrationMs
import com.spendr.app.kt.ui.SpendrApp
import com.spendr.app.kt.ui.theme.SpendrTheme

/** App-wide haptic hook honoring the vibration strength setting. */
val LocalVibrate = staticCompositionLocalOf { {} }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPeakRefreshRate()
        val container = (application as SpendrApplication).container
        setContent {
            val settings by container.settings.settings
                .collectAsState(initial = Settings())
            CompositionLocalProvider(
                LocalVibrate provides { container.vibrator.vibrate(settings.vibrationMs) },
            ) {
                SpendrTheme(settings = settings) {
                    SpendrApp(container)
                }
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * OEM frame-rate policy pins third-party apps to 60Hz unless the app
     * asks, so opt the window into the display's fastest mode at the current
     * resolution. Without this, every animation runs at half rate on 120Hz
     * panels no matter how cheap the frames are.
     */
    private fun requestPeakRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return
        val current = display.mode
        val peak = display.supportedModes
            .filter {
                it.physicalWidth == current.physicalWidth &&
                    it.physicalHeight == current.physicalHeight
            }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = peak.modeId
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data ?: return
        SpendrApplication.pendingDeepLink.value = intent.data?.toString()
    }
}
