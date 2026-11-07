package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AudioTrack
import com.example.data.model.CoverArtOption
import com.example.data.model.TagDiff
import com.example.ui.theme.*

@Composable
fun PreviewDiffDialog(
    originalTrack: AudioTrack,
    draftTrack: AudioTrack,
    selectedCover: CoverArtOption?,
    diffList: List<TagDiff>,
    isSaving: Boolean,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("preview_diff_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = StudioDarkBg,
            tonalElevation = 12.dp,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Previsualizar Cambios",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                        )
                        Text(
                            text = "Compara antes de escribir en el archivo ${draftTrack.format.name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = StudioTextSecondary)
                        )
                    }

                    if (!isSaving) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = StudioTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cover Art Comparison Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Original Cover
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Carátula Actual",
                                fontSize = 11.sp,
                                color = StudioTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioCardSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (originalTrack.hasCover && originalTrack.coverArtBytes != null) {
                                    val bitmap = remember(originalTrack.coverArtBytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(originalTrack.coverArtBytes, 0, originalTrack.coverArtBytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = StudioTextMuted)
                                    }
                                } else {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = StudioTextMuted)
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )

                        // New Proposed Cover
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Nueva Carátula",
                                fontSize = 11.sp,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioCardSurfaceVariant)
                                    .border(2.dp, EmeraldAccent, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedCover?.bytes != null) {
                                    val bitmap = remember(selectedCover.bytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(selectedCover.bytes, 0, selectedCover.bytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else if (!selectedCover?.url.isNullOrBlank()) {
                                    AsyncImage(
                                        model = selectedCover?.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (originalTrack.hasCover && originalTrack.coverArtBytes != null) {
                                    val bitmap = remember(originalTrack.coverArtBytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(originalTrack.coverArtBytes, 0, originalTrack.coverArtBytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = StudioTextMuted)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tag comparison table
                Text(
                    text = "Campos de Metadatos",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = StudioTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(diffList) { diff ->
                        DiffFieldRow(diff = diff)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StudioTextSecondary
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onConfirmSave,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("confirm_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiffFieldRow(diff: TagDiff) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (diff.isChanged) EmeraldDark.copy(alpha = 0.12f) else StudioCardSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (diff.isChanged) EmeraldAccent.copy(alpha = 0.5f) else StudioBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diff.fieldName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (diff.isChanged) EmeraldAccent else StudioTextSecondary
                    )
                )

                if (diff.isChanged) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Modificado",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Old value
                Text(
                    text = diff.originalValue.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (diff.isChanged) StudioTextMuted else StudioTextSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (diff.isChanged) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(14.dp)
                    )

                    // New value
                    Text(
                        text = diff.newValue.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StudioTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
