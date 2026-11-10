package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.MusicFolder
import com.example.player.PlayerState
import com.example.ui.theme.*
import com.example.ui.util.AudioThumbnailLoader

@Composable
fun AudioCoverThumbnail(
    track: AudioTrack,
    modifier: Modifier = Modifier,
    placeholderIconSize: androidx.compose.ui.unit.Dp = 30.dp
) {
    val context = LocalContext.current
    val bitmapState = produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = track.id,
        key2 = track.hasCover,
        key3 = track.coverArtBytes
    ) {
        if (track.coverArtBytes != null) {
            value = try {
                BitmapFactory.decodeByteArray(track.coverArtBytes, 0, track.coverArtBytes.size)
            } catch (e: Exception) {
                null
            }
        } else if (track.hasCover) {
            value = AudioThumbnailLoader.loadThumbnail(context, track.fileUri)
        } else {
            value = null
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Carátula de ${track.displayTitle}",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            tint = StudioTextMuted,
            modifier = Modifier.size(placeholderIconSize)
        )
    }
}

@Composable
fun TrackCard(
    track: AudioTrack,
    isPlaying: Boolean,
    onTrackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_card_${track.id}")
            .clickable(onClick = onTrackClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isPlaying) LocalAccentColor.current.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail with Format Badge
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AudioCoverThumbnail(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    placeholderIconSize = 28.dp
                )

                // Format badge overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = if (track.format == AudioFormat.MP3) EmeraldDark.copy(alpha = 0.9f) else VioletDark.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = track.format.name,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Track details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.displayArtist} • ${track.displayAlbum}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tag Status & Duration Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (track.isComplete) {
                        StatusChip(
                            label = "Completo",
                            color = LocalAccentColor.current,
                            icon = Icons.Default.CheckCircle
                        )
                    } else if (!track.hasCover) {
                        StatusChip(
                            label = "Sin carátula",
                            color = AmberAccent,
                            icon = Icons.Default.Image
                        )
                    } else {
                        StatusChip(
                            label = "Incompleto",
                            color = RoseAccent,
                            icon = Icons.Default.Warning
                        )
                    }

                    if (track.hasLyrics) {
                        StatusChip(
                            label = "Letra",
                            color = CyanAccent,
                            icon = Icons.Default.Description
                        )
                    }

                    if (track.durationMs > 0) {
                        Text(
                            text = track.formattedDuration,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Play / Pause Button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isPlaying) LocalAccentColor.current else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                    .testTag("track_play_${track.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = if (isPlaying) Color.Black else StudioTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun TrackGridCard(
    track: AudioTrack,
    isPlaying: Boolean,
    onTrackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_grid_card_${track.id}")
            .clickable(onClick = onTrackClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(
                    if (isPlaying) accent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Square Cover Art with Floating Badge & Play Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AudioCoverThumbnail(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    placeholderIconSize = 42.dp
                )

                // Format badge overlay (Top-Start)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = if (track.format == AudioFormat.MP3) EmeraldDark.copy(alpha = 0.9f) else VioletDark.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = track.format.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Play / Pause Circle Button (Bottom-End)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onPlayPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = if (isPlaying) accent else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Info Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = track.displayArtist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (track.hasLyrics) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = CyanAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "LRC",
                                color = CyanAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderGridCard(
    folder: MusicFolder,
    trackCount: Int,
    onFolderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("folder_grid_card_${folder.uriString}")
            .clickable(onClick = onFolderClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Desvincular carpeta",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = folder.relativePath,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$trackCount canciones",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.4f))))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    track: AudioTrack?,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = track != null && playerState.activeTrackId != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (track == null) return@AnimatedVisibility

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("mini_player_bar"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LocalAccentColor.current.copy(alpha = 0.5f), VioletAccent.copy(alpha = 0.5f))))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioCoverThumbnail(
                            track = track,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIconSize = 22.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenEditor)
                    ) {
                        Text(
                            text = track.displayTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.displayArtist,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onOpenEditor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar etiquetas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(LocalAccentColor.current, CircleShape)
                            .testTag("mini_player_toggle")
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Progress Bar
                val progress = if (playerState.durationMs > 0) {
                    playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = LocalAccentColor.current,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}
