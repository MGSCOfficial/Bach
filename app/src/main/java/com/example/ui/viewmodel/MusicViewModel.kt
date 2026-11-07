package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.CoverArtOption
import com.example.data.model.OnlineCandidate
import com.example.data.model.TagDiff
import com.example.data.remote.OnlineMetadataRepository
import com.example.data.tagger.AudioTagReader
import com.example.data.tagger.AudioTagWriter
import com.example.player.AudioPreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class LibraryFilter(val label: String) {
    ALL("Todas"),
    MISSING_COVER("Sin Carátula"),
    MISSING_TAGS("Incompletas"),
    MP3_ONLY("MP3"),
    M4A_ONLY("M4A")
}

data class LibraryUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilter = LibraryFilter.ALL,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

data class EditorUiState(
    val originalTrack: AudioTrack? = null,
    val draftTrack: AudioTrack? = null,
    val onlineCandidates: List<OnlineCandidate> = emptyList(),
    val isSearchingOnline: Boolean = false,
    val selectedCandidate: OnlineCandidate? = null,
    val coverArtOptions: List<CoverArtOption> = emptyList(),
    val selectedCoverOption: CoverArtOption? = null,
    val isLoadingCovers: Boolean = false,
    val showCandidateSheet: Boolean = false,
    val showCoverPickerSheet: Boolean = false,
    val showDiffDialog: Boolean = false,
    val diffList: List<TagDiff> = emptyList(),
    val isSaving: Boolean = false,
    val saveNotification: String? = null
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnlineMetadataRepository()
    val audioPlayer = AudioPreviewPlayer(application)

    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    init {
        loadSampleTracks()
    }

    // ==================== LIBRARY MANAGEMENT ====================

    fun loadSampleTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            _libraryState.update { it.copy(isLoading = true, statusMessage = "Cargando biblioteca...") }
            try {
                val context = getApplication<Application>()
                val samplesDir = File(context.filesDir, "sample_tracks").apply { mkdirs() }
                val assetManager = context.assets
                val sampleNames = listOf(
                    "queen_bohemian.mp3",
                    "michael_jackson_billie.m4a",
                    "eagles_hotel_california.mp3",
                    "daft_punk_get_lucky.m4a"
                )

                val loadedTracks = mutableListOf<AudioTrack>()
                for (name in sampleNames) {
                    val destFile = File(samplesDir, name)
                    if (!destFile.exists() || destFile.length() == 0L) {
                        try {
                            assetManager.open("sample_tracks/$name").use { input ->
                                FileOutputStream(destFile).use { output -> input.copyTo(output) }
                            }
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Could not copy asset sample $name", e)
                        }
                    }

                    if (destFile.exists() && destFile.length() > 0L) {
                        val track = AudioTagReader.readFromFile(destFile)
                        if (track != null) {
                            loadedTracks.add(track)
                        }
                    }
                }

                _libraryState.update { state ->
                    val combined = (loadedTracks + state.tracks).distinctBy { it.fileName }
                    state.copy(
                        tracks = combined,
                        filteredTracks = applyFilterAndSearch(combined, state.activeFilter, state.searchQuery),
                        isLoading = false,
                        statusMessage = "Se cargaron ${loadedTracks.size} pistas de prueba."
                    )
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error loading sample tracks", e)
                _libraryState.update { it.copy(isLoading = false, statusMessage = "Error al cargar muestras") }
            }
        }
    }

    fun addFilesFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _libraryState.update { it.copy(isLoading = true, statusMessage = "Importando archivos...") }
            val context = getApplication<Application>()
            val imported = mutableListOf<AudioTrack>()

            for (uri in uris) {
                try {
                    val (fileName, size) = queryFileInfo(context, uri)
                    val format = AudioFormat.fromFileName(fileName)
                    val track = AudioTagReader.readTrack(context, uri, fileName, size)
                    if (track != null) {
                        imported.add(track)
                    }
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Error importing uri $uri", e)
                }
            }

            _libraryState.update { state ->
                val newTracks = (imported + state.tracks).distinctBy { it.fileUri }
                state.copy(
                    tracks = newTracks,
                    filteredTracks = applyFilterAndSearch(newTracks, state.activeFilter, state.searchQuery),
                    isLoading = false,
                    statusMessage = "Importadas ${imported.size} canciones."
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _libraryState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTracks = applyFilterAndSearch(state.tracks, state.activeFilter, query)
            )
        }
    }

    fun onFilterChanged(filter: LibraryFilter) {
        _libraryState.update { state ->
            state.copy(
                activeFilter = filter,
                filteredTracks = applyFilterAndSearch(state.tracks, filter, state.searchQuery)
            )
        }
    }

    private fun applyFilterAndSearch(
        tracks: List<AudioTrack>,
        filter: LibraryFilter,
        query: String
    ): List<AudioTrack> {
        val filtered = when (filter) {
            LibraryFilter.ALL -> tracks
            LibraryFilter.MISSING_COVER -> tracks.filter { !it.hasCover }
            LibraryFilter.MISSING_TAGS -> tracks.filter { !it.isComplete }
            LibraryFilter.MP3_ONLY -> tracks.filter { it.format == AudioFormat.MP3 }
            LibraryFilter.M4A_ONLY -> tracks.filter { it.format == AudioFormat.M4A }
        }

        if (query.isBlank()) return filtered
        val q = query.trim().lowercase()
        return filtered.filter {
            it.displayTitle.lowercase().contains(q) ||
                    it.displayArtist.lowercase().contains(q) ||
                    it.displayAlbum.lowercase().contains(q) ||
                    it.fileName.lowercase().contains(q)
        }
    }

    // ==================== EDITOR WORKFLOW ====================

    fun openTrackEditor(track: AudioTrack) {
        _editorState.value = EditorUiState(
            originalTrack = track,
            draftTrack = track.copy(),
            selectedCoverOption = if (track.hasCover && track.coverArtBytes != null) {
                CoverArtOption(
                    source = "Actual del archivo",
                    bytes = track.coverArtBytes,
                    label = "Carátula incrustada",
                    isSelected = true
                )
            } else null
        )
    }

    fun closeTrackEditor() {
        audioPlayer.stop()
        _editorState.value = EditorUiState()
    }

    fun updateDraftField(
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        albumArtist: String? = null,
        year: String? = null,
        genre: String? = null,
        trackNumber: String? = null,
        discNumber: String? = null,
        comment: String? = null
    ) {
        _editorState.update { state ->
            val draft = state.draftTrack ?: return@update state
            state.copy(
                draftTrack = draft.copy(
                    title = title ?: draft.title,
                    artist = artist ?: draft.artist,
                    album = album ?: draft.album,
                    albumArtist = albumArtist ?: draft.albumArtist,
                    year = year ?: draft.year,
                    genre = genre ?: draft.genre,
                    trackNumber = trackNumber ?: draft.trackNumber,
                    discNumber = discNumber ?: draft.discNumber,
                    comment = comment ?: draft.comment
                )
            )
        }
    }

    fun searchOnlineForCurrentTrack(customQuery: String? = null) {
        val draft = _editorState.value.draftTrack ?: return
        val query = customQuery?.ifBlank { null }
            ?: when {
                draft.artist.isNotBlank() && draft.title.isNotBlank() -> "${draft.artist} ${draft.title}"
                draft.title.isNotBlank() -> draft.title
                else -> draft.fileName.substringBeforeLast(".")
            }

        viewModelScope.launch {
            _editorState.update { it.copy(isSearchingOnline = true, showCandidateSheet = true) }
            val candidates = repository.searchAllSources(query)
            _editorState.update {
                it.copy(
                    isSearchingOnline = false,
                    onlineCandidates = candidates
                )
            }
        }
    }

    fun selectCandidate(candidate: OnlineCandidate) {
        viewModelScope.launch {
            _editorState.update { state ->
                val draft = state.draftTrack ?: return@update state
                state.copy(
                    selectedCandidate = candidate,
                    showCandidateSheet = false,
                    draftTrack = draft.copy(
                        title = candidate.title.ifBlank { draft.title },
                        artist = candidate.artist.ifBlank { draft.artist },
                        album = candidate.album.ifBlank { draft.album },
                        albumArtist = candidate.albumArtist.ifBlank { draft.albumArtist.ifBlank { candidate.artist } },
                        year = candidate.year.ifBlank { draft.year },
                        genre = candidate.genre.ifBlank { draft.genre },
                        trackNumber = candidate.trackNumber.ifBlank { draft.trackNumber },
                        discNumber = candidate.discNumber.ifBlank { draft.discNumber }
                    ),
                    isLoadingCovers = true
                )
            }

            // Fetch cover art options from MusicBrainz Cover Art Archive, iTunes, Deezer
            val covers = repository.fetchCoverArtOptions(candidate)
            _editorState.update { state ->
                val bestCover = covers.firstOrNull { it.isSelected } ?: covers.firstOrNull()
                state.copy(
                    coverArtOptions = covers,
                    selectedCoverOption = bestCover ?: state.selectedCoverOption,
                    isLoadingCovers = false
                )
            }
        }
    }

    fun openCoverPicker() {
        _editorState.update { it.copy(showCoverPickerSheet = true) }
    }

    fun selectCoverOption(option: CoverArtOption) {
        _editorState.update { state ->
            state.copy(
                selectedCoverOption = option,
                showCoverPickerSheet = false,
                coverArtOptions = state.coverArtOptions.map {
                    it.copy(isSelected = it.id == option.id)
                }
            )
        }
    }

    fun setCustomCoverFromBytes(bytes: ByteArray, label: String = "Imagen seleccionada") {
        val customOption = CoverArtOption(
            source = "Personalizada",
            bytes = bytes,
            label = label,
            isSelected = true
        )
        _editorState.update { state ->
            state.copy(
                selectedCoverOption = customOption,
                showCoverPickerSheet = false,
                coverArtOptions = listOf(customOption) + state.coverArtOptions
            )
        }
    }

    fun hideCandidateSheet() {
        _editorState.update { it.copy(showCandidateSheet = false) }
    }

    fun hideCoverPickerSheet() {
        _editorState.update { it.copy(showCoverPickerSheet = false) }
    }

    // ==================== DIFF PREVIEW & APPLY ====================

    fun showDiffPreview() {
        val original = _editorState.value.originalTrack ?: return
        val draft = _editorState.value.draftTrack ?: return
        val selectedCover = _editorState.value.selectedCoverOption

        val diffs = listOf(
            TagDiff("Título", original.title, draft.title),
            TagDiff("Artista", original.artist, draft.artist),
            TagDiff("Álbum", original.album, draft.album),
            TagDiff("Artista del Álbum", original.albumArtist, draft.albumArtist),
            TagDiff("Año", original.year, draft.year),
            TagDiff("Género", original.genre, draft.genre),
            TagDiff("Pista #", original.trackNumber, draft.trackNumber),
            TagDiff("Disco #", original.discNumber, draft.discNumber),
            TagDiff(
                "Carátula",
                if (original.hasCover) "Incrustada (${original.coverArtBytes?.size ?: 0} B)" else "Sin carátula",
                selectedCover?.label ?: if (draft.hasCover) "Mantener actual" else "Sin carátula",
                isChanged = selectedCover != null && (selectedCover.bytes != original.coverArtBytes || selectedCover.url != null)
            )
        )

        _editorState.update { it.copy(diffList = diffs, showDiffDialog = true) }
    }

    fun hideDiffDialog() {
        _editorState.update { it.copy(showDiffDialog = false) }
    }

    fun applyAndSaveTags() {
        val original = _editorState.value.originalTrack ?: return
        val draft = _editorState.value.draftTrack ?: return
        val coverOption = _editorState.value.selectedCoverOption

        viewModelScope.launch(Dispatchers.IO) {
            _editorState.update { it.copy(isSaving = true) }

            // Download image bytes if URL option was chosen
            val coverBytes: ByteArray? = when {
                coverOption?.bytes != null -> coverOption.bytes
                coverOption?.url != null -> repository.downloadImageBytes(coverOption.url)
                else -> original.coverArtBytes
            }

            val savedFile = AudioTagWriter.writeTags(
                context = getApplication(),
                originalTrack = original,
                updatedTrack = draft,
                coverBytes = coverBytes
            )

            if (savedFile != null) {
                val updatedTrack = draft.copy(
                    filePath = savedFile.absolutePath,
                    fileUri = Uri.fromFile(savedFile).toString(),
                    hasCover = coverBytes != null && coverBytes.isNotEmpty(),
                    coverArtBytes = coverBytes,
                    sizeBytes = savedFile.length(),
                    dateModified = System.currentTimeMillis()
                )

                _libraryState.update { state ->
                    val updatedList = state.tracks.map {
                        if (it.id == original.id) updatedTrack else it
                    }
                    state.copy(
                        tracks = updatedList,
                        filteredTracks = applyFilterAndSearch(updatedList, state.activeFilter, state.searchQuery),
                        statusMessage = "¡Etiquetas guardadas con éxito en ${savedFile.name}!"
                    )
                }

                _editorState.update {
                    it.copy(
                        isSaving = false,
                        showDiffDialog = false,
                        originalTrack = updatedTrack,
                        draftTrack = updatedTrack.copy(),
                        saveNotification = "¡Metadatos y carátula aplicados correctamente!"
                    )
                }
            } else {
                _editorState.update {
                    it.copy(
                        isSaving = false,
                        showDiffDialog = false,
                        saveNotification = "Error al escribir los metadatos en el archivo."
                    )
                }
            }
        }
    }

    fun dismissSaveNotification() {
        _editorState.update { it.copy(saveNotification = null) }
    }

    private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "audio_track"
        var size = 0L
        if (uri.scheme == "file") {
            val f = File(uri.path ?: "")
            return Pair(f.name, f.length())
        }

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        return Pair(name, size)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
