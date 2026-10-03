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
import com.spendr.app.kt.data.vision.VisionConfiguration

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
    val vision: VisionConfiguration = VisionConfiguration(),
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
            vision = VisionConfiguration(
                url = p[KEY_VISION_URL] ?: VisionConfiguration().url,
                token = p[KEY_VISION_TOKEN] ?: VisionConfiguration().token,
                provider = p[KEY_VISION_PROVIDER].orEmpty(),
                model = p[KEY_VISION_MODEL].orEmpty(),
                reasoningEffort = p[KEY_VISION_EFFORT].orEmpty(),
            ),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setVision(configuration: VisionConfiguration) = store.edit {
        val config = configuration.validated(requireToken = false)
        it[KEY_VISION_URL] = config.url
        it[KEY_VISION_TOKEN] = config.token
        it[KEY_VISION_PROVIDER] = config.provider
        it[KEY_VISION_MODEL] = config.model
        it[KEY_VISION_EFFORT] = config.reasoningEffort
    }
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
        val KEY_VISION_URL = stringPreferencesKey("vision_url")
        val KEY_VISION_TOKEN = stringPreferencesKey("vision_token")
        val KEY_VISION_PROVIDER = stringPreferencesKey("vision_provider")
        val KEY_VISION_MODEL = stringPreferencesKey("vision_model")
        val KEY_VISION_EFFORT = stringPreferencesKey("vision_effort")
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
