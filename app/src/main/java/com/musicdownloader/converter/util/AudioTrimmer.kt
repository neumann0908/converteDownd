package com.musicdownloader.converter.util

import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import android.os.Build
import java.io.File
import java.nio.ByteBuffer

object AudioTrimmer {
    fun trim(srcPath: String, dstPath: String, startMs: Long, endMs: Long) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(srcPath)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            var mime = ""

            for (i in 0 until trackCount) {
                val f = extractor.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (m.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    mime = m
                    break
                }
            }

            require(audioTrackIndex != -1 && format != null) { "No se encontró una pista de audio válida en el archivo." }

            extractor.selectTrack(audioTrackIndex)
            
            val startTimeUs = startMs * 1000L
            val endTimeUs = endMs * 1000L

            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            // Seleccionar dinámicamente el formato de salida según el códec (AAC o WebM/Opus)
            val muxerFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mime.contains("opus")) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            muxer = MediaMuxer(dstPath, muxerFormat)
            val destTrackIndex = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
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
    }
}
