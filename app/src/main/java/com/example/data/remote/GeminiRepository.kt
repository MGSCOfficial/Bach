package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.data.model.LyricsResult
import com.example.data.model.MetadataSource
import com.example.data.model.OnlineCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiModelOption(
    val id: String,
    val displayName: String
)

class GeminiRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "GeminiRepository"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val FALLBACK_MODELS = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash"
        )

        fun resolveModel(model: String): String = when (model.trim()) {
            "gemini-2.5-flash", "gemini-1.5-flash", "gemini-2.0-flash" -> "gemini-3.5-flash-lite"
            "gemini-2.5-pro", "gemini-1.5-pro" -> "gemini-3.5-flash"
            else -> model.trim().ifBlank { "gemini-3.5-flash-lite" }
        }
    }

    private fun optimizeImageForVision(rawBytes: ByteArray, maxDimension: Int = 512): ByteArray {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            var inSampleSize = 1
            val maxSide = maxOf(options.outWidth, options.outHeight)
            while (maxSide / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions) ?: return rawBytes

            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
                val newWidth = (width * ratio).toInt()
                val newHeight = (height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            val out = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            if (scaledBitmap != bitmap) bitmap.recycle()
            scaledBitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Image optimization failed, using original bytes", e)
            rawBytes
        }
    }

    private suspend fun executeWithModelCascade(
        preferredModel: String,
        apiKey: String,
        buildRequestBody: () -> String
    ): Pair<String, String> {
        val targetModels = mutableListOf<String>()
        val primary = resolveModel(preferredModel)
        targetModels.add(primary)
        for (fb in FALLBACK_MODELS) {
            if (fb != primary && !targetModels.contains(fb)) {
                targetModels.add(fb)
            }
        }

        var lastErrorMsg = "No se pudo conectar con el servicio de Gemini."

        for (modelName in targetModels) {
            val endpoint = "$BASE_URL/$modelName:generateContent?key=${apiKey.trim()}"
            val requestBodyStr = buildRequestBody()
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyStr.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            for (attempt in 1..2) {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        return Pair(responseBody, modelName)
                    }

                    val code = response.code
                    lastErrorMsg = parseErrorMessage(code, responseBody)
                    Log.w(TAG, "[$modelName] attempt $attempt returned HTTP $code: $lastErrorMsg")

                    if (code == 503 || code == 429) {
                        Log.w(TAG, "[$modelName] server overloaded ($code), cascading immediately...")
                        break
                    } else if (code == 404) {
                        break
                    } else if (code == 403 || code == 400) {
                        throw Exception(lastErrorMsg)
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("403") == true || e.message?.contains("400") == true) {
                        throw e
                    }
                    Log.w(TAG, "[$modelName] attempt $attempt threw exception: ${e.message}")
                    lastErrorMsg = e.message ?: "Error de red con Gemini"
                    if (e is java.net.SocketTimeoutException) {
                        Log.w(TAG, "[$modelName] timeout encountered, cascading immediately...")
                        break
                    }
                    if (attempt == 1) {
                        delay(1000)
                    }
                }
            }
            Log.i(TAG, "Cascading from $modelName to next available model...")
        }

        throw Exception(lastErrorMsg)
    }

    suspend fun analyzeCoverImage(
        coverBytes: ByteArray,
        apiKey: String,
        model: String = "gemini-3.5-flash-lite",
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
            val optimizedBytes = optimizeImageForVision(coverBytes, maxDimension = 512)
            val base64Image = Base64.encodeToString(optimizedBytes, Base64.NO_WRAP)

            val (responseBody, _) = executeWithModelCascade(model, apiKey) {
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
                requestJson.toString()
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
        model: String = "gemini-3.5-flash-lite"
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
            append("Transcribe o proporciona la letra completa y real de la canción \"$title\" ")
            if (artist.isNotBlank()) append("del artista o artistas \"$artist\" ")
            if (album.isNotBlank()) append("(álbum o proyecto \"$album\") ")
            append(".\n\n")
            append("Instrucciones obligatorias:\n")
            append("1. Si la canción es un lanzamiento reciente, single colaborativo o filtración popular (como temas de 2024 a 2026), proporciona las líneas líricas reales que se cantan en la canción sin negarte ni emitir disclaimers discográficos.\n")
            append("2. Si tienes las marcas de tiempo precisas, entrégala en formato LRC sincronizado ([mm:ss.xx] texto). Si no, entrégala en texto plano estructurado en estrofas.\n")
            append("3. REGLA ESTRICTA: NO incluyas saludos, introducciones, disclaimers ni comentarios adicionales (NUNCA digas \"Aquí tienes la letra:\" o similar).\n")
            append("4. NO encierres la letra en bloques markdown con comillas triples (```).\n")
            append("5. Entrega únicamente las líneas directas de la letra.")
        }

        try {
            val (responseBody, usedModel) = executeWithModelCascade(model, apiKey) {
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
                requestJson.toString()
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
                    source = "Google Gemini ($usedModel)"
                )
            } else {
                LyricsResult(
                    trackName = title,
                    artistName = artist,
                    plainLyrics = cleanedText,
                    syncedLyrics = null,
                    source = "Google Gemini ($usedModel)"
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
        model: String = "gemini-3.5-flash-lite"
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
            val (responseBody, _) = executeWithModelCascade(model, apiKey) {
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
                requestJson.toString()
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

    suspend fun fetchAvailableModels(apiKey: String): Result<List<GeminiModelOption>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Clave API de Gemini vacía"))
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = parseErrorMessage(response.code, response.body?.string().orEmpty())
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body?.string().orEmpty()
                val root = JSONObject(body)
                val arr = root.optJSONArray("models") ?: JSONArray()
                val list = mutableListOf<GeminiModelOption>()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val rawName = m.optString("name", "")
                    val modelId = rawName.removePrefix("models/")
                    val displayName = m.optString("displayName", modelId)
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val supportsGen = methods?.let { mArr ->
                        (0 until mArr.length()).any { mArr.optString(it) == "generateContent" }
                    } ?: false

                    if (supportsGen && (modelId.contains("flash", ignoreCase = true) || modelId.contains("pro", ignoreCase = true))) {
                        list.add(GeminiModelOption(id = modelId, displayName = displayName))
                    }
                }
                val sorted = list.sortedBy { opt ->
                    when {
                        opt.id.contains("flash-lite", ignoreCase = true) -> 0
                        opt.id.contains("flash", ignoreCase = true) -> 1
                        else -> 2
                    }
                }
                Result.success(sorted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar modelos de Gemini", e)
            Result.failure(e)
        }
    }

    suspend fun identifyTrackFromAudio(
        audioBytes: ByteArray,
        audioMimeType: String,
        apiKey: String,
        model: String = "gemini-3.5-flash-lite"
    ): Result<OnlineCandidate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Clave API de Gemini no configurada."))
        }
        if (audioBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("El archivo de audio está vacío."))
        }

        val prompt = buildString {
            append("Eres un sistema de reconocimiento acústico musical experto.\n")
            append("Escucha atentamente el audio adjunto e identifica la canción.\n")
            append("Responde EXCLUSIVAMENTE con un objeto JSON válido con las siguientes claves:\n")
            append("\"title\", \"artist\", \"album\", \"year\", \"genre\", \"trackNumber\".\n")
            append("Si algún dato no es identificable con certeza, déjalo como cadena vacía (\"\").")
        }

        try {
            val (responseBody, _) = executeWithModelCascade(model, apiKey) {
                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", audioMimeType)
                                        put("data", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
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
                requestJson.toString()
            }

            val textOutput = extractCandidateText(responseBody)
            val parsed = JSONObject(textOutput)

            val candidate = OnlineCandidate(
                source = MetadataSource.GEMINI,
                title = parsed.optString("title", "").trim(),
                artist = parsed.optString("artist", "").trim(),
                album = parsed.optString("album", "").trim(),
                year = parsed.optString("year", "").trim(),
                genre = parsed.optString("genre", "").trim(),
                trackNumber = parsed.optString("trackNumber", "").trim(),
                matchScore = 95
            )

            Result.success(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error en identifyTrackFromAudio", e)
            Result.failure(e)
        }
    }

    suspend fun synchronizeLyricsWithAudio(
        audioBytes: ByteArray,
        audioMimeType: String,
        plainLyrics: String,
        apiKey: String,
        model: String = "gemini-3.5-flash-lite"
    ): Result<LyricsResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Clave API de Gemini no configurada."))
        }
        if (audioBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("El archivo de audio está vacío."))
        }
        if (plainLyrics.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No hay letra para sincronizar."))
        }

        val prompt = buildString {
            append("Eres un sincronizador y transcriptor de audio profesional experto en formato LRC sincronizado.\n")
            append("Escucha atentamente el audio provisto.\n")
            append("A continuación se presenta la letra base de la canción:\n\n")
            append("--- LETRA BASE ---\n")
            append(plainLyrics.trim())
            append("\n--- FIN LETRA BASE ---\n\n")
            append("INSTRUCCIONES OBLIGATORIAS:\n")
            append("1. Escucha la voz y la música en el audio y asigna la marca de tiempo exacta [mm:ss.xx] al inicio exacto de cada verso donde el vocalista canta o rapea la frase.\n")
            append("2. Conserva íntegramente las palabras y el idioma de la letra base provista.\n")
            append("3. REGLA ESTRICTA DE SALIDA: Devuelve ÚNICAMENTE las líneas de texto en formato LRC estándar ([mm:ss.xx] verso).\n")
            append("4. NO incluyas introducciones, comentarios, disclaimers, notas ni bloques de código markdown (```).")
        }

        try {
            val (responseBody, usedModel) = executeWithModelCascade(model, apiKey) {
                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", audioMimeType)
                                        put("data", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
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
                    }
                    put("generationConfig", genConfig)
                }
                requestJson.toString()
            }

            val textOutput = extractCandidateText(responseBody).trim()
            val cleanLrc = textOutput
                .replace("```lrc", "")
                .replace("```", "")
                .trim()

            val result = LyricsResult(
                trackName = "",
                artistName = "",
                plainLyrics = plainLyrics,
                syncedLyrics = cleanLrc,
                source = "Google Gemini ($usedModel • Sincronización de Audio)"
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error en synchronizeLyricsWithAudio", e)
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
