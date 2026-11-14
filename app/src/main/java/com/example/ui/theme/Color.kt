package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Bach Classical & Elegant Palette
val BachGoldAccent = Color(0xFFC6A56E)
val BachGoldLight = Color(0xFFDFBF88)
val BachGoldDark = Color(0xFFA88950)

val NavySlateAccent = Color(0xFF2563EB)
val RoyalBronzeAccent = Color(0xFFB45309)
val BaroqueEmeraldAccent = Color(0xFF059669)
val ImperialVioletAccent = Color(0xFF7C3AED)
val CabernetAccent = Color(0xFF9F1239)

// Legacy compatibility aliases
val EmeraldAccent = BaroqueEmeraldAccent
val VioletAccent = ImperialVioletAccent
val CyanAccent = Color(0xFF0284C7)
val AmberAccent = RoyalBronzeAccent
val RoseAccent = CabernetAccent
val BlueAccent = NavySlateAccent

// Dark Studio Surfaces
val StudioDarkBg = Color(0xFF0D0F14)
val StudioCardSurface = Color(0xFF161922)
val StudioCardSurfaceVariant = Color(0xFF202534)
val StudioBorder = Color(0xFF2C3246)
val StudioTextPrimary = Color(0xFFF9FAFB)
val StudioTextSecondary = Color(0xFF9CA3AF)
val StudioTextMuted = Color(0xFF6B7280)

// Pure AMOLED Dark Surfaces
val AmoledDarkBg = Color(0xFF000000)
val AmoledCardSurface = Color(0xFF0A0C10)
val AmoledCardSurfaceVariant = Color(0xFF12151D)
val AmoledBorder = Color(0xFF1E222D)

// Dynamic Local Accent Provider
val LocalAccentColor = staticCompositionLocalOf { BachGoldAccent }
