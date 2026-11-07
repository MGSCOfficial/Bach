package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = VioletDark,
    onSecondaryContainer = Color.White,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = StudioTextPrimary,
    surface = StudioCardSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioCardSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = RoseAccent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
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

@Composable
fun SonicTagTheme(
    darkTheme: Boolean = true, // Default to sleek music studio dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SonicTagTheme(darkTheme = darkTheme, content = content)
}
