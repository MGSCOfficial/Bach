package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.MusicFolder
import com.example.data.tagger.FastTagExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter

class FolderManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sound_id_folders", Context.MODE_PRIVATE)
    private val keyFolders = "saved_tree_uris"
    private val cacheFile = File(context.filesDir, "sound_id_library_cache.json")

    fun getSavedFolderUris(): List<String> {
        val set = prefs.getStringSet(keyFolders, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun addFolderUri(treeUri: Uri): MusicFolder? {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (e: Exception) {
            Log.w("FolderManager", "Could not take persistable permission on $treeUri", e)
        }

        val treeDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val folderName = treeDoc.name ?: "Carpeta de Música"
        val uriStr = treeUri.toString()

        val set = prefs.getStringSet(keyFolders, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(uriStr)
        prefs.edit().putStringSet(keyFolders, set).apply()

        return MusicFolder(uriString = uriStr, displayName = folderName)
    }

    fun removeFolder(uriString: String) {
        val set = prefs.getStringSet(keyFolders, emptySet())?.toMutableSet() ?: return
        set.remove(uriString)
        prefs.edit().putStringSet(keyFolders, set).apply()
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(Uri.parse(uriString), flags)
        } catch (ignored: Exception) {}
    }

    // ==================== FAST CACHE ====================

    fun loadCachedTracks(): Pair<List<MusicFolder>, List<AudioTrack>> {
        val savedUris = getSavedFolderUris()
        if (!cacheFile.exists() || savedUris.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        return try {
            val jsonStr = cacheFile.readText()
            val root = JSONObject(jsonStr)

            val foldersArr = root.optJSONArray("folders") ?: JSONArray()
            val foldersList = mutableListOf<MusicFolder>()
            for (i in 0 until foldersArr.length()) {
                val fObj = foldersArr.getJSONObject(i)
                foldersList.add(
                    MusicFolder(
                        uriString = fObj.getString("uri"),
                        displayName = fObj.getString("name"),
                        trackCount = fObj.optInt("count", 0)
                    )
                )
            }

            val tracksArr = root.optJSONArray("tracks") ?: JSONArray()
            val tracksList = mutableListOf<AudioTrack>()
            for (i in 0 until tracksArr.length()) {
                val tObj = tracksArr.getJSONObject(i)
                val formatStr = tObj.optString("format", "MP3")
                tracksList.add(
                    AudioTrack(
                        id = tObj.optString("id"),
                        fileUri = tObj.getString("uri"),
                        fileName = tObj.getString("name"),
                        format = if (formatStr == "M4A") AudioFormat.M4A else AudioFormat.MP3,
                        title = tObj.optString("title"),
                        artist = tObj.optString("artist"),
                        album = tObj.optString("album"),
                        albumArtist = tObj.optString("albumArtist"),
                        year = tObj.optString("year"),
                        genre = tObj.optString("genre"),
                        trackNumber = tObj.optString("trackNumber"),
                        discNumber = tObj.optString("discNumber"),
                        comment = tObj.optString("comment"),
                        lyrics = tObj.optString("lyrics"),
                        durationMs = tObj.optLong("duration", 0L),
                        sizeBytes = tObj.optLong("size", 0L),
                        hasCover = tObj.optBoolean("hasCover", false),
                        hasLyrics = tObj.optBoolean("hasLyrics", false),
                        parentFolderUri = tObj.optString("parentFolder", null),
                        treeUriString = tObj.optString("treeUri", null),
                        dateModified = tObj.optLong("modified", System.currentTimeMillis())
                    )
                )
            }

            Pair(foldersList, tracksList)
        } catch (e: Exception) {
            Log.e("FolderManager", "Failed to read cached tracks", e)
            Pair(emptyList(), emptyList())
        }
    }

    fun saveTracksToCache(folders: List<MusicFolder>, tracks: List<AudioTrack>) {
        try {
            val root = JSONObject()

            val foldersArr = JSONArray()
            for (f in folders) {
                val fObj = JSONObject()
                fObj.put("uri", f.uriString)
                fObj.put("name", f.displayName)
                fObj.put("count", f.trackCount)
                foldersArr.put(fObj)
            }
            root.put("folders", foldersArr)

            val tracksArr = JSONArray()
            for (t in tracks) {
                val tObj = JSONObject()
                tObj.put("id", t.id)
                tObj.put("uri", t.fileUri)
                tObj.put("name", t.fileName)
                tObj.put("format", t.format.name)
                tObj.put("title", t.title)
                tObj.put("artist", t.artist)
                tObj.put("album", t.album)
                tObj.put("albumArtist", t.albumArtist)
                tObj.put("year", t.year)
                tObj.put("genre", t.genre)
                tObj.put("trackNumber", t.trackNumber)
                tObj.put("discNumber", t.discNumber)
                tObj.put("comment", t.comment)
                tObj.put("lyrics", t.lyrics)
                tObj.put("duration", t.durationMs)
                tObj.put("size", t.sizeBytes)
                tObj.put("hasCover", t.hasCover)
                tObj.put("hasLyrics", t.hasLyrics)
                tObj.put("parentFolder", t.parentFolderUri)
                tObj.put("treeUri", t.treeUriString)
                tObj.put("modified", t.dateModified)
                tracksArr.put(tObj)
            }
            root.put("tracks", tracksArr)

            cacheFile.writeText(root.toString())
        } catch (e: Exception) {
            Log.e("FolderManager", "Failed to write tracks cache", e)
        }
    }

    fun updateSingleTrackInCache(updatedTrack: AudioTrack) {
        val (folders, cachedTracks) = loadCachedTracks()
        if (cachedTracks.isEmpty()) return
        val newTracks = cachedTracks.map {
            if (it.id == updatedTrack.id || it.fileUri == updatedTrack.fileUri) updatedTrack else it
        }
        saveTracksToCache(folders, newTracks)
    }

    // ==================== ULTRA-FAST BATCH SCANNER ====================

    suspend fun scanFolders(): Pair<List<MusicFolder>, List<AudioTrack>> = withContext(Dispatchers.IO) {
        val folderUris = getSavedFolderUris()
        val foldersList = mutableListOf<MusicFolder>()
        val allTracks = mutableListOf<AudioTrack>()

        for (uriStr in folderUris) {
            try {
                val treeUri = Uri.parse(uriStr)
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                val folderName = treeDoc?.name ?: "Carpeta de Música"

                val tracksInFolder = mutableListOf<AudioTrack>()
                scanDirectoryFast(treeUri, treeDocId, uriStr, tracksInFolder)

                foldersList.add(
                    MusicFolder(
                        uriString = uriStr,
                        displayName = folderName,
                        trackCount = tracksInFolder.size
                    )
                )
                allTracks.addAll(tracksInFolder)
            } catch (e: Exception) {
                Log.e("FolderManager", "Error fast-scanning folder: $uriStr", e)
            }
        }

        // Save to cache so next loads take 0 ms
        saveTracksToCache(foldersList, allTracks)

        return@withContext Pair(foldersList, allTracks)
    }

    private fun scanDirectoryFast(
        treeUri: Uri,
        parentDocumentId: String,
        treeUriString: String,
        outTracks: MutableList<AudioTrack>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val subDirs = mutableListOf<String>()
        val audioFiles = mutableListOf<AudioFileInfo>()
        val lrcNames = mutableSetOf<String>()

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = cursor.getLong(sizeIdx)
                    val lastMod = cursor.getLong(modIdx)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subDirs.add(docId)
                    } else {
                        val lower = name.lowercase()
                        if (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".mp4") || lower.endsWith(".aac")) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            audioFiles.add(AudioFileInfo(fileUri, docId, name, size, lastMod))
                        } else if (lower.endsWith(".lrc")) {
                            lrcNames.add(name.substringBeforeLast(".").lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FolderManager", "Cursor query failed for $parentDocumentId", e)
            return
        }

        // Parse audio files using microsecond FastTagExtractor
        for (info in audioFiles) {
            try {
                val track = FastTagExtractor.extractQuickMetadata(
                    context = context,
                    uri = info.uri,
                    fileName = info.name,
                    fileSize = info.size,
                    lastModified = info.lastModified
                )

                val base = info.name.substringBeforeLast(".").lowercase()
                val hasLrcFile = lrcNames.contains(base)

                val parentFolderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId).toString()

                val enriched = track.copy(
                    hasLyrics = track.hasLyrics || hasLrcFile,
                    parentFolderUri = parentFolderUri,
                    treeUriString = treeUriString
                )
                outTracks.add(enriched)
            } catch (e: Exception) {
                Log.w("FolderManager", "Failed quick meta for ${info.name}", e)
            }
        }

        // Recurse into subdirectories
        for (subDirId in subDirs) {
            scanDirectoryFast(treeUri, subDirId, treeUriString, outTracks)
        }
    }

    fun saveLrcFile(track: AudioTrack, lyricsText: String): Boolean {
        if (lyricsText.isBlank()) return false
        val parentUriStr = track.parentFolderUri ?: return false

        try {
            val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentUriStr))
                ?: DocumentFile.fromSingleUri(context, Uri.parse(parentUriStr))
                ?: return false

            val baseName = track.fileName.substringBeforeLast(".")
            val lrcFileName = "$baseName.lrc"

            var lrcDoc = parentDoc.findFile(lrcFileName)
            if (lrcDoc == null) {
                lrcDoc = parentDoc.createFile("text/plain", lrcFileName)
            }

            if (lrcDoc != null) {
                context.contentResolver.openOutputStream(lrcDoc.uri, "wt")?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(lyricsText)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            Log.e("FolderManager", "Failed to save .lrc file for ${track.fileName}", e)
        }
        return false
    }

    private data class AudioFileInfo(
        val uri: Uri,
        val docId: String,
        val name: String,
        val size: Long,
        val lastModified: Long
    )
}
