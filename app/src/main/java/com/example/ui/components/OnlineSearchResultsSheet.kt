package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MetadataSource
import com.example.data.model.OnlineCandidate
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResultsSheet(
    candidates: List<OnlineCandidate>,
    isLoading: Boolean,
    initialQuery: String,
    onSearchQuery: (String) -> Unit,
    onSelectCandidate: (OnlineCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedSourceFilter by remember { mutableStateOf<MetadataSource?>(null) }

    val filteredCandidates = remember(candidates, selectedSourceFilter) {
        if (selectedSourceFilter == null) candidates
        else candidates.filter { it.source == selectedSourceFilter }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioDarkBg,
        tonalElevation = 16.dp,
        modifier = Modifier.testTag("online_results_sheet")
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
                Text(
                    text = "Resultados en línea",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search query input field with external compact search button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("online_search_input"),
                    placeholder = {
                        Text(
                            text = "Buscar por artista o título...",
                            fontSize = 13.sp,
                            color = StudioTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StudioCardSurface,
                        unfocusedContainerColor = StudioCardSurface,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { onSearchQuery(searchQuery) }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSearchQuery(searchQuery) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Buscar",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Source filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSourceFilter == null,
                    onClick = { selectedSourceFilter = null },
                    label = { Text("Todas (${candidates.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldAccent,
                        selectedLabelColor = Color.Black
                    )
                )

                FilterChip(
                    selected = selectedSourceFilter == MetadataSource.MUSICBRAINZ,
                    onClick = { selectedSourceFilter = MetadataSource.MUSICBRAINZ },
                    label = { Text("MusicBrainz") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VioletAccent,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedSourceFilter == MetadataSource.ITUNES,
                    onClick = { selectedSourceFilter = MetadataSource.ITUNES },
                    label = { Text("iTunes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoseAccent,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedSourceFilter == MetadataSource.DEEZER,
                    onClick = { selectedSourceFilter = MetadataSource.DEEZER },
                    label = { Text("Deezer") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                        Text(
                            text = "Consultando MusicBrainz, iTunes y Deezer...",
                            color = StudioTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (filteredCandidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron coincidencias. Prueba ajustando el nombre en el buscador.",
                        color = StudioTextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCandidates, key = { it.id }) { candidate ->
                        CandidateDetailedCard(
                            candidate = candidate,
                            onSelect = { onSelectCandidate(candidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateDetailedCard(
    candidate: OnlineCandidate,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidate_card_${candidate.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Cover Thumbnail + Badges + Title/Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioCardSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!candidate.coverArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidate.coverArtUrl,
                            contentDescription = "Carátula de ${candidate.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = StudioTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeColor = when (candidate.source) {
                            MetadataSource.MUSICBRAINZ -> VioletAccent
                            MetadataSource.ITUNES -> RoseAccent
                            MetadataSource.DEEZER -> CyanAccent
                            MetadataSource.MANUAL -> EmeraldAccent
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = candidate.source.displayName,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (candidate.matchScore > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldDark.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${candidate.matchScore}% Coincidencia",
                                    color = EmeraldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = candidate.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = candidate.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldAccent
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Symmetrical, well-structured Metadata Grid
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioCardSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Álbum + Año
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetadataFieldPreview(
                            label = "Álbum",
                            value = candidate.album.ifBlank { "Desconocido" },
                            modifier = Modifier.weight(1f)
                        )
                        MetadataFieldPreview(
                            label = "Año",
                            value = candidate.year.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Género + Pista #
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetadataFieldPreview(
                            label = "Género",
                            value = candidate.genre.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                        MetadataFieldPreview(
                            label = "Pista #",
                            value = candidate.trackNumber.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Apply Button (concise text, no clipping)
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_apply_candidate_${candidate.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aplicar metadatos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MetadataFieldPreview(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = StudioTextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = if (value == "—") StudioTextMuted else StudioTextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
