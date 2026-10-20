package com.musicdownloader.converter.util

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object AudioTrimmer {
    fun trim(srcPath: String, dstPath: String, startMs: Long, endMs: Long) {
        val srcFile = File(srcPath)
        require(srcFile.exists()) { "El archivo de origen no existe." }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(srcPath)
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

            require(audioTrackIndex != -1 && format != null) { "No se encontró una pista de audio válida." }

            extractor.selectTrack(audioTrackIndex)

            val startTimeUs = startMs * 1000L
            val endTimeUs = endMs * 1000L

            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val dstFile = File(dstPath)
            val outputStream = FileOutputStream(dstFile)

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endTimeUs) break

                if (sampleTimeUs >= startTimeUs) {
                    val bytes = ByteArray(sampleSize)
                    buffer.get(bytes, 0, sampleSize)
                    outputStream.write(bytes)
                }

                extractor.advance()
            }

            outputStream.flush()
            outputStream.close()
            extractor.release()
        } catch (e: Exception) {
            try { extractor.release() } catch (_: Exception) {}
            throw Exception("Error recortando archivo: ${e.localizedMessage ?: e.toString()}")
        }
    }
}
