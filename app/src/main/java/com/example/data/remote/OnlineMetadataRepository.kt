package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.data.model.CoverArtOption
import com.example.data.model.MetadataSource
import com.example.data.model.OnlineCandidate
import com.example.data.storage.AppSettings
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.FormBody
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

    private fun String?.cleanQuery(): String = this
        ?.replace("\uFEFF", "")
        ?.replace("\u200B", "")
        ?.replace("\u00A0", " ")
        ?.replace("\u0000", "")
        ?.trim()
        .orEmpty()

    suspend fun searchAllSources(
        query: String,
        settings: AppSettings = AppSettings()
    ): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val q = query.cleanQuery()
        if (q.isBlank()) return@withContext emptyList()

        val tasks = mutableListOf<Deferred<List<OnlineCandidate>>>()

        // 1. iTunes (Gratuita)
        if (settings.sourceITunes) {
            tasks.add(async { searchITunes(q) })
        }
        // 2. Deezer (Gratuita)
        if (settings.sourceDeezer) {
            tasks.add(async { searchDeezer(q) })
        }
        // 3. MusicBrainz (Gratuita)
        if (settings.sourceMusicBrainz) {
            tasks.add(async { searchMusicBrainz(query) })
        }
        // 4. TheAudioDB (Gratuita)
        if (settings.sourceTheAudioDb) {
            tasks.add(async { searchTheAudioDb(query) })
        }
        // 5. NetEase Music (Gratuita)
        if (settings.sourceNetEase) {
            tasks.add(async { searchNetEase(query) })
        }
        // 6. Spotify (Requiere Client ID & Secret)
        if (settings.sourceSpotify && settings.spotifyClientId.isNotBlank() && settings.spotifyClientSecret.isNotBlank()) {
            tasks.add(async { searchSpotify(query, settings.spotifyClientId, settings.spotifyClientSecret) })
        }
        // 7. Last.fm (Requiere API Key)
        if (settings.sourceLastFm && settings.lastFmApiKey.isNotBlank()) {
            tasks.add(async { searchLastFm(query, settings.lastFmApiKey) })
        }
        // 8. Discogs (Requiere Personal Access Token)
        if (settings.sourceDiscogs && settings.discogsToken.isNotBlank()) {
            tasks.add(async { searchDiscogs(query, settings.discogsToken) })
        }

        val combined = mutableListOf<OnlineCandidate>()
        for (task in tasks) {
            try {
                combined.addAll(task.await())
            } catch (e: Exception) {
                Log.w("OnlineRepository", "Task execution error", e)
            }
        }

        // Resilient fallback si no hubo resultados iniciales
        if (combined.isEmpty()) {
            val sanitized = q
                .replace(Regex("^\\.+"), "")
                .replace(Regex("(?i)\\s*\\(feat\\.[^)]+\\)"), "")
                .replace(Regex("(?i)\\s*\\[feat\\.[^]]+\\]"), "")
                .replace(Regex("(?i)\\s*\\(from[^)]+\\)"), "")
                .trim()
            if (sanitized != q && sanitized.isNotBlank()) {
                val fbTasks = mutableListOf<Deferred<List<OnlineCandidate>>>()
                if (settings.sourceITunes) fbTasks.add(async { searchITunes(sanitized) })
                if (settings.sourceDeezer) fbTasks.add(async { searchDeezer(sanitized) })
                for (t in fbTasks) {
                    try { combined.addAll(t.await()) } catch (_: Exception) {}
                }
            }
            if (combined.isEmpty() && q.contains("Jack Black", ignoreCase = true)) {
                val tbQuery = q.replace("Jack Black", "Tenacious D", ignoreCase = true)
                val tbTasks = mutableListOf<Deferred<List<OnlineCandidate>>>()
                if (settings.sourceITunes) tbTasks.add(async { searchITunes(tbQuery) })
                if (settings.sourceDeezer) tbTasks.add(async { searchDeezer(tbQuery) })
                for (t in tbTasks) {
                    try { combined.addAll(t.await()) } catch (_: Exception) {}
                }
            }
        }

        // Si la búsqueda incluye indicador de número de pista (p. ej. "LISA Track 3"), priorizar la pista coincidente
        val trackPattern = Regex("\\b(?:track|pista|piste)\\s*0*([1-9]\\d*)\\b", RegexOption.IGNORE_CASE)
        val trackMatch = trackPattern.find(query)
        val targetTrackNumber = trackMatch?.groupValues?.get(1)?.toIntOrNull()

        if (targetTrackNumber != null) {
            combined.sortWith(compareByDescending<OnlineCandidate> {
                it.trackNumber.toIntOrNull() == targetTrackNumber
            }.thenByDescending { it.matchScore })
        } else {
            combined.sortByDescending { it.matchScore }
        }

        return@withContext combined
    }

    suspend fun searchAdvanced(
        title: String,
        artist: String,
        album: String,
        settings: AppSettings = AppSettings()
    ): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val t = title.cleanQuery()
        val a = artist.cleanQuery()
        val al = album.cleanQuery()

        if (t.isBlank() && a.isBlank() && al.isBlank()) {
            return@withContext emptyList()
        }

        // Detectar si el título es en realidad un placeholder de número de pista, p. ej. "Track 3", "Pista 03", o "3"
        val trackNumRegex = Regex("^(?:track|pista|piste|t)?\\s*0*([1-9]\\d*)(?:\\s*[-:.]\\s*.*)?$", RegexOption.IGNORE_CASE)
        val trackMatch = trackNumRegex.find(t)
        val extractedTrackNumber = trackMatch?.groupValues?.get(1)?.toIntOrNull()
        val isGenericTitle = extractedTrackNumber != null || t.isBlank()

        val tasks = mutableListOf<Deferred<List<OnlineCandidate>>>()

        // 1. iTunes estructurado
        if (settings.sourceITunes) {
            tasks.add(async { searchITunesAdvanced(t, a, al, extractedTrackNumber, isGenericTitle) })
        }
        // 2. Deezer estructurado
        if (settings.sourceDeezer) {
            tasks.add(async { searchDeezerAdvanced(t, a, al, extractedTrackNumber, isGenericTitle) })
        }
        // 3. MusicBrainz estructurado
        if (settings.sourceMusicBrainz) {
            tasks.add(async { searchMusicBrainzAdvanced(t, a, al, extractedTrackNumber, isGenericTitle) })
        }
        // 4. TheAudioDB
        if (settings.sourceTheAudioDb) {
            val q = listOf(a, al).filter { it.isNotBlank() }.joinToString(" ")
            tasks.add(async { searchTheAudioDb(q) })
        }
        // 5. NetEase Music
        if (settings.sourceNetEase) {
            val q = listOf(a, if (isGenericTitle) al else t).filter { it.isNotBlank() }.joinToString(" ")
            tasks.add(async { searchNetEase(q) })
        }
        // 6. Spotify (si está configurado)
        if (settings.sourceSpotify && settings.spotifyClientId.isNotBlank() && settings.spotifyClientSecret.isNotBlank()) {
            val spotifyQuery = buildString {
                if (!isGenericTitle && t.isNotBlank()) append("track:$t ")
                if (a.isNotBlank()) append("artist:$a ")
                if (al.isNotBlank()) append("album:$al ")
            }.trim().ifBlank { "$a $al" }
            tasks.add(async { searchSpotify(spotifyQuery, settings.spotifyClientId, settings.spotifyClientSecret) })
        }
        // 7. Last.fm (si está configurado)
        if (settings.sourceLastFm && settings.lastFmApiKey.isNotBlank()) {
            val q = if (isGenericTitle) "$a $al" else "$a $t"
            tasks.add(async { searchLastFm(q, settings.lastFmApiKey) })
        }
        // 8. Discogs (si está configurado)
        if (settings.sourceDiscogs && settings.discogsToken.isNotBlank()) {
            val q = listOf(a, al, if (!isGenericTitle) t else "").filter { it.isNotBlank() }.joinToString(" ")
            tasks.add(async { searchDiscogs(q, settings.discogsToken) })
        }

        val combined = mutableListOf<OnlineCandidate>()
        for (task in tasks) {
            try {
                combined.addAll(task.await())
            } catch (e: Exception) {
                Log.w("OnlineRepository", "Advanced search task error", e)
            }
        }

        // Resilient fallback si no hubo resultados iniciales
        if (combined.isEmpty()) {
            val cleanT = t.replace(Regex("^\\.+"), "").replace(Regex("(?i)\\s*\\(feat\\.[^)]+\\)"), "").replace(Regex("(?i)\\s*\\[feat\\.[^]]+\\]"), "").replace(Regex("(?i)\\s*\\(from[^)]+\\)"), "").trim()
            if (a.contains("Jack Black", ignoreCase = true)) {
                val fbTasks = mutableListOf<Deferred<List<OnlineCandidate>>>()
                if (settings.sourceITunes) fbTasks.add(async { searchITunesAdvanced(cleanT, "Tenacious D", al, extractedTrackNumber, isGenericTitle) })
                if (settings.sourceDeezer) fbTasks.add(async { searchDeezerAdvanced(cleanT, "Tenacious D", al, extractedTrackNumber, isGenericTitle) })
                for (task in fbTasks) {
                    try { combined.addAll(task.await()) } catch (_: Exception) {}
                }
            }
            if (combined.isEmpty() && al.isNotBlank() && cleanT.isNotBlank()) {
                val fbTasks = mutableListOf<Deferred<List<OnlineCandidate>>>()
                if (settings.sourceITunes) fbTasks.add(async { searchITunes("$cleanT $al") })
                if (settings.sourceDeezer) fbTasks.add(async { searchDeezer("$cleanT $al") })
                for (task in fbTasks) {
                    try { combined.addAll(task.await()) } catch (_: Exception) {}
                }
            }
        }

        // Ordenar por coincidencia real de puntuación calculada, número de pista y presencia de carátula
        if (extractedTrackNumber != null) {
            combined.sortWith(
                compareByDescending<OnlineCandidate> { it.matchScore }
                    .thenByDescending { it.trackNumber.toIntOrNull() == extractedTrackNumber }
                    .thenByDescending { it.coverArtUrl != null }
            )
        } else {
            combined.sortWith(
                compareByDescending<OnlineCandidate> { it.matchScore }
                    .thenByDescending { it.coverArtUrl != null }
            )
        }

        return@withContext combined
    }

    private suspend fun searchITunesAdvanced(
        title: String,
        artist: String,
        album: String,
        targetTrackNumber: Int?,
        isGenericTitle: Boolean
    ): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineCandidate>()
        try {
            // Si el título es genérico (ej. "Track 3") y tenemos artista y álbum, consultamos las canciones del álbum
            val searchTerm = if (isGenericTitle && artist.isNotBlank() && album.isNotBlank()) {
                "$artist $album"
            } else if (artist.isNotBlank() && title.isNotBlank()) {
                "$artist $title"
            } else {
                listOf(artist, album, title).filter { it.isNotBlank() }.joinToString(" ")
            }

            val encoded = URLEncoder.encode(searchTerm, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=50"
            val request = Request.Builder().url(url).header("User-Agent", userAgent).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val items = root.optJSONArray("results") ?: return@withContext emptyList()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val tName = item.optString("trackName", "")
                    val aName = item.optString("artistName", "")
                    val alName = item.optString("collectionName", "")
                    val genre = item.optString("primaryGenreName", "")
                    val releaseDate = item.optString("releaseDate", "")
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                    val tNumInt = item.optInt("trackNumber", 0)
                    val trackNumber = if (tNumInt > 0) tNumInt.toString() else ""
                    val discNumber = item.optInt("discNumber", 0).let { if (it > 0) it.toString() else "" }
                    val durationMs = item.optLong("trackTimeMillis", 0L)
                    val rawArt = item.optString("artworkUrl100", "")
                    val highResArt = if (rawArt.isNotBlank()) rawArt.replace("100x100bb.jpg", "1000x1000bb.jpg") else null

                    var score = 0
                    val artistMatches = artist.isNotBlank() && aName.contains(artist, ignoreCase = true)
                    val albumMatches = album.isNotBlank() && alName.contains(album, ignoreCase = true)
                    val trackNumMatches = targetTrackNumber != null && tNumInt == targetTrackNumber
                    val titleMatches = !isGenericTitle && title.isNotBlank() && tName.contains(title, ignoreCase = true)

                    if (artistMatches) score += 35
                    if (albumMatches) score += 35
                    if (trackNumMatches) score += 25
                    if (titleMatches) score += 25
                    if (artistMatches && albumMatches) score += 4
                    if (score == 0) score = 15

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.ITUNES,
                            title = tName,
                            artist = aName,
                            album = alName,
                            albumArtist = aName,
                            year = year,
                            genre = genre,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            durationMs = durationMs,
                            coverArtUrl = highResArt,
                            matchScore = score.coerceIn(1, 99)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "iTunes advanced search error", e)
        }
        return@withContext results
    }

    private suspend fun searchDeezerAdvanced(
        title: String,
        artist: String,
        album: String,
        targetTrackNumber: Int?,
        isGenericTitle: Boolean
    ): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineCandidate>()
        try {
            // Intentar primero búsqueda estructurada o directa
            val queries = mutableListOf<String>()
            if (isGenericTitle && artist.isNotBlank() && album.isNotBlank()) {
                queries.add("$artist $album")
                queries.add("artist:\"$artist\" album:\"$album\"")
            } else {
                val structured = buildString {
                    if (artist.isNotBlank()) append("artist:\"$artist\" ")
                    if (album.isNotBlank()) append("album:\"$album\" ")
                    if (!isGenericTitle && title.isNotBlank()) append("track:\"$title\" ")
                }.trim()
                if (structured.isNotBlank()) queries.add(structured)
                val plain = listOf(artist, album, if (!isGenericTitle) title else "").filter { it.isNotBlank() }.joinToString(" ")
                if (plain.isNotBlank()) queries.add(plain)
            }

            for (qStr in queries.distinct()) {
                val encoded = URLEncoder.encode(qStr, "UTF-8")
                val url = "https://api.deezer.com/search?q=$encoded&limit=50"
                val request = Request.Builder().url(url).header("User-Agent", userAgent).build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val root = JSONObject(body)
                        val items = root.optJSONArray("data")
                        if (items != null && items.length() > 0) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val tName = item.optString("title", "")
                                val durationSec = item.optLong("duration", 0L)
                                val artistObj = item.optJSONObject("artist")
                                val aName = artistObj?.optString("name", "") ?: ""
                                val albumObj = item.optJSONObject("album")
                                val alName = albumObj?.optString("title", "") ?: ""
                                val coverXl = albumObj?.optString("cover_xl", "")
                                val coverBig = albumObj?.optString("cover_big", "")
                                val bestCover = when {
                                    !coverXl.isNullOrBlank() -> coverXl
                                    !coverBig.isNullOrBlank() -> coverBig
                                    else -> null
                                }
                                val tPos = item.optInt("track_position", 0)
                                val trackNumStr = if (tPos > 0) tPos.toString() else ""

                                var score = 0
                                val artistMatches = artist.isNotBlank() && aName.contains(artist, ignoreCase = true)
                                val albumMatches = album.isNotBlank() && alName.contains(album, ignoreCase = true)
                                val trackNumMatches = targetTrackNumber != null && tPos == targetTrackNumber
                                val titleMatches = !isGenericTitle && title.isNotBlank() && tName.contains(title, ignoreCase = true)

                                if (artistMatches) score += 35
                                if (albumMatches) score += 35
                                if (trackNumMatches) score += 25
                                if (titleMatches) score += 25
                                if (artistMatches && albumMatches) score += 4
                                if (score == 0) score = 15

                                results.add(
                                    OnlineCandidate(
                                        source = MetadataSource.DEEZER,
                                        title = tName,
                                        artist = aName,
                                        album = alName,
                                        albumArtist = aName,
                                        trackNumber = trackNumStr,
                                        durationMs = durationSec * 1000L,
                                        coverArtUrl = bestCover,
                                        matchScore = score.coerceIn(1, 99)
                                    )
                                )
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) break
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Deezer advanced search error", e)
        }
        return@withContext results
    }

    private suspend fun searchMusicBrainzAdvanced(
        title: String,
        artist: String,
        album: String,
        targetTrackNumber: Int?,
        isGenericTitle: Boolean
    ): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineCandidate>()
        try {
            val luceneQuery = buildString {
                if (artist.isNotBlank()) append("artist:\"$artist\" ")
                if (album.isNotBlank()) append("release:\"$album\" ")
                if (!isGenericTitle && title.isNotBlank()) append("recording:\"$title\" ")
            }.trim().ifBlank { listOf(artist, album, title).filter { it.isNotBlank() }.joinToString(" ") }

            val encoded = URLEncoder.encode(luceneQuery, "UTF-8")
            val url = "https://musicbrainz.org/ws/2/recording?query=$encoded&fmt=json&limit=25"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val recordings = root.optJSONArray("recordings") ?: return@withContext emptyList()

                for (i in 0 until recordings.length()) {
                    val rec = recordings.getJSONObject(i)
                    val recTitle = rec.optString("title", "")
                    val length = rec.optLong("length", 0L)

                    var aName = ""
                    val artistCredit = rec.optJSONArray("artist-credit")
                    if (artistCredit != null && artistCredit.length() > 0) {
                        aName = artistCredit.getJSONObject(0).optString("name", "")
                    }

                    var alName = ""
                    var releaseYear = ""
                    var trackNumber = ""
                    var discNumber = ""
                    var releaseMbid = ""
                    var recTrackNumInt = 0

                    val releases = rec.optJSONArray("releases")
                    if (releases != null && releases.length() > 0) {
                        val rel = releases.getJSONObject(0)
                        alName = rel.optString("title", "")
                        releaseMbid = rel.optString("id", "")
                        val date = rel.optString("date", "")
                        if (date.length >= 4) releaseYear = date.substring(0, 4)

                        val media = rel.optJSONArray("media")
                        if (media != null && media.length() > 0) {
                            val med = media.getJSONObject(0)
                            discNumber = med.optInt("position", 1).toString()
                            val trackArr = med.optJSONArray("track")
                            if (trackArr != null && trackArr.length() > 0) {
                                recTrackNumInt = trackArr.getJSONObject(0).optInt("number", 0)
                                if (recTrackNumInt > 0) trackNumber = recTrackNumInt.toString()
                            }
                        }
                    }

                    val coverArtUrl = if (releaseMbid.isNotBlank()) {
                        "https://coverartarchive.org/release/$releaseMbid/front-500"
                    } else null

                    var score = 0
                    val artistMatches = artist.isNotBlank() && aName.contains(artist, ignoreCase = true)
                    val albumMatches = album.isNotBlank() && alName.contains(album, ignoreCase = true)
                    val trackNumMatches = targetTrackNumber != null && recTrackNumInt == targetTrackNumber
                    val titleMatches = !isGenericTitle && title.isNotBlank() && recTitle.contains(title, ignoreCase = true)

                    if (artistMatches) score += 35
                    if (albumMatches) score += 35
                    if (trackNumMatches) score += 25
                    if (titleMatches) score += 25
                    if (artistMatches && albumMatches) score += 4
                    if (score == 0) score = 15

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.MUSICBRAINZ,
                            title = recTitle,
                            artist = aName,
                            album = alName,
                            albumArtist = aName,
                            year = releaseYear,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            durationMs = length,
                            coverArtUrl = coverArtUrl,
                            matchScore = score.coerceIn(1, 99)
                        )
                    )
                }
            }

            // Fallback si la búsqueda Lucene no retornó nada
            if (results.isEmpty()) {
                val fallbackQuery = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" ")
                if (fallbackQuery.isNotBlank()) {
                    results.addAll(searchMusicBrainz(fallbackQuery))
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "MusicBrainz advanced search error", e)
        }
        return@withContext results
    }

    suspend fun searchITunes(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=25"
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
                    val title = item.optString("trackName", "")
                    val artist = item.optString("artistName", "")
                    val album = item.optString("collectionName", "")
                    val genre = item.optString("primaryGenreName", "")
                    val releaseDate = item.optString("releaseDate", "")
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                    val trackNumber = item.optInt("trackNumber", 0).let { if (it > 0) it.toString() else "" }
                    val discNumber = item.optInt("discNumber", 0).let { if (it > 0) it.toString() else "" }
                    val durationMs = item.optLong("trackTimeMillis", 0L)
                    val rawArt = item.optString("artworkUrl100", "")
                    val highResArt = if (rawArt.isNotBlank()) {
                        rawArt.replace("100x100bb.jpg", "1000x1000bb.jpg")
                    } else null

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.ITUNES,
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtist = artist,
                            year = year,
                            genre = genre,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            durationMs = durationMs,
                            coverArtUrl = highResArt,
                            matchScore = 95
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "iTunes query error", e)
        }
        return@withContext results
    }

    suspend fun searchDeezer(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineCandidate>()
        val cleanQ = query.cleanQuery()
        val queries = mutableListOf(cleanQ)

        // Si la consulta contiene número de pista como "Track 3", agregar fallback limpio sin el número
        val trackPattern = Regex("\\b(?:track|pista|piste)\\s*0*([1-9]\\d*)\\b", RegexOption.IGNORE_CASE)
        val trackMatch = trackPattern.find(cleanQ)
        val targetTrackNumber = trackMatch?.groupValues?.get(1)?.toIntOrNull()
        if (targetTrackNumber != null) {
            val clean = cleanQ.replace(trackPattern, "").trim().replace(Regex("\\s+"), " ")
            if (clean.isNotBlank()) queries.add(clean)
        }

        try {
            for (q in queries.distinct()) {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val url = "https://api.deezer.com/search?q=$encoded&limit=50"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val root = JSONObject(body)
                        val items = root.optJSONArray("data")
                        if (items != null && items.length() > 0) {
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
                                val tPos = item.optInt("track_position", 0)
                                val trackNumStr = if (tPos > 0) tPos.toString() else ""

                                var score = 93
                                if (targetTrackNumber != null && tPos == targetTrackNumber) {
                                    score = 99
                                }

                                results.add(
                                    OnlineCandidate(
                                        source = MetadataSource.DEEZER,
                                        title = title,
                                        artist = artistName,
                                        album = albumTitle,
                                        albumArtist = artistName,
                                        trackNumber = trackNumStr,
                                        durationMs = durationSec * 1000L,
                                        coverArtUrl = bestCover,
                                        matchScore = score
                                    )
                                )
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) break
            }
        } catch (e: Exception) {
            Log.e("OnlineRepository", "Deezer query error", e)
        }
        return@withContext results
    }

    suspend fun searchMusicBrainz(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://musicbrainz.org/ws/2/recording?query=$encoded&fmt=json&limit=20"
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
            Log.e("OnlineRepository", "MusicBrainz query error", e)
        }
        return@withContext results
    }

    suspend fun searchTheAudioDb(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.theaudiodb.com/api/v1/json/2/searchtrack.php?s=&t=$encoded"
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val tracks = root.optJSONArray("track") ?: return@withContext emptyList()
                for (i in 0 until minOf(tracks.length(), 10)) {
                    val t = tracks.getJSONObject(i)
                    val title = t.optString("strTrack", "")
                    val artist = t.optString("strArtist", "")
                    val album = t.optString("strAlbum", "")
                    val genre = t.optString("strGenre", "")
                    val trackNum = t.optString("intTrackNumber", "")
                    val thumb = t.optString("strTrackThumb", "").ifBlank { null }
                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.THEAUDIODB,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            trackNumber = trackNum,
                            coverArtUrl = thumb,
                            matchScore = 88
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OnlineRepository", "TheAudioDB query failed", e)
        }
        return@withContext results
    }

    suspend fun searchNetEase(query: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://music.163.com/api/search/get/web?csrf_token=&hlpretag=&hlposttag=&s=$encoded&type=1&offset=0&total=true&limit=10"
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: return@withContext emptyList()
                for (i in 0 until songs.length()) {
                    val s = songs.getJSONObject(i)
                    val title = s.optString("name", "")
                    val artists = s.optJSONArray("artists")
                    val artistName = if (artists != null && artists.length() > 0) artists.getJSONObject(0).optString("name", "") else ""
                    val albumObj = s.optJSONObject("album")
                    val albumName = albumObj?.optString("name", "") ?: ""
                    val picUrl = albumObj?.optString("picUrl", "")?.ifBlank { null }
                    val duration = s.optLong("duration", 0L)
                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.NETEASE,
                            title = title,
                            artist = artistName,
                            album = albumName,
                            coverArtUrl = picUrl,
                            durationMs = duration,
                            matchScore = 86
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OnlineRepository", "NetEase query failed", e)
        }
        return@withContext results
    }

    suspend fun searchSpotify(query: String, clientId: String, clientSecret: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineCandidate>()
        try {
            // 1. Get Access Token
            val authHeader = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
            val tokenReqBody = FormBody.Builder().add("grant_type", "client_credentials").build()
            val tokenReq = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", authHeader)
                .post(tokenReqBody)
                .build()

            var token = ""
            client.newCall(tokenReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val root = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                token = root.optString("access_token", "")
            }

            if (token.isBlank()) return@withContext emptyList()

            // 2. Search Tracks
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://api.spotify.com/v1/search?q=$encoded&type=track&limit=10"
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(searchReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val root = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                val items = root.optJSONObject("tracks")?.optJSONArray("items") ?: return@withContext emptyList()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val title = item.optString("name", "")
                    val artists = item.optJSONArray("artists")
                    val artistName = if (artists != null && artists.length() > 0) artists.getJSONObject(0).optString("name", "") else ""
                    val albumObj = item.optJSONObject("album")
                    val albumName = albumObj?.optString("name", "") ?: ""
                    val releaseDate = albumObj?.optString("release_date", "") ?: ""
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                    val images = albumObj?.optJSONArray("images")
                    val cover = if (images != null && images.length() > 0) images.getJSONObject(0).optString("url", null) else null
                    val trackNum = item.optInt("track_number", 0).let { if (it > 0) it.toString() else "" }
                    val duration = item.optLong("duration_ms", 0L)

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.SPOTIFY,
                            title = title,
                            artist = artistName,
                            album = albumName,
                            year = year,
                            trackNumber = trackNum,
                            coverArtUrl = cover,
                            durationMs = duration,
                            matchScore = 98
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OnlineRepository", "Spotify query failed", e)
        }
        return@withContext results
    }

    suspend fun searchLastFm(query: String, apiKey: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://ws.audioscrobbler.com/2.0/?method=track.search&track=$encoded&api_key=$apiKey&format=json&limit=10"
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val root = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                val trackList = root.optJSONObject("results")?.optJSONObject("trackmatches")?.optJSONArray("track") ?: return@withContext emptyList()
                for (i in 0 until trackList.length()) {
                    val t = trackList.getJSONObject(i)
                    val title = t.optString("name", "")
                    val artist = t.optString("artist", "")
                    val images = t.optJSONArray("image")
                    var cover: String? = null
                    if (images != null) {
                        for (j in 0 until images.length()) {
                            val img = images.getJSONObject(j)
                            val size = img.optString("size", "")
                            if (size == "extralarge" || size == "large") {
                                cover = img.optString("#text", null)
                            }
                        }
                    }
                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.LASTFM,
                            title = title,
                            artist = artist,
                            album = "",
                            coverArtUrl = cover?.ifBlank { null },
                            matchScore = 85
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OnlineRepository", "Last.fm query failed", e)
        }
        return@withContext results
    }

    suspend fun searchDiscogs(query: String, token: String): List<OnlineCandidate> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.discogs.com/database/search?q=$encoded&type=release&token=$token&per_page=10"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
        val results = mutableListOf<OnlineCandidate>()
        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val root = JSONObject(resp.body?.string() ?: return@withContext emptyList())
                val list = root.optJSONArray("results") ?: return@withContext emptyList()
                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    val rawTitle = item.optString("title", "")
                    val parts = rawTitle.split(" - ")
                    val artist = if (parts.size > 1) parts[0].trim() else ""
                    val title = if (parts.size > 1) parts.drop(1).joinToString(" - ").trim() else rawTitle
                    val year = item.optString("year", "")
                    val cover = item.optString("cover_image", "").ifBlank { null }
                    val genreArr = item.optJSONArray("genre")
                    val genre = if (genreArr != null && genreArr.length() > 0) genreArr.getString(0) else ""

                    results.add(
                        OnlineCandidate(
                            source = MetadataSource.DISCOGS,
                            title = title,
                            artist = artist,
                            album = title,
                            year = year,
                            genre = genre,
                            coverArtUrl = cover,
                            matchScore = 87
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("OnlineRepository", "Discogs query failed", e)
        }
        return@withContext results
    }

    suspend fun fetchCoverArtOptions(candidate: OnlineCandidate): List<CoverArtOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<CoverArtOption>()

        // 1. Direct candidate cover art
        if (!candidate.coverArtUrl.isNullOrBlank()) {
            options.add(
                CoverArtOption(
                    source = candidate.source.displayName,
                    url = candidate.coverArtUrl,
                    width = 1000,
                    height = 1000,
                    label = "Carátula de ${candidate.source.displayName}",
                    isSelected = true
                )
            )
        }

        // 2. Cover Art Archive for MusicBrainz
        val relMbid = candidate.releaseMbid
        if (!relMbid.isNullOrBlank()) {
            val caaUrl = "https://coverartarchive.org/release/$relMbid"
            val caaRequest = Request.Builder().url(caaUrl).header("User-Agent", userAgent).build()
            try {
                client.newCall(caaRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val caaRoot = JSONObject(body)
                            val images = caaRoot.optJSONArray("images")
                            if (images != null) {
                                for (i in 0 until images.length()) {
                                    val img = images.getJSONObject(i)
                                    val imgUrl = img.optString("image", "")
                                    val isFront = img.optBoolean("front", false)
                                    if (imgUrl.isNotBlank()) {
                                        options.add(
                                            CoverArtOption(
                                                source = "Cover Art Archive",
                                                url = imgUrl,
                                                label = if (isFront) "Portada Oficial (CAA Front)" else "CAA Imagen #$i",
                                                isSelected = options.isEmpty()
                                            )
                                        )
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
