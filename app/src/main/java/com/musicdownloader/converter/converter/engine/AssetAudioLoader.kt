package com.musicdownloader.converter.converter.engine

import android.content.Context
import android.util.Log
import java.io.File

object AssetAudioLoader {

    private const val TAG = "AssetAudioLoader"

    /**
     * Escanea la carpeta interna filesDir/kits/ recursivamente y devuelve los archivos .wav 
     * que coincidan con la categoría solicitada. Si el filtro estricto no halla coincidencias,
     * actúa como red de seguridad devolviendo todos los audios disponibles.
     */
    fun getFilteredSamplesForCategory(context: Context, categoryKeyword: String): List<String> {
        return try {
            val kitsDir = File(context.filesDir, "kits")
            if (!kitsDir.exists()) {
                Log.w(TAG, "La carpeta interna de kits no existe en: ${kitsDir.absolutePath}")
                return emptyList()
            }

            val allWavFiles = mutableListOf<String>()
            
            // Recorrido recursivo profundo buscando exclusivamente archivos de audio .wav
            kitsDir.walkTopDown().forEach { file ->
                if (file.isFile && file.name.lowercase().endsWith(".wav")) {
                    allWavFiles.add(file.absolutePath)
                }
            }

            if (allWavFiles.isEmpty()) {
                Log.w(TAG, "No se encontraron archivos .wav en el directorio kits/")
                return emptyList()
            }
            
            // Intento de filtrado inteligente por palabra clave (ej. "kick", "snare")
            val filtered = allWavFiles.filter { it.lowercase().contains(categoryKeyword.lowercase()) }
            
            // 🛡️ Red de seguridad infalible: Si la categoría no coincide exactamente con ningún nombre,
            // devolvemos todos los .wav disponibles para que el usuario pueda elegirlos libremente sin bloqueos.
            if (filtered.isEmpty()) {
                Log.i(TAG, "Filtro estricto para '$categoryKeyword' sin resultados. Aplicando respaldo con todos los .wav disponibles.")
                return allWavFiles
            }

            filtered
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar y filtrar samples desde memoria interna: ${e.message}")
            emptyList()
        }
    }

    /**
     * Carga el sample apuntando directamente a su ruta absoluta en el almacenamiento interno en el motor Oboe.
     */
    fun loadSampleToPad(context: Context, absolutePath: String, padIndex: Int) {
        try {
            val file = File(absolutePath)
            if (!file.exists()) {
                Log.e(TAG, "El archivo de audio no existe en la ruta: $absolutePath")
                return
            }

            Log.i(TAG, "Solicitando carga para el archivo interno: ${file.name} en el pad: $padIndex")

            val audioEngine = AudioEngine()
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
