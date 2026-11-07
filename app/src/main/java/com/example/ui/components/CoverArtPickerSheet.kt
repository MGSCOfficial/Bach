package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CoverArtOption
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverArtPickerSheet(
    options: List<CoverArtOption>,
    selectedOption: CoverArtOption?,
    isLoading: Boolean,
    onSelectOption: (CoverArtOption) -> Unit,
    onCustomImagePicked: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        onCustomImagePicked(bytes)
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkBg,
        tonalElevation = 16.dp,
        modifier = Modifier.testTag("cover_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                        text = "Elige la Carátula",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                    )
                    Text(
                        text = "Cover Art Archive • iTunes • Deezer • Galería",
                        style = MaterialTheme.typography.bodySmall.copy(color = StudioTextSecondary)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = StudioTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option to pick custom image from device gallery
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_gallery_image_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EmeraldAccent
                ),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldAccent))
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar imagen desde el dispositivo", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buscando portadas en alta resolución...", color = StudioTextSecondary)
                    }
                }
            } else if (options.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay carátulas disponibles en línea. Puedes cargar una imagen desde tu dispositivo.",
                        color = StudioTextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(options, key = { it.id }) { option ->
                        val isSelected = selectedOption?.id == option.id || (selectedOption?.url != null && selectedOption.url == option.url)
                        CoverCard(
                            option = option,
                            isSelected = isSelected,
                            onSelect = { onSelectOption(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoverCard(
    option: CoverArtOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) EmeraldAccent else StudioBorder,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(StudioCardSurfaceVariant)
            ) {
                if (option.bytes != null) {
                    val bitmap = remember(option.bytes) {
                        try {
                            BitmapFactory.decodeByteArray(option.bytes, 0, option.bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = option.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (!option.url.isNullOrBlank()) {
                    AsyncImage(
                        model = option.url,
                        contentDescription = option.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Checkmark if selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(EmeraldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionada",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Resolution Badge
                if (option.width > 0 && option.height > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "${option.width}x${option.height}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StudioTextPrimary
                    ),
                    maxLines = 1
                )
                Text(
                    text = option.source,
                    style = MaterialTheme.typography.labelSmall.copy(color = StudioTextSecondary),
                    maxLines = 1
                )
            }
        }
    }
}
