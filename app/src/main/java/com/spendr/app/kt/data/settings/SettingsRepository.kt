package com.spendr.app.kt.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ColorSource { DEFAULT, WALLPAPER, USER }
enum class VibrationStrength { OFF, LIGHT, DEFAULT, STRONG, CUSTOM }
enum class BackupFrequency { OFF, DAILY, WEEKLY, MONTHLY }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.DEFAULT,
    val userSeed: String? = null,
    val vibrationStrength: VibrationStrength = VibrationStrength.DEFAULT,
    val customVibrationMs: Int = 25,
    val backupFrequency: BackupFrequency = BackupFrequency.OFF,
    val backupRotation: Int = 7,
    val backupDirectoryUri: String? = null,
    val lastBackupAt: Long? = null,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "spendr_settings")

/** Port of the RN `settingsStore` to DataStore Preferences. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            themeMode = p[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorSource = p[KEY_COLOR_SOURCE]?.let { runCatching { ColorSource.valueOf(it) }.getOrNull() }
                ?: ColorSource.DEFAULT,
            userSeed = p[KEY_USER_SEED],
            vibrationStrength = p[KEY_VIBRATION]?.let { runCatching { VibrationStrength.valueOf(it) }.getOrNull() }
                ?: VibrationStrength.DEFAULT,
            customVibrationMs = (p[KEY_CUSTOM_VIBRATION_MS] ?: 25).coerceIn(1, 200),
            backupFrequency = p[KEY_BACKUP_FREQUENCY]?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() }
                ?: BackupFrequency.OFF,
            backupRotation = (p[KEY_BACKUP_ROTATION] ?: 7).let { maxOf(1, minOf(365, it)) },
            backupDirectoryUri = p[KEY_BACKUP_DIR],
            lastBackupAt = p[KEY_LAST_BACKUP_AT],
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setColorSource(source: ColorSource) = store.edit { it[KEY_COLOR_SOURCE] = source.name }
    suspend fun setUserSeed(hex: String?) = store.edit {
        if (hex == null) it.remove(KEY_USER_SEED) else it[KEY_USER_SEED] = hex
    }

    suspend fun setVibration(strength: VibrationStrength, customMs: Int? = null) = store.edit {
        it[KEY_VIBRATION] = strength.name
        if (customMs != null) it[KEY_CUSTOM_VIBRATION_MS] = customMs.coerceIn(1, 200)
    }

    suspend fun setBackup(
        frequency: BackupFrequency? = null,
        rotation: Int? = null,
        directoryUri: String? = null,
        lastBackupAt: Long? = null,
    ) = store.edit { p ->
        frequency?.let { p[KEY_BACKUP_FREQUENCY] = it.name }
        rotation?.let { p[KEY_BACKUP_ROTATION] = maxOf(1, minOf(365, it)) }
        if (directoryUri != null) p[KEY_BACKUP_DIR] = directoryUri
        if (lastBackupAt != null) p[KEY_LAST_BACKUP_AT] = lastBackupAt
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLOR_SOURCE = stringPreferencesKey("color_source")
        val KEY_USER_SEED = stringPreferencesKey("user_seed")
        val KEY_VIBRATION = stringPreferencesKey("vibration_strength")
        val KEY_CUSTOM_VIBRATION_MS = intPreferencesKey("custom_vibration_ms")
        val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val KEY_BACKUP_ROTATION = intPreferencesKey("backup_rotation")
        val KEY_BACKUP_DIR = stringPreferencesKey("backup_directory_uri")
        val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }
}
