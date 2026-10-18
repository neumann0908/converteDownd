package com.musicdownloader.converter.data.engine

import android.content.Context
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.musicdownloader.converter.converter.engine.AudioConverterEngine
import com.musicdownloader.converter.converter.engine.ConversionConfig
import com.musicdownloader.converter.converter.engine.ConversionStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class Media3AudioConverterEngine(
    private val context: Context
) : AudioConverterEngine {

    override fun convert(
        inputFile: File,
        outputFile: File,
        config: ConversionConfig
    ): Flow<ConversionStatus> = callbackFlow {
        if (!inputFile.exists()) {
            trySend(ConversionStatus.Failed(IllegalArgumentException("El archivo de origen no existe")))
            close()
            return@callbackFlow
        }

        outputFile.parentFile?.mkdirs()

        val mediaItem = androidx.media3.common.MediaItem.fromUri(inputFile.toURI())
        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
        val composition = Composition.Builder(androidx.media3.transformer.EditedMediaItemSequence(editedMediaItem)).build()

        val transformer = Transformer.Builder(context)
            .setAudioMimeType(config.targetFormat.mimeType)
            .build()

        try {
            transformer.start(composition, outputFile.absolutePath)
            
            // Nota: El progreso detallado en tiempo real se maneja mediante listener de Transformer en versiones compatibles
            trySend(ConversionStatus.Progress(50f)) // Progreso estimado de inicialización
            
        } catch (e: Exception) {
            trySend(ConversionStatus.Failed(e))
            close()
        }

        awaitClose {
            try {
                transformer.cancel()
            } catch (_: Exception) { }
        }
    }
}
