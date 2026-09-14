package com.spendr.app.kt

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow

class SpendrApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        /** Latest cold/warm deep link URI, consumed by the nav host. */
        val pendingDeepLink = MutableStateFlow<String?>(null)
    }
}
