package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioTrack
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.TrackCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onTrackSelect: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()

    // SAF file picker for selecting audio files (.mp3, .m4a)
    val openFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addFilesFromUris(uris)
        }
    }

    val activePlayingTrack = remember(playerState.activeTrackId, libraryState.tracks) {
        libraryState.tracks.firstOrNull { it.id == playerState.activeTrackId }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        containerColor = StudioDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(EmeraldAccent, VioletAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SonicTag",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StudioTextPrimary
                                )
                            )
                            Text(
                                text = "Editor de Metadatos MP3 y M4A",
                                style = MaterialTheme.typography.labelSmall.copy(color = StudioTextSecondary)
                            )
                        }
                    }
                },
                actions = {
                    // Load / Reload demo sample tracks
                    IconButton(
                        onClick = { viewModel.loadSampleTracks() },
                        modifier = Modifier.testTag("reload_samples_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar canciones de muestra",
                            tint = StudioTextSecondary
                        )
                    }

                    // Open SAF File Picker
                    IconButton(
                        onClick = {
                            openFilesLauncher.launch(
                                arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/*")
                            )
                        },
                        modifier = Modifier.testTag("open_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Abrir archivos del dispositivo",
                            tint = EmeraldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    openFilesLauncher.launch(
                        arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/*")
                    )
                },
                containerColor = EmeraldAccent,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = if (activePlayingTrack != null) 64.dp else 16.dp)
                    .testTag("fab_add_tracks")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir archivos de música")
            }
        },
        bottomBar = {
            MiniPlayerBar(
                playerState = playerState,
                track = activePlayingTrack,
                onPlayPauseClick = { viewModel.audioPlayer.togglePlayPause() },
                onSeek = { pos -> viewModel.audioPlayer.seekTo(pos) },
                onOpenEditor = {
                    activePlayingTrack?.let { onTrackSelect(it) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Local search input
            OutlinedTextField(
                value = libraryState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                placeholder = { Text("Buscar en la biblioteca...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = StudioTextSecondary)
                },
                trailingIcon = {
                    if (libraryState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = StudioTextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = StudioBorder,
                    focusedContainerColor = StudioCardSurface,
                    unfocusedContainerColor = StudioCardSurface,
                    focusedTextColor = StudioTextPrimary,
                    unfocusedTextColor = StudioTextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryFilter.values().forEach { filter ->
                    val isSelected = libraryState.activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterChanged(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldAccent,
                            selectedLabelColor = Color.Black,
                            containerColor = StudioCardSurface,
                            labelColor = StudioTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) EmeraldAccent else StudioBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats info line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${libraryState.filteredTracks.size} canciones encontradas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StudioTextMuted
                    )
                )

                if (libraryState.statusMessage != null) {
                    Text(
                        text = libraryState.statusMessage ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldAccent),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tracks List or Empty State
            if (libraryState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldAccent)
                }
            } else if (libraryState.filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = StudioTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay canciones en la lista",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = StudioTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Carga las canciones de muestra o abre archivos MP3/M4A de tu dispositivo.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StudioTextSecondary
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadSampleTracks() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCardSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cargar Canciones Demo", color = StudioTextPrimary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = if (activePlayingTrack != null) 80.dp else 24.dp)
                ) {
                    items(libraryState.filteredTracks, key = { it.id }) { track ->
                        val isPlaying = playerState.isPlaying && playerState.activeTrackId == track.id
                        TrackCard(
                            track = track,
                            isPlaying = isPlaying,
                            onTrackClick = { onTrackSelect(track) },
                            onPlayPauseClick = {
                                viewModel.audioPlayer.playTrack(
                                    trackId = track.id,
                                    uriString = track.fileUri,
                                    durationMs = track.durationMs
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
