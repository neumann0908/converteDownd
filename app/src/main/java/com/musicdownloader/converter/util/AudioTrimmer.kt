package com.musicdownloader.converter.util

import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
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

            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            val cleanFormat = MediaFormat().apply {
                setString(MediaFormat.KEY_MIME, format.getString(MediaFormat.KEY_MIME))
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    setInteger(MediaFormat.KEY_SAMPLE_RATE, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                }
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    setInteger(MediaFormat.KEY_CHANNEL_COUNT, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                }
                if (format.containsKey("csd-0")) {
                    setByteBuffer("csd-0", format.getByteBuffer("csd-0"))
                }
            }

            val destTrackIndex = muxer.addTrack(cleanFormat)
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
    }
}
