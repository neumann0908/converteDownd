package com.musicdownloader.converter.converter.engine

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
     * Carga un arreglo de muestras PCM flotantes en un índice de pad determinado.
     */
    external fun nativeLoadSample(padIndex: Int, pcmData: FloatArray)
}
