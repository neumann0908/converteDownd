package com.musicdownloader.converter.converter.engine

import android.content.Context
import android.util.Log
import java.io.File

object AssetAudioLoader {

    private const val TAG = "AssetAudioLoader"

    /**
     * Escanea la carpeta interna filesDir/kits/ y devuelve únicamente los archivos 
     * que coincidan con la categoría solicitada (ej. "kick", "snare", "hat").
     */
    fun getFilteredSamplesForCategory(context: Context, categoryKeyword: String): List<String> {
        return try {
            val kitsDir = File(context.filesDir, "kits")
            if (!kitsDir.exists()) {
                Log.w(TAG, "La carpeta interna de kits no existe en: ${kitsDir.absolutePath}")
                return emptyList()
            }

            // Recorre recursivamente o busca en las subcarpetas de kits
            val allFiles = mutableListOf<String>()
            kitsDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    // Guardamos la ruta relativa o el nombre para identificarlo
                    allFiles.add(file.absolutePath)
                }
            }
            
            // Filtra de forma inteligente ignorando mayúsculas/minúsculas según la palabra clave
            allFiles.filter { it.lowercase().contains(categoryKeyword.lowercase()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar y filtrar samples desde memoria interna: ${e.message}")
            emptyList()
        }
    }

    /**
     * Carga el sample apuntando directamente a su ruta absoluta en el almacenamiento interno.
     */
    fun loadSampleToPad(context: Context, absolutePath: String, padIndex: Int) {
        try {
            val file = File(absolutePath)
            if (!file.exists()) {
                Log.e(TAG, "El archivo de audio no existe en la ruta: $absolutePath")
                return
            }

            Log.i(TAG, "Solicitando carga para el archivo interno: ${file.name} en el pad: $padIndex")

            // Llamada al motor nativo usando la ruta absoluta del archivo extraído
            val audioEngine = AudioEngine()
            // Nota: Si tu función nativa en C++ espera un descriptor de archivo o ruta de archivo (fd / filepath),
            // puedes pasarle file.absolutePath directamente si el motor soporta lectura por ruta.
            val success = audioEngine.nativeLoadSampleByPath(file.absolutePath, padIndex)

            if (!success) {
                Log.e(TAG, "Fallo al cargar el sample nativamente desde ruta: ${file.absolutePath}")
            } else {
                Log.i(TAG, "¡Sample interno cargado con éxito en el pad $padIndex!")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica en loadSampleToPad: ${e.message}")
            e.printStackTrace()
        }
    }
}
