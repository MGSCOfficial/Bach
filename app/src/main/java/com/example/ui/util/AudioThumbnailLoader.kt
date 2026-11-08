package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.example.data.tagger.AudioTagReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de miniaturas para pistas de audio con caché LRU en memoria.
 * Decodifica carátulas incrustadas en un hilo de fondo (IO) y las redimensiona
 * a un tamaño de miniatura (128x128 max) con formato RGB_565 para mantener el uso de RAM al mínimo.
 */
object AudioThumbnailLoader {

    // Caché LRU en memoria con límite de 200 miniaturas (~6 MB máx en RAM)
    private val memoryCache = object : LruCache<String, Bitmap>(200) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun loadThumbnail(context: Context, fileUri: String): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(fileUri)?.let { return@withContext it }

        try {
            val uri = Uri.parse(fileUri)
            val bytes = AudioTagReader.loadEmbeddedPicture(context, uri) ?: return@withContext null

            // 1. Obtener dimensiones sin cargar bitmap completo en RAM
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val targetDimension = 140
            var sampleSize = 1
            if (boundsOptions.outHeight > targetDimension || boundsOptions.outWidth > targetDimension) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / sampleSize) >= targetDimension && (halfWidth / sampleSize) >= targetDimension) {
                    sampleSize *= 2
                }
            }

            // 2. Decodificar escalado usando RGB_565 (50% menos consumo de RAM que ARGB_8888)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = maxOf(1, sampleSize)
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            if (bitmap != null) {
                memoryCache.put(fileUri, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun invalidate(fileUri: String) {
        memoryCache.remove(fileUri)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
