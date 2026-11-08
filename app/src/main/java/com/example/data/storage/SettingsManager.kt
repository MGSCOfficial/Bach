package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode(val label: String) {
    DARK("Modo Oscuro"),
    LIGHT("Modo Claro"),
    SYSTEM("Seguir al Sistema")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val autoSearchLyrics: Boolean = true,
    val defaultSaveLrc: Boolean = true,
    val defaultEmbedLyrics: Boolean = true
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sound_id_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_AUTO_SEARCH_LYRICS = "pref_auto_search_lyrics"
        private const val KEY_DEFAULT_SAVE_LRC = "pref_default_save_lrc"
        private const val KEY_DEFAULT_EMBED_LYRICS = "pref_default_embed_lyrics"
    }

    fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }

        return AppSettings(
            themeMode = themeMode,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            autoSearchLyrics = prefs.getBoolean(KEY_AUTO_SEARCH_LYRICS, true),
            defaultSaveLrc = prefs.getBoolean(KEY_DEFAULT_SAVE_LRC, true),
            defaultEmbedLyrics = prefs.getBoolean(KEY_DEFAULT_EMBED_LYRICS, true)
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setAutoSearchLyrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SEARCH_LYRICS, enabled).apply()
    }

    fun setDefaultSaveLrc(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_SAVE_LRC, enabled).apply()
    }

    fun setDefaultEmbedLyrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_EMBED_LYRICS, enabled).apply()
    }
}
