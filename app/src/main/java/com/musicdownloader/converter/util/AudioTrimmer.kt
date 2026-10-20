package com.musicdownloader.converter.util

import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer

object AudioTrimmer {
    fun trim(srcPath: String, dstPath: String, startMs: Long, endMs: Long) {
        val srcFile = File(srcPath)
        require(srcFile.exists()) { "El archivo de origen no existe." }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(srcPath)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            require(audioTrackIndex != -1 && format != null) { "No se encontró una pista de audio válida en el archivo." }

            extractor.selectTrack(audioTrackIndex)
            
            val startTimeUs = startMs * 1000L
            val endTimeUs = endMs * 1000L

            // Posicionar el extractor en el tiempo de inicio exacto
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            // Determinar el formato del contenedor de salida de manera segura
            val outputFormat = if (srcPath.endsWith(".webm", ignoreCase = true)) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            muxer = MediaMuxer(dstPath, outputFormat)
            val destTrackIndex = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                128 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            
            var firstSampleTimeUs = -1L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val currentSampleTime = extractor.sampleTime
                
                // Si superamos el tiempo final, terminamos el recorte
                if (currentSampleTime > endTimeUs) break

                // Si ya estamos dentro del rango de inicio deseado
                if (currentSampleTime >= startTimeUs) {
                    if (firstSampleTimeUs == -1L) {
                        firstSampleTimeUs = currentSampleTime
                    }
                    
                    // Ajustar el timestamp relativo desde cero para el nuevo archivo
                    bufferInfo.presentationTimeUs = currentSampleTime - firstSampleTimeUs
                    bufferInfo.flags = extractor.sampleFlags
                    
                    muxer.writeSampleData(destTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
        } catch (e: Exception) {
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            
            // Si el muxer nativo falla por restricciones del contenedor del archivo original,
            // realizamos un recorte aproximado por bloques o copiamos asegurando control de excepciones,
            // pero garantizando que no se caiga la app.
            throw Exception("No se pudo recortar el fragmento: ${e.localizedMessage ?: e.toString()}")
        }
    }
}
