package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    initialTitle: String = "",
    initialArtist: String = "",
    initialAlbum: String = "",
    activeSources: List<MetadataSource> = listOf(MetadataSource.ITUNES, MetadataSource.MUSICBRAINZ, MetadataSource.DEEZER),
    onSearchQuery: (String) -> Unit,
    onAdvancedSearch: (title: String, artist: String, album: String) -> Unit = { _, _, _ -> },
    onSelectCandidate: (OnlineCandidate) -> Unit,
    onDismiss: () -> Unit,
    onGeminiSearch: (() -> Unit)? = null
) {
    val accent = LocalAccentColor.current
    val cleanInitialQuery = initialQuery.replace("\uFEFF", "").replace("\u200B", "").trim()
    val cleanInitialTitle = initialTitle.replace("\uFEFF", "").replace("\u200B", "").trim()
    val cleanInitialArtist = initialArtist.replace("\uFEFF", "").replace("\u200B", "").trim()
    val cleanInitialAlbum = initialAlbum.replace("\uFEFF", "").replace("\u200B", "").trim()

    var searchQuery by remember(initialQuery) { mutableStateOf(cleanInitialQuery) }
    var selectedSourceFilter by remember { mutableStateOf<MetadataSource?>(null) }
    var previewCandidate by remember { mutableStateOf<OnlineCandidate?>(null) }

    // Advanced search state
    var showAdvancedSearch by remember { mutableStateOf(false) }
    var advTitle by remember(initialTitle) { mutableStateOf(cleanInitialTitle) }
    var advArtist by remember(initialArtist) { mutableStateOf(cleanInitialArtist) }
    var advAlbum by remember(initialAlbum) { mutableStateOf(cleanInitialAlbum) }

    val filteredCandidates = remember(candidates, selectedSourceFilter) {
        if (selectedSourceFilter == null) candidates
        else candidates.filter { it.source == selectedSourceFilter }
    }

    // Candidate preview dialog when tapped on image or text
    previewCandidate?.let { candidate ->
        CandidatePreviewDialog(
            candidate = candidate,
            onApply = {
                previewCandidate = null
                onSelectCandidate(candidate)
            },
            onDismiss = { previewCandidate = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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

            // Search query input field with external compact search button + Advanced toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar por artista o título...",
                                    fontSize = 13.sp,
                                    color = StudioTextMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = StudioTextPrimary,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(accent),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { onSearchQuery(searchQuery.replace("\uFEFF", "").replace("\u200B", "").trim()) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("online_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
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

                Spacer(modifier = Modifier.width(6.dp))

                // Advanced Search Toggle Button
                IconButton(
                    onClick = { showAdvancedSearch = !showAdvancedSearch },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showAdvancedSearch) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = if (showAdvancedSearch) accent else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Búsqueda avanzada",
                        tint = if (showAdvancedSearch) accent else StudioTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { onSearchQuery(searchQuery.replace("\uFEFF", "").replace("\u200B", "").trim()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
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

            // Advanced Search Expandable Panel
            AnimatedVisibility(
                visible = showAdvancedSearch,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioCardSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(StudioBorder)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Búsqueda avanzada por campos",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            )
                            Text(
                                text = "iTunes • Deezer • MB • Spotify...",
                                fontSize = 10.sp,
                                color = StudioTextMuted
                            )
                        }

                        // Title Field
                        AdvancedInputField(
                            label = "Título de la canción",
                            value = advTitle,
                            placeholder = "Ej: Double Fantasy",
                            onValueChange = { advTitle = it }
                        )

                        // Artist Field
                        AdvancedInputField(
                            label = "Nombre del artista",
                            value = advArtist,
                            placeholder = "Ej: The Weeknd",
                            onValueChange = { advArtist = it }
                        )

                        // Album Field
                        AdvancedInputField(
                            label = "Nombre del álbum",
                            value = advAlbum,
                            placeholder = "Ej: The Idol",
                            onValueChange = { advAlbum = it }
                        )

                        // Action Buttons: Clear & Search
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    advTitle = ""
                                    advArtist = ""
                                    advAlbum = ""
                                }
                            ) {
                                Text(
                                    text = "Limpiar",
                                    color = StudioTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    val t = advTitle.replace("\uFEFF", "").replace("\u200B", "").trim()
                                    val a = advArtist.replace("\uFEFF", "").replace("\u200B", "").trim()
                                    val al = advAlbum.replace("\uFEFF", "").replace("\u200B", "").trim()
                                    if (t.isNotBlank() || a.isNotBlank() || al.isNotBlank()) {
                                        searchQuery = listOf(a, t, al).filter { it.isNotBlank() }.joinToString(" - ")
                                        onAdvancedSearch(t, a, al)
                                        showAdvancedSearch = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Buscar campos",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        selectedContainerColor = accent,
                        selectedLabelColor = Color.Black
                    )
                )

                val allDisplaySources = remember(candidates, activeSources) {
                    (activeSources + candidates.map { it.source }).distinct()
                }

                allDisplaySources.forEach { src ->
                    val count = candidates.count { it.source == src }
                    val srcColor = when (src) {
                        MetadataSource.MUSICBRAINZ -> VioletAccent
                        MetadataSource.ITUNES -> RoseAccent
                        MetadataSource.DEEZER -> CyanAccent
                        MetadataSource.THEAUDIODB -> AmberAccent
                        MetadataSource.NETEASE -> RoseAccent
                        MetadataSource.SPOTIFY -> EmeraldAccent
                        MetadataSource.LASTFM -> AmberAccent
                        MetadataSource.DISCOGS -> CyanAccent
                        MetadataSource.GEMINI -> VioletAccent
                        MetadataSource.MANUAL -> accent
                    }
                    FilterChip(
                        selected = selectedSourceFilter == src,
                        onClick = {
                            selectedSourceFilter = if (selectedSourceFilter == src) null else src
                        },
                        label = { Text("${src.displayName} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = srcColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
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
                        CircularProgressIndicator(color = accent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consultando fuentes en línea...",
                            color = StudioTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (filteredCandidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No se encontraron coincidencias en las fuentes activas.",
                            color = StudioTextMuted,
                            fontSize = 14.sp
                        )
                        if (onGeminiSearch != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onGeminiSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resolver con Google Gemini AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
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
                            onPreview = { previewCandidate = candidate },
                            onSelect = { onSelectCandidate(candidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val accent = LocalAccentColor.current
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(3.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CandidateDetailedCard(
    candidate: OnlineCandidate,
    onPreview: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidate_card_${candidate.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.outlineVariant))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Tappable Header & Info Section -> triggers Preview Dialog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPreview)
            ) {
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
                            val accent = LocalAccentColor.current
                            val badgeColor = when (candidate.source) {
                                MetadataSource.MUSICBRAINZ -> VioletAccent
                                MetadataSource.ITUNES -> RoseAccent
                                MetadataSource.DEEZER -> CyanAccent
                                MetadataSource.THEAUDIODB -> AmberAccent
                                MetadataSource.NETEASE -> RoseAccent
                                MetadataSource.SPOTIFY -> EmeraldAccent
                                MetadataSource.LASTFM -> AmberAccent
                                MetadataSource.DISCOGS -> CyanAccent
                                MetadataSource.GEMINI -> VioletAccent
                                MetadataSource.MANUAL -> accent
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
                                    color = accent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${candidate.matchScore}% Coincidencia",
                                        color = accent,
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
                                color = LocalAccentColor.current
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Toca para previsualizar detalles completos",
                            fontSize = 10.sp,
                            color = StudioTextMuted
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Apply Button (concise text, no clipping)
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalAccentColor.current,
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
fun CandidatePreviewDialog(
    candidate: OnlineCandidate,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previsualización",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Large Cover Art
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!candidate.coverArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidate.coverArtUrl,
                            contentDescription = "Carátula completa de ${candidate.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = StudioTextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title and Artist
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = candidate.artist,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAccentColor.current
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Source and Match Score Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accent = LocalAccentColor.current
                    val badgeColor = when (candidate.source) {
                        MetadataSource.MUSICBRAINZ -> VioletAccent
                        MetadataSource.ITUNES -> RoseAccent
                        MetadataSource.DEEZER -> CyanAccent
                        MetadataSource.THEAUDIODB -> AmberAccent
                        MetadataSource.NETEASE -> RoseAccent
                        MetadataSource.SPOTIFY -> EmeraldAccent
                        MetadataSource.LASTFM -> AmberAccent
                        MetadataSource.DISCOGS -> CyanAccent
                        MetadataSource.GEMINI -> VioletAccent
                        MetadataSource.MANUAL -> accent
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Fuente: ${candidate.source.displayName}",
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (candidate.matchScore > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${candidate.matchScore}% Coincidencia",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Table of Metadata to Apply
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioCardSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogFieldRow("Álbum", candidate.album.ifBlank { "—" })
                        DialogFieldRow("Artista del Álbum", candidate.albumArtist.ifBlank { candidate.artist.ifBlank { "—" } })
                        DialogFieldRow("Año de Lanzamiento", candidate.year.ifBlank { "—" })
                        DialogFieldRow("Género", candidate.genre.ifBlank { "—" })
                        DialogFieldRow("Número de Pista", candidate.trackNumber.ifBlank { "—" })
                        if (candidate.discNumber.isNotBlank()) {
                            DialogFieldRow("Número de Disco", candidate.discNumber)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Cancel & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            color = StudioTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalAccentColor.current,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aplicar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = StudioTextMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = StudioTextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
            textAlign = TextAlign.End
        )
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
