package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AudioFormat
import com.example.data.storage.ThemeMode
import com.example.ui.components.AudioFormatBadge
import com.example.ui.components.CoverArtPickerSheet
import com.example.ui.components.LyricsPickerSheet
import com.example.ui.components.OnlineSearchResultsSheet
import com.example.ui.components.PreviewDiffDialog
import com.example.ui.theme.*
import com.example.ui.util.FileRenameHelper
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val accent = LocalAccentColor.current
    val isAmoled = settings.amoledDark && settings.themeMode != ThemeMode.LIGHT
    val screenBg = if (isAmoled) AmoledDarkBg else MaterialTheme.colorScheme.background
    val cardBg = if (isAmoled) AmoledCardSurface else MaterialTheme.colorScheme.surface
    val cardVariantBg = if (isAmoled) AmoledCardSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val cardBorderColor = if (isAmoled) AmoledBorder else MaterialTheme.colorScheme.outlineVariant
    val bottomBarBg = if (isAmoled) AmoledDarkBg else MaterialTheme.colorScheme.surface

    BackHandler {
        viewModel.closeTrackEditor()
        onBack()
    }

    val draft = editorState.draftTrack
    val original = editorState.originalTrack

    LaunchedEffect(editorState.saveNotification) {
        editorState.saveNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSaveNotification()
        }
    }

    if (draft == null || original == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    val isThisTrackPlaying = playerState.isPlaying && playerState.activeTrackId == original.id

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("song_editor_screen"),
        containerColor = screenBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bach — Editor",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = draft.fileName,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeTrackEditor()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    AudioFormatBadge(
                        format = draft.format,
                        fontSize = 11.sp,
                        shape = RoundedCornerShape(6.dp),
                        paddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = { viewModel.showDiffPreview() },
                        modifier = Modifier.testTag("preview_diff_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Previsualizar diferencias",
                            tint = accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bottomBarBg,
                tonalElevation = if (isAmoled) 0.dp else 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorderColor))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showDiffPreview() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_preview_diff"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(accent))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Previsualizar",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { viewModel.applyAndSaveTags() },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("btn_save_tags"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = !editorState.isSaving
                    ) {
                        if (editorState.isSaving) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardando...",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar Archivo",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ==================== AUTO-RENAME STATUS BANNER ====================
            if (settings.autoRenameFile) {
                val targetFileName = remember(draft.title, draft.artist, draft.album, draft.trackNumber, draft.year, settings.fileRenamePattern) {
                    FileRenameHelper.resolveFileName(settings.fileRenamePattern, draft)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = cardVariantBg,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Renombrado activo al guardar:",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = targetFileName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ==================== ALBUM ART HERO CARD ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("album_art_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorderColor))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardVariantBg)
                            .clickable { viewModel.openCoverPicker() }
                            .testTag("album_cover_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Check in draft or selected option
                        val activeBytes = draft.coverArtBytes ?: editorState.selectedCoverOption?.bytes
                        val coverUrl = editorState.selectedCoverOption?.url

                        if (activeBytes != null && activeBytes.isNotEmpty()) {
                            val bitmap = remember(activeBytes) {
                                try {
                                    BitmapFactory.decodeByteArray(activeBytes, 0, activeBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Carátula",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (!coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Carátula",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Sin Carátula", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }

                        // Edit Cover Art Pill Button
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clickable { viewModel.openCoverPicker() },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.85f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(accent))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                Text("Cambiar Carátula", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val coverLabel = editorState.selectedCoverOption?.label
                        ?: if (draft.hasCover) "Carátula asignada (${draft.coverArtBytes?.size ?: 0} B)" else "Sin imagen asignada"
                    Text(
                        text = coverLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1
                    )

                    val selectedCoverBytes = editorState.selectedCoverOption?.bytes
                    val hasCoverBytes = (draft.coverArtBytes != null && draft.coverArtBytes.isNotEmpty()) ||
                            (selectedCoverBytes != null && selectedCoverBytes.isNotEmpty())

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.identifyTrackWithGemini(context) },
                            enabled = !editorState.isGeminiLoading,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = VioletAccent.copy(alpha = 0.12f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_gemini_cover_ocr")
                        ) {
                            if (editorState.isGeminiLoading && !editorState.isGeminiSyncingLyrics) {
                                CircularProgressIndicator(color = VioletAccent, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analizando...", color = VioletAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val label = if (hasCoverBytes) "Por Carátula" else "Por Metadatos"
                                Text(label, color = VioletAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.identifyTrackWithAudioUsingGemini(context) },
                            enabled = !editorState.isGeminiLoading,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("btn_gemini_audio_id")
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Por Audio", color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== IN-EDITOR AUDIO PREVIEW PLAYER ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorderColor))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.audioPlayer.playTrack(
                                trackId = original.id,
                                uriString = original.fileUri,
                                durationMs = original.durationMs
                            )
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (isThisTrackPlaying) accent else cardVariantBg, CircleShape)
                            .testTag("editor_play_button")
                    ) {
                        Icon(
                            imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisTrackPlaying) "Pausar" else "Escuchar",
                            tint = if (isThisTrackPlaying) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Escuchar Pista de Audio",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        val currentSec = playerState.currentPositionMs / 1000
                        val totalSec = (if (playerState.durationMs > 0) playerState.durationMs else original.durationMs) / 1000
                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    if (original.durationMs > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = cardVariantBg
                        ) {
                            Text(
                                text = original.formattedDuration,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== ONLINE AUTO-SEARCH BANNER ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.searchOnlineForCurrentTrack() }
                    .testTag("auto_search_online_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardVariantBg),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.6f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.secondary)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buscar en MusicBrainz • iTunes • Deezer",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Aplica todos los metadatos y carátula con 1 toque",
                            style = MaterialTheme.typography.bodySmall.copy(color = accent)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (editorState.selectedCandidate != null) {
                val candidate = editorState.selectedCandidate!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_online_candidate_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(accent.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Metadatos aplicados de ${candidate.source.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${candidate.title} — ${candidate.artist} (${candidate.album})",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                maxLines = 1
                            )
                        }
                        TextButton(
                            onClick = { viewModel.searchOnlineForCurrentTrack() }
                        ) {
                            Text("Cambiar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ==================== LYRICS SECTION (NEW) ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lyrics_section_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (draft.hasLyrics) accent.copy(alpha = 0.5f) else cardBorderColor)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Letra de la Canción",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (draft.lyrics.isNotBlank()) {
                            TextButton(
                                onClick = { viewModel.clearLyrics() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_clear_lyrics")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Borrar Letra",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Borrar Letra",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filas de acciones líricas: LRCLIB (Online) y Gemini (Transcribir Audio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.searchLyricsForCurrentTrack() },
                            enabled = !editorState.isSearchingLyrics && !editorState.isGeminiLoading && !editorState.isGeminiTranscribing,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_search_lyrics"),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            if (editorState.isSearchingLyrics) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscando...", color = Color.Black, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscar en LRCLIB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.transcribeLyricsFromAudioWithGemini(context) },
                            enabled = !editorState.isGeminiTranscribing && !editorState.isSearchingLyrics && !editorState.isGeminiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_gemini_transcribe"),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            if (editorState.isGeminiTranscribing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transcribiendo...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transcribir Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Acciones secundarias: Sincronizar (si ya hay letra) o Generar por Texto (IA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (draft.lyrics.isNotBlank()) {
                            FilledTonalButton(
                                onClick = { viewModel.synchronizeLyricsWithAudioUsingGemini(context) },
                                enabled = !editorState.isGeminiSyncingLyrics && !editorState.isGeminiLoading && !editorState.isGeminiTranscribing,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_gemini_sync_lyrics"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                if (editorState.isGeminiSyncingLyrics) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sincronizando...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sincronizar Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        TextButton(
                            onClick = { viewModel.fetchLyricsWithGemini(context) },
                            enabled = !editorState.isGeminiLoading && !editorState.isSearchingLyrics && !editorState.isGeminiTranscribing,
                            modifier = Modifier
                                .then(if (draft.lyrics.isNotBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                .testTag("btn_gemini_text_lyrics"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            if (editorState.isGeminiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generando texto...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generar por Texto (IA)", color = VioletAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lyrics Text Field
                    OutlinedTextField(
                        value = draft.lyrics,
                        onValueChange = { viewModel.updateDraftField(lyrics = it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .testTag("input_lyrics"),
                        placeholder = { Text("Escribe o pega aquí la letra, o usa el botón 'Buscar en LRCLIB'...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = cardBorderColor,
                            focusedContainerColor = cardVariantBg,
                            unfocusedContainerColor = cardVariantBg,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = accent
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lyrics Save Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guardar archivo .lrc en carpeta",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Crea archivo con la letra sincronizada junto al audio",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = editorState.saveLrcFile,
                            onCheckedChange = { viewModel.setSaveLrcFile(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incrustar letra en la canción",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Guarda la letra dentro de la etiqueta ${if (draft.format == AudioFormat.MP3) "USLT (ID3v2)" else "©lyr (MP4)"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = editorState.embedLyrics,
                            onCheckedChange = { viewModel.setEmbedLyrics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = accent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==================== METADATA INPUT FORM ====================
            Text(
                text = "Metadatos de la Pista",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            EditorTextField(
                label = "Título de la Canción",
                value = draft.title,
                onValueChange = { viewModel.updateDraftField(title = it) },
                icon = Icons.Default.MusicNote,
                isAmoled = isAmoled,
                testTag = "input_title"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Artista
            EditorTextField(
                label = "Artista / Intérprete",
                value = draft.artist,
                onValueChange = { viewModel.updateDraftField(artist = it) },
                icon = Icons.Default.Person,
                isAmoled = isAmoled,
                testTag = "input_artist"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Álbum
            EditorTextField(
                label = "Nombre del Álbum",
                value = draft.album,
                onValueChange = { viewModel.updateDraftField(album = it) },
                icon = Icons.Default.Album,
                isAmoled = isAmoled,
                testTag = "input_album"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Artista del Álbum
            EditorTextField(
                label = "Artista del Álbum",
                value = draft.albumArtist,
                onValueChange = { viewModel.updateDraftField(albumArtist = it) },
                icon = Icons.Default.Group,
                isAmoled = isAmoled,
                testTag = "input_album_artist"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Año & Género Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorTextField(
                    label = "Año",
                    value = draft.year,
                    onValueChange = { viewModel.updateDraftField(year = it) },
                    icon = Icons.Default.CalendarToday,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(0.35f),
                    testTag = "input_year"
                )

                EditorTextField(
                    label = "Género",
                    value = draft.genre,
                    onValueChange = { viewModel.updateDraftField(genre = it) },
                    icon = Icons.Default.Category,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(0.65f),
                    testTag = "input_genre"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pista # & Disco # Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorTextField(
                    label = "Pista #",
                    value = draft.trackNumber,
                    onValueChange = { viewModel.updateDraftField(trackNumber = it) },
                    icon = Icons.Default.FormatListNumbered,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f),
                    testTag = "input_track_number"
                )

                EditorTextField(
                    label = "Disco #",
                    value = draft.discNumber,
                    onValueChange = { viewModel.updateDraftField(discNumber = it) },
                    icon = Icons.Default.DiscFull,
                    isAmoled = isAmoled,
                    modifier = Modifier.weight(1f),
                    testTag = "input_disc_number"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Comentario
            EditorTextField(
                label = "Comentarios / Notas",
                value = draft.comment,
                onValueChange = { viewModel.updateDraftField(comment = it) },
                icon = Icons.Default.Comment,
                singleLine = false,
                isAmoled = isAmoled,
                testTag = "input_comment"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==================== MODAL SHEETS & DIALOGS ====================

    if (editorState.showCandidateSheet) {
        val cleanArtist = draft.artist.replace("\uFEFF", "").replace("\u200B", "").trim()
        val cleanTitle = draft.title.replace("\uFEFF", "").replace("\u200B", "").trim()
        val cleanAlbum = draft.album.replace("\uFEFF", "").replace("\u200B", "").trim()
        val queryDefault = "$cleanArtist $cleanTitle".trim().ifBlank { draft.fileName.substringBeforeLast(".") }

        OnlineSearchResultsSheet(
            candidates = editorState.onlineCandidates,
            isLoading = editorState.isSearchingOnline,
            initialQuery = queryDefault,
            initialTitle = cleanTitle,
            initialArtist = cleanArtist,
            initialAlbum = cleanAlbum,
            activeSources = viewModel.getActiveMetadataSources(),
            onSearchQuery = { q -> viewModel.searchOnlineForCurrentTrack(q) },
            onAdvancedSearch = { t, a, al -> viewModel.searchOnlineAdvanced(t, a, al) },
            onSelectCandidate = { candidate -> viewModel.selectCandidate(candidate) },
            onDismiss = { viewModel.hideCandidateSheet() },
            onGeminiSearch = { viewModel.searchWithGeminiInResults(context, queryDefault) }
        )
    }

    if (editorState.showCoverPickerSheet) {
        CoverArtPickerSheet(
            options = editorState.coverArtOptions,
            selectedOption = editorState.selectedCoverOption,
            isLoading = editorState.isLoadingCovers,
            selectedTargetSize = if (editorState.selectedTargetCoverSize > 0) editorState.selectedTargetCoverSize else settings.targetCoverSizePx,
            onTargetSizeSelected = { size -> viewModel.updateTargetCoverSize(size) },
            onSelectOption = { opt -> viewModel.selectCoverOption(opt) },
            onCustomImagePicked = { bytes -> viewModel.setCustomCoverFromBytes(bytes) },
            onDismiss = { viewModel.hideCoverPickerSheet() },
            isAmoled = isAmoled
        )
    }

    if (editorState.showLyricsPickerSheet) {
        LyricsPickerSheet(
            candidates = editorState.lyricsCandidates,
            isLoading = editorState.isSearchingLyrics,
            onSelectLyrics = { candidate -> viewModel.applySelectedLyrics(candidate) },
            onDismiss = { viewModel.dismissLyricsPicker() },
            isAmoled = isAmoled
        )
    }

    if (editorState.showDiffDialog) {
        PreviewDiffDialog(
            originalTrack = original,
            draftTrack = draft,
            selectedCover = editorState.selectedCoverOption,
            diffList = editorState.diffList,
            isSaving = editorState.isSaving,
            onConfirmSave = { viewModel.applyAndSaveTags() },
            onDismiss = { viewModel.hideDiffDialog() }
        )
    }
}

@Composable
fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    testTag: String = "",
    isAmoled: Boolean = false
) {
    val accent = LocalAccentColor.current
    val containerBg = if (isAmoled) AmoledCardSurface else MaterialTheme.colorScheme.surface
    val borderCol = if (isAmoled) AmoledBorder else MaterialTheme.colorScheme.outlineVariant

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = borderCol,
            focusedContainerColor = containerBg,
            unfocusedContainerColor = containerBg,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = accent,
            focusedLabelColor = accent,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        singleLine = singleLine,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = if (singleLine) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Default
        )
    )
}
