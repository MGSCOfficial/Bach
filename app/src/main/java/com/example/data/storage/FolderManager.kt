package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.MusicFolder
import com.example.data.tagger.AudioTagReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class FolderManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sound_id_folders", Context.MODE_PRIVATE)
    private val keyFolders = "saved_tree_uris"

    fun getSavedFolderUris(): List<String> {
        val set = prefs.getStringSet(keyFolders, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun addFolderUri(treeUri: Uri): MusicFolder? {
        try {
            // Take permanent persistable permissions
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

    suspend fun scanFolders(): Pair<List<MusicFolder>, List<AudioTrack>> = withContext(Dispatchers.IO) {
        val folderUris = getSavedFolderUris()
        val foldersList = mutableListOf<MusicFolder>()
        val allTracks = mutableListOf<AudioTrack>()

        for (uriStr in folderUris) {
            try {
                val uri = Uri.parse(uriStr)
                val treeDoc = DocumentFile.fromTreeUri(context, uri) ?: continue
                val folderName = treeDoc.name ?: "Carpeta"

                val tracksInFolder = mutableListOf<AudioTrack>()
                traverseDirectory(treeDoc, uriStr, tracksInFolder)

                foldersList.add(
                    MusicFolder(
                        uriString = uriStr,
                        displayName = folderName,
                        trackCount = tracksInFolder.size
                    )
                )
                allTracks.addAll(tracksInFolder)
            } catch (e: Exception) {
                Log.e("FolderManager", "Error scanning folder: $uriStr", e)
            }
        }

        return@withContext Pair(foldersList, allTracks)
    }

    private fun traverseDirectory(
        dir: DocumentFile,
        treeUriString: String,
        outTracks: MutableList<AudioTrack>
    ) {
        val files = dir.listFiles()
        // Index all .lrc files in this directory for quick lookup
        val lrcFilesMap = mutableMapOf<String, DocumentFile>()
        for (f in files) {
            val name = f.name ?: continue
            if (name.lowercase().endsWith(".lrc")) {
                val base = name.substringBeforeLast(".").lowercase()
                lrcFilesMap[base] = f
            }
        }

        for (file in files) {
            if (file.isDirectory) {
                traverseDirectory(file, treeUriString, outTracks)
            } else {
                val name = file.name ?: continue
                val lower = name.lowercase()
                if (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".mp4") || lower.endsWith(".aac")) {
                    val track = AudioTagReader.readTrack(context, file.uri, name, file.length())
                    if (track != null) {
                        // Check if companion .lrc file exists
                        val baseName = name.substringBeforeLast(".").lowercase()
                        val lrcDoc = lrcFilesMap[baseName]
                        var lyricsText = track.lyrics
                        if (lyricsText.isBlank() && lrcDoc != null) {
                            try {
                                context.contentResolver.openInputStream(lrcDoc.uri)?.use { stream ->
                                    lyricsText = stream.bufferedReader().readText()
                                }
                            } catch (e: Exception) {
                                Log.w("FolderManager", "Error reading lrc for $name", e)
                            }
                        }

                        val enriched = track.copy(
                            lyrics = lyricsText,
                            hasLyrics = lyricsText.isNotBlank(),
                            parentFolderUri = dir.uri.toString(),
                            treeUriString = treeUriString
                        )
                        outTracks.add(enriched)
                    }
                }
            }
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

            // Look for existing .lrc or create new
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
}
