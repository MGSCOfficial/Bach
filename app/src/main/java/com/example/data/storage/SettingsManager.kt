package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode(val label: String) {
    DARK("Modo Oscuro"),
    LIGHT("Modo Claro"),
    SYSTEM("Seguir al Sistema")
}

enum class AccentColorPreset(val id: String, val displayName: String, val hexColor: Long) {
    EMERALD("emerald", "Esmeralda", 0xFF10B981),
    VIOLET("violet", "Violeta", 0xFF8B5CF6),
    CYAN("cyan", "Cian", 0xFF06B6D4),
    AMBER("amber", "Ámbar", 0xFFF59E0B),
    ROSE("rose", "Rosa", 0xFFF43F5E),
    BLUE("blue", "Azul", 0xFF3B82F6)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val accentPreset: AccentColorPreset = AccentColorPreset.EMERALD,
    val amoledDark: Boolean = false,

    // Lyrics behavior
    val autoSearchLyrics: Boolean = true,
    val defaultSaveLrc: Boolean = true,
    val defaultEmbedLyrics: Boolean = true,

    // Metadata Sources (Gratuitas)
    val sourceMusicBrainz: Boolean = true,
    val sourceITunes: Boolean = true,
    val sourceDeezer: Boolean = true,
    val sourceTheAudioDb: Boolean = false,
    val sourceNetEase: Boolean = false,

    // Metadata Sources (Con API Key)
    val sourceSpotify: Boolean = false,
    val sourceLastFm: Boolean = false,
    val sourceDiscogs: Boolean = false,
    val spotifyClientId: String = "",
    val spotifyClientSecret: String = "",
    val lastFmApiKey: String = "",
    val discogsToken: String = "",

    // Lyrics Sources
    val lyricsSourceLrclib: Boolean = true,
    val lyricsSourceNetEase: Boolean = false,
    val lyricsSourceGenius: Boolean = false,
    val lyricsSourceMusixmatch: Boolean = false,
    val geniusToken: String = "",
    val musixmatchApiKey: String = "",

    // Google Gemini AI
    val geminiEnabled: Boolean = false,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash"
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sound_id_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_ACCENT_PRESET = "pref_accent_preset"
        private const val KEY_AMOLED_DARK = "pref_amoled_dark"

        private const val KEY_AUTO_SEARCH_LYRICS = "pref_auto_search_lyrics"
        private const val KEY_DEFAULT_SAVE_LRC = "pref_default_save_lrc"
        private const val KEY_DEFAULT_EMBED_LYRICS = "pref_default_embed_lyrics"

        // Metadata Sources
        private const val KEY_SRC_MUSICBRAINZ = "pref_src_musicbrainz"
        private const val KEY_SRC_ITUNES = "pref_src_itunes"
        private const val KEY_SRC_DEEZER = "pref_src_deezer"
        private const val KEY_SRC_THEAUDIODB = "pref_src_theaudiodb"
        private const val KEY_SRC_NETEASE = "pref_src_netease"

        private const val KEY_SRC_SPOTIFY = "pref_src_spotify"
        private const val KEY_SRC_LASTFM = "pref_src_lastfm"
        private const val KEY_SRC_DISCOGS = "pref_src_discogs"
        private const val KEY_KEY_SPOTIFY_ID = "pref_key_spotify_id"
        private const val KEY_KEY_SPOTIFY_SECRET = "pref_key_spotify_secret"
        private const val KEY_KEY_LASTFM = "pref_key_lastfm"
        private const val KEY_KEY_DISCOGS = "pref_key_discogs"

        // Lyrics Sources
        private const val KEY_LYR_LRCLIB = "pref_lyr_lrclib"
        private const val KEY_LYR_NETEASE = "pref_lyr_netease"
        private const val KEY_LYR_GENIUS = "pref_lyr_genius"
        private const val KEY_LYR_MUSIXMATCH = "pref_lyr_musixmatch"
        private const val KEY_KEY_GENIUS = "pref_key_genius"
        private const val KEY_KEY_MUSIXMATCH = "pref_key_musixmatch"

