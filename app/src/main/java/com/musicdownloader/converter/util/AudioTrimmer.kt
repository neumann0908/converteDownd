package com.musicdownloader.converter.util

import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer

object AudioTrimmer {
    fun trim(srcPath: String, dstPath: String, startMs: Long, endMs: Long): Boolean {
        var extractor = MediaExtractor()
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

            if (audioTrackIndex == -1 || format == null) return false

            extractor.selectTrack(audioTrackIndex)
            
            val startTimeUs = startMs * 1000L
            val endTimeUs = endMs * 1000L

            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val destTrackIndex = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endTimeUs) break

                if (bufferInfo.presentationTimeUs >= startTimeUs) {
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(destTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            return false
        }
    }
}
