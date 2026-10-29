package com.musicdownloader.converter.converter.engine

object AudioEngine {

    init {
        // Carga la librería compartida nativa compilada mediante CMakeLists.txt (add_library("musicengine"))
        System.loadLibrary("musicengine")
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
     * Carga de forma nativa un sample de audio utilizando su ruta absoluta en el almacenamiento interno.
     * @param filePath Ruta absoluta del archivo de audio en la memoria interna (filesDir/kits/).
     * @param padIndex Índice del pad donde se asignará el sample.
     * @return true si el archivo se abrió y cargó con éxito en el motor nativo, false en caso contrario.
     */
    external fun nativeLoadSampleByPath(filePath: String, padIndex: Int): Boolean
}
