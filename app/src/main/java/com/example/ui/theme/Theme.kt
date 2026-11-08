package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.storage.AccentColorPreset
import com.example.data.storage.ThemeMode

private fun buildDarkColorScheme(accent: Color, amoledDark: Boolean): ColorScheme {
    val bg = if (amoledDark) AmoledDarkBg else StudioDarkBg
    val surface = if (amoledDark) AmoledCardSurface else StudioCardSurface
    val surfaceVariant = if (amoledDark) AmoledCardSurfaceVariant else StudioCardSurfaceVariant
    val outline = if (amoledDark) AmoledBorder else StudioBorder

    return darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = VioletAccent,
        onSecondary = Color.White,
        secondaryContainer = VioletDark,
        onSecondaryContainer = Color.White,
        tertiary = CyanAccent,
        onTertiary = Color.Black,
        background = bg,
        onBackground = StudioTextPrimary,
        surface = surface,
        onSurface = StudioTextPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = StudioTextSecondary,
        outline = outline,
        error = RoseAccent,
        onError = Color.White
    )
}

private fun buildLightColorScheme(accent: Color): ColorScheme {
    return lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.15f),
        onPrimaryContainer = Color.Black,
        secondary = VioletDark,
        onSecondary = Color.White,
        tertiary = CyanAccent,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color.White,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFFCBD5E1),
        error = RoseAccent
    )
}

@Composable
fun SonicTagTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    accentPreset: AccentColorPreset = AccentColorPreset.EMERALD,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val isSystemDynamicAvailable = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val (colorScheme, activeAccent) = when {
        isSystemDynamicAvailable -> {
            val dynamicScheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }

            val finalScheme = if (darkTheme && amoledDark) {
                dynamicScheme.copy(
                    background = AmoledDarkBg,
                    surface = AmoledCardSurface,
                    surfaceVariant = AmoledCardSurfaceVariant,
                    outline = AmoledBorder
                )
            } else {
                dynamicScheme
            }
            Pair(finalScheme, dynamicScheme.primary)
        }
        darkTheme -> {
            val presetColor = Color(accentPreset.hexColor)
            Pair(buildDarkColorScheme(presetColor, amoledDark), presetColor)
        }
        else -> {
            val presetColor = Color(accentPreset.hexColor)
            Pair(buildLightColorScheme(presetColor), presetColor)
        }
    }

    CompositionLocalProvider(LocalAccentColor provides activeAccent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SonicTagTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
