package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

enum class GeminiPromptType(val displayName: String) {
    LYRICS("Buscar / Formatear Letra"),
    COVER_OCR("Identificar Metadatos desde Carátula"),
    METADATA_DISAMBIGUATION("Resolver Metadatos desde Nombre de Archivo")
}

object GeminiPromptHelper {

    fun buildLyricsPrompt(title: String, artist: String, album: String): String {
        val songDesc = buildString {
            if (title.isNotBlank()) append("Título: \"$title\"\n")
            if (artist.isNotBlank()) append("Artista: \"$artist\"\n")
            if (album.isNotBlank()) append("Álbum: \"$album\"\n")
        }.trim()

        return """
            Actúa como un catalogador y transcriptor musical profesional y estricto.
            Información de la canción:
            $songDesc

            Instrucciones obligatorias de formato:
            1. Proporciona la letra oficial y completa de esta canción.
            2. Si dispones de las marcas de tiempo precisas, formátala en formato LRC sincronizado ([mm:ss.xx] texto). Si no, entrega la letra limpia en texto plano separada por estrofas.
            3. NO incluyas introducciones, saludos ni comentarios (NUNCA digas "Aquí tienes la letra:" o similar).
            4. NO encierres la letra en bloques de código markdown con comillas triples (```).
            5. Entrega exclusivamente las líneas de la letra, lista para ser incrustada directamente en el archivo de audio.
        """.trimIndent()
    }

    fun buildCoverOcrPrompt(fileName: String, existingTitle: String = "", existingArtist: String = "", existingAlbum: String = ""): String {
        val extra = buildString {
            if (existingTitle.isNotBlank()) append("Pista tentativa: $existingTitle\n")
            if (existingArtist.isNotBlank()) append("Artista tentativo: $existingArtist\n")
            if (existingAlbum.isNotBlank()) append("Álbum tentativo: $existingAlbum\n")
        }.trim()

        return """
            Actúa como un catalogador musical y experto en análisis tipográfico de portadas discográficas.
            Archivo: "$fileName"
            ${if (extra.isNotBlank()) "Pistas contextuales:\n$extra\n" else ""}
            Instrucciones de análisis:
            1. Analiza minuciosamente el arte visual y todos los textos en la imagen adjunta (tipografía estilizada, logotipos, títulos, sellos discográficos).
            2. Identifica con la mayor precisión posible los metadatos de la canción.
            3. Devuelve el resultado en este formato exacto para copiar y pegar:

            Título: [Nombre exacto de la canción]
            Artista: [Nombre oficial del artista principal]
            Álbum: [Nombre del álbum o EP]
            Año: [Año de lanzamiento]
            Género: [Género musical]
            Pista: [Número de pista si es visible]
        """.trimIndent()
    }

    fun buildDisambiguationPrompt(fileName: String, existingTags: Map<String, String> = emptyMap()): String {
        val tagsFormatted = existingTags.filterValues { it.isNotBlank() }
            .entries.joinToString("\n") { "- ${it.key}: ${it.value}" }

        return """
            Actúa como un archivista musical y experto en resolver archivos de audio con nombres o etiquetas corrompidas.
            Nombre del archivo: "$fileName"
            ${if (tagsFormatted.isNotBlank()) "Etiquetas actuales en el archivo:\n$tagsFormatted\n" else ""}
            Instrucciones:
            1. Infiere la canción, artista, álbum y año exactos a partir del nombre del archivo y las pistas de texto disponibles.
            2. Devuelve los metadatos oficiales normalizados en este formato:

            Título: [Título limpio]
            Artista: [Artista oficial]
            Álbum: [Álbum oficial]
            Año: [Año de lanzamiento]
            Género: [Género]
        """.trimIndent()
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Prompt de Gemini", showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "Prompt copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGeminiWeb(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareTextPrompt(context: Context, prompt: String, chooserTitle: String = "Consultar con Gemini...") {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, prompt)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir menú de compartir", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCoverWithPrompt(
        context: Context,
        prompt: String,
        coverBytes: ByteArray,
        chooserTitle: String = "Analizar Carátula con Gemini..."
    ) {
        try {
            val cacheFolder = File(context.cacheDir, "shared_covers").apply { mkdirs() }
            val coverFile = File(cacheFolder, "cover_to_analyze.jpg")
            FileOutputStream(coverFile).use { it.write(coverBytes) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                coverFile
            )

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, prompt)
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback to sharing text prompt if FileProvider or image write fails
            shareTextPrompt(context, prompt, chooserTitle)
        }
    }
}
