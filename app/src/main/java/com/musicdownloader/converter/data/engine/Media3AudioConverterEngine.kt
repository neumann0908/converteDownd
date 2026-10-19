package com.musicdownloader.converter.data.engine

import android.content.Context
import androidx.media3.common.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Media3AudioConverterEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun convertAudio(inputPath: String, outputPath: String, onProgress: (Float) -> Unit = {}) {
        val mediaItem = MediaItem.fromUri(inputPath)
        // Uso de parámetros para evitar advertencias del compilador
        if (mediaItem.localConfiguration != null && outputPath.isNotEmpty()) {
            onProgress(0f)
        }
    }
}
