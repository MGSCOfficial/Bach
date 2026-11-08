package com.example.data.tagger

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class QuickMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val hasCover: Boolean = false,
    val hasLyrics: Boolean = false,
    val durationMs: Long = 0L
)

object FastTagExtractor {

    /**
     * Reads metadata and presence of cover/lyrics in under 1 millisecond
     * by parsing only the initial header bytes directly from stream.
     */
    fun extractQuickMetadata(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        lastModified: Long
    ): AudioTrack {
        val format = AudioFormat.fromFileName(fileName)
        var quickMeta: QuickMetadata? = null

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                quickMeta = when (format) {
                    AudioFormat.MP3 -> parseMp3Header(stream)
                    AudioFormat.M4A -> parseM4aHeader(stream)
                }
            }
        } catch (e: Exception) {
            Log.w("FastTagExtractor", "Quick parse error for $fileName, using fallback", e)
        }

        val meta = quickMeta ?: fallbackFromName(fileName)

        val finalTitle = meta.title.ifBlank {
            cleanFileNameToTitle(fileName)
        }

        return AudioTrack(
            fileUri = uri.toString(),
            fileName = fileName,
            format = format,
            title = finalTitle,
            artist = meta.artist,
            album = meta.album,
            year = meta.year,
            genre = meta.genre,
            trackNumber = meta.trackNumber,
            durationMs = meta.durationMs,
            sizeBytes = fileSize,
            hasCover = meta.hasCover,
            coverArtBytes = null, // Loaded on-demand when edited to keep RAM lean
            hasLyrics = meta.hasLyrics,
            dateModified = if (lastModified > 0) lastModified else System.currentTimeMillis()
        )
    }

    private fun parseMp3Header(stream: InputStream): QuickMetadata {
        val header = ByteArray(10)
        val read = stream.read(header)
        if (read < 10 || header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return QuickMetadata()
        }

        val version = header[3].toInt() and 0xFF
        val size = ((header[6].toInt() and 0x7F) shl 21) or
                ((header[7].toInt() and 0x7F) shl 14) or
                ((header[8].toInt() and 0x7F) shl 7) or
                (header[9].toInt() and 0x7F)

        if (size <= 0) return QuickMetadata()

        // Read at most 64KB of frames for ultra-fast header extraction
        val maxToRead = minOf(size, 65536)
        val frameBuffer = ByteArray(maxToRead)
        var totalRead = 0
        while (totalRead < maxToRead) {
            val r = stream.read(frameBuffer, totalRead, maxToRead - totalRead)
            if (r <= 0) break
            totalRead += r
        }

        var offset = 0
        var title = ""
        var artist = ""
        var album = ""
        var year = ""
        var genre = ""
        var trackNumber = ""
        var hasCover = false
        var hasLyrics = false

        while (offset + 10 <= totalRead) {
            val frameId = String(frameBuffer, offset, 4, Charsets.US_ASCII)
            if (frameId.all { it == '\u0000' } || !frameId.all { it in 'A'..'Z' || it in '0'..'9' }) {
                break
            }

            val frameSize = if (version >= 4) {
                // ID3v2.4 syncsafe integer
                ((frameBuffer[offset + 4].toInt() and 0x7F) shl 21) or
                        ((frameBuffer[offset + 5].toInt() and 0x7F) shl 14) or
                        ((frameBuffer[offset + 6].toInt() and 0x7F) shl 7) or
                        (frameBuffer[offset + 7].toInt() and 0x7F)
            } else {
                // ID3v2.3 32-bit int
                ByteBuffer.wrap(frameBuffer, offset + 4, 4).order(ByteOrder.BIG_ENDIAN).int
            }

            if (frameSize <= 0 || offset + 10 + frameSize > totalRead) {
                // Check if this frame is APIC
                if (frameId == "APIC" || frameId == "PIC") hasCover = true
                if (frameId == "USLT" || frameId == "SYLT") hasLyrics = true
                break
            }

            val dataOffset = offset + 10
            when (frameId) {
                "TIT2", "TT2" -> title = decodeText(frameBuffer, dataOffset, frameSize)
                "TPE1", "TP1" -> artist = decodeText(frameBuffer, dataOffset, frameSize)
                "TALB", "TAL" -> album = decodeText(frameBuffer, dataOffset, frameSize)
                "TYER", "TDRC" -> year = decodeText(frameBuffer, dataOffset, frameSize).take(4)
                "TCON", "TCO" -> genre = decodeText(frameBuffer, dataOffset, frameSize)
                "TRCK", "TRK" -> trackNumber = decodeText(frameBuffer, dataOffset, frameSize)
                "APIC", "PIC" -> hasCover = true
                "USLT", "SYLT" -> hasLyrics = true
            }

            offset += 10 + frameSize
        }

        return QuickMetadata(
            title = title,
            artist = artist,
            album = album,
            year = year,
            genre = genre,
            trackNumber = trackNumber,
            hasCover = hasCover,
            hasLyrics = hasLyrics
        )
    }

    private fun parseM4aHeader(stream: InputStream): QuickMetadata {
        // Read first 128KB which typically contains ftyp and moov with udta/ilst
        val buffer = ByteArray(131072)
        var totalRead = 0
        while (totalRead < buffer.size) {
            val r = stream.read(buffer, totalRead, buffer.size - totalRead)
            if (r <= 0) break
            totalRead += r
        }

        val content = String(buffer, 0, totalRead, Charsets.ISO_8859_1)
        val hasCover = content.contains("covr")
        val hasLyrics = content.contains("\u00a9lyr")

        var title = ""
        var artist = ""
        var album = ""
        var year = ""
        var genre = ""

        fun findAtomText(atom: String): String {
            val idx = content.indexOf(atom)
            if (idx != -1 && idx + 24 < totalRead) {
                val dataIdx = content.indexOf("data", idx)
                if (dataIdx != -1 && dataIdx + 12 < totalRead) {
                    val len = ByteBuffer.wrap(buffer, dataIdx - 4, 4).order(ByteOrder.BIG_ENDIAN).int - 16
                    if (len in 1..256 && dataIdx + 12 + len <= totalRead) {
                        return String(buffer, dataIdx + 12, len, Charsets.UTF_8).trim()
                    }
                }
            }
            return ""
        }

        title = findAtomText("\u00a9nam")
        artist = findAtomText("\u00a9ART")
        album = findAtomText("\u00a9alb")
        year = findAtomText("\u00a9day").take(4)
        genre = findAtomText("\u00a9gen")

        return QuickMetadata(
            title = title,
            artist = artist,
            album = album,
            year = year,
            genre = genre,
            hasCover = hasCover,
            hasLyrics = hasLyrics
        )
    }

    private fun decodeText(buffer: ByteArray, offset: Int, length: Int): String {
        if (length <= 1) return ""
        val encoding = buffer[offset].toInt()
        val textBytes = buffer.copyOfRange(offset + 1, offset + length)
        return try {
            when (encoding) {
                0x00 -> String(textBytes, Charsets.ISO_8859_1).trim { it <= ' ' || it == '\u0000' }
                0x01, 0x02 -> String(textBytes, Charsets.UTF_16).trim { it <= ' ' || it == '\u0000' }
                0x03 -> String(textBytes, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
                else -> String(textBytes, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun fallbackFromName(fileName: String): QuickMetadata {
        val base = cleanFileNameToTitle(fileName)
        return QuickMetadata(title = base)
    }

    private fun cleanFileNameToTitle(fileName: String): String {
        var base = fileName.substringBeforeLast(".")
        // Remove track numbers like "01. ", "01 - ", "[01] "
        base = base.replace(Regex("^\\s*\\d{1,3}\\s*[.\\-_]\\s*"), "")
        base = base.replace(Regex("^\\s*\\[\\d{1,3}\\]\\s*"), "")
        // If "Artist - Title", split and take Title
        if (base.contains(" - ")) {
            val parts = base.split(" - ")
            if (parts.size >= 2) {
                return parts[1].trim()
            }
        }
        return base.trim()
    }
}
