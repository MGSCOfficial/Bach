package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.data.model.LyricsResult
import com.example.data.model.MetadataSource
import com.example.data.model.OnlineCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "GeminiRepository"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun resolveModel(model: String): String = when (model.trim()) {
            "gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash" -> "gemini-3.8-flash"
            "gemini-2.5-pro", "gemini-1.5-pro" -> "gemini-3.8-pro"
            else -> model.trim().ifBlank { "gemini-3.8-flash" }
        }
    }

    suspend fun analyzeCoverImage(
        coverBytes: ByteArray,
        apiKey: String,
        model: String = "gemini-3.8-flash",
        hintTitle: String = "",
        hintArtist: String = ""
    ): Result<OnlineCandidate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Clave API de Gemini no configurada. Añádela en Ajustes.")
            )
        }
        if (coverBytes.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No hay carátula incrustada para analizar.")
            )
        }

        val prompt = buildString {
            append("Eres un catalogador musical y experto en tipografía y diseño discográfico. ")
            append("Analiza minuciosamente el arte visual y todos los textos en esta carátula. ")
            append("Identifica el título exacto de la canción o álbum, artista principal, álbum, año de lanzamiento, género musical y número de pista si es visible. ")
            if (hintTitle.isNotBlank() || hintArtist.isNotBlank()) {
                append("Pistas tentativas en el archivo: ")
                if (hintTitle.isNotBlank()) append("Título tentative=\"$hintTitle\" ")
                if (hintArtist.isNotBlank()) append("Artista tentative=\"$hintArtist\" ")
            }
            append("Responde EXCLUSIVAMENTE con un JSON válido con estas claves: ")
            append("\"title\", \"artist\", \"album\", \"year\", \"genre\", \"trackNumber\". ")
            append("Si algún dato no es deducible, déjalo como cadena vacía (\"\").")
        }

        try {
            val base64Image = Base64.encodeToString(coverBytes, Base64.NO_WRAP)

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "$BASE_URL/${resolveModel(model)}:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.w(TAG, "Gemini vision failed: HTTP ${response.code} -> $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val textOutput = extractCandidateText(responseBody)
            if (textOutput.isBlank()) {
                return@withContext Result.failure(Exception("Gemini no devolvió texto en su respuesta."))
            }

            val parsed = JSONObject(textOutput)
            val title = parsed.optString("title", "").trim()
            val artist = parsed.optString("artist", "").trim()
            val album = parsed.optString("album", "").trim()
            val year = parsed.optString("year", "").trim()
            val genre = parsed.optString("genre", "").trim()
            val trackNumber = parsed.optString("trackNumber", "").trim()

            val candidate = OnlineCandidate(
                source = MetadataSource.GEMINI,
                title = if (title.isNotBlank()) title else hintTitle.ifBlank { "Desconocido" },
                artist = if (artist.isNotBlank()) artist else hintArtist.ifBlank { "Desconocido" },
                album = album,
                year = year,
                genre = genre,
                trackNumber = trackNumber,
                matchScore = 98
            )

            Result.success(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeCoverImage", e)
            Result.failure(e)
        }
    }

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        apiKey: String,
        model: String = "gemini-3.8-flash"
    ): Result<LyricsResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Clave API de Gemini no configurada. Añádela en Ajustes.")
            )
        }
        if (title.isBlank() && artist.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Se requiere título o artista para buscar la letra.")
            )
        }

        val prompt = buildString {
            append("Actúa como un catalogador musical y transcriptor profesional. ")
            append("Proporciona la letra oficial y completa de la canción \"$title\" ")
            if (artist.isNotBlank()) append("del artista \"$artist\" ")
            if (album.isNotBlank()) append("(álbum \"$album\") ")
            append(".\n\n")
            append("Instrucciones obligatorias:\n")
            append("1. Si tienes las marcas de tiempo precisas, entrégala en formato LRC sincronizado ([mm:ss.xx] texto). Si no, entrégala en texto plano estructurado en estrofas.\n")
            append("2. REGLA ESTRICTA: NO incluyas saludos, introducciones ni comentarios adicionales (NUNCA digas \"Aquí tienes la letra:\" o similar).\n")
            append("3. NO encierres la letra en bloques markdown con comillas triples (```).\n")
            append("4. Entrega únicamente las líneas directas de la letra.")
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "$BASE_URL/${resolveModel(model)}:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.w(TAG, "Gemini lyrics failed: HTTP ${response.code} -> $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val rawText = extractCandidateText(responseBody)
            val cleanedText = rawText
                .replace(Regex("^```[a-zA-Z]*\\s*\\n?"), "")
                .replace(Regex("\\n?\\s*```$"), "")
                .trim()

            if (cleanedText.isBlank()) {
                return@withContext Result.failure(Exception("Gemini devolvió una letra vacía."))
            }

            val isSynced = cleanedText.contains(Regex("\\[\\d{2}:\\d{2}"))
            val lyricsResult = if (isSynced) {
                LyricsResult(
                    trackName = title,
                    artistName = artist,
                    plainLyrics = null,
                    syncedLyrics = cleanedText,
                    source = "Google Gemini ($model)"
                )
            } else {
                LyricsResult(
                    trackName = title,
                    artistName = artist,
                    plainLyrics = cleanedText,
                    syncedLyrics = null,
                    source = "Google Gemini ($model)"
                )
            }

            Result.success(lyricsResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchLyrics", e)
            Result.failure(e)
        }
    }

    suspend fun resolveMetadataFromText(
        fileName: String,
        existingTags: Map<String, String>,
        apiKey: String,
        model: String = "gemini-3.8-flash"
    ): Result<OnlineCandidate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Clave API de Gemini no configurada. Añádela en Ajustes.")
            )
        }

        val prompt = buildString {
            append("Eres un archivista musical experto en resolver canciones con etiquetas o nombres de archivo corrompidos.\n")
            append("Nombre de archivo: \"$fileName\"\n")
            if (existingTags.isNotEmpty()) {
                append("Etiquetas actuales en el archivo:\n")
                existingTags.filterValues { it.isNotBlank() }.forEach { (k, v) ->
                    append("- $k: $v\n")
                }
            }
            append("\nInfiere y normaliza el título oficial, artista principal, álbum, año de lanzamiento, género musical y número de pista.\n")
            append("Responde EXCLUSIVAMENTE con un JSON válido con estas claves: ")
            append("\"title\", \"artist\", \"album\", \"year\", \"genre\", \"trackNumber\". ")
            append("Si algún dato no es deducible, déjalo como cadena vacía (\"\").")
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "$BASE_URL/${resolveModel(model)}:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.w(TAG, "Gemini text disambiguation failed: HTTP ${response.code} -> $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val textOutput = extractCandidateText(responseBody)
            val parsed = JSONObject(textOutput)

            val candidate = OnlineCandidate(
                source = MetadataSource.GEMINI,
                title = parsed.optString("title", "").trim().ifBlank { fileName.substringBeforeLast(".") },
                artist = parsed.optString("artist", "").trim().ifBlank { "Desconocido" },
                album = parsed.optString("album", "").trim(),
                year = parsed.optString("year", "").trim(),
                genre = parsed.optString("genre", "").trim(),
                trackNumber = parsed.optString("trackNumber", "").trim(),
                matchScore = 95
            )

            Result.success(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error in resolveMetadataFromText", e)
            Result.failure(e)
        }
    }

    private fun extractCandidateText(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun parseErrorMessage(code: Int, responseBody: String): String {
        val defaultMsg = when (code) {
            400 -> "Solicitud inválida a Gemini (400)"
            403 -> "Clave API de Gemini inválida o sin permisos (403)"
            429 -> "Límite de cuota excedido en Google AI Studio (429)"
            500 -> "Error interno en los servidores de Google Gemini (500)"
            503 -> "Servicio de Gemini no disponible temporalmente (503)"
            else -> "Error de red con Gemini ($code)"
        }

        return try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            val serverMessage = error?.optString("message").orEmpty()
            if (serverMessage.isNotBlank()) {
                "$defaultMsg: $serverMessage"
            } else {
                defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }
}
