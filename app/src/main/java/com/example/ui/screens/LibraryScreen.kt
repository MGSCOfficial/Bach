package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioTrack
import com.example.data.model.SortOrder
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
    var showSortMenu by remember { mutableStateOf(false) }

    // SAF directory picker (ACTION_OPEN_DOCUMENT_TREE)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.addMusicFolder(treeUri)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sound ID",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StudioTextPrimary
                                )
                            )
                            Text(
                                text = "Gestor de Metadatos, Portadas y Letras",
                                style = MaterialTheme.typography.labelSmall.copy(color = StudioTextSecondary)
                            )
                        }
                    }
                },
                actions = {
                    // Manual rescan button
                    IconButton(
                        onClick = { viewModel.rescanSavedFolders() },
                        modifier = Modifier.testTag("btn_rescan_folders")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-escanear carpetas",
                            tint = StudioTextSecondary
                        )
                    }

                    // Add folder button
                    IconButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.testTag("btn_add_folder_appbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Seleccionar carpeta de música",
                            tint = EmeraldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { folderPickerLauncher.launch(null) },
                containerColor = EmeraldAccent,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = if (activePlayingTrack != null) 64.dp else 16.dp)
                    .testTag("fab_add_folder")
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Añadir carpeta de música")
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
            // Folders Horizontal List (if any folders added)
            if (libraryState.folders.isNotEmpty()) {
                Text(
                    text = "Carpetas de Música Seleccionadas",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = StudioTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    libraryState.folders.forEach { folder ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCardSurface,
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${folder.displayName} (${folder.trackCount})",
                                    color = StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar carpeta",
                                    tint = StudioTextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.removeMusicFolder(folder.uriString) }
                                )
                            }
                        }
                    }

                    // Quick "+ Carpeta" chip
                    AssistChip(
                        onClick = { folderPickerLauncher.launch(null) },
                        label = { Text("+ Carpeta", color = EmeraldAccent, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = StudioCardSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search text field + Sort button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = libraryState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_search_input"),
                    placeholder = { Text("Buscar por título, artista o álbum...") },
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

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Button and Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioCardSurface)
                            .testTag("btn_sort_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar canciones",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(StudioCardSurfaceVariant)
                    ) {
                        Text(
                            text = "Ordenar por:",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldAccent, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )

                        SortOrder.values().forEach { order ->
                            val isSelected = libraryState.sortOrder == order
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = order.label,
                                            color = if (isSelected) EmeraldAccent else StudioTextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.onSortOrderChanged(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
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

            Spacer(modifier = Modifier.height(8.dp))

            // Active sort indicator + Track count line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${libraryState.filteredTracks.size} canciones",
                    style = MaterialTheme.typography.labelSmall.copy(color = StudioTextMuted)
                )

                // Current sort label chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioCardSurface,
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = libraryState.sortOrder.label,
                            color = StudioTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Area
            if (libraryState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Cargando música...", color = StudioTextSecondary, fontSize = 13.sp)
                    }
                }
            } else if (libraryState.folders.isEmpty()) {
                // Empty state: No folder chosen yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .testTag("empty_state_no_folders_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldDark.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Selecciona una Carpeta con Música",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Elige la carpeta donde guardas tus canciones MP3 o M4A. Sound ID detectará los archivos en menos de 2 segundos y te permitirá editar carátulas y metadatos con facilidad.",
                                style = MaterialTheme.typography.bodySmall.copy(color = StudioTextSecondary),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_select_music_folder_cta")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar Carpeta", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (libraryState.filteredTracks.isEmpty()) {
                // Empty state: Folders selected, but no songs matched filter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicOff, contentDescription = null, tint = StudioTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron canciones con este filtro",
                            style = MaterialTheme.typography.bodyMedium.copy(color = StudioTextSecondary)
                        )
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
