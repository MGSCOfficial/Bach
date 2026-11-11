package com.example.data.remote

import android.util.Log
import com.example.data.model.LyricsResult
import com.example.data.storage.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "SoundID/1.0.0 ( Android; contact@soundid.app )"

    private fun optCleanLyrics(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val raw = obj.optString(key, "").trim()
        if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) return null
        return raw
    }

    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        durationMs: Long = 0L,
        settings: AppSettings = AppSettings()
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext null

        // 1. LRCLIB (Default Free Provider)
        if (settings.lyricsSourceLrclib) {
            val lrcLibResult = fetchFromLrcLib(trackName, artistName, albumName, durationMs)
            if (lrcLibResult != null) return@withContext lrcLibResult
        }

        // 2. NetEase Cloud Music (Alternative Free Provider for Synced Lyrics)
        if (settings.lyricsSourceNetEase) {
            val netEaseResult = fetchFromNetEase(trackName, artistName)
            if (netEaseResult != null) return@withContext netEaseResult
        }

        // 3. Genius API (User Token)
        if (settings.lyricsSourceGenius && settings.geniusToken.isNotBlank()) {
            val geniusResult = fetchFromGenius(trackName, artistName, settings.geniusToken)
            if (geniusResult != null) return@withContext geniusResult
        }

        // 4. Musixmatch API (User API Key)
        if (settings.lyricsSourceMusixmatch && settings.musixmatchApiKey.isNotBlank()) {
            val musixmatchResult = fetchFromMusixmatch(trackName, artistName, settings.musixmatchApiKey)
            if (musixmatchResult != null) return@withContext musixmatchResult
        }

        return@withContext null
    }

    suspend fun searchAllLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null
    ): List<LyricsResult> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<LyricsResult>()
        if (trackName.isBlank() && artistName.isBlank()) return@withContext emptyList()

        // 1. Exact match attempt with LRCLIB /api/get
        if (trackName.isNotBlank() && artistName.isNotBlank()) {
            try {
                val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
                val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
                val url = StringBuilder("https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist").apply {
                    if (!albumName.isNullOrBlank()) append("&album_name=").append(URLEncoder.encode(albumName, "UTF-8"))
                }.toString()

                val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { b ->
                            val obj = JSONObject(b)
                            val plain = optCleanLyrics(obj, "plainLyrics")
                            val synced = optCleanLyrics(obj, "syncedLyrics")
                            if (!plain.isNullOrBlank() || !synced.isNullOrBlank()) {
                                candidates.add(
                                    LyricsResult(
                                        trackName = obj.optString("trackName", trackName),
                                        artistName = obj.optString("artistName", artistName),
                                        plainLyrics = plain,
                                        syncedLyrics = synced,
                                        source = "LRCLIB (Coincidencia Exacta)"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("LyricsRepository", "Error en /api/get exacto de LRCLIB", e)
            }
        }

        // 2. Broad search query via LRCLIB /api/search?q=...
        try {
            val query = listOf(artistName, trackName).filter { it.isNotBlank() }.joinToString(" ")
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"
            val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { b ->
                        val arr = JSONArray(b)
                        for (i in 0 until minOf(arr.length(), 15)) {
                            val item = arr.getJSONObject(i)
                            val plain = optCleanLyrics(item, "plainLyrics")
                            val synced = optCleanLyrics(item, "syncedLyrics")
                            if (!plain.isNullOrBlank() || !synced.isNullOrBlank()) {
                                val tName = item.optString("trackName", trackName)
                                val aName = item.optString("artistName", artistName)
                                val albName = item.optString("albumName", "")
                                val srcLabel = if (albName.isNotBlank()) "LRCLIB • $albName" else "LRCLIB"
                                val res = LyricsResult(
                                    trackName = tName,
                                    artistName = aName,
                                    plainLyrics = plain,
                                    syncedLyrics = synced,
                                    source = srcLabel
                                )
                                // Avoid exact duplicate lyrics text
                                if (candidates.none { it.bestLyrics.trim() == res.bestLyrics.trim() }) {
                                    candidates.add(res)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error buscando candidatos de letra en LRCLIB", e)
        }

        // 3. Fallback con artista principal si es colaboración y no hubo resultados
        if (candidates.isEmpty() && artistName.isNotBlank() && trackName.isNotBlank()) {
            val primaryArtist = artistName.split(",", "&", "feat.", "ft.", ";", "•", "x", "X").first().trim()
            if (primaryArtist.isNotBlank() && !primaryArtist.equals(artistName, ignoreCase = true)) {
                try {
                    val query2 = "$primaryArtist $trackName"
                    val encodedQuery2 = URLEncoder.encode(query2, "UTF-8")
                    val url2 = "https://lrclib.net/api/search?q=$encodedQuery2"
                    val request2 = Request.Builder().url(url2).header("User-Agent", userAgent).build()
                    client.newCall(request2).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.string()?.let { b ->
                                val arr = JSONArray(b)
                                for (i in 0 until minOf(arr.length(), 15)) {
                                    val item = arr.getJSONObject(i)
                                    val plain = optCleanLyrics(item, "plainLyrics")
                                    val synced = optCleanLyrics(item, "syncedLyrics")
                                    if (!plain.isNullOrBlank() || !synced.isNullOrBlank()) {
                                        val tName = item.optString("trackName", trackName)
                                        val aName = item.optString("artistName", primaryArtist)
                                        val albName = item.optString("albumName", "")
                                        val srcLabel = if (albName.isNotBlank()) "LRCLIB • $albName" else "LRCLIB"
                                        val res = LyricsResult(
                                            trackName = tName,
                                            artistName = aName,
                                            plainLyrics = plain,
                                            syncedLyrics = synced,
                                            source = srcLabel
                                        )
                                        if (candidates.none { it.bestLyrics.trim() == res.bestLyrics.trim() }) {
                                            candidates.add(res)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LyricsRepository", "Error en fallback con artista principal en LRCLIB", e)
                }
            }
        }

        candidates
    }

    private fun fetchFromLrcLib(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationMs: Long
    ): LyricsResult? {
        // Exact match
        try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            val sb = StringBuilder("https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist")
            if (!albumName.isNullOrBlank()) {
                sb.append("&album_name=").append(URLEncoder.encode(albumName, "UTF-8"))
            }
            if (durationMs > 0) {
                sb.append("&duration=").append(durationMs / 1000)
            }

            val request = Request.Builder()
                .url(sb.toString())
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val obj = JSONObject(body)
                        val plain = optCleanLyrics(obj, "plainLyrics")
                        val synced = optCleanLyrics(obj, "syncedLyrics")
                        if (!plain.isNullOrBlank() || !synced.isNullOrBlank()) {
                            return LyricsResult(
                                trackName = obj.optString("trackName", trackName),
                                artistName = obj.optString("artistName", artistName),
                                plainLyrics = plain,
                                syncedLyrics = synced,
                                source = "LRCLIB"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "Exact match failed for $artistName - $trackName", e)
        }

        // Search match
        try {
            val query = "$artistName $trackName".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val first = arr.getJSONObject(0)
                            val plain = optCleanLyrics(first, "plainLyrics")
                            val synced = optCleanLyrics(first, "syncedLyrics")
                            if (!plain.isNullOrBlank() || !synced.isNullOrBlank()) {
                                return LyricsResult(
                                    trackName = first.optString("trackName", trackName),
                                    artistName = first.optString("artistName", artistName),
                                    plainLyrics = plain,
                                    syncedLyrics = synced,
                                    source = "LRCLIB Search"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Search failed for $artistName - $trackName", e)
        }

        return null
    }

    private fun fetchFromNetEase(trackName: String, artistName: String): LyricsResult? {
        try {
            val query = "$artistName $trackName".trim()
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://music.163.com/api/search/get/web?csrf_token=&hlpretag=&hlposttag=&s=$encoded&type=1&offset=0&total=true&limit=1"
            val searchReq = Request.Builder().url(searchUrl).header("User-Agent", userAgent).build()

            var songId: Long = 0L
            client.newCall(searchReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: return null
                if (songs.length() > 0) {
                    songId = songs.getJSONObject(0).optLong("id", 0L)
                }
            }

            if (songId <= 0L) return null

            val lyricUrl = "https://music.163.com/api/song/lyric?os=pc&id=$songId&lv=-1&kv=-1&tv=-1"
            val lyricReq = Request.Builder().url(lyricUrl).header("User-Agent", userAgent).build()
            client.newCall(lyricReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val lrcObj = root.optJSONObject("lrc")
                val lyricText = lrcObj?.optString("lyric", null)?.trim()
                if (!lyricText.isNullOrBlank() && !lyricText.equals("null", ignoreCase = true)) {
                    return LyricsResult(
                        trackName = trackName,
                        artistName = artistName,
                        syncedLyrics = lyricText,
                        plainLyrics = null,
                        source = "NetEase"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "NetEase lyric fetch failed", e)
        }
        return null
    }

    private fun fetchFromGenius(trackName: String, artistName: String, token: String): LyricsResult? {
        try {
            val query = "$artistName $trackName".trim()
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.genius.com/search?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val hits = root.optJSONObject("response")?.optJSONArray("hits") ?: return null
                if (hits.length() > 0) {
                    val hit = hits.getJSONObject(0).optJSONObject("result") ?: return null
                    val fullTitle = hit.optString("full_title", "$artistName - $trackName")
                    val songUrl = hit.optString("url", "")
                    if (songUrl.isNotBlank()) {
                        return LyricsResult(
                            trackName = trackName,
                            artistName = artistName,
                            plainLyrics = "Letra anotada disponible en Genius:\n$songUrl",
                            syncedLyrics = null,
                            source = "Genius"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "Genius fetch failed", e)
        }
        return null
    }

    private fun fetchFromMusixmatch(trackName: String, artistName: String, apiKey: String): LyricsResult? {
        try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            val url = "https://api.musixmatch.com/ws/1.1/matcher.lyrics.get?q_track=$encodedTrack&q_artist=$encodedArtist&apikey=$apiKey"
            val request = Request.Builder().url(url).header("User-Agent", userAgent).build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val lyricsBody = root.optJSONObject("message")?.optJSONObject("body")?.optJSONObject("lyrics")
                val text = lyricsBody?.optString("lyrics_body", null)?.trim()
                if (!text.isNullOrBlank() && !text.equals("null", ignoreCase = true)) {
                    return LyricsResult(
                        trackName = trackName,
                        artistName = artistName,
                        plainLyrics = text,
                        syncedLyrics = null,
                        source = "Musixmatch"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "Musixmatch fetch failed", e)
        }
        return null
    }
}
