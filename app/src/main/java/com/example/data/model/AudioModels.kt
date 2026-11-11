package com.example.data.model

import java.util.UUID

enum class AudioFormat {
    MP3,
    M4A;

    companion object {
        fun fromFileName(name: String): AudioFormat {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".m4a") || lower.endsWith(".mp4") || lower.endsWith(".aac") -> M4A
                else -> MP3
            }
        }
    }
}

enum class MetadataSource(val displayName: String) {
    MUSICBRAINZ("MusicBrainz"),
    ITUNES("iTunes"),
    DEEZER("Deezer"),
    THEAUDIODB("TheAudioDB"),
    NETEASE("NetEase"),
    SPOTIFY("Spotify"),
    LASTFM("Last.fm"),
    DISCOGS("Discogs"),
    GEMINI("Google Gemini"),
    MANUAL("Manual");
}

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val fileUri: String,
    val filePath: String? = null,
    val fileName: String,
    val format: AudioFormat,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val comment: String = "",
    val lyrics: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val hasCover: Boolean = false,
    val coverArtBytes: ByteArray? = null,
    val hasLyrics: Boolean = false,
    val parentFolderUri: String? = null,
    val treeUriString: String? = null,
    val dateModified: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = if (title.isNotBlank()) title else fileName.substringBeforeLast(".")

    val displayArtist: String
        get() = if (artist.isNotBlank()) artist else "Desconocido"

    val displayAlbum: String
        get() = if (album.isNotBlank()) album else "Sin Álbum"

    val isComplete: Boolean
        get() = title.isNotBlank() && artist.isNotBlank() && album.isNotBlank() && hasCover

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioTrack

        if (id != other.id) return false
        if (fileUri != other.fileUri) return false
        if (filePath != other.filePath) return false
        if (fileName != other.fileName) return false
        if (format != other.format) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (albumArtist != other.albumArtist) return false
        if (year != other.year) return false
        if (genre != other.genre) return false
        if (trackNumber != other.trackNumber) return false
        if (discNumber != other.discNumber) return false
        if (comment != other.comment) return false
        if (lyrics != other.lyrics) return false
        if (durationMs != other.durationMs) return false
        if (sizeBytes != other.sizeBytes) return false
        if (hasCover != other.hasCover) return false
        if (hasLyrics != other.hasLyrics) return false
        if (parentFolderUri != other.parentFolderUri) return false
        if (treeUriString != other.treeUriString) return false
        if (dateModified != other.dateModified) return false
        if (coverArtBytes != null) {
            if (other.coverArtBytes == null) return false
            if (!coverArtBytes.contentEquals(other.coverArtBytes)) return false
        } else if (other.coverArtBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + albumArtist.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + lyrics.hashCode()
        return result
    }
}

data class MusicFolder(
    val uriString: String,
    val displayName: String,
    val trackCount: Int = 0,
    val treeUriString: String? = null,
    val relativePath: String = ""
)

fun AudioTrack.belongsTo(folder: MusicFolder): Boolean {
    return this.parentFolderUri == folder.uriString ||
            (this.treeUriString == folder.uriString && this.parentFolderUri.isNullOrEmpty()) ||
            (folder.treeUriString == this.treeUriString && folder.relativePath.isEmpty() && this.parentFolderUri.isNullOrEmpty())
}

data class LyricsResult(
    val id: String = UUID.randomUUID().toString(),
    val trackName: String,
    val artistName: String,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val source: String = "LRCLIB"
) {
    val bestLyrics: String
        get() {
            val s = syncedLyrics?.trim()
            if (!s.isNullOrBlank() && !s.equals("null", ignoreCase = true)) return s
            val p = plainLyrics?.trim()
            if (!p.isNullOrBlank() && !p.equals("null", ignoreCase = true)) return p
            return ""
        }
}

data class OnlineCandidate(
    val id: String = UUID.randomUUID().toString(),
    val source: MetadataSource,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val durationMs: Long = 0L,
    val coverArtUrl: String? = null,
    val mbid: String? = null,
    val releaseMbid: String? = null,
    val matchScore: Int = 100
)

data class CoverArtOption(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val url: String? = null,
    val bytes: ByteArray? = null,
    val width: Int = 0,
    val height: Int = 0,
    val label: String,
    val isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CoverArtOption
        return id == other.id && isSelected == other.isSelected && url == other.url && label == other.label
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isSelected.hashCode()
        return result
    }
}

data class TagDiff(
    val fieldName: String,
    val originalValue: String,
    val newValue: String,
    val isChanged: Boolean = originalValue.trim() != newValue.trim()
)
