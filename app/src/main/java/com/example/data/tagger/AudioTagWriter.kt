package com.example.data.tagger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioTagWriter {

    /**
     * Resizes cover art image bytes to a bounding box targetDimensionPx while preserving aspect ratio.
     */
    fun resizeCoverArt(bytes: ByteArray, targetDimensionPx: Int, quality: Int = 90): ByteArray {
        if (targetDimensionPx <= 0 || bytes.isEmpty()) return bytes
        return try {
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val width = originalBitmap.width
            val height = originalBitmap.height
            if (width <= targetDimensionPx && height <= targetDimensionPx) {
                return bytes
            }

            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1f) {
                targetDimensionPx to (targetDimensionPx / ratio).toInt().coerceAtLeast(1)
            } else {
                (targetDimensionPx * ratio).toInt().coerceAtLeast(1) to targetDimensionPx
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.w("AudioTagWriter", "Failed to resize cover art", e)
            bytes
        }
    }

    /**
     * Writes updated ID3v2 tags or MP4 atoms directly to the actual audio file on storage
     * using the ContentResolver stream for SAF documents or standard File for local paths.
     */
    fun writeTagsDirectly(
        context: Context,
        track: AudioTrack,
        updatedTrack: AudioTrack,
        coverBytes: ByteArray?,
        lyricsText: String? = null,
        targetCoverDimensionPx: Int = 0
    ): Boolean {
        return try {
            val uri = Uri.parse(track.fileUri)

            val processedCoverBytes = if (coverBytes != null && targetCoverDimensionPx > 0) {
                resizeCoverArt(coverBytes, targetCoverDimensionPx)
            } else {
                coverBytes
            }

            // Read original source bytes
            val sourceBytes: ByteArray = if (uri.scheme == "file") {
                File(uri.path ?: "").readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("No se pudo leer el archivo: ${track.fileUri}")
            }

            // Encode tags
            val finalBytes = when (updatedTrack.format) {
                AudioFormat.MP3 -> writeMp3Tags(sourceBytes, updatedTrack, processedCoverBytes, lyricsText)
                AudioFormat.M4A -> writeM4aTags(sourceBytes, updatedTrack, processedCoverBytes, lyricsText)
            }

            // Write directly to user's real file
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                FileOutputStream(f).use { it.write(finalBytes) }
            } else {
                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(finalBytes)
                    stream.flush()
                } ?: throw IllegalStateException("No se pudo abrir flujo de escritura para $uri")
            }

            Log.i("AudioTagWriter", "Successfully wrote ${finalBytes.size} bytes directly to ${track.fileName}")
            true
        } catch (e: Exception) {
            Log.e("AudioTagWriter", "Failed to write tags directly to ${track.fileName}", e)
            false
        }
    }

    // ==================== MP3 ID3v2.3 WRITER ====================

    fun writeMp3Tags(
        originalBytes: ByteArray,
        track: AudioTrack,
        coverBytes: ByteArray?,
        lyricsText: String? = null
    ): ByteArray {
        // 1. Identify and skip old ID3v2 tag if present
        var audioStartOffset = 0
        if (originalBytes.size >= 10 &&
            originalBytes[0] == 'I'.code.toByte() &&
            originalBytes[1] == 'D'.code.toByte() &&
            originalBytes[2] == '3'.code.toByte()
        ) {
            val size = ((originalBytes[6].toInt() and 0x7F) shl 21) or
                    ((originalBytes[7].toInt() and 0x7F) shl 14) or
                    ((originalBytes[8].toInt() and 0x7F) shl 7) or
                    (originalBytes[9].toInt() and 0x7F)
            audioStartOffset = 10 + size
            if (audioStartOffset > originalBytes.size) {
                audioStartOffset = 0
            }
        }

        // 2. Build ID3v2.3 frames
        val framesStream = ByteArrayOutputStream()

        fun writeTextFrame(frameId: String, text: String) {
            if (text.isBlank()) return
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val frameData = ByteArray(1 + textBytes.size)
            frameData[0] = 0x03 // UTF-8 encoding
            System.arraycopy(textBytes, 0, frameData, 1, textBytes.size)

            framesStream.write(frameId.toByteArray(Charsets.US_ASCII))
            val sizeBuf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(frameData.size).array()
            framesStream.write(sizeBuf)
            framesStream.write(byteArrayOf(0x00, 0x00)) // flags
            framesStream.write(frameData)
        }

        writeTextFrame("TIT2", track.title)
        writeTextFrame("TPE1", track.artist)
        writeTextFrame("TALB", track.album)
        writeTextFrame("TPE2", track.albumArtist.ifBlank { track.artist })
        writeTextFrame("TYER", track.year)
        writeTextFrame("TCON", track.genre)
        writeTextFrame("TRCK", track.trackNumber)
        writeTextFrame("TPOS", track.discNumber)
        if (track.comment.isNotBlank()) {
            val commData = ByteArrayOutputStream()
            commData.write(0x03) // UTF-8
            commData.write("eng".toByteArray(Charsets.US_ASCII))
            commData.write(0x00) // empty short description
            commData.write(track.comment.toByteArray(Charsets.UTF_8))
            val cBytes = commData.toByteArray()
            framesStream.write("COMM".toByteArray(Charsets.US_ASCII))
            framesStream.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(cBytes.size).array())
            framesStream.write(byteArrayOf(0x00, 0x00))
            framesStream.write(cBytes)
        }

        // Embedded Lyrics (USLT frame in ID3v2.3)
        // If lyricsText is null, do not write any USLT frame (respects embedLyrics=false or stripped lyrics)
        val lyrics = lyricsText?.trim() ?: ""
        if (lyrics.isNotBlank() && !lyrics.equals("null", ignoreCase = true)) {
            val usltData = ByteArrayOutputStream()
            usltData.write(0x03) // UTF-8 encoding
            usltData.write("eng".toByteArray(Charsets.US_ASCII)) // language: 3 bytes
            usltData.write(0x00) // null-terminated descriptor
            usltData.write(lyrics.toByteArray(Charsets.UTF_8))
            val uBytes = usltData.toByteArray()
            framesStream.write("USLT".toByteArray(Charsets.US_ASCII))
            framesStream.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(uBytes.size).array())
            framesStream.write(byteArrayOf(0x00, 0x00)) // flags
            framesStream.write(uBytes)
        }

        // APIC (Attached picture / Cover art)
        val picBytes = coverBytes ?: track.coverArtBytes
        if (picBytes != null && picBytes.isNotEmpty()) {
            val isPng = picBytes.size > 8 &&
                    picBytes[0] == 0x89.toByte() &&
                    picBytes[1] == 0x50.toByte() &&
                    picBytes[2] == 0x4E.toByte() &&
                    picBytes[3] == 0x47.toByte()
            val mime = if (isPng) "image/png" else "image/jpeg"

            val apicStream = ByteArrayOutputStream()
            apicStream.write(0x03) // UTF-8 encoding
            apicStream.write(mime.toByteArray(Charsets.US_ASCII))
            apicStream.write(0x00) // null terminator for MIME
            apicStream.write(0x03) // Picture type: 3 = Cover (front)
            apicStream.write(0x00) // Empty description + null terminator
            apicStream.write(picBytes)

            val apicBytes = apicStream.toByteArray()
            framesStream.write("APIC".toByteArray(Charsets.US_ASCII))
            framesStream.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(apicBytes.size).array())
            framesStream.write(byteArrayOf(0x00, 0x00)) // flags
            framesStream.write(apicBytes)
        }

        val allFrames = framesStream.toByteArray()

        // 3. Build ID3v2.3 header with syncsafe size
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 0x03 // version 2.3
        header[4] = 0x00
        header[5] = 0x00 // flags
        // syncsafe size (4 bytes: 7 bits each)
        val frameSize = allFrames.size
        header[6] = ((frameSize shr 21) and 0x7F).toByte()
        header[7] = ((frameSize shr 14) and 0x7F).toByte()
        header[8] = ((frameSize shr 7) and 0x7F).toByte()
        header[9] = (frameSize and 0x7F).toByte()

        // 4. Combine new ID3v2 tag + raw audio stream
        val audioLength = originalBytes.size - audioStartOffset
        val result = ByteArray(10 + allFrames.size + audioLength)
        System.arraycopy(header, 0, result, 0, 10)
        System.arraycopy(allFrames, 0, result, 10, allFrames.size)
        System.arraycopy(originalBytes, audioStartOffset, result, 10 + allFrames.size, audioLength)

        return result
    }

    // ==================== M4A / MP4 ATOM WRITER ====================

    fun writeM4aTags(
        originalBytes: ByteArray,
        track: AudioTrack,
        coverBytes: ByteArray?,
        lyricsText: String? = null
    ): ByteArray {
        val newUdta = buildM4aUdtaAtom(track, coverBytes ?: track.coverArtBytes, lyricsText?.trim() ?: "")
        val (moovStart, moovLength) = findTopLevelAtom(originalBytes, "moov")

        if (moovStart == -1 || moovLength <= 8) {
            return originalBytes
        }

        val oldMoovBytes = originalBytes.copyOfRange(moovStart, moovStart + moovLength)
        val newMoovBytes = injectUdtaIntoMoov(oldMoovBytes, newUdta)
        val delta = newMoovBytes.size - oldMoovBytes.size

        // If moov is before mdat, update chunk offsets in stco
        val (mdatStart, _) = findTopLevelAtom(originalBytes, "mdat")
        val finalMoovBytes = if (delta != 0 && mdatStart > moovStart) {
            adjustChunkOffsets(newMoovBytes, delta)
        } else {
            newMoovBytes
        }

        // Reconstruct file: [before moov] + [finalMoovBytes] + [after moov]
        val out = ByteArrayOutputStream(originalBytes.size + delta)
        out.write(originalBytes, 0, moovStart)
        out.write(finalMoovBytes)
        val afterMoovStart = moovStart + moovLength
        if (afterMoovStart < originalBytes.size) {
            out.write(originalBytes, afterMoovStart, originalBytes.size - afterMoovStart)
        }
        return out.toByteArray()
    }

    private fun findTopLevelAtom(bytes: ByteArray, atomName: String): Pair<Int, Int> {
        var offset = 0
        while (offset + 8 <= bytes.size) {
            val size = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int
            val name = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            val actualSize = if (size == 1 && offset + 16 <= bytes.size) {
                ByteBuffer.wrap(bytes, offset + 8, 8).order(ByteOrder.BIG_ENDIAN).long.toInt()
            } else if (size == 0) {
                bytes.size - offset
            } else {
                size
            }

            if (name == atomName) {
                return Pair(offset, actualSize)
            }
            if (actualSize <= 0) break
            offset += actualSize
        }
        return Pair(-1, 0)
    }

    private fun buildM4aUdtaAtom(
        track: AudioTrack,
        coverBytes: ByteArray?,
        lyrics: String
    ): ByteArray {
        val ilstStream = ByteArrayOutputStream()

        fun writeTextItem(fourCc: String, value: String) {
            if (value.isBlank()) return
            val valBytes = value.toByteArray(Charsets.UTF_8)
            val dataAtomLen = 16 + valBytes.size

            val itemLen = 8 + dataAtomLen
            val buf = ByteBuffer.allocate(itemLen).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(itemLen)
            buf.put(fourCc.toByteArray(Charsets.ISO_8859_1))

            // data atom
            buf.putInt(dataAtomLen)
            buf.put("data".toByteArray(Charsets.US_ASCII))
            buf.putInt(0x00000001) // UTF-8 text type
            buf.putInt(0x00000000) // locale
            buf.put(valBytes)

            ilstStream.write(buf.array())
        }

        fun writeInt16Item(fourCc: String, index: Int, total: Int) {
            if (index <= 0) return
            val itemLen = 8 + 24
            val buf = ByteBuffer.allocate(itemLen).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(itemLen)
            buf.put(fourCc.toByteArray(Charsets.ISO_8859_1))

            // data atom
            buf.putInt(24)
            buf.put("data".toByteArray(Charsets.US_ASCII))
            buf.putInt(0x00000000) // raw int
            buf.putInt(0x00000000)
            buf.putShort(0)
            buf.putShort(index.toShort())
            buf.putShort(total.toShort())
            buf.putShort(0)

            ilstStream.write(buf.array())
        }

        writeTextItem("\u00a9nam", track.title)
        writeTextItem("\u00a9ART", track.artist)
        writeTextItem("\u00a9alb", track.album)
        writeTextItem("aART", track.albumArtist.ifBlank { track.artist })
        writeTextItem("\u00a9day", track.year)
        writeTextItem("\u00a9gen", track.genre)
        track.trackNumber.toIntOrNull()?.let { writeInt16Item("trkn", it, 0) }
        track.discNumber.toIntOrNull()?.let { writeInt16Item("disk", it, 0) }

        // Embedded lyrics in M4A (©lyr atom)
        if (lyrics.isNotBlank()) {
            writeTextItem("\u00a9lyr", lyrics)
        }

        // covr (Cover art in M4A)
        if (coverBytes != null && coverBytes.isNotEmpty()) {
            val isPng = coverBytes.size > 8 &&
                    coverBytes[0] == 0x89.toByte() &&
                    coverBytes[1] == 0x50.toByte() &&
                    coverBytes[2] == 0x4E.toByte() &&
                    coverBytes[3] == 0x47.toByte()
            val flagType = if (isPng) 0x0000000E else 0x0000000D // PNG or JPEG

            val dataLen = 16 + coverBytes.size
            val itemLen = 8 + dataLen
            val buf = ByteBuffer.allocate(itemLen).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(itemLen)
            buf.put("covr".toByteArray(Charsets.US_ASCII))

            buf.putInt(dataLen)
            buf.put("data".toByteArray(Charsets.US_ASCII))
            buf.putInt(flagType)
            buf.putInt(0x00000000)
            buf.put(coverBytes)

            ilstStream.write(buf.array())
        }

        val ilstBytes = ilstStream.toByteArray()
        val ilstAtomLen = 8 + ilstBytes.size
        val ilstAtom = ByteBuffer.allocate(ilstAtomLen).order(ByteOrder.BIG_ENDIAN)
        ilstAtom.putInt(ilstAtomLen)
        ilstAtom.put("ilst".toByteArray(Charsets.US_ASCII))
        ilstAtom.put(ilstBytes)

        // hdlr atom inside meta
        val hdlrAtom = ByteBuffer.allocate(33).order(ByteOrder.BIG_ENDIAN)
        hdlrAtom.putInt(33)
        hdlrAtom.put("hdlr".toByteArray(Charsets.US_ASCII))
        hdlrAtom.putInt(0) // version & flags
        hdlrAtom.putInt(0) // pre-defined
        hdlrAtom.put("mdir".toByteArray(Charsets.US_ASCII)) // component subtype
        hdlrAtom.put("appl".toByteArray(Charsets.US_ASCII)) // component manufacturer
        hdlrAtom.putInt(0) // component flags
        hdlrAtom.putInt(0) // component flags mask
        hdlrAtom.put(0.toByte()) // component name null string

        // meta atom
        val metaLen = 12 + hdlrAtom.capacity() + ilstAtomLen
        val metaAtom = ByteBuffer.allocate(metaLen).order(ByteOrder.BIG_ENDIAN)
        metaAtom.putInt(metaLen)
        metaAtom.put("meta".toByteArray(Charsets.US_ASCII))
        metaAtom.putInt(0) // version/flags
        metaAtom.put(hdlrAtom.array())
        metaAtom.put(ilstAtom.array())

        // udta atom
        val udtaLen = 8 + metaLen
        val udtaAtom = ByteBuffer.allocate(udtaLen).order(ByteOrder.BIG_ENDIAN)
        udtaAtom.putInt(udtaLen)
        udtaAtom.put("udta".toByteArray(Charsets.US_ASCII))
        udtaAtom.put(metaAtom.array())

        return udtaAtom.array()
    }

    private fun injectUdtaIntoMoov(moovBytes: ByteArray, newUdtaBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var offset = 8 // skip original moov header
        var replaced = false

        while (offset + 8 <= moovBytes.size) {
            val atomSize = ByteBuffer.wrap(moovBytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int
            val atomName = String(moovBytes, offset + 4, 4, Charsets.US_ASCII)
            if (atomSize <= 0) break

            if (atomName == "udta") {
                out.write(newUdtaBytes)
                replaced = true
            } else {
                out.write(moovBytes, offset, atomSize)
            }
            offset += atomSize
        }

        if (!replaced) {
            out.write(newUdtaBytes)
        }

        val childBytes = out.toByteArray()
        val newMoovLen = 8 + childBytes.size
        val result = ByteBuffer.allocate(newMoovLen).order(ByteOrder.BIG_ENDIAN)
        result.putInt(newMoovLen)
        result.put("moov".toByteArray(Charsets.US_ASCII))
        result.put(childBytes)
        return result.array()
    }

    private fun adjustChunkOffsets(moovBytes: ByteArray, delta: Int): ByteArray {
        val buffer = ByteBuffer.wrap(moovBytes).order(ByteOrder.BIG_ENDIAN)
        scanAndAdjustStco(buffer, 8, moovBytes.size, delta)
        return moovBytes
    }

    private fun scanAndAdjustStco(buffer: ByteBuffer, start: Int, end: Int, delta: Int) {
        var pos = start
        while (pos + 8 <= end) {
            val atomSize = buffer.getInt(pos)
            if (atomSize <= 0 || pos + atomSize > end) break

            val atomType = ByteArray(4)
            buffer.position(pos + 4)
            buffer.get(atomType)
            val name = String(atomType, Charsets.US_ASCII)

            when (name) {
                "trak", "mdia", "minf", "stbl" -> {
                    scanAndAdjustStco(buffer, pos + 8, pos + atomSize, delta)
                }
                "stco" -> {
                    if (pos + 16 <= end) {
                        val entryCount = buffer.getInt(pos + 12)
                        var entryPos = pos + 16
                        for (i in 0 until entryCount) {
                            if (entryPos + 4 > end) break
                            val currentOffset = buffer.getInt(entryPos)
                            buffer.putInt(entryPos, currentOffset + delta)
                            entryPos += 4
                        }
                    }
                }
            }
            pos += atomSize
        }
    }
}
