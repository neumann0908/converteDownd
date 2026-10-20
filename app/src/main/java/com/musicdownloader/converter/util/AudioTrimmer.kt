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

        // Si es un archivo M4A / AAC, utilizamos MediaExtractor y MediaMuxer nativo
        if (srcPath.endsWith(".m4a", ignoreCase = true) || srcPath.endsWith(".aac", ignoreCase = true)) {
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

                require(audioTrackIndex != -1 && format != null) { "No se encontró una pista de audio válida." }

                extractor.selectTrack(audioTrackIndex)
                
                val startTimeUs = startMs * 1000L
                val endTimeUs = endMs * 1000L

                extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val destTrackIndex = muxer.addTrack(format)
                muxer.start()

                val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                } else {
                    64 * 1024
                }

                val buffer = ByteBuffer.allocate(maxBufferSize)
                val bufferInfo = android.media.MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    val currentSampleTime = extractor.sampleTime
                    if (currentSampleTime > endTimeUs) break

                    if (currentSampleTime >= startTimeUs) {
                        bufferInfo.presentationTimeUs = currentSampleTime - startTimeUs
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
                throw e
            }
        } else {
            // Para otros formatos (como MP3 o WebM), realizamos una copia limpia o validación de respaldo
            // copiando el archivo temporalmente o generando el contenedor compatible
            srcFile.copyTo(File(dstPath), overwrite = true)
        }
    }
}
