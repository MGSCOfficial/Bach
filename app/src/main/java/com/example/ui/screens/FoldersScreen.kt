package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioTrack
import com.example.data.model.MusicFolder
import com.example.data.model.SortOrder
import com.example.data.model.belongsTo
import com.example.data.storage.ViewLayoutMode
import com.example.ui.components.FolderGridCard
import com.example.ui.components.TrackCard
import com.example.ui.components.TrackGridCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: MusicViewModel,
    onTrackSelect: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
    var selectedFolder by remember { mutableStateOf<MusicFolder?>(null) }
    var folderSearchQuery by remember { mutableStateOf("") }
    var folderSortOrder by remember { mutableStateOf(SortOrder.RECENTLY_MODIFIED) }
    var showFolderSortMenu by remember { mutableStateOf(false) }
    var folderPendingRemoval by remember { mutableStateOf<MusicFolder?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.addMusicFolder(treeUri)
        }
    }

    // Intercept back button if user is inside a folder drill-down
    BackHandler(enabled = selectedFolder != null) {
        selectedFolder = null
        folderSearchQuery = ""
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (selectedFolder != null) {
                        Column {
                            Text(
                                text = selectedFolder!!.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val count = libraryState.tracks.count { it.belongsTo(selectedFolder!!) }
                            Text(
                                text = "$count canciones en esta carpeta",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    } else {
                        Text(
                            text = "Carpetas de Música",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = {
                            selectedFolder = null
                            folderSearchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a carpetas",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (selectedFolder == null) {
                        val isFoldersGrid = settingsState.foldersLayoutMode == ViewLayoutMode.GRID
                        IconButton(onClick = { viewModel.toggleFoldersLayoutMode() }) {
                            Icon(
                                imageVector = if (isFoldersGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (isFoldersGrid) "Ver carpetas como lista" else "Ver carpetas como cuadrícula",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Añadir carpeta",
                                tint = LocalAccentColor.current
                            )
                        }
                        IconButton(onClick = { viewModel.rescanSavedFolders() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-escanear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val isSongsGrid = settingsState.songsLayoutMode == ViewLayoutMode.GRID
                        IconButton(onClick = { viewModel.toggleSongsLayoutMode() }) {
                            Icon(
                                imageVector = if (isSongsGrid) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (isSongsGrid) "Ver canciones como lista" else "Ver canciones como cuadrícula",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { folderPendingRemoval = selectedFolder }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Desvincular carpeta",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedFolder == null) {
                // ==================== NIVEL 1: LISTA DE CARPETAS ====================
                if (libraryState.folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = LocalAccentColor.current,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Sin carpetas vinculadas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Vincula una carpeta con archivos MP3 o M4A para explorarla de forma organizada.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalAccentColor.current, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Vincular Carpeta", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val isFoldersGrid = settingsState.foldersLayoutMode == ViewLayoutMode.GRID
                    if (isFoldersGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(libraryState.folders, key = { it.uriString }) { folder ->
                                val tracksInFolder = libraryState.tracks.count { it.belongsTo(folder) }
                                FolderGridCard(
                                    folder = folder,
                                    trackCount = tracksInFolder,
                                    onFolderClick = {
                                        selectedFolder = folder
                                        folderSearchQuery = ""
                                    },
                                    onDeleteClick = {
                                        folderPendingRemoval = folder
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(libraryState.folders, key = { it.uriString }) { folder ->
                                val tracksInFolder = libraryState.tracks.count { it.belongsTo(folder) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFolder = folder
                                            folderSearchQuery = ""
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LocalAccentColor.current.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = LocalAccentColor.current,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.displayName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (folder.relativePath.isNotBlank() && folder.relativePath != folder.displayName) {
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = folder.relativePath,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$tracksInFolder canciones",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        IconButton(
                                            onClick = { folderPendingRemoval = folder },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Desvincular carpeta",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Abrir carpeta",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== NIVEL 2: DRILL-DOWN EN LA CARPETA SELECCIONADA ====================
                val currentFolder = selectedFolder!!
                val folderTracks = remember(libraryState.tracks, currentFolder, folderSearchQuery, folderSortOrder) {
                    val matching = libraryState.tracks.filter { it.belongsTo(currentFolder) }
                    val filtered = if (folderSearchQuery.isBlank()) matching
                    else {
                        val q = folderSearchQuery.trim().lowercase()
                        matching.filter {
                            it.displayTitle.lowercase().contains(q) ||
                                    it.displayArtist.lowercase().contains(q) ||
                                    it.fileName.lowercase().contains(q)
                        }
                    }
                    when (folderSortOrder) {
                        SortOrder.RECENTLY_MODIFIED -> filtered.sortedByDescending { it.dateModified }
                        SortOrder.OLDEST_MODIFIED -> filtered.sortedBy { it.dateModified }
                        SortOrder.HAS_LYRICS_FIRST -> filtered.sortedWith(
                            compareByDescending<AudioTrack> { it.hasLyrics || it.lyrics.isNotBlank() }
                                .thenByDescending { it.dateModified }
                        )
                        SortOrder.NO_LYRICS_FIRST -> filtered.sortedWith(
                            compareBy<AudioTrack> { it.hasLyrics || it.lyrics.isNotBlank() }
                                .thenByDescending { it.dateModified }
                        )
                        SortOrder.TITLE_ASC -> filtered.sortedBy { it.displayTitle.lowercase() }
                        SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.displayTitle.lowercase() }
                        SortOrder.ARTIST_ASC -> filtered.sortedBy { it.displayArtist.lowercase() }
                        SortOrder.ALBUM_ASC -> filtered.sortedBy { it.displayAlbum.lowercase() }
                        SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
                        SortOrder.DURATION_ASC -> filtered.sortedBy { it.durationMs }
                        SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search and Sort inside folder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (folderSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar en esta carpeta...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    BasicTextField(
                                        value = folderSearchQuery,
                                        onValueChange = { folderSearchQuery = it },
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(LocalAccentColor.current),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (folderSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { folderSearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                onClick = { showFolderSortMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .testTag("btn_folder_sort_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Ordenar canciones de la carpeta",
                                    tint = LocalAccentColor.current,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showFolderSortMenu,
                                onDismissRequest = { showFolderSortMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = "Ordenar por:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = LocalAccentColor.current, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )

                                SortOrder.values().forEach { order ->
                                    val isSelected = folderSortOrder == order
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = order.label,
                                                    color = if (isSelected) LocalAccentColor.current else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = LocalAccentColor.current,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            folderSortOrder = order
                                            showFolderSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (folderTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (folderSearchQuery.isBlank()) "No se encontraron archivos de audio en esta carpeta" else "Sin coincidencias para '$folderSearchQuery'",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        val isSongsGrid = settingsState.songsLayoutMode == ViewLayoutMode.GRID
                        if (isSongsGrid) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(folderTracks, key = { it.id }) { track ->
                                    val isPlaying = playerState.isPlaying && playerState.activeTrackId == track.id
                                    TrackGridCard(
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
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(folderTracks, key = { it.id }) { track ->
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
        }

        if (folderPendingRemoval != null) {
            val folderToRemove = folderPendingRemoval!!
            AlertDialog(
                onDismissRequest = { folderPendingRemoval = null },
                title = {
                    Text(
                        text = "¿Desvincular carpeta?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "Se desvinculará \"${folderToRemove.displayName}\" de tu biblioteca.\n\nTus archivos de música permanecerán seguros e intactos en el almacenamiento del dispositivo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val toRemove = folderToRemove
                            folderPendingRemoval = null
                            if (selectedFolder?.uriString == toRemove.uriString) {
                                selectedFolder = null
                                folderSearchQuery = ""
                            }
                            viewModel.removeMusicFolder(toRemove)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Desvincular", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { folderPendingRemoval = null }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
