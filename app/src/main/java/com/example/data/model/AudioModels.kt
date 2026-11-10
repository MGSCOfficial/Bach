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
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
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
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class TagDiff(
    val fieldName: String,
    val originalValue: String,
    val newValue: String,
    val isChanged: Boolean = originalValue.trim() != newValue.trim()
)
