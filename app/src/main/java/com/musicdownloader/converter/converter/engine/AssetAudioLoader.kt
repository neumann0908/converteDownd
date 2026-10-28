package com.musicdownloader.converter.converter.engine

import android.content.Context
import java.io.DataInputStream

object AssetAudioLoader {

    /**
     * Escanea la carpeta assets/kits/ y devuelve únicamente los archivos 
     * que coincidan con la categoría solicitada (ej. "kick", "snare", "hat").
     */
    fun getFilteredSamplesForCategory(context: Context, categoryKeyword: String): List<String> {
        return try {
            val assetManager = context.assets
            val allFiles = assetManager.list("kits")?.toList() ?: emptyList()
            
            // Filtra de forma inteligente ignorando mayúsculas/minúsculas
            allFiles.filter { it.lowercase().contains(categoryKeyword.lowercase()) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Carga un archivo de audio PCM/WAV crudo desde la carpeta assets/kits/ y lo inyecta en el pad nativo.
     */
    fun loadSampleToPad(context: Context, assetPath: String, padIndex: Int) {
        try {
            val assetManager = context.assets
            // Verificación cruzada: Asegura que la ruta apunte correctamente al directorio kits/
            val fullPath = if (assetPath.startsWith("kits/")) assetPath else "kits/$assetPath"
            
            val inputStream = assetManager.open(fullPath)
            val dataInputStream = DataInputStream(inputStream)
            
            // Leemos los bytes y los convertimos a FloatArray (formato que exige Oboe Float)
            val bytes = dataInputStream.readBytes()
            dataInputStream.close()

            // Conversión básica de bytes PCM 16-bit a Float (-1.0f a 1.0f)
            val floatSize = bytes.size / 2
            val floatData = FloatArray(floatSize)
            
            for (i in 0 until floatSize) {
                val byte1 = bytes[i * 2].toInt() and 0xFF
                val byte2 = bytes[i * 2 + 1].toInt()
                val shortVal = (byte2 shl 8) or byte1
                floatData[i] = shortVal.toFloat() / 32768.0f
            }

            // Enviamos los datos procesados al motor nativo en C++
            val audioEngine = AudioEngine()
            audioEngine.nativeLoadSample(padIndex, floatData)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

