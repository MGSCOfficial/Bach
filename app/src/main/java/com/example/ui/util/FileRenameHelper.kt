package com.example.ui.util

import com.example.data.model.AudioTrack

object FileRenameHelper {

    fun resolvePattern(pattern: String, track: AudioTrack): String {
        val ext = track.fileName.substringAfterLast(".", "")
        val trackNum = track.trackNumber.trim().let { num ->
            val parsed = num.toIntOrNull()
            if (parsed != null && parsed < 10 && !num.startsWith("0")) {
                "0$parsed"
            } else if (num.isNotBlank()) {
                num
            } else {
                ""
            }
        }

        val artist = track.artist.trim().ifBlank { "Desconocido" }
        val title = track.title.trim().ifBlank { track.fileName.substringBeforeLast(".") }
        val album = track.album.trim().ifBlank { "Desconocido" }
        val year = track.year.trim().ifBlank { "" }

        var resolved = pattern
            .replace("%artist%", artist, ignoreCase = true)
            .replace("%title%", title, ignoreCase = true)
            .replace("%album%", album, ignoreCase = true)
            .replace("%track%", trackNum, ignoreCase = true)
            .replace("%year%", year, ignoreCase = true)

        // Remove redundant separators if track or year was empty
        resolved = resolved
            .replace(Regex("^\\s*\\.\\s*"), "")
            .replace(Regex("\\s*-\\s*-\\s*"), " - ")
            .replace(Regex("^\\s*-\\s*"), "")
            .replace(Regex("\\s*-\\s*$"), "")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (resolved.isBlank()) {
            resolved = title
        }

        return if (ext.isNotBlank()) "$resolved.$ext" else resolved
    }
}
