package com.example.data.remote

import android.util.Log
import com.example.data.model.LyricsResult
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
        durationMs: Long = 0L
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext null

        // 1. Try exact match /api/get
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
                            return@withContext LyricsResult(
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

        // 2. Fallback: Search endpoint /api/search?q=
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
                                return@withContext LyricsResult(
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

        return@withContext null
    }
}
