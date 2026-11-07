package com.example.data.remote

import android.util.Log
import com.example.data.model.CoverArtOption
import com.example.data.model.MetadataSource
import com.example.data.model.OnlineCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OnlineMetadataRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "SonicTag/1.0.0 ( Android; contact@sonictag.app )"

    suspend fun searchAllSources(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val itunesDeferred = async { searchITunes(query) }
        val musicBrainzDeferred = async { searchMusicBrainz(query) }
        val deezerDeferred = async { searchDeezer(query) }

        val itunesList = try { itunesDeferred.await() } catch (e: Exception) {
            Log.e("OnlineRepository", "iTunes error", e)
            emptyList()
        }
        val mbList = try { musicBrainzDeferred.await() } catch (e: Exception) {
            Log.e("OnlineRepository", "MusicBrainz error", e)
            emptyList()
        }
        val deezerList = try { deezerDeferred.await() } catch (e: Exception) {
            Log.e("OnlineRepository", "Deezer error", e)
            emptyList()
        }

        // Combine and prioritize high match results
        val combined = mutableListOf<OnlineCandidate>()
        combined.addAll(itunesList)
        combined.addAll(deezerList)
        combined.addAll(mbList)

        return@withContext combined
    }

    suspend fun searchITunes(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=15"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val items = root.optJSONArray("results") ?: return@withContext emptyList()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val trackName = item.optString("trackName", "")
                    val artistName = item.optString("artistName", "")
                    val collectionName = item.optString("collectionName", "")
                    val genre = item.optString("primaryGenreName", "")
                    val releaseDate = item.optString("releaseDate", "")
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                    val trackNumber = item.optInt("trackNumber", 0).let { if (it > 0) it.toString() else "" }
                    val discNumber = item.optInt("discNumber", 0).let { if (it > 0) it.toString() else "" }
                    val durationMs = item.optLong("trackTimeMillis", 0L)
                    val rawArtwork = item.optString("artworkUrl100", "")
                    // Enhance artwork to 1000x1000
                    val highResArtwork = if (rawArtwork.isNotBlank()) {
                        rawArtwork.replace("100x100bb", "1000x1000bb")
                    } else null

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.ITUNES,
                            title = trackName,
                            artist = artistName,
                            album = collectionName,
                            albumArtist = artistName,
                            year = year,
                            genre = genre,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            durationMs = durationMs,
                            coverArtUrl = highResArtwork,
                            matchScore = 95 - (i * 2)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Failed iTunes search for: $query", e)
        }
        results
    }

    suspend fun searchDeezer(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.deezer.com/search?q=$encoded&limit=15"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val items = root.optJSONArray("data") ?: return@withContext emptyList()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val title = item.optString("title", "")
                    val durationSec = item.optLong("duration", 0L)
                    val artistObj = item.optJSONObject("artist")
                    val artistName = artistObj?.optString("name", "") ?: ""
                    val albumObj = item.optJSONObject("album")
                    val albumTitle = albumObj?.optString("title", "") ?: ""
                    val coverXl = albumObj?.optString("cover_xl", "")
                    val coverBig = albumObj?.optString("cover_big", "")
                    val bestCover = when {
                        !coverXl.isNullOrBlank() -> coverXl
                        !coverBig.isNullOrBlank() -> coverBig
                        else -> null
                    }

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.DEEZER,
                            title = title,
                            artist = artistName,
                            album = albumTitle,
                            albumArtist = artistName,
                            durationMs = durationSec * 1000,
                            coverArtUrl = bestCover,
                            matchScore = 92 - (i * 2)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Failed Deezer search for: $query", e)
        }
        results
    }

    suspend fun searchMusicBrainz(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://musicbrainz.org/ws/2/recording?query=$encoded&fmt=json&limit=15"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val recordings = root.optJSONArray("recordings") ?: return@withContext emptyList()

                for (i in 0 until recordings.length()) {
                    val rec = recordings.getJSONObject(i)
                    val title = rec.optString("title", "")
                    val mbid = rec.optString("id", "")
                    val score = rec.optInt("score", 90)
                    val length = rec.optLong("length", 0L)

                    var artistName = ""
                    val artistCredit = rec.optJSONArray("artist-credit")
                    if (artistCredit != null && artistCredit.length() > 0) {
                        artistName = artistCredit.getJSONObject(0).optString("name", "")
                    }

                    var albumTitle = ""
                    var releaseYear = ""
                    var releaseMbid: String? = null
                    var trackNum = ""

                    val releases = rec.optJSONArray("releases")
                    if (releases != null && releases.length() > 0) {
                        val firstRel = releases.getJSONObject(0)
                        albumTitle = firstRel.optString("title", "")
                        releaseMbid = if (firstRel.has("id")) firstRel.getString("id") else null
                        val date = firstRel.optString("date", "")
                        if (date.length >= 4) {
                            releaseYear = date.substring(0, 4)
                        }
                    }

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.MUSICBRAINZ,
                            title = title,
                            artist = artistName,
                            album = albumTitle,
                            albumArtist = artistName,
                            year = releaseYear,
                            trackNumber = trackNum,
                            durationMs = length,
                            mbid = mbid,
                            releaseMbid = releaseMbid,
                            matchScore = score
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Failed MusicBrainz search for: $query", e)
        }
        results
    }

    suspend fun fetchCoverArtOptions(
        candidate: OnlineCandidate
    ): List<CoverArtOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<CoverArtOption>()

        // 1. If candidate already has cover url (iTunes or Deezer)
        if (!candidate.coverArtUrl.isNullOrBlank()) {
            val srcName = candidate.source.displayName
            options.add(
                CoverArtOption(
                    source = "$srcName Ultra HD",
                    url = candidate.coverArtUrl,
                    width = 1000,
                    height = 1000,
                    label = "$srcName Original (1000x1000)",
                    isSelected = true
                )
            )
            // If it's iTunes, also add 600px version
            if (candidate.source == MetadataSource.ITUNES) {
                val mediumUrl = candidate.coverArtUrl.replace("1000x1000bb", "600x600bb")
                options.add(
                    CoverArtOption(
                        source = "iTunes HD",
                        url = mediumUrl,
                        width = 600,
                        height = 600,
                        label = "iTunes Standard (600x600)",
                        isSelected = false
                    )
                )
            }
        }

        // 2. Query Cover Art Archive if we have a releaseMbid or if candidate is MusicBrainz
        val relMbid = candidate.releaseMbid
        if (!relMbid.isNullOrBlank()) {
            try {
                val caaUrl = "https://coverartarchive.org/release/$relMbid"
                val request = Request.Builder()
                    .url(caaUrl)
                    .header("User-Agent", userAgent)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val root = JSONObject(body)
                            val images = root.optJSONArray("images")
                            if (images != null) {
                                for (i in 0 until images.length()) {
                                    val img = images.getJSONObject(i)
                                    val isFront = img.optBoolean("front", false)
                                    val originalUrl = img.optString("image", "")
                                    val thumbs = img.optJSONObject("thumbnails")
                                    val url1200 = if (thumbs != null && thumbs.has("1200")) thumbs.getString("1200") else null
                                    val url500 = if (thumbs != null && thumbs.has("500")) thumbs.getString("500") else null

                                    if (isFront && originalUrl.isNotBlank()) {
                                        options.add(
                                            CoverArtOption(
                                                source = "Cover Art Archive",
                                                url = url1200 ?: originalUrl,
                                                width = 1200,
                                                height = 1200,
                                                label = "Cover Art Archive Front (1200px)",
                                                isSelected = options.isEmpty()
                                            )
                                        )
                                        if (url500 != null) {
                                            options.add(
                                                CoverArtOption(
                                                    source = "Cover Art Archive",
                                                    url = url500,
                                                    width = 500,
                                                    height = 500,
                                                    label = "Cover Art Archive Medium (500px)",
                                                    isSelected = false
                                                )
                                            )
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("OnlineRepository", "No Cover Art Archive images for $relMbid", e)
            }
        }

        // 3. Fallback search on iTunes if candidate was from MusicBrainz and has no cover
        if (options.isEmpty()) {
            val searchQuery = "${candidate.artist} ${candidate.album.ifBlank { candidate.title }}"
            val itunesFallback = searchITunes(searchQuery)
            itunesFallback.firstOrNull { !it.coverArtUrl.isNullOrBlank() }?.let { itMatch ->
                options.add(
                    CoverArtOption(
                        source = "iTunes Fallback",
                        url = itMatch.coverArtUrl,
                        width = 1000,
                        height = 1000,
                        label = "iTunes Cover (1000x1000)",
                        isSelected = true
                    )
                )
            }
        }

        return@withContext options
    }

    suspend fun downloadImageBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.bytes()
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Failed to download image from $url", e)
        }
        return@withContext null
    }
}
