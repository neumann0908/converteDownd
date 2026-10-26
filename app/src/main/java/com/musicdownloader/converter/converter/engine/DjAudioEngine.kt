package com.musicdownloader.converter.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class DjAudioEngine(private val context: Context) {

    private var mediaPlayerA: MediaPlayer? = null
    private var mediaPlayerB: MediaPlayer? = null

    private var mediaRecorder: MediaRecorder? = null
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

    fun startRecording(): Boolean {
        return try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            currentRecordingFile = File(downloadDir, "DJ_Mix_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentRecordingFile?.absolutePath)
                prepare()
                start()
            }
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            false
        }
    }

    fun stopRecording(): File? {
        try {
            val elapsed = System.currentTimeMillis() - recordingStartTime
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            if (elapsed < 2000) {
                currentRecordingFile?.delete()
                return null
            }
            return currentRecordingFile
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            try { currentRecordingFile?.delete() } catch (_: Exception) {}
            return null
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    fun release() {
        releaseRecorder()
        mediaPlayerA?.release()
        mediaPlayerB?.release()
        mediaPlayerA = null
        mediaPlayerB = null
    }
}
