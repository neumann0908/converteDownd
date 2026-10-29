package com.musicdownloader.converter.converter.engine

import android.content.res.AssetManager

class AudioEngine {

    companion object {
        init {
            // Carga la librería compartida nativa compilada mediante CMakeLists.txt (add_library("musicengine"))
            System.loadLibrary("musicengine")
        }
    }

    /**
     * Inicia el flujo de audio de ultra baja latencia utilizando Google Oboe en C++.
     * @return true si el stream abrió correctamente, false si hubo algún fallo en el hardware de audio.
     */
    external fun nativeStart(): Boolean

    /**
     * Dispara un pad específico de forma instantánea a ultra baja latencia.
     */
    external fun nativePlayPad(padIndex: Int)

    /**
     * Carga de forma nativa un asset de audio directamente mediante el AssetManager de Android.
     * @param assetManager Gestor de assets de la aplicación.
     * @param filename Nombre o ruta relativa del archivo de audio dentro de kits/.
     * @return true si el archivo se abrió y cargó con éxito en el motor nativo, false en caso contrario.
     */
    external fun nativeLoadSample(assetManager: AssetManager, filename: String): Boolean
}
