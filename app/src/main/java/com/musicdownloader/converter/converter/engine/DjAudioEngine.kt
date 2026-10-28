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
import android.media.PlaybackParams
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
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

    private var equalizerA: Equalizer? = null
    private var equalizerB: Equalizer? = null

    private var reverbA: PresetReverb? = null
    private var reverbB: PresetReverb? = null

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

    // --- 🎛️ Volúmenes base calculados por el Crossfader ---
    private var volumeA: Float = 0.5f
    private var volumeB: Float = 0.5f

    // --- 🎧 Headphone Cue / Split Audio States ---
    private var isSplitEnabled: Boolean = false
    private var cueAActive: Boolean = false
    private var cueBActive: Boolean = false

    // --- 🎛️ Parámetros de Tempo, Key Shift y Master Tempo ---
    private var speedA: Float = 1.0f
    private var pitchA: Float = 1.0f
    private var isMasterTempoA: Boolean = false

    private var speedB: Float = 1.0f
    private var pitchB: Float = 1.0f
    private var isMasterTempoB: Boolean = false

    // --- 🎛️ Estados de Stems para Deck A ---
    private var isVocalsMutedA = false
    private var isDrumsMutedA = false
    private var isBassMutedA = false

    // --- 🎛️ Estados de Stems para Deck B ---
    private var isVocalsMutedB = false
    private var isDrumsMutedB = false
    private var isBassMutedB = false

    // --- ⚡ Bucles Automáticos (Auto-Loops) ---
    private var loopThreadA: Thread? = null
    private var isLoopingActiveA = false

    private var loopThreadB: Thread? = null
    private var isLoopingActiveB = false

    fun loadTrackA(path: String, onPrepared: () -> Unit = {}) {
        try {
            stopAutoLoopA()
            mediaPlayerA?.release()
            equalizerA?.release()
            reverbA?.release()
            isVocalsMutedA = false
            isDrumsMutedA = false
            isBassMutedA = false
            speedA = 1.0f
            pitchA = 1.0f
            isMasterTempoA = false

            mediaPlayerA = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                isLooping = true
                setOnPreparedListener {
                    try {
                        equalizerA = Equalizer(0, audioSessionId).apply {
                            enabled = true
                        }
                        reverbA = PresetReverb(0, audioSessionId).apply {
                            preset = PresetReverb.PRESET_LARGEHALL
                            enabled = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onPrepared()
                }
            }
            isPlayingA = false
            applyVolumes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTrackB(path: String, onPrepared: () -> Unit = {}) {
        try {
            stopAutoLoopB()
            mediaPlayerB?.release()
            equalizerB?.release()
            reverbB?.release()
            isVocalsMutedB = false
            isDrumsMutedB = false
            isBassMutedB = false
            speedB = 1.0f
            pitchB = 1.0f
            isMasterTempoB = false

            mediaPlayerB = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                isLooping = true
                setOnPreparedListener {
                    try {
                        equalizerB = Equalizer(0, audioSessionId).apply {
                            enabled = true
                        }
                        reverbB = PresetReverb(0, audioSessionId).apply {
                            preset = PresetReverb.PRESET_LARGEHALL
                            enabled = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onPrepared()
                }
            }
            isPlayingB = false
            applyVolumes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqualizerA(low: Float, mid: Float, high: Float) {
        equalizerA?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                if (numBands >= 3) {
                    val lowBand = 0.toShort()
                    val midBand = (numBands / 2).toShort()
                    val highBand = (numBands - 1).toShort()

                    eq.setBandLevel(lowBand, dbToLevel(low, eq))
                    eq.setBandLevel(midBand, dbToLevel(mid, eq))
                    eq.setBandLevel(highBand, dbToLevel(high, eq))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setEqualizerB(low: Float, mid: Float, high: Float) {
        equalizerB?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                if (numBands >= 3) {
                    val lowBand = 0.toShort()
                    val midBand = (numBands / 2).toShort()
                    val highBand = (numBands - 1).toShort()

                    eq.setBandLevel(lowBand, dbToLevel(low, eq))
                    eq.setBandLevel(midBand, dbToLevel(mid, eq))
                    eq.setBandLevel(highBand, dbToLevel(high, eq))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dbToLevel(value: Float, equalizer: Equalizer): Short {
        val range = equalizer.bandLevelRange
        val minLevel = range[0].toFloat()
        val maxLevel = range[1].toFloat()
        val normalized = (value.coerceIn(-1f, 1f) + 1f) / 2f
        return (minLevel + normalized * (maxLevel - minLevel)).toInt().toShort()
    }

    // --- 🎧 Configuración de Split Audio / Headphone Cue ---
    fun setSplitAudioMode(enabled: Boolean, cueA: Boolean, cueB: Boolean) {
        isSplitEnabled = enabled
        cueAActive = cueA
        cueBActive = cueB
        applyVolumes()
    }

    private fun applyVolumes() {
        try {
            if (isSplitEnabled) {
                val leftVolA = volumeA
                val rightVolA = if (cueAActive) 1.0f else 0.0f
                mediaPlayerA?.setVolume(leftVolA, rightVolA)

                val leftVolB = volumeB
                val rightVolB = if (cueBActive) 1.0f else 0.0f
                mediaPlayerB?.setVolume(leftVolB, rightVolB)
            } else {
                mediaPlayerA?.setVolume(volumeA, volumeA)
                mediaPlayerB?.setVolume(volumeB, volumeB)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 🎛️ Control de Tonalidad (Key Shift & Master Tempo) ---
    fun setTempoDeckA(speed: Float) {
        speedA = speed.coerceIn(0.5f, 2.0f)
        updatePlaybackParamsA()
    }

    fun setKeyShiftA(semitones: Float) {
        pitchA = Math.pow(2.0, semitones.toDouble() / 12.0).toFloat()
        updatePlaybackParamsA()
    }

    fun toggleMasterTempoA(enabled: Boolean) {
        isMasterTempoA = enabled
        updatePlaybackParamsA()
    }

    private fun updatePlaybackParamsA() {
        try {
            mediaPlayerA?.let { player ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = player.playbackParams ?: PlaybackParams()
                    params.speed = speedA
                    params.pitch = if (isMasterTempoA) 1.0f else pitchA
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTempoDeckB(speed: Float) {
        speedB = speed.coerceIn(0.5f, 2.0f)
        updatePlaybackParamsB()
    }

    fun setKeyShiftB(semitones: Float) {
        pitchB = Math.pow(2.0, semitones.toDouble() / 12.0).toFloat()
        updatePlaybackParamsB()
    }

    fun toggleMasterTempoB(enabled: Boolean) {
        isMasterTempoB = enabled
        updatePlaybackParamsB()
    }

    private fun updatePlaybackParamsB() {
        try {
            mediaPlayerB?.let { player ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = player.playbackParams ?: PlaybackParams()
                    params.speed = speedB
                    params.pitch = if (isMasterTempoB) 1.0f else pitchB
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 🎛️ Físicas de Touch Scratch Real Avanzado ---
    fun scratchDeckA(delta: Float) {
        mediaPlayerA?.let { player ->
            try {
                val currentPos = player.currentPosition
                val newPos = (currentPos - (delta * 20).toInt()).coerceAtLeast(0)
                player.seekTo(newPos)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val scratchSpeed = (1.0f + (delta * 0.08f)).coerceIn(0.2f, 3.0f)
                    player.playbackParams = player.playbackParams.apply { speed = scratchSpeed }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun releaseScratchA() {
        updatePlaybackParamsA()
    }

    fun scratchDeckB(delta: Float) {
        mediaPlayerB?.let { player ->
            try {
                val currentPos = player.currentPosition
                val newPos = (currentPos - (delta * 20).toInt()).coerceAtLeast(0)
                player.seekTo(newPos)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val scratchSpeed = (1.0f + (delta * 0.08f)).coerceIn(0.2f, 3.0f)
                    player.playbackParams = player.playbackParams.apply { speed = scratchSpeed }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun releaseScratchB() {
        updatePlaybackParamsB()
    }

    // --- 🎛️ Control de Stems para Deck A ---
    fun toggleStemVocalsA(muted: Boolean) {
        isVocalsMutedA = muted
        applyStemsA()
    }

    fun toggleStemDrumsA(muted: Boolean) {
        isDrumsMutedA = muted
        applyStemsA()
    }

    fun toggleStemBassA(muted: Boolean) {
        isBassMutedA = muted
        applyStemsA()
    }

    private fun applyStemsA() {
        val lowVal = if (isBassMutedA) -1f else 0f
        val midVal = if (isVocalsMutedA) -1f else 0f
        val highVal = if (isDrumsMutedA) -1f else 0f
        setEqualizerA(lowVal, midVal, highVal)
    }

    // --- 🎛️ Control de Stems para Deck B ---
    fun toggleStemVocalsB(muted: Boolean) {
        isVocalsMutedB = muted
        applyStemsB()
    }

    fun toggleStemDrumsB(muted: Boolean) {
        isDrumsMutedB = muted
        applyStemsB()
    }

    fun toggleStemBassB(muted: Boolean) {
        isBassMutedB = muted
        applyStemsB()
    }

    private fun applyStemsB() {
        val lowVal = if (isBassMutedB) -1f else 0f
        val midVal = if (isVocalsMutedB) -1f else 0f
        val highVal = if (isDrumsMutedB) -1f else 0f
        setEqualizerB(lowVal, midVal, highVal)
    }

    // --- ⚡ FX Pad: Echo / Reverb ---
    fun toggleEchoA(enabled: Boolean) {
        reverbA?.enabled = enabled
    }

    fun toggleEchoB(enabled: Boolean) {
        reverbB?.enabled = enabled
    }

    // --- ⚡ FX Pad: Low-Pass / High-Pass Filter ---
    fun toggleFilterA(mode: String) {
        when (mode) {
            "LP" -> setEqualizerA(0.8f, 0.2f, -1f)
            "HP" -> setEqualizerA(-1f, -0.5f, 0.8f)
            else -> setEqualizerA(0f, 0f, 0f)
        }
    }

    fun toggleFilterB(mode: String) {
        when (mode) {
            "LP" -> setEqualizerB(0.8f, 0.2f, -1f)
            "HP" -> setEqualizerB(-1f, -0.5f, 0.8f)
            else -> setEqualizerB(0f, 0f, 0f)
        }
    }

    // --- ⚡ FX Pad: Backspin / Brake ---
    fun triggerBrakeA() {
        thread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaPlayerA?.playbackParams = PlaybackParams().apply { speed = 0.1f }
                    Thread.sleep(300)
                    mediaPlayerA?.pause()
                    updatePlaybackParamsA()
                } else {
                    mediaPlayerA?.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerBrakeB() {
        thread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaPlayerB?.playbackParams = PlaybackParams().apply { speed = 0.1f }
                    Thread.sleep(300)
                    mediaPlayerB?.pause()
                    updatePlaybackParamsB()
                } else {
                    mediaPlayerB?.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- ⚡ Bucles Automáticos (Auto-Loops) ---
    fun startAutoLoopA(beats: Int, bpm: Int) {
        stopAutoLoopA()
        if (bpm <= 0) return
        val beatDurationMs = (60000.0 / bpm).toLong()
        val loopDurationMs = beatDurationMs * beats

        isLoopingActiveA = true
        loopThreadA = thread(start = true) {
            try {
                while (isLoopingActiveA) {
                    val player = mediaPlayerA ?: break
                    if (!player.isPlaying) break
                    val startPos = player.currentPosition
                    
                    val segment = 50L
                    val steps = loopDurationMs / segment
                    for (i in 0 until steps) {
                        if (!isLoopingActiveA || !player.isPlaying) break
                        Thread.sleep(segment)
                    }
                    if (isLoopingActiveA && player.isPlaying) {
                        player.seekTo(startPos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopAutoLoopA() {
        isLoopingActiveA = false
        loopThreadA?.interrupt()
        loopThreadA = null
    }

    fun startAutoLoopB(beats: Int, bpm: Int) {
        stopAutoLoopB()
        if (bpm <= 0) return
        val beatDurationMs = (60000.0 / bpm).toLong()
        val loopDurationMs = beatDurationMs * beats

        isLoopingActiveB = true
        loopThreadB = thread(start = true) {
            try {
                while (isLoopingActiveB) {
                    val player = mediaPlayerB ?: break
                    if (!player.isPlaying) break
                    val startPos = player.currentPosition
                    
                    val segment = 50L
                    val steps = loopDurationMs / segment
                    for (i in 0 until steps) {
                        if (!isLoopingActiveB || !player.isPlaying) break
                        Thread.sleep(segment)
                    }
                    if (isLoopingActiveB && player.isPlaying) {
                        player.seekTo(startPos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopAutoLoopB() {
        isLoopingActiveB = false
        loopThreadB?.interrupt()
        loopThreadB = null
    }

    fun seekToDeckA(positionMs: Int) {
        try {
            mediaPlayerA?.seekTo(positionMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekToDeckB(positionMs: Int) {
        try {
            mediaPlayerB?.seekTo(positionMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentPositionA(): Int {
        return try {
            mediaPlayerA?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentPositionB(): Int {
        return try {
            mediaPlayerB?.currentPosition ?: 0
        } catch (e: Exception) {
            0
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

        volumeA = cos(angleA).toFloat()
        volumeB = sin(angleB).toFloat()

        applyVolumes()
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
    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")
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

        val format = AudioFormat.Builder()
            .setEncoding(audioFormat)
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .build()

        val builder = AudioRecord.Builder()
        
        val configMethod = builder.javaClass.getMethod("setAudioPlaybackCaptureConfiguration", AudioPlaybackCaptureConfiguration::class.java)
        configMethod.invoke(builder, playbackConf)

        val formatMethod = builder.javaClass.getMethod("setAudioFormat", AudioFormat::class.java)
        formatMethod.invoke(builder, format)

        val bufferMethod = builder.javaClass.getMethod("setBufferSizeInBytes", Int::class.javaPrimitiveType!!)
        bufferMethod.invoke(builder, bufferSize)

        val buildMethod = builder.javaClass.getMethod("build")
        return buildMethod.invoke(builder) as AudioRecord
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
        stopAutoLoopA()
        stopAutoLoopB()
        equalizerA?.release()
        equalizerB?.release()
        reverbA?.release()
        reverbB?.release()
        equalizerA = null
        equalizerB = null
        reverbA = null
        reverbB = null
        mediaPlayerA?.release()
        mediaPlayerB?.release()
        mediaPlayerA = null
        mediaPlayerB = null
    }
}
