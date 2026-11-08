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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
    var showFolderManagerSheet by remember { mutableStateOf(false) }

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

    if (showFolderManagerSheet) {
        FolderManagerBottomSheet(
            folders = libraryState.folders,
            onAddFolder = { folderPickerLauncher.launch(null) },
            onRemoveFolder = { uri -> viewModel.removeMusicFolder(uri) },
            onRescan = {
                viewModel.rescanSavedFolders()
                showFolderManagerSheet = false
            },
            onDismiss = { showFolderManagerSheet = false }
        )
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
                                .size(28.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sound ID",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                        )
                    }
                },
                actions = {
                    // Folder manager button with badge
                    IconButton(
                        onClick = { showFolderManagerSheet = true },
                        modifier = Modifier.testTag("btn_manage_folders")
                    ) {
                        BadgedBox(
                            badge = {
                                if (libraryState.folders.isNotEmpty()) {
                                    Badge(
                                        containerColor = EmeraldAccent,
                                        contentColor = Color.Black
                                    ) {
                                        Text("${libraryState.folders.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Carpetas de música",
                                tint = StudioTextSecondary
                            )
                        }
                    }

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
            // 1. Sleek Folder Summary Bar (only when folders exist)
            if (libraryState.folders.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFolderManagerSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${libraryState.folders.size} carpetas vinculadas (${libraryState.tracks.size} canciones)",
                                color = StudioTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Gestionar ▾",
                            color = EmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Compact Search Field + Sort Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioCardSurface,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (libraryState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar canción, artista o álbum...",
                                    fontSize = 13.sp,
                                    color = StudioTextMuted
                                )
                            }
                            BasicTextField(
                                value = libraryState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                textStyle = TextStyle(
                                    color = StudioTextPrimary,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(EmeraldAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("library_search_input")
                            )
                        }
                        if (libraryState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Button and Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCardSurface)
                            .testTag("btn_sort_menu")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Ordenar canciones",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
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
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
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

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Compact Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryFilter.values().forEach { filter ->
                    val isSelected = libraryState.activeFilter == filter
                    val labelText = when (filter) {
                        LibraryFilter.ALL -> "Todas (${libraryState.filteredTracks.size})"
                        else -> filter.label
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EmeraldAccent else StudioCardSurface,
                        modifier = Modifier
                            .height(28.dp)
                            .clickable { viewModel.onFilterChanged(filter) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = labelText,
                                color = if (isSelected) Color.Black else StudioTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerBottomSheet(
    folders: List<com.example.data.model.MusicFolder>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkBg,
        tonalElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carpetas de Música Vinculadas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = StudioTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (folders.isEmpty()) {
                Text(
                    text = "No hay carpetas vinculadas.",
                    color = StudioTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                folders.forEach { folder ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = StudioCardSurface,
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    color = StudioTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${folder.trackCount} canciones",
                                    color = StudioTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFolder(folder.uriString) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Quitar carpeta",
                                    tint = RoseAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioTextPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-escanear", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onAddFolder()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir carpeta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
