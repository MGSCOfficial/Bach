package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AudioFormat
import com.example.ui.components.CoverArtPickerSheet
import com.example.ui.components.OnlineSearchResultsSheet
import com.example.ui.components.PreviewDiffDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                .background(StudioDarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmeraldAccent)
        }
        return
    }

    val isThisTrackPlaying = playerState.isPlaying && playerState.activeTrackId == original.id

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("song_editor_screen"),
        containerColor = StudioDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sound ID — Editor",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                        )
                        Text(
                            text = draft.fileName,
                            style = MaterialTheme.typography.labelSmall.copy(color = StudioTextSecondary),
                            maxLines = 1
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
                            tint = StudioTextPrimary
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (draft.format == AudioFormat.MP3) EmeraldDark else VioletDark,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = draft.format.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showDiffPreview() },
                        modifier = Modifier.testTag("preview_diff_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Previsualizar diferencias",
                            tint = EmeraldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioCardSurface,
                tonalElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldAccent))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previsualizar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.applyAndSaveTags() },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_save_tags"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        enabled = !editorState.isSaving
                    ) {
                        if (editorState.isSaving) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar en Archivo", color = Color.Black, fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ==================== ALBUM ART HERO CARD ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("album_art_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StudioCardSurfaceVariant)
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
                                Icon(Icons.Default.Album, contentDescription = null, tint = StudioTextMuted, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Sin Carátula", color = StudioTextMuted, fontSize = 12.sp)
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
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldAccent))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                Text("Cambiar Carátula", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val coverLabel = editorState.selectedCoverOption?.label
                        ?: if (draft.hasCover) "Carátula asignada (${draft.coverArtBytes?.size ?: 0} B)" else "Sin imagen asignada"
                    Text(
                        text = coverLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = StudioTextSecondary),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== IN-EDITOR AUDIO PREVIEW PLAYER ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
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
                            .background(if (isThisTrackPlaying) EmeraldAccent else StudioCardSurfaceVariant, CircleShape)
                            .testTag("editor_play_button")
                    ) {
                        Icon(
                            imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisTrackPlaying) "Pausar" else "Escuchar",
                            tint = if (isThisTrackPlaying) Color.Black else StudioTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Escuchar Pista de Audio",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = StudioTextPrimary
                            )
                        )

                        val currentSec = playerState.currentPositionMs / 1000
                        val totalSec = (if (playerState.durationMs > 0) playerState.durationMs else original.durationMs) / 1000
                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall.copy(color = StudioTextMuted)
                        )
                    }

                    if (original.durationMs > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioCardSurfaceVariant
                        ) {
                            Text(
                                text = original.formattedDuration,
                                color = StudioTextSecondary,
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
                colors = CardDefaults.cardColors(containerColor = StudioCardSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(EmeraldAccent.copy(alpha = 0.6f), VioletAccent.copy(alpha = 0.6f))
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
                                Brush.linearGradient(listOf(EmeraldAccent, VioletAccent)),
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
                                color = StudioTextPrimary
                            )
                        )
                        Text(
                            text = "Aplica todos los metadatos y carátula con 1 toque",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldAccent)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==================== LYRICS SECTION (NEW) ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lyrics_section_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (draft.hasLyrics) CyanAccent.copy(alpha = 0.5f) else StudioBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Letra de la Canción",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary
                                )
                            )
                        }

                        Button(
                            onClick = { viewModel.searchLyricsForCurrentTrack() },
                            enabled = !editorState.isSearchingLyrics,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_search_lyrics")
                        ) {
                            if (editorState.isSearchingLyrics) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buscando...", color = Color.Black, fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buscar en LRCLIB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = StudioBorder,
                            focusedContainerColor = StudioCardSurfaceVariant,
                            unfocusedContainerColor = StudioCardSurfaceVariant,
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary
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
                                    color = StudioTextPrimary
                                )
                            )
                            Text(
                                text = "Crea archivo con la letra sincronizada junto al audio",
                                style = MaterialTheme.typography.labelSmall.copy(color = StudioTextMuted)
                            )
                        }
                        Switch(
                            checked = editorState.saveLrcFile,
                            onCheckedChange = { viewModel.setSaveLrcFile(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanAccent)
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
                                    color = StudioTextPrimary
                                )
                            )
                            Text(
                                text = "Guarda la letra dentro de la etiqueta ${if (draft.format == AudioFormat.MP3) "USLT (ID3v2)" else "©lyr (MP4)"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = StudioTextMuted)
                            )
                        }
                        Switch(
                            checked = editorState.embedLyrics,
                            onCheckedChange = { viewModel.setEmbedLyrics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldAccent)
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
                    color = StudioTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            EditorTextField(
                label = "Título de la Canción",
                value = draft.title,
                onValueChange = { viewModel.updateDraftField(title = it) },
                icon = Icons.Default.MusicNote,
                testTag = "input_title"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Artista
            EditorTextField(
                label = "Artista / Intérprete",
                value = draft.artist,
                onValueChange = { viewModel.updateDraftField(artist = it) },
                icon = Icons.Default.Person,
                testTag = "input_artist"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Álbum
            EditorTextField(
                label = "Nombre del Álbum",
                value = draft.album,
                onValueChange = { viewModel.updateDraftField(album = it) },
                icon = Icons.Default.Album,
                testTag = "input_album"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Artista del Álbum
            EditorTextField(
                label = "Artista del Álbum",
                value = draft.albumArtist,
                onValueChange = { viewModel.updateDraftField(albumArtist = it) },
                icon = Icons.Default.Group,
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
                    modifier = Modifier.weight(1f),
                    testTag = "input_year"
                )

                EditorTextField(
                    label = "Género",
                    value = draft.genre,
                    onValueChange = { viewModel.updateDraftField(genre = it) },
                    icon = Icons.Default.Category,
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                    testTag = "input_track_number"
                )

                EditorTextField(
                    label = "Disco #",
                    value = draft.discNumber,
                    onValueChange = { viewModel.updateDraftField(discNumber = it) },
                    icon = Icons.Default.DiscFull,
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
                testTag = "input_comment"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==================== MODAL SHEETS & DIALOGS ====================

    if (editorState.showCandidateSheet) {
        OnlineSearchResultsSheet(
            candidates = editorState.onlineCandidates,
            isLoading = editorState.isSearchingOnline,
            initialQuery = "${draft.artist} ${draft.title}".trim().ifBlank { draft.fileName.substringBeforeLast(".") },
            onSearchQuery = { q -> viewModel.searchOnlineForCurrentTrack(q) },
            onSelectCandidate = { candidate -> viewModel.selectCandidate(candidate) },
            onDismiss = { viewModel.hideCandidateSheet() }
        )
    }

    if (editorState.showCoverPickerSheet) {
        CoverArtPickerSheet(
            options = editorState.coverArtOptions,
            selectedOption = editorState.selectedCoverOption,
            isLoading = editorState.isLoadingCovers,
            onSelectOption = { opt -> viewModel.selectCoverOption(opt) },
            onCustomImagePicked = { bytes -> viewModel.setCustomCoverFromBytes(bytes) },
            onDismiss = { viewModel.hideCoverPickerSheet() }
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
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldAccent,
            unfocusedBorderColor = StudioBorder,
            focusedContainerColor = StudioCardSurface,
            unfocusedContainerColor = StudioCardSurface,
            focusedTextColor = StudioTextPrimary,
            unfocusedTextColor = StudioTextPrimary,
            focusedLabelColor = EmeraldAccent,
            unfocusedLabelColor = StudioTextSecondary
        ),
        singleLine = singleLine
    )
}
