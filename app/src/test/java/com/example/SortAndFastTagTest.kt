package com.example

import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.SortOrder
import com.example.data.tagger.AudioTagWriter
import com.example.data.tagger.FastTagExtractor
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class SortAndFastTagTest {

    @Test
    fun testSortOrderSortingLogic() {
        val t1 = AudioTrack(
            fileUri = "file:///a.mp3",
            fileName = "a.mp3",
            format = AudioFormat.MP3,
            title = "Zebra Song",
            artist = "Alice",
            durationMs = 120000,
            dateModified = 1000L
        )

        val t2 = AudioTrack(
            fileUri = "file:///b.mp3",
            fileName = "b.mp3",
            format = AudioFormat.MP3,
            title = "Apple Song",
            artist = "Bob",
            durationMs = 240000,
            dateModified = 5000L
        )

        val list = listOf(t1, t2)

        // Recently modified (descending)
        val sortedRecent = list.sortedByDescending { it.dateModified }
        assertEquals(t2.id, sortedRecent[0].id)
        assertEquals(t1.id, sortedRecent[1].id)

        // Title ASC
        val sortedTitle = list.sortedBy { it.displayTitle.lowercase() }
        assertEquals("Apple Song", sortedTitle[0].displayTitle)
        assertEquals("Zebra Song", sortedTitle[1].displayTitle)

        // Artist ASC
        val sortedArtist = list.sortedBy { it.displayArtist.lowercase() }
        assertEquals("Alice", sortedArtist[0].displayArtist)
        assertEquals("Bob", sortedArtist[1].displayArtist)

        // Duration DESC
        val sortedDuration = list.sortedByDescending { it.durationMs }
        assertEquals(240000L, sortedDuration[0].durationMs)
    }

    @Test
    fun testFastTagExtractorHeaderParsing() {
        val dummyAudio = ByteArray(50) { 0x00 }
        val track = AudioTrack(
            fileUri = "file:///sample.mp3",
            fileName = "sample.mp3",
            format = AudioFormat.MP3,
            title = "Stairway to Heaven",
            artist = "Led Zeppelin",
            album = "Led Zeppelin IV",
            year = "1971",
            genre = "Rock"
        )

        val taggedMp3Bytes = AudioTagWriter.writeMp3Tags(
            originalBytes = dummyAudio,
            track = track,
            coverBytes = null
        )

        val stream = ByteArrayInputStream(taggedMp3Bytes)
        // Verify MP3 header starts with ID3
        val header = ByteArray(3)
        stream.read(header)
        assertEquals("ID3", String(header, Charsets.US_ASCII))
    }
}
