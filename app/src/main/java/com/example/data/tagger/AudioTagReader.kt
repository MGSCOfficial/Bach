package com.example.data.tagger

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import java.io.File

object AudioTagReader {

    fun loadEmbeddedPicture(context: Context, uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (e: Exception) {
            Log.w("AudioTagReader", "Could not load embedded picture for $uri", e)
            null
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }
    }

    fun readTrack(context: Context, uri: Uri, fileName: String, sizeBytes: Long): AudioTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().trim()
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty().trim()
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE).orEmpty().trim()
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty().trim()
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).orEmpty().trim()
            val discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).orEmpty().trim()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            val coverBytes = retriever.embeddedPicture
            val format = AudioFormat.fromFileName(fileName)

            AudioTrack(
                fileUri = uri.toString(),
                filePath = if (uri.scheme == "file") uri.path else null,
                fileName = fileName,
                format = format,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                year = year,
                genre = genre,
                trackNumber = trackNumber,
                discNumber = discNumber,
                durationMs = durationMs,
                sizeBytes = sizeBytes,
                hasCover = coverBytes != null && coverBytes.isNotEmpty(),
                coverArtBytes = coverBytes
            )
        } catch (e: Exception) {
            Log.e("AudioTagReader", "Error reading metadata for $fileName", e)
            val format = AudioFormat.fromFileName(fileName)
            AudioTrack(
                fileUri = uri.toString(),
                fileName = fileName,
                format = format,
                title = fileName.substringBeforeLast("."),
                sizeBytes = sizeBytes
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun readFromFile(file: File): AudioTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().trim()
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty().trim()
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE).orEmpty().trim()
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty().trim()
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).orEmpty().trim()
            val discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).orEmpty().trim()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val coverBytes = retriever.embeddedPicture
            val format = AudioFormat.fromFileName(file.name)

            AudioTrack(
                fileUri = Uri.fromFile(file).toString(),
                filePath = file.absolutePath,
                fileName = file.name,
                format = format,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                year = year,
                genre = genre,
                trackNumber = trackNumber,
                discNumber = discNumber,
                durationMs = durationMs,
                sizeBytes = file.length(),
                hasCover = coverBytes != null && coverBytes.isNotEmpty(),
                coverArtBytes = coverBytes
            )
        } catch (e: Exception) {
            Log.e("AudioTagReader", "Error reading file ${file.name}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }
}
