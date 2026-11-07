package com.example

import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.tagger.AudioTagWriter
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTagWriterTest {

    @Test
    fun testAudioFormatDetection() {
        assertEquals(AudioFormat.MP3, AudioFormat.fromFileName("song.mp3"))
        assertEquals(AudioFormat.MP3, AudioFormat.fromFileName("SONG.MP3"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromFileName("audio.m4a"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromFileName("track.mp4"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromFileName("track.aac"))
    }

    @Test
    fun testMp3TagWritingGeneratesValidId3v2Header() {
        val dummyAudio = ByteArray(100) { 0x55.toByte() }
        val dummyCover = ByteArray(64) { 0xAA.toByte() }

        val track = AudioTrack(
            fileUri = "file:///dummy.mp3",
            fileName = "dummy.mp3",
            format = AudioFormat.MP3,
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            year = "1975",
            genre = "Rock",
            trackNumber = "1"
        )

        val taggedBytes = AudioTagWriter.writeMp3Tags(
            originalBytes = dummyAudio,
            track = track,
            coverBytes = dummyCover
        )

        // Must start with ID3 header
        assertEquals('I'.code.toByte(), taggedBytes[0])
        assertEquals('D'.code.toByte(), taggedBytes[1])
        assertEquals('3'.code.toByte(), taggedBytes[2])
        assertEquals(0x03.toByte(), taggedBytes[3]) // ID3v2.3

        // Contains text frame IDs
        val stringContent = String(taggedBytes, Charsets.ISO_8859_1)
        assertTrue(stringContent.contains("TIT2"))
        assertTrue(stringContent.contains("Bohemian Rhapsody"))
        assertTrue(stringContent.contains("TPE1"))
        assertTrue(stringContent.contains("Queen"))
        assertTrue(stringContent.contains("TALB"))
        assertTrue(stringContent.contains("A Night at the Opera"))
        assertTrue(stringContent.contains("TYER"))
        assertTrue(stringContent.contains("1975"))
        assertTrue(stringContent.contains("TCON"))
        assertTrue(stringContent.contains("Rock"))
        assertTrue(stringContent.contains("APIC")) // Cover art frame
    }

    @Test
    fun testM4aTagWritingInjectsUdtaAtom() {
        // Construct a minimal dummy M4A file with ftyp, moov, and mdat atoms
        val ftyp = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            .putInt(16)
            .put("ftyp".toByteArray(Charsets.US_ASCII))
            .put("M4A ".toByteArray(Charsets.US_ASCII))
            .putInt(0)
            .array()

        val moov = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            .putInt(16)
            .put("moov".toByteArray(Charsets.US_ASCII))
            .putInt(8)
            .put("mvhd".toByteArray(Charsets.US_ASCII))
            .array()

        val mdat = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN)
            .putInt(24)
            .put("mdat".toByteArray(Charsets.US_ASCII))
            .put(ByteArray(16))
            .array()

        val dummyM4a = ftyp + moov + mdat

        val track = AudioTrack(
            fileUri = "file:///dummy.m4a",
            fileName = "dummy.m4a",
            format = AudioFormat.M4A,
            title = "Billie Jean",
            artist = "Michael Jackson",
            album = "Thriller",
            year = "1982",
            genre = "Pop",
            trackNumber = "2"
        )

        val taggedBytes = AudioTagWriter.writeM4aTags(
            originalBytes = dummyM4a,
            track = track,
            coverBytes = null
        )

        val stringContent = String(taggedBytes, Charsets.ISO_8859_1)
        assertTrue(stringContent.contains("moov"))
        assertTrue(stringContent.contains("udta"))
        assertTrue(stringContent.contains("meta"))
        assertTrue(stringContent.contains("ilst"))
        assertTrue(stringContent.contains("\u00a9nam")) // Title atom
        assertTrue(stringContent.contains("Billie Jean"))
        assertTrue(stringContent.contains("\u00a9ART")) // Artist atom
        assertTrue(stringContent.contains("Michael Jackson"))
        assertTrue(stringContent.contains("\u00a9alb")) // Album atom
        assertTrue(stringContent.contains("Thriller"))
    }
}
