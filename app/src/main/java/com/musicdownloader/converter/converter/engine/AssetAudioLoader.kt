package com.musicdownloader.converter.converter.engine

import android.content.res.AssetManager
import android.content.Context
import android.util.Log

object AssetAudioLoader {

    private const val TAG = "AssetAudioLoader"

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
            Log.e(TAG, "Error al listar y filtrar samples: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delega la carga del sample directamente al motor nativo en C++ utilizando 
     * el AssetManager para un rendimiento óptimo de ultra baja latencia con Oboe.
     */
    fun loadSampleToPad(context: Context, assetPath: String, padIndex: Int) {
        try {
            val assetManager = context.assets
            
            // Normalizamos la ruta: eliminamos el prefijo "kits/" si ya lo trae, 
            // ya que el motor C++ se encarga de concatenarlo de forma segura.
            val cleanFilename = if (assetPath.startsWith("kits/")) {
                assetPath.removePrefix("kits/")
            } else {
                assetPath
            }
            
            Log.i(TAG, "Solicitando carga nativa para el archivo: $cleanFilename en el pad: $padIndex")

            // Llamada al puente nativo optimizado
            val audioEngine = AudioEngine()
            val success = audioEngine.nativeLoadSample(assetManager, cleanFilename)

            if (!success) {
                Log.e(TAG, "Fallo al cargar el sample nativamente: $cleanFilename")
            } else {
                Log.i(TAG, "¡Sample cargado con éxito en el pad $padIndex!")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica en loadSampleToPad: ${e.message}")
            e.printStackTrace()
        }
    }
}
