package com.musicdownloader.converter.converter.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

object AssetAudioLoader {

    private const val TAG = "AssetAudioLoader"

    /**
     * Escanea la carpeta interna filesDir/kits/ recursivamente y busca archivos de audio (.ogg o .wav).
     */
    fun getFilteredSamplesForCategory(context: Context, categoryKeyword: String): List<String> {
        return try {
            val kitsDir = File(context.filesDir, "kits")
            if (!kitsDir.exists()) {
                Log.w(TAG, "La carpeta interna de kits no existe en: ${kitsDir.absolutePath}")
                return emptyList()
            }

            val allAudioFiles = mutableListOf<String>()
            
            // Búsqueda recursiva profunda de .ogg y .wav
            kitsDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val nameLower = file.name.lowercase()
                    if (nameLower.endsWith(".ogg") || nameLower.endsWith(".wav")) {
                        allAudioFiles.add(file.absolutePath)
                    }
                }
            }

            if (allAudioFiles.isEmpty()) {
                Log.w(TAG, "No se encontraron archivos de audio (.ogg / .wav) en kits/")
                return emptyList()
            }
            
            // Filtrado inteligente por palabra clave
            val filtered = allAudioFiles.filter { it.lowercase().contains(categoryKeyword.lowercase()) }
            
            // 🛡️ Red de seguridad: Si la categoría no coincide, devolvemos todos los audios disponibles
            if (filtered.isEmpty()) {
                Log.i(TAG, "Filtro para '$categoryKeyword' sin resultados. Respaldo con todos los audios.")
                return allAudioFiles
            }

            filtered
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar samples: ${e.message}")
            emptyList()
        }
    }

    /**
     * Carga el sample en el pad. Normaliza tanto .ogg como .wav complejos a un formato 
     * PCM 16-bit estándar garantizado, compatible con el motor C++ / Oboe.
     */
    fun loadSampleToPad(context: Context, absolutePath: String, padIndex: Int): Boolean {
        try {
            val file = File(absolutePath)
            if (!file.exists()) {
                Log.e(TAG, "El archivo de audio no existe en la ruta: $absolutePath")
                return false
            }

            val nameLower = file.name.lowercase()
            
            // 🛡️ Normalizamos tanto .ogg como .wav para asegurar que Oboe reciba un PCM de 16-bit puro
            val targetFileToLoad = if (nameLower.endsWith(".ogg") || nameLower.endsWith(".wav")) {
                val decodedDir = File(context.cacheDir, "decoded_wavs").apply { mkdirs() }
                val wavFile = File(decodedDir, "${file.nameWithoutExtension}_${file.length()}.wav")
                
                // Si ya fue procesado y convertido anteriormente, lo reutilizamos instantáneamente de la caché
                if (wavFile.exists() && wavFile.length() > 44) {
                    wavFile
                } else {
                    val successConversion = convertAudioToStandardWav(file, wavFile)
                    if (!successConversion || !wavFile.exists()) {
                        Log.e(TAG, "Fallo al normalizar el archivo de audio a WAV 16-bit: ${file.name}")
                        return false
                    }
                    wavFile
                }
            } else {
                file
            }

            Log.i(TAG, "Enviando archivo PCM normalizado al motor Oboe: ${targetFileToLoad.name} en el pad: $padIndex")

            // Usamos el objeto global unificado (Singleton) AudioEngine
            val success = AudioEngine.nativeLoadSampleByPath(targetFileToLoad.absolutePath, padIndex)

            if (!success) {
                Log.e(TAG, "El motor C++ rechazó la ruta: ${targetFileToLoad.absolutePath}")
                return false
            } else {
                Log.i(TAG, "¡Sample cargado y sonando con éxito en el pad $padIndex!")
                return true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Excepción crítica en loadSampleToPad: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 🔄 Normalizador universal: Transforma cualquier archivo de audio (.ogg o .wav complejo) 
     * en un archivo .wav PCM estándar de 16-bit mediante MediaCodec y MediaExtractor.
     */
    private fun convertAudioToStandardWav(inputFile: File, outputWavFile: File): Boolean {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmData = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            val timeoutUs = 10000L

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmData.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val pcmBytes = pcmData.toByteArray()
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            writeWavHeaderAndData(outputWavFile, pcmBytes, sampleRate, channelCount, 16)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun writeWavHeaderAndData(wavFile: File, pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte() tus
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        wavFile.outputStream().use { fos ->
            fos.write(header)
            fos.write(pcmData)
        }
    }
}
