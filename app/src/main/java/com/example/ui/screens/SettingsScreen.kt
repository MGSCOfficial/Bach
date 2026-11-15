package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.storage.AccentColorPreset
import com.example.data.storage.ThemeMode
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.*
import com.example.ui.util.FileRenameHelper
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val availableModels by viewModel.availableGeminiModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingGeminiModels.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(settings.geminiApiKey) {
        if (settings.geminiApiKey.isNotBlank() && availableModels.isEmpty()) {
            viewModel.fetchAvailableGeminiModels()
        }
    }

    // State for API Key modal configuration
    var activeApiKeyTarget by remember { mutableStateOf<ApiKeyTarget?>(null) }
    var showGplDialog by remember { mutableStateOf(false) }

    // Dialog for App Update
    val showUpdateDetailsDialog by viewModel.showUpdateDetailsDialog.collectAsStateWithLifecycle()
    if (showUpdateDetailsDialog && updateInfo != null && updateInfo!!.isNewer) {
        AppUpdateDialog(
            info = updateInfo!!,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    activeApiKeyTarget?.let { target ->
        ApiKeyConfigDialog(
            target = target,
            currentKey1 = target.getCurrentKey1(settings),
            currentKey2 = target.getCurrentKey2(settings),
            onSave = { k1, k2 ->
                target.save(viewModel, k1, k2)
                activeApiKeyTarget = null
            },
            onDismiss = { activeApiKeyTarget = null }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ==================== SECCIÓN 1: APARIENCIA ====================
            SettingsSectionHeader(title = "Apariencia", icon = Icons.Default.Palette, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tema de la aplicación",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Theme selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { viewModel.updateThemeMode(mode) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.DARK -> "Oscuro"
                                            ThemeMode.LIGHT -> "Claro"
                                            ThemeMode.SYSTEM -> "Sistema"
                                        },
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Material You toggle
                    SettingsSwitchRow(
                        title = "Material You (Color dinámico)",
                        subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Adapta los colores del sistema operativo a la app"
                        } else {
                            "Disponible en Android 12 o superior"
                        },
                        checked = settings.dynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateDynamicColor(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // AMOLED Black toggle
                    SettingsSwitchRow(
                        title = "Negro AMOLED Puro",
                        subtitle = "Fondo 100% negro azabache (#000000) para máxima autonomía en pantallas OLED",
                        checked = settings.amoledDark,
                        enabled = settings.themeMode != ThemeMode.LIGHT,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateAmoledDark(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Accent color picker
                    Text(
                        text = "Color de acento personalizado",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Material You está activo. Desactívalo para elegir un color fijo."
                        } else {
                            "Selecciona el color de acento principal de la interfaz"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentColorPreset.values().forEach { preset ->
                            val presetColor = Color(preset.hexColor)
                            val isChosen = settings.accentPreset == preset && (!settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(presetColor)
                                    .clickable {
                                        if (settings.dynamicColor) {
                                            viewModel.updateDynamicColor(false)
                                        }
                                        viewModel.updateAccentPreset(preset)
                                    }
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECCIÓN 2: FUENTES DE METADATOS Y CARÁTULAS ====================
            SettingsSectionHeader(title = "Fuentes de Metadatos y Carátulas", icon = Icons.Default.CloudSync, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // MusicBrainz (Free, Default)
                    SettingsSourceRow(
                        name = "MusicBrainz",
                        description = "Base de datos abierta y colaborativa de música libre",
                        checked = settings.sourceMusicBrainz,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("musicbrainz", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // iTunes (Free, Default)
                    SettingsSourceRow(
                        name = "iTunes Search",
                        description = "Carátulas de altísima resolución (hasta 3000px) y catálogo oficial",
                        checked = settings.sourceITunes,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("itunes", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Deezer (Free, Default)
                    SettingsSourceRow(
                        name = "Deezer API",
                        description = "Excelente cobertura de lanzamientos modernos y metadatos",
                        checked = settings.sourceDeezer,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("deezer", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // TheAudioDB (Free alternative)
                    SettingsSourceRow(
                        name = "TheAudioDB",
                        description = "Catálogo comunitario abierto con carátulas de álbumes",
                        checked = settings.sourceTheAudioDb,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("theaudiodb", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // NetEase Cloud Music (Free alternative)
                    SettingsSourceRow(
                        name = "NetEase Cloud Music",
                        description = "Catálogo masivo con alta fidelidad para música asiática y global",
                        checked = settings.sourceNetEase,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("netease", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Spotify Web API (Requires Client ID + Secret)
                    SettingsSourceRow(
                        name = "Spotify Web API",
                        description = "Requiere Client ID y Client Secret de Spotify Developers",
                        checked = settings.sourceSpotify,
                        hasApiKey = true,
                        keyConfigured = settings.spotifyClientId.isNotBlank() && settings.spotifyClientSecret.isNotBlank(),
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("spotify", it) },
                        onConfigureKey = { activeApiKeyTarget = ApiKeyTarget.SPOTIFY }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Last.fm (Requires API Key)
                    SettingsSourceRow(
                        name = "Last.fm",
                        description = "Metadatos y biografía de artistas. Requiere API Key",
                        checked = settings.sourceLastFm,
                        hasApiKey = true,
                        keyConfigured = settings.lastFmApiKey.isNotBlank(),
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("lastfm", it) },
                        onConfigureKey = { activeApiKeyTarget = ApiKeyTarget.LASTFM }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Discogs (Requires Token)
                    SettingsSourceRow(
                        name = "Discogs",
                        description = "Base de datos más exhaustiva para vinilos y rarezas. Requiere Token",
                        checked = settings.sourceDiscogs,
                        hasApiKey = true,
                        keyConfigured = settings.discogsToken.isNotBlank(),
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("discogs", it) },
                        onConfigureKey = { activeApiKeyTarget = ApiKeyTarget.DISCOGS }
                    )
                }
            }

            // ==================== SECCIÓN 3: FUENTES DE LETRAS ====================
            SettingsSectionHeader(title = "Fuentes de Búsqueda de Letras", icon = Icons.Default.Lyrics, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // LRCLIB (Free, default)
                    SettingsSourceRow(
                        name = "LRCLIB",
                        description = "Base de letras sincronizadas (.lrc) y no sincronizadas gratuitas",
                        checked = settings.lyricsSourceLrclib,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("lrclib", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // NetEase Lyrics (Free alternative)
                    SettingsSourceRow(
                        name = "NetEase Lyrics",
                        description = "Servicio de letras sincronizadas con excelente catálogo internacional",
                        checked = settings.lyricsSourceNetEase,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("netease_lyrics", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Genius (Requires Client Access Token)
                    SettingsSourceRow(
                        name = "Genius",
                        description = "Letras completas anotadas. Requiere Client Access Token",
                        checked = settings.lyricsSourceGenius,
                        hasApiKey = true,
                        keyConfigured = settings.geniusToken.isNotBlank(),
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateSourceEnabled("genius", it) },
                        onConfigureKey = { activeApiKeyTarget = ApiKeyTarget.GENIUS }
                    )
                }
            }

            // ==================== SECCIÓN 4: COMPORTAMIENTO POR DEFECTO ====================
            SettingsSectionHeader(title = "Comportamiento por Defecto", icon = Icons.Default.Tune, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsSwitchRow(
                        title = "Buscar letras automáticamente",
                        subtitle = "Al aplicar metadatos online, consulta fuentes activas al instante",
                        checked = settings.autoSearchLyrics,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateAutoSearchLyrics(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSwitchRow(
                        title = "Guardar archivo .lrc complementario",
                        subtitle = "Crea un archivo .lrc sincronizado junto a la canción (protección contra null activa)",
                        checked = settings.defaultSaveLrc,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateDefaultSaveLrc(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSwitchRow(
                        title = "Incrustar letra en el archivo",
                        subtitle = "Escribe la letra directamente dentro de los tags del archivo MP3/M4A",
                        checked = settings.defaultEmbedLyrics,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateDefaultEmbedLyrics(it) }
                    )
                }
            }

            // ==================== SECCIÓN 5: INTELIGENCIA ARTIFICIAL (GOOGLE GEMINI) ====================
            SettingsSectionHeader(title = "Inteligencia Artificial (Google Gemini)", icon = Icons.Default.AutoAwesome, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsSwitchRow(
                        title = "Habilitar Google Gemini",
                        subtitle = "Identificación multimodal (OCR de carátula) y búsqueda/transcripción de letras",
                        checked = settings.geminiEnabled,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateGeminiEnabled(it) }
                    )

                    if (settings.geminiEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Fila de API Key con botón para configurar y enlace directo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Clave de API de Gemini",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (settings.geminiApiKey.isNotBlank()) "Configurada (••••${settings.geminiApiKey.takeLast(4)})" else "No configurada (Requerida para funciones in-app)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (settings.geminiApiKey.isNotBlank()) accent else MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                            IconButton(
                                onClick = { activeApiKeyTarget = ApiKeyTarget.GEMINI },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Configurar Clave Gemini",
                                    tint = if (settings.geminiApiKey.isNotBlank()) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón para obtener API Key gratuita en Google AI Studio
                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Obtener clave gratuita en Google AI Studio", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Selector de Modelo Dinámico
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Modelo de Gemini",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Activo: ${settings.geminiModel}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = accent,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            if (settings.geminiApiKey.isNotBlank()) {
                                TextButton(
                                    onClick = { viewModel.fetchAvailableGeminiModels() },
                                    enabled = !isLoadingModels,
                                    colors = ButtonDefaults.textButtonColors(contentColor = accent)
                                ) {
                                    if (isLoadingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = accent
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Consultando...", fontSize = 11.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refrescar modelos",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Refrescar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val displayModels = if (availableModels.isNotEmpty()) {
                            availableModels.map { it.id to it.displayName }
                        } else {
                            listOf(
                                "gemini-4-hydrogen" to "Gemini 4 Hydrogen",
                                "gemini-3.8-flash" to "Gemini 3.8 Flash",
                                "gemini-3.6-flash" to "Gemini 3.6 Flash",
                                "gemini-3.5-flash-lite" to "Gemini 3.5 Flash Lite",
                                "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite",
                                "gemini-flash-latest" to "Gemini Flash (Latest)",
                                "gemini-flash-lite-latest" to "Gemini Flash Lite (Latest)"
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            displayModels.forEach { (modelId, modelLabel) ->
                                val selected = settings.geminiModel == modelId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (selected) accent else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateGeminiModel(modelId) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = modelLabel,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = modelId,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== SECCIÓN 5B: ORGANIZACIÓN Y RENOMBRADO ====================
            SettingsSectionHeader(title = "Renombrado de Archivos", icon = Icons.Default.Edit, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsSwitchRow(
                        title = "Renombrar archivos según etiquetas",
                        subtitle = "Actualiza automáticamente el nombre del archivo al guardar los cambios en el editor",
                        checked = settings.autoRenameFile,
                        accentColor = accent,
                        onCheckedChange = { viewModel.updateAutoRenameFile(it) }
                    )

                    if (settings.autoRenameFile) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Patrón de renombrado",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = settings.fileRenamePattern,
                            onValueChange = { viewModel.updateFileRenamePattern(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Variables rápidas (toca para insertar):",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tokens = listOf("%artist%", "%title%", "%album%", "%track%", "%year%")
                            tokens.forEach { token ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.updateFileRenamePattern("${settings.fileRenamePattern} $token".trim())
                                    },
                                    label = { Text(token, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sampleTrack = remember {
                            com.example.data.model.AudioTrack(
                                id = "sample",
                                fileUri = "",
                                fileName = "audio.mp3",
                                title = "Starboy",
                                artist = "The Weeknd",
                                album = "Starboy",
                                trackNumber = "01",
                                year = "2016",
                                format = com.example.data.model.AudioFormat.MP3
                            )
                        }
                        val sampleName = remember(settings.fileRenamePattern) {
                            FileRenameHelper.resolveFileName(settings.fileRenamePattern, sampleTrack)
                        }
                        Text(
                            text = "Vista previa: $sampleName",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accent,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== SECCIÓN 5C: TAMAÑO DE CARÁTULA POR DEFECTO ====================
            SettingsSectionHeader(title = "Resolución de Carátula", icon = Icons.Default.Image, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tamaño predeterminado al incrustar:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Redimensiona la imagen para optimizar el peso del archivo de audio",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizes = listOf(
                            0 to "Original",
                            1000 to "1000 px",
                            960 to "960 px",
                            600 to "600 px",
                            500 to "500 px",
                            250 to "250 px"
                        )
                        sizes.forEach { (size, label) ->
                            val isSelected = settings.targetCoverSizePx == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateDefaultCoverSize(size) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== SECCIÓN 6: ACERCA DE ====================
            SettingsSectionHeader(title = "Acerca de", icon = Icons.Default.Info, tint = accent)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_bach_logo),
                            contentDescription = "Bach Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(76.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bach",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Versión 1.6.5",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "by: MGSCOfficial",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                onClick = { viewModel.checkForUpdates(manual = true, context = context) },
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(11.dp),
                                            strokeWidth = 2.dp,
                                            color = accent
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Comprobando...",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Comprobar actualizaciones",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showGplDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Licencia: GNU GPLv3",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            }
                        }

                        val uriHandler = LocalUriHandler.current
                        TextButton(
                            onClick = {
                                uriHandler.openUri("https://github.com/MGSCOfficial/Bach")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = accent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Repositorio",
                                fontSize = 12.sp,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (showGplDialog) {
                val uriHandler = LocalUriHandler.current
                AlertDialog(
                    onDismissRequest = { showGplDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "GNU General Public License v3.0",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Copyright © 2026 Moisés Gabriel Silva Castro (MGSCOfficial)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bach es software libre y de código abierto bajo los términos de la Licencia Pública General de GNU (GPLv3) publicada por la Free Software Foundation.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tus libertades y derechos garantizados:\n" +
                                        "• Libertad 0: Ejecutar el programa para cualquier propósito.\n" +
                                        "• Libertad 1: Estudiar el código fuente y modificarlo según tus necesidades.\n" +
                                        "• Libertad 2: Redistribuir copias del software libremente.\n" +
                                        "• Libertad 3: Mejorar el programa y publicar versiones modificadas.\n\n" +
                                        "Cláusula Copyleft: Cualquier redistribución o trabajo derivado debe permanecer bajo la misma licencia GNU GPLv3 y liberar el código fuente.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sin Garantía: El software se distribuye con la esperanza de ser útil, pero SIN NINGUNA GARANTÍA implícita de COMERCIABILIDAD o IDONEIDAD PARA UN PROPÓSITO PARTICULAR.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showGplDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                        ) {
                            Text("Entendido", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver en GNU.org", fontSize = 12.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = tint
            )
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsSourceRow(
    name: String,
    description: String,
    checked: Boolean,
    hasApiKey: Boolean = false,
    keyConfigured: Boolean = false,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    onConfigureKey: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (hasApiKey) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (keyConfigured) accentColor.copy(alpha = 0.2f) else AmberAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (keyConfigured) "API Configurada" else "Requiere API",
                            color = if (keyConfigured) accentColor else AmberAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasApiKey && onConfigureKey != null) {
                IconButton(
                    onClick = onConfigureKey,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Configurar Clave API",
                        tint = if (keyConfigured) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accentColor,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

enum class ApiKeyTarget(
    val title: String,
    val field1Label: String,
    val field2Label: String? = null,
    val helpUrl: String? = null
) {
    SPOTIFY("Spotify Web API", "Client ID", "Client Secret", "https://developer.spotify.com/dashboard"),
    LASTFM("Last.fm API", "API Key", null, "https://www.last.fm/api/account/create"),
    DISCOGS("Discogs", "Personal Access Token", null, "https://www.discogs.com/settings/developers"),
    GENIUS("Genius API", "Client Access Token", null, "https://genius.com/api-clients/new"),
    GEMINI("Google Gemini (AI Studio)", "API Key", null, "https://aistudio.google.com/app/apikey");

    fun getCurrentKey1(settings: com.example.data.storage.AppSettings): String = when (this) {
        SPOTIFY -> settings.spotifyClientId
        LASTFM -> settings.lastFmApiKey
        DISCOGS -> settings.discogsToken
        GENIUS -> settings.geniusToken
        GEMINI -> settings.geminiApiKey
    }

    fun getCurrentKey2(settings: com.example.data.storage.AppSettings): String = when (this) {
        SPOTIFY -> settings.spotifyClientSecret
        else -> ""
    }

    fun save(viewModel: MusicViewModel, key1: String, key2: String) {
        when (this) {
            SPOTIFY -> {
                viewModel.updateApiKey("spotify_id", key1)
                viewModel.updateApiKey("spotify_secret", key2)
            }
            LASTFM -> viewModel.updateApiKey("lastfm", key1)
            DISCOGS -> viewModel.updateApiKey("discogs", key1)
            GENIUS -> viewModel.updateApiKey("genius", key1)
            GEMINI -> viewModel.updateGeminiApiKey(key1)
        }
    }
}

@Composable
fun ApiKeyConfigDialog(
    target: ApiKeyTarget,
    currentKey1: String,
    currentKey2: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var val1 by remember { mutableStateOf(currentKey1) }
    var val2 by remember { mutableStateOf(currentKey2) }
    val accent = LocalAccentColor.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configurar ${target.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Introduce tus credenciales para habilitar búsquedas con esta fuente:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!target.helpUrl.isNullOrBlank()) {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Surface(
                        onClick = { uriHandler.openUri(target.helpUrl) },
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "¿No tienes tu clave? Solicitar API Key aquí ↗",
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Field 1
                Column {
                    Text(
                        text = target.field1Label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = val1,
                                onValueChange = { val1 = it },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Optional Field 2
                if (target.field2Label != null) {
                    Column {
                        Text(
                            text = target.field2Label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = val2,
                                    onValueChange = { val2 = it },
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(accent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(val1.trim(), val2.trim()) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
