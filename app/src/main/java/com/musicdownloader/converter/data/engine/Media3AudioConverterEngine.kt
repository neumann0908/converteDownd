package com.musicdownloader.converter.data.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3AudioConverterEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun convertAudio(inputPath: String, outputPath: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputUri = Uri.parse(inputPath)
            val mediaItem = MediaItem.fromUri(inputUri)
            
            val transformer = Transformer.Builder(context)
                .setAudioMimeType("audio/mp4a-latm")
                .build()

            val outputFile = File(outputPath)
            if (outputFile.exists()) {
                outputFile.delete()
            }

            transformer.start(mediaItem, outputFile.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
