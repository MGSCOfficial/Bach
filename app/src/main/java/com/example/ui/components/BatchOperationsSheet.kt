package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.data.model.MusicFolder
import com.example.data.model.belongsTo
import com.example.ui.theme.*
import com.example.ui.viewmodel.BatchOperationType
import com.example.ui.viewmodel.BatchProgressState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchOperationsSheet(
    folders: List<MusicFolder>,
    allTracks: List<AudioTrack>,
    batchState: BatchProgressState,
    fileRenamePattern: String,
    initialSelectedFolder: MusicFolder? = null,
    onStartBatchRename: (List<MusicFolder>) -> Unit,
    onStartBatchAutoTag: (List<MusicFolder>, Boolean) -> Unit,
    onCancelBatch: () -> Unit,
    onDismissBatchProgress: () -> Unit,
    onDismiss: () -> Unit,
    isAmoled: Boolean = false
) {
    val accent = LocalAccentColor.current
    val sheetBg = if (isAmoled) AmoledDarkBg else MaterialTheme.colorScheme.surface
    val cardBg = if (isAmoled) AmoledCardSurface else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (isAmoled) AmoledBorder else MaterialTheme.colorScheme.outlineVariant

    // Selection state of folders
    val selectedFolderUris = remember(initialSelectedFolder, folders) {
        mutableStateListOf<String>().apply {
            if (initialSelectedFolder != null) {
                add(initialSelectedFolder.uriString)
            } else {
                addAll(folders.map { it.uriString })
            }
        }
    }

    val selectedFolders = remember(selectedFolderUris.size, folders) {
        folders.filter { selectedFolderUris.contains(it.uriString) }
    }

    val totalMatchingTracks = remember(selectedFolders, allTracks) {
        allTracks.count { track -> selectedFolders.any { track.belongsTo(it) } }
    }

    var onlyMissingTags by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!batchState.isRunning) {
                onDismissBatchProgress()
                onDismiss()
            }
        },
        containerColor = sheetBg,
        tonalElevation = if (isAmoled) 0.dp else 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Acciones Masivas por Carpeta",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Aplica cambios a múltiples archivos simultáneamente",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (!batchState.isRunning) {
                    IconButton(onClick = {
                        onDismissBatchProgress()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== VISTA EN EJECUCIÓN / PROGRESO ====================
            if (batchState.isRunning || batchState.isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(if (batchState.isFinished) accent else cardBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (batchState.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accent,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = batchState.type.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (batchState.isRunning) "Procesando ${batchState.currentItemIndex} de ${batchState.totalItems}"
                                    else "Completado",
                                    style = MaterialTheme.typography.labelSmall.copy(color = accent)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val progress = if (batchState.totalItems > 0) {
                            batchState.currentItemIndex.toFloat() / batchState.totalItems.toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = batchState.statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${batchState.successCount}", color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Exitosos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${batchState.skippedCount}", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Sin cambios", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${batchState.errorCount}", color = RoseAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Errores", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (batchState.isRunning) {
                            OutlinedButton(
                                onClick = onCancelBatch,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancelar Operación", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onDismissBatchProgress()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Aceptar y Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // ==================== CONFIGURACIÓN Y DISPARADOR ====================
                // Sección de selección de carpetas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(cardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Carpetas a procesar (${selectedFolders.size} de ${folders.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )

                            val allSelected = selectedFolderUris.size == folders.size
                            TextButton(
                                onClick = {
                                    if (allSelected) {
                                        selectedFolderUris.clear()
                                    } else {
                                        selectedFolderUris.clear()
                                        selectedFolderUris.addAll(folders.map { it.uriString })
                                    }
                                }
                            ) {
                                Text(
                                    text = if (allSelected) "Deseleccionar" else "Todas",
                                    fontSize = 11.sp,
                                    color = accent
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            items(folders, key = { it.uriString }) { folder ->
                                val isChecked = selectedFolderUris.contains(folder.uriString)
                                val folderTracksCount = allTracks.count { it.belongsTo(folder) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedFolderUris.remove(folder.uriString)
                                            else selectedFolderUris.add(folder.uriString)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            if (check) selectedFolderUris.add(folder.uriString)
                                            else selectedFolderUris.remove(folder.uriString)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = accent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.displayName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$folderTracksCount canciones",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Acciones Disponibles
                Text(
                    text = "Selecciona la operación masiva ($totalMatchingTracks canciones listas):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opción 1: Renombrado Masivo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(cardBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Renombrado Masivo de Archivos",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                )
                                Text(
                                    text = "Patrón activo: $fileRenamePattern",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onStartBatchRename(selectedFolders) },
                            enabled = selectedFolders.isNotEmpty() && totalMatchingTracks > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iniciar Renombrado Masivo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opción 2: Búsqueda y Auto-Etiquetado Masivo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(cardBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Búsqueda y Auto-Etiquetado Masivo",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                )
                                Text(
                                    text = "Consulta iTunes, MusicBrainz y Deezer e incrusta metadatos y portada",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onlyMissingTags = !onlyMissingTags }
                        ) {
                            Checkbox(
                                checked = onlyMissingTags,
                                onCheckedChange = { onlyMissingTags = it },
                                colors = CheckboxDefaults.colors(checkedColor = VioletAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Solo canciones incompletas o sin carátula",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onStartBatchAutoTag(selectedFolders, onlyMissingTags) },
                            enabled = selectedFolders.isNotEmpty() && totalMatchingTracks > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iniciar Auto-Etiquetado Masivo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
