package com.spendr.app.kt

import android.content.Intent
import android.os.Bundle
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

    private fun handleDeepLink(intent: Intent?) {
        intent?.data ?: return
        SpendrApplication.pendingDeepLink.value = intent.data?.toString()
    }
}