        // Gemini AI
        private const val KEY_GEMINI_ENABLED = "pref_gemini_enabled"
        private const val KEY_GEMINI_API_KEY = "pref_gemini_api_key"
        private const val KEY_GEMINI_MODEL = "pref_gemini_model"
    }

    fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }

        val accentStr = prefs.getString(KEY_ACCENT_PRESET, AccentColorPreset.EMERALD.name) ?: AccentColorPreset.EMERALD.name
        val accentPreset = try {
            AccentColorPreset.valueOf(accentStr)
        } catch (e: Exception) {
            AccentColorPreset.EMERALD
        }

        return AppSettings(
            themeMode = themeMode,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            accentPreset = accentPreset,
            amoledDark = prefs.getBoolean(KEY_AMOLED_DARK, false),

            autoSearchLyrics = prefs.getBoolean(KEY_AUTO_SEARCH_LYRICS, true),
            defaultSaveLrc = prefs.getBoolean(KEY_DEFAULT_SAVE_LRC, true),
            defaultEmbedLyrics = prefs.getBoolean(KEY_DEFAULT_EMBED_LYRICS, true),

            sourceMusicBrainz = prefs.getBoolean(KEY_SRC_MUSICBRAINZ, true),
            sourceITunes = prefs.getBoolean(KEY_SRC_ITUNES, true),
            sourceDeezer = prefs.getBoolean(KEY_SRC_DEEZER, true),
            sourceTheAudioDb = prefs.getBoolean(KEY_SRC_THEAUDIODB, false),
            sourceNetEase = prefs.getBoolean(KEY_SRC_NETEASE, false),

            sourceSpotify = prefs.getBoolean(KEY_SRC_SPOTIFY, false),
            sourceLastFm = prefs.getBoolean(KEY_SRC_LASTFM, false),
            sourceDiscogs = prefs.getBoolean(KEY_SRC_DISCOGS, false),
            spotifyClientId = prefs.getString(KEY_KEY_SPOTIFY_ID, "") ?: "",
            spotifyClientSecret = prefs.getString(KEY_KEY_SPOTIFY_SECRET, "") ?: "",
            lastFmApiKey = prefs.getString(KEY_KEY_LASTFM, "") ?: "",
            discogsToken = prefs.getString(KEY_KEY_DISCOGS, "") ?: "",

            lyricsSourceLrclib = prefs.getBoolean(KEY_LYR_LRCLIB, true),
            lyricsSourceNetEase = prefs.getBoolean(KEY_LYR_NETEASE, false),
            lyricsSourceGenius = prefs.getBoolean(KEY_LYR_GENIUS, false),
            lyricsSourceMusixmatch = prefs.getBoolean(KEY_LYR_MUSIXMATCH, false),
            geniusToken = prefs.getString(KEY_KEY_GENIUS, "") ?: "",
            musixmatchApiKey = prefs.getString(KEY_KEY_MUSIXMATCH, "") ?: "",

            geminiEnabled = prefs.getBoolean(KEY_GEMINI_ENABLED, false),
            geminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: "",
            geminiModel = prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setAccentPreset(preset: AccentColorPreset) {
        prefs.edit().putString(KEY_ACCENT_PRESET, preset.name).apply()
    }

    fun setAmoledDark(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED_DARK, enabled).apply()
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

    fun setSourceEnabled(sourceKey: String, enabled: Boolean) {
        val prefKey = when (sourceKey) {
            "musicbrainz" -> KEY_SRC_MUSICBRAINZ
            "itunes" -> KEY_SRC_ITUNES
            "deezer" -> KEY_SRC_DEEZER
            "theaudiodb" -> KEY_SRC_THEAUDIODB
            "netease" -> KEY_SRC_NETEASE
            "spotify" -> KEY_SRC_SPOTIFY
            "lastfm" -> KEY_SRC_LASTFM
            "discogs" -> KEY_SRC_DISCOGS
            "lrclib" -> KEY_LYR_LRCLIB
            "netease_lyrics" -> KEY_LYR_NETEASE
            "genius" -> KEY_LYR_GENIUS
            "musixmatch" -> KEY_LYR_MUSIXMATCH
            else -> return
        }
        prefs.edit().putBoolean(prefKey, enabled).apply()
    }

    fun setApiKey(keyIdentifier: String, value: String) {
        val prefKey = when (keyIdentifier) {
            "spotify_id" -> KEY_KEY_SPOTIFY_ID
            "spotify_secret" -> KEY_KEY_SPOTIFY_SECRET
            "lastfm" -> KEY_KEY_LASTFM
            "discogs" -> KEY_KEY_DISCOGS
            "genius" -> KEY_KEY_GENIUS
            "musixmatch" -> KEY_KEY_MUSIXMATCH
            "gemini" -> KEY_GEMINI_API_KEY
            else -> return
        }
        prefs.edit().putString(prefKey, value.trim()).apply()
    }

    fun setGeminiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GEMINI_ENABLED, enabled).apply()
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun setGeminiModel(model: String) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model.trim()).apply()
    }
}
