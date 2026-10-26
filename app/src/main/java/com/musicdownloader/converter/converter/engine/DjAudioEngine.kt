package com.musicdownloader.converter.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class DjAudioEngine(private val context: Context) {

    private var mediaPlayerA: MediaPlayer? = null
    private var mediaPlayerB: MediaPlayer? = null

    private var audioRecord: AudioRecord? = null
    private var mediaMuxer: MediaMuxer? = null
    private var mediaCodec: MediaCodec? = null
    private var recordingThread: Thread? = null
    private var isRecordingActive = false
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    var isPlayingA: Boolean = false
        private set
    var isPlayingB: Boolean = false
        private set

    fun loadTrackA(path: String, onPrepared: () -> Unit = {}) {
        try {
            mediaPlayerA?.release()
            mediaPlayerA = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                isLooping = true
                setOnPreparedListener { onPrepared() }
            }
            isPlayingA = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTrackB(path: String, onPrepared: () -> Unit = {}) {
        try {
            mediaPlayerB?.release()
            mediaPlayerB = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                isLooping = true
                setOnPreparedListener { onPrepared() }
            }
            isPlayingB = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayA(pathIfNull: String?): Boolean {
        mediaPlayerA?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlayingA = false
            } else {
                player.start()
                isPlayingA = true
            }
        } ?: pathIfNull?.let {
            loadTrackA(it) {
                mediaPlayerA?.start()
                isPlayingA = true
            }
        }
        return isPlayingA
    }

    fun togglePlayB(pathIfNull: String?): Boolean {
        mediaPlayerB?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlayingB = false
            } else {
                player.start()
                isPlayingB = true
            }
        } ?: pathIfNull?.let {
            loadTrackB(it) {
                mediaPlayerB?.start()
                isPlayingB = true
            }
        }
        return isPlayingB
    }

    fun updateCrossfader(value: Float) {
        val position = value.coerceIn(0f, 1f)
        val angleA = (1.0f - position) * (Math.PI / 2.0)
        val angleB = position * (Math.PI / 2.0)

        val volA = cos(angleA).toFloat()
        val volB = sin(angleB).toFloat()

        try {
            mediaPlayerA?.setVolume(volA, volA)
            mediaPlayerB?.setVolume(volB, volB)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRecording(mediaProjection: MediaProjection? = null): Boolean {
        return try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            currentRecordingFile = File(downloadDir, "DJ_Mix_${System.currentTimeMillis()}.m4a")

            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096

            audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                createPlaybackCaptureRecord(mediaProjection, sampleRate, channelConfig, audioFormat, bufferSize)
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    android.media.MediaRecorder.AudioSource.DEFAULT,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            mediaMuxer = MediaMuxer(currentRecordingFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            audioRecord?.startRecording()
            isRecordingActive = true
            recordingStartTime = System.currentTimeMillis()

            startEncodingLoop()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun createPlaybackCaptureRecord(
        mediaProjection: MediaProjection,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int
    ): AudioRecord {
        val playbackConf = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        return AudioRecord.Builder()
            .setAudioPlaybackCaptureConfiguration(playbackConf)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    private fun startEncodingLoop() {
        recordingThread = thread(start = true) {
            val bufferInfo = MediaCodec.BufferInfo()
            val readBuffer = ByteArray(4096)
            var trackIndex = -1
            var muxerStarted = false

            try {
                while (isRecordingActive) {
                    val record = audioRecord ?: break
                    val codec = mediaCodec ?: break
                    val muxer = mediaMuxer ?: break

                    val bytesRead = record.read(readBuffer, 0, readBuffer.size)
                    if (bytesRead > 0) {
                        val inputBufferIndex = codec.dequeueInputBuffer(10000)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer: ByteBuffer? = codec.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(readBuffer, 0, bytesRead)
                            codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, System.nanoTime() / 1000, 0)
                        }
                    }

                    val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording(): File? {
        try {
            isRecordingActive = false
            recordingThread?.join(1000)
            recordingThread = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null

            mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaMuxer = null

            val elapsed = System.currentTimeMillis() - recordingStartTime
            val fileLength = currentRecordingFile?.length() ?: 0L

            if (elapsed < 2000 || fileLength < 10240L) {
                currentRecordingFile?.delete()
                currentRecordingFile = null
                return null
            }

            val savedFile = currentRecordingFile
            currentRecordingFile = null
            return savedFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { currentRecordingFile?.delete() } catch (_: Exception) {}
            currentRecordingFile = null
            return null
        }
    }

    fun release() {
        stopRecording()
        mediaPlayerA?.release()
        mediaPlayerB?.release()
        mediaPlayerA = null
        mediaPlayerB = null
    }
}
