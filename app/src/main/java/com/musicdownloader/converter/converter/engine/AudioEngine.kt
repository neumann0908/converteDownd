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
     * Detiene y cierra el stream de Oboe de forma limpia para liberar los recursos de hardware de audio.
     */
    external fun nativeStop()

    /**
     * Pausa la reproducción aplicando un desvanecimiento suave (fade-out) en todas las voces activas 
     * para eliminar chasquidos o ruidos parásitos.
     */
    external fun nativePause()

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

    /**
     * Configura en tiempo real los parámetros de audio (Pitch, Pan, Volumen) para un pad específico.
     */
    external fun nativeSetPadParams(padIndex: Int, pitch: Float, pan: Float, volume: Float)

    /**
     * Renderiza y exporta un pad o pista a un archivo físico .wav en el almacenamiento.
     */
    external fun nativeExportWavTest(outputPath: String, padIndex: Int): Boolean
}
