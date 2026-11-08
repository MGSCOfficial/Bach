package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.LyricsRepository
import com.example.data.remote.OnlineMetadataRepository
import com.example.data.storage.AccentColorPreset
import com.example.data.storage.AppSettings
import com.example.data.storage.FolderManager
import com.example.data.storage.SettingsManager
import com.example.data.storage.ThemeMode
import com.example.data.tagger.AudioTagReader
import com.example.data.tagger.AudioTagWriter
import com.example.player.AudioPreviewPlayer
import com.example.ui.util.AudioThumbnailLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryFilter(val label: String) {
    ALL("Todas"),
    MISSING_COVER("Sin Carátula"),
    MISSING_LYRICS("Sin Letra"),
    MISSING_TAGS("Incompletas"),
    MP3_ONLY("MP3"),
    M4A_ONLY("M4A")
}

data class LibraryUiState(
    val folders: List<MusicFolder> = emptyList(),
    val tracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilter = LibraryFilter.ALL,
    val sortOrder: SortOrder = SortOrder.RECENTLY_MODIFIED,
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
    val isSearchingLyrics: Boolean = false,
    val saveLrcFile: Boolean = true,
    val embedLyrics: Boolean = true,
    val showCandidateSheet: Boolean = false,
    val showCoverPickerSheet: Boolean = false,
    val showDiffDialog: Boolean = false,
    val diffList: List<TagDiff> = emptyList(),
    val isSaving: Boolean = false,
    val saveNotification: String? = null
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val onlineRepo = OnlineMetadataRepository()
    private val lyricsRepo = LyricsRepository()
    val folderManager = FolderManager(application)
    val audioPlayer = AudioPreviewPlayer(application)
    val settingsManager = SettingsManager(application)

    private val _settings = MutableStateFlow(settingsManager.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
        _settings.update { it.copy(themeMode = mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        settingsManager.setDynamicColor(enabled)
        _settings.update { it.copy(dynamicColor = enabled) }
    }

    fun updateAccentPreset(preset: AccentColorPreset) {
        settingsManager.setAccentPreset(preset)
        _settings.update { it.copy(accentPreset = preset) }
    }

    fun updateAmoledDark(enabled: Boolean) {
        settingsManager.setAmoledDark(enabled)
        _settings.update { it.copy(amoledDark = enabled) }
    }

    fun updateAutoSearchLyrics(enabled: Boolean) {
        settingsManager.setAutoSearchLyrics(enabled)
        _settings.update { it.copy(autoSearchLyrics = enabled) }
    }

    fun updateDefaultSaveLrc(enabled: Boolean) {
        settingsManager.setDefaultSaveLrc(enabled)
        _settings.update { it.copy(defaultSaveLrc = enabled) }
    }

    fun updateDefaultEmbedLyrics(enabled: Boolean) {
        settingsManager.setDefaultEmbedLyrics(enabled)
        _settings.update { it.copy(defaultEmbedLyrics = enabled) }
    }

    fun updateSourceEnabled(sourceKey: String, enabled: Boolean) {
        settingsManager.setSourceEnabled(sourceKey, enabled)
        _settings.value = settingsManager.loadSettings()
    }

    fun updateApiKey(keyIdentifier: String, value: String) {
        settingsManager.setApiKey(keyIdentifier, value)
        _settings.value = settingsManager.loadSettings()
    }

    init {
        loadInitialLibrary()
    }

    // ==================== FAST LIBRARY INITIALIZATION & SCAN ====================

    private fun loadInitialLibrary() {
        viewModelScope.launch {
            // 1. Instantly load from local JSON cache (< 10ms)
            val (cachedFolders, cachedTracks) = folderManager.loadCachedTracks()
            if (cachedTracks.isNotEmpty()) {
                _libraryState.update { state ->
                    state.copy(
                        folders = cachedFolders,
                        tracks = cachedTracks,
                        filteredTracks = applyFilterSearchAndSort(
                            cachedTracks,
                            state.activeFilter,
                            state.searchQuery,
                            state.sortOrder
                        ),
                        isLoading = false,
                        statusMessage = "${cachedTracks.size} canciones cargadas al instante."
                    )
                }
            } else {
                // If cache is empty but we have saved folders, run initial fast scan
                val savedUris = folderManager.getSavedFolderUris()
                if (savedUris.isNotEmpty()) {
                    rescanSavedFolders()
                } else {
                    _libraryState.update { it.copy(statusMessage = "Selecciona una carpeta para comenzar.") }
                }
            }
        }
    }

    fun rescanSavedFolders() {
        viewModelScope.launch {
            _libraryState.update { it.copy(isLoading = true, statusMessage = "Escaneando carpetas de música...") }
            val (folders, tracks) = folderManager.scanFolders()
            _libraryState.update { state ->
                state.copy(
                    folders = folders,
                    tracks = tracks,
                    filteredTracks = applyFilterSearchAndSort(
                        tracks,
                        state.activeFilter,
                        state.searchQuery,
                        state.sortOrder
                    ),
                    isLoading = false,
                    statusMessage = if (folders.isEmpty()) {
                        "Selecciona una carpeta para cargar tu música."
                    } else {
                        "Encontradas ${tracks.size} canciones en ${folders.size} carpetas."
                    }
                )
            }
        }
    }

    fun addMusicFolder(treeUri: Uri) {
        viewModelScope.launch {
            _libraryState.update { it.copy(isLoading = true, statusMessage = "Analizando carpeta...") }
            folderManager.addFolderUri(treeUri)
            val (folders, tracks) = folderManager.scanFolders()
            _libraryState.update { state ->
                state.copy(
                    folders = folders,
                    tracks = tracks,
                    filteredTracks = applyFilterSearchAndSort(
                        tracks,
                        state.activeFilter,
                        state.searchQuery,
                        state.sortOrder
                    ),
                    isLoading = false,
                    statusMessage = "Carpeta añadida con ${tracks.size} canciones."
                )
            }
        }
    }

    fun removeMusicFolder(uriString: String) {
        viewModelScope.launch {
            folderManager.removeFolder(uriString)
            val (folders, tracks) = folderManager.scanFolders()
            _libraryState.update { state ->
                state.copy(
                    folders = folders,
                    tracks = tracks,
                    filteredTracks = applyFilterSearchAndSort(
                        tracks,
                        state.activeFilter,
                        state.searchQuery,
                        state.sortOrder
                    ),
                    statusMessage = "Carpeta desvinculada."
                )
            }
        }
    }

    // ==================== SEARCH, FILTER & SORT ====================

    fun onSearchQueryChanged(query: String) {
        _libraryState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTracks = applyFilterSearchAndSort(
                    state.tracks,
                    state.activeFilter,
                    query,
                    state.sortOrder
                )
            )
        }
    }

    fun onFilterChanged(filter: LibraryFilter) {
        _libraryState.update { state ->
            state.copy(
                activeFilter = filter,
                filteredTracks = applyFilterSearchAndSort(
                    state.tracks,
                    filter,
                    state.searchQuery,
                    state.sortOrder
                )
            )
        }
    }

    fun onSortOrderChanged(order: SortOrder) {
        _libraryState.update { state ->
            state.copy(
                sortOrder = order,
                filteredTracks = applyFilterSearchAndSort(
                    state.tracks,
                    state.activeFilter,
                    state.searchQuery,
                    order
                )
            )
        }
    }

    private fun applyFilterSearchAndSort(
        tracks: List<AudioTrack>,
        filter: LibraryFilter,
        query: String,
        sortOrder: SortOrder
    ): List<AudioTrack> {
        // 1. Filter
        val filtered = when (filter) {
            LibraryFilter.ALL -> tracks
            LibraryFilter.MISSING_COVER -> tracks.filter { !it.hasCover }
            LibraryFilter.MISSING_LYRICS -> tracks.filter { !it.hasLyrics }
            LibraryFilter.MISSING_TAGS -> tracks.filter { !it.isComplete }
            LibraryFilter.MP3_ONLY -> tracks.filter { it.format == AudioFormat.MP3 }
            LibraryFilter.M4A_ONLY -> tracks.filter { it.format == AudioFormat.M4A }
        }

        // 2. Search
        val searched = if (query.isBlank()) {
            filtered
        } else {
            val q = query.trim().lowercase()
            filtered.filter {
                it.displayTitle.lowercase().contains(q) ||
                        it.displayArtist.lowercase().contains(q) ||
                        it.displayAlbum.lowercase().contains(q) ||
                        it.fileName.lowercase().contains(q)
            }
        }

        // 3. Sort
        return when (sortOrder) {
            SortOrder.RECENTLY_MODIFIED -> searched.sortedByDescending { it.dateModified }
            SortOrder.TITLE_ASC -> searched.sortedBy { it.displayTitle.lowercase() }
            SortOrder.TITLE_DESC -> searched.sortedByDescending { it.displayTitle.lowercase() }
            SortOrder.ARTIST_ASC -> searched.sortedBy { it.displayArtist.lowercase() }
            SortOrder.ALBUM_ASC -> searched.sortedBy { it.displayAlbum.lowercase() }
            SortOrder.DURATION_DESC -> searched.sortedByDescending { it.durationMs }
            SortOrder.DURATION_ASC -> searched.sortedBy { it.durationMs }
            SortOrder.SIZE_DESC -> searched.sortedByDescending { it.sizeBytes }
        }
    }

    // ==================== EDITOR WORKFLOW ====================

    fun openTrackEditor(track: AudioTrack) {
        val initialCover = if (track.hasCover && track.coverArtBytes != null) {
            CoverArtOption(
                source = "Actual del archivo",
                bytes = track.coverArtBytes,
                label = "Carátula incrustada",
                isSelected = true
            )
        } else null

        _editorState.value = EditorUiState(
            originalTrack = track,
            draftTrack = track.copy(),
            selectedCoverOption = initialCover,
            coverArtOptions = if (initialCover != null) listOf(initialCover) else emptyList(),
            saveLrcFile = _settings.value.defaultSaveLrc,
            embedLyrics = _settings.value.defaultEmbedLyrics
        )

        // If track hasCover is true but cover bytes were not loaded during quick scan, load on demand
        if (track.hasCover && track.coverArtBytes == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val bytes = AudioTagReader.loadEmbeddedPicture(getApplication(), Uri.parse(track.fileUri))
                if (bytes != null && bytes.isNotEmpty()) {
                    _editorState.update { state ->
                        val updatedDraft = state.draftTrack?.copy(coverArtBytes = bytes, hasCover = true)
                        val opt = CoverArtOption(
                            source = "Actual del archivo",
                            bytes = bytes,
                            label = "Carátula incrustada (${bytes.size / 1024} KB)",
                            isSelected = true
                        )
                        state.copy(
                            draftTrack = updatedDraft,
                            selectedCoverOption = opt,
                            coverArtOptions = listOf(opt)
                        )
                    }
                }
            }
        }
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
        comment: String? = null,
        lyrics: String? = null
    ) {
        _editorState.update { state ->
            val draft = state.draftTrack ?: return@update state
            val newLyrics = lyrics ?: draft.lyrics
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
                    comment = comment ?: draft.comment,
                    lyrics = newLyrics,
                    hasLyrics = newLyrics.isNotBlank()
                )
            )
        }
    }

    fun setSaveLrcFile(enabled: Boolean) {
        _editorState.update { it.copy(saveLrcFile = enabled) }
    }

    fun setEmbedLyrics(enabled: Boolean) {
        _editorState.update { it.copy(embedLyrics = enabled) }
    }

    fun getActiveMetadataSources(): List<MetadataSource> {
        val s = _settings.value
        val list = mutableListOf<MetadataSource>()
        if (s.sourceITunes) list.add(MetadataSource.ITUNES)
        if (s.sourceMusicBrainz) list.add(MetadataSource.MUSICBRAINZ)
        if (s.sourceDeezer) list.add(MetadataSource.DEEZER)
        if (s.sourceTheAudioDb) list.add(MetadataSource.THEAUDIODB)
        if (s.sourceNetEase) list.add(MetadataSource.NETEASE)
        if (s.sourceSpotify) list.add(MetadataSource.SPOTIFY)
        if (s.sourceLastFm) list.add(MetadataSource.LASTFM)
        if (s.sourceDiscogs) list.add(MetadataSource.DISCOGS)
        return list
    }

    fun searchOnlineForCurrentTrack(customQuery: String? = null) {
        val draft = _editorState.value.draftTrack ?: return

        viewModelScope.launch {
            _editorState.update { it.copy(isSearchingOnline = true, showCandidateSheet = true) }
            val cleanTitle = draft.title.replace("\uFEFF", "").replace("\u200B", "").trim()
            val cleanArtist = draft.artist.replace("\uFEFF", "").replace("\u200B", "").trim()
            val cleanAlbum = draft.album.replace("\uFEFF", "").replace("\u200B", "").trim()

            val candidates = if (customQuery == null && cleanAlbum.isNotBlank() && (cleanArtist.isNotBlank() || cleanTitle.isNotBlank())) {
                onlineRepo.searchAdvanced(cleanTitle, cleanArtist, cleanAlbum, _settings.value)
            } else {
                val query = customQuery?.replace("\uFEFF", "")?.replace("\u200B", "")?.trim()?.ifBlank { null }
                    ?: when {
                        cleanArtist.isNotBlank() && cleanTitle.isNotBlank() -> "$cleanArtist $cleanTitle"
                        cleanTitle.isNotBlank() -> cleanTitle
                        else -> draft.fileName.substringBeforeLast(".").replace("\uFEFF", "").replace("\u200B", "").trim()
                    }
                onlineRepo.searchAllSources(query, _settings.value)
            }
            _editorState.update {
                it.copy(
                    isSearchingOnline = false,
                    onlineCandidates = candidates
                )
            }
        }
    }

    fun searchOnlineAdvanced(title: String, artist: String, album: String) {
        viewModelScope.launch {
            _editorState.update { it.copy(isSearchingOnline = true, showCandidateSheet = true) }
            val cleanTitle = title.replace("\uFEFF", "").replace("\u200B", "").trim()
            val cleanArtist = artist.replace("\uFEFF", "").replace("\u200B", "").trim()
            val cleanAlbum = album.replace("\uFEFF", "").replace("\u200B", "").trim()
            val candidates = onlineRepo.searchAdvanced(cleanTitle, cleanArtist, cleanAlbum, _settings.value)
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
            // 1. Immediately apply ALL text metadata to draft
            _editorState.update { state ->
                val draft = state.draftTrack ?: return@update state
                state.copy(
                    selectedCandidate = candidate,
                    showCandidateSheet = false,
                    draftTrack = draft.copy(
                        title = candidate.title.ifBlank { draft.title },
                        artist = candidate.artist.ifBlank { draft.artist },
                        album = candidate.album.ifBlank { draft.album },
                        albumArtist = candidate.albumArtist.ifBlank { candidate.artist },
                        year = candidate.year.ifBlank { draft.year },
                        genre = candidate.genre.ifBlank { draft.genre },
                        trackNumber = candidate.trackNumber.ifBlank { draft.trackNumber },
                        discNumber = candidate.discNumber.ifBlank { draft.discNumber }
                    ),
                    isLoadingCovers = true,
                    saveNotification = "Metadatos aplicados: ${candidate.title} — ${candidate.artist}"
                )
            }

            // 2. Fetch cover art options from Cover Art Archive, iTunes, Deezer
            val covers = onlineRepo.fetchCoverArtOptions(candidate)

            // If candidate has a cover URL, download bytes immediately so it's loaded in memory
            var bestCover = covers.firstOrNull { it.isSelected } ?: covers.firstOrNull()
            if (bestCover != null && bestCover.bytes == null && !bestCover.url.isNullOrBlank()) {
                val downloaded = onlineRepo.downloadImageBytes(bestCover.url)
                if (downloaded != null) {
                    bestCover = bestCover.copy(bytes = downloaded)
                }
            }

            _editorState.update { state ->
                val updatedDraft = if (bestCover?.bytes != null) {
                    state.draftTrack?.copy(
                        coverArtBytes = bestCover.bytes,
                        hasCover = true
                    )
                } else {
                    state.draftTrack
                }

                state.copy(
                    coverArtOptions = covers,
                    selectedCoverOption = bestCover ?: state.selectedCoverOption,
                    draftTrack = updatedDraft,
                    isLoadingCovers = false
                )
            }

            // 3. Search and auto-populate lyrics from LRCLIB if enabled in settings
            if (_settings.value.autoSearchLyrics) {
                searchLyricsForCurrentTrack()
            }
        }
    }

    fun searchLyricsForCurrentTrack() {
        val draft = _editorState.value.draftTrack ?: return
        if (draft.title.isBlank() && draft.artist.isBlank()) return

        viewModelScope.launch {
            _editorState.update { it.copy(isSearchingLyrics = true) }
            val result = lyricsRepo.fetchLyrics(
                trackName = draft.title,
                artistName = draft.artist,
                albumName = draft.album,
                durationMs = draft.durationMs,
                settings = _settings.value
            )

            _editorState.update { state ->
                val curDraft = state.draftTrack ?: return@update state
                val clean = result?.bestLyrics?.trim().orEmpty()
                val hasValid = result != null && clean.isNotBlank() && !clean.equals("null", ignoreCase = true)
                if (hasValid) {
                    state.copy(
                        isSearchingLyrics = false,
                        draftTrack = curDraft.copy(
                            lyrics = clean,
                            hasLyrics = true
                        ),
                        saveNotification = "Letra encontrada y asignada (${result?.source})."
                    )
                } else {
                    state.copy(
                        isSearchingLyrics = false,
                        saveNotification = "No se encontró letra en línea para esta canción."
                    )
                }
            }
        }
    }

    fun openCoverPicker() {
        _editorState.update { it.copy(showCoverPickerSheet = true) }
    }

    fun selectCoverOption(option: CoverArtOption) {
        viewModelScope.launch {
            var finalOption = option
            if (option.bytes == null && !option.url.isNullOrBlank()) {
                val downloaded = onlineRepo.downloadImageBytes(option.url)
                if (downloaded != null) {
                    finalOption = option.copy(bytes = downloaded)
                }
            }

            _editorState.update { state ->
                val draft = state.draftTrack
                val updatedDraft = if (finalOption.bytes != null) {
                    draft?.copy(
                        coverArtBytes = finalOption.bytes,
                        hasCover = true
                    )
                } else draft

                state.copy(
                    selectedCoverOption = finalOption,
                    draftTrack = updatedDraft,
                    showCoverPickerSheet = false,
                    coverArtOptions = state.coverArtOptions.map {
                        it.copy(isSelected = it.id == option.id)
                    }
                )
            }
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
            val draft = state.draftTrack?.copy(
                coverArtBytes = bytes,
                hasCover = true
            )
            state.copy(
                draftTrack = draft,
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

        val coverChanged = when {
            selectedCover?.bytes != null && selectedCover.bytes != original.coverArtBytes -> true
            selectedCover?.url != null -> true
            else -> false
        }

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
                isChanged = coverChanged
            ),
            TagDiff(
                "Letra de Canción",
                if (original.hasLyrics) "${original.lyrics.lines().size} versos" else "Sin letra",
                if (draft.hasLyrics) "${draft.lyrics.lines().size} versos" else "Sin letra",
                isChanged = original.lyrics.trim() != draft.lyrics.trim()
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
        val saveLrc = _editorState.value.saveLrcFile
        val embedLyrics = _editorState.value.embedLyrics

        viewModelScope.launch(Dispatchers.IO) {
            _editorState.update { it.copy(isSaving = true) }

            // Ensure cover bytes are ready
            val coverBytes: ByteArray? = when {
                coverOption?.bytes != null -> coverOption.bytes
                coverOption?.url != null -> onlineRepo.downloadImageBytes(coverOption.url)
                draft.coverArtBytes != null -> draft.coverArtBytes
                else -> original.coverArtBytes
            }

            val cleanLyrics = draft.lyrics.trim()
            val hasValidLyrics = cleanLyrics.isNotBlank() && !cleanLyrics.equals("null", ignoreCase = true)
            val lyricsToEmbed = if (embedLyrics && hasValidLyrics) cleanLyrics else if (embedLyrics && draft.lyrics.isBlank()) "" else null

            // 1. Write tags directly to original file via ContentResolver SAF stream
            val success = AudioTagWriter.writeTagsDirectly(
                context = getApplication(),
                track = original,
                updatedTrack = draft,
                coverBytes = coverBytes,
                lyricsText = lyricsToEmbed
            )

            // 2. Save .lrc file in folder if requested and valid lyrics exist
            if (saveLrc && hasValidLyrics) {
                val lrcSaved = folderManager.saveLrcFile(original, cleanLyrics)
                if (lrcSaved) {
                    Log.i("MusicViewModel", "Saved .lrc companion file for ${original.fileName}")
                }
            }

            if (success) {
                val updatedTrack = draft.copy(
                    hasCover = coverBytes != null && coverBytes.isNotEmpty(),
                    coverArtBytes = coverBytes,
                    hasLyrics = hasValidLyrics,
                    dateModified = System.currentTimeMillis()
                )

                // Invalidate cached thumbnail for this track
                AudioThumbnailLoader.invalidate(original.fileUri)

                // 3. Atomically update ONLY this track in memory and in cache (NO FULL RESCAN!)
                folderManager.updateSingleTrackInCache(updatedTrack)

                _libraryState.update { state ->
                    val updatedList = state.tracks.map {
                        if (it.id == original.id || it.fileUri == original.fileUri) updatedTrack else it
                    }
                    state.copy(
                        tracks = updatedList,
                        filteredTracks = applyFilterSearchAndSort(
                            updatedList,
                            state.activeFilter,
                            state.searchQuery,
                            state.sortOrder
                        ),
                        statusMessage = "¡Guardado exitosamente en ${original.fileName}!"
                    )
                }

                _editorState.update {
                    it.copy(
                        isSaving = false,
                        showDiffDialog = false,
                        originalTrack = updatedTrack,
                        draftTrack = updatedTrack.copy(),
                        saveNotification = "¡Archivo actualizado con éxito en tu carpeta!"
                    )
                }
            } else {
                _editorState.update {
                    it.copy(
                        isSaving = false,
                        showDiffDialog = false,
                        saveNotification = "Error: no se pudo escribir directamente en el archivo."
                    )
                }
            }
        }
    }

    fun dismissSaveNotification() {
        _editorState.update { it.copy(saveNotification = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
