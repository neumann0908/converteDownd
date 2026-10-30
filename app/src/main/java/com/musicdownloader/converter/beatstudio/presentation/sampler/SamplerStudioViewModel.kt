package com.musicdownloader.converter.beatstudio.presentation.sampler

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

@HiltViewModel
class SamplerStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SamplerStudioState())
    val uiState: StateFlow<SamplerStudioState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var stopJob: Job? = null

    // 🎙️ Simular o iniciar grabación de muestras desde el micrófono
    fun toggleRecording() {
        _uiState.update { currentState ->
            val newRecordingState = !currentState.isRecording
            if (newRecordingState) {
                currentState.copy(
                    isRecording = true,
                    currentTrack = SamplerTrack(
                        sampleName = "Mic_Sample_01.wav",
                        filePath = null,
                        totalDurationMs = 3000L,
                        endPoint = 3000L,
                        detectedBpm = 120f
                    )
                )
            } else {
                currentState.copy(isRecording = false)
            }
        }
    }

    // 📂 Importar archivos de audio reales extrayendo su duración exacta en milisegundos
    fun importAudioFile(fileName: String, initialDurationMs: Long, filePath: String = "") {
        var durationMs = initialDurationMs
        
        if (durationMs <= 0 && !filePath.isNullOrEmpty()) {
            // Intento 1: MediaMetadataRetriever
            try {
                val retriever = MediaMetadataRetriever()
                if (filePath.startsWith("content://")) {
                    retriever.setDataSource(context, Uri.parse(filePath))
                } else {
                    retriever.setDataSource(filePath)
                }
                val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (timeString != null) {
                    durationMs = timeString.toLong()
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Intento 2: MediaPlayer como respaldo seguro para URIs de contenido
            if (durationMs <= 0) {
                try {
                    val tempMp = if (filePath.startsWith("content://")) {
                        MediaPlayer.create(context, Uri.parse(filePath))
                    } else {
                        MediaPlayer.create(context, Uri.parse(File(filePath).toURI().toString()))
                    }
                    if (tempMp != null) {
                        durationMs = tempMp.duration.toLong()
                        tempMp.release()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (durationMs <= 0) durationMs = 10000L // Fallback predeterminado de 10s

        _uiState.update { currentState ->
            val sliceCount = currentState.currentTrack.slices.size.coerceAtLeast(4)
            val sliceDuration = durationMs / sliceCount
            val initialSlices = List(sliceCount) { index ->
                AudioSlice(
                    id = index,
                    startSample = index * sliceDuration,
                    endSample = (index + 1) * sliceDuration
                )
            }
            currentState.copy(
                currentTrack = SamplerTrack(
                    sampleName = fileName,
                    filePath = filePath.ifEmpty { "storage/emulated/0/Music/$fileName" },
                    totalDurationMs = durationMs,
                    detectedBpm = 124.0f,
                    endPoint = durationMs,
                    slices = initialSlices,
                    autoChopMode = currentState.currentTrack.autoChopMode
                )
            )
        }
    }

    // ▶️ Reproducir fragmento exacto con Seek Completion Listener y parada automática por pad
    fun playSlice(slice: AudioSlice) {
        val track = _uiState.value.currentTrack
        if (track.filePath.isNullOrEmpty()) return

        try {
            stopJob?.cancel()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (track.filePath!!.startsWith("content://")) {
                    setDataSource(context, Uri.parse(track.filePath))
                } else {
                    setDataSource(track.filePath)
                }

                setOnPreparedListener { mp ->
                    val startMs = slice.startSample.coerceAtLeast(0L).toInt()
                    val endMs = if (slice.endSample > startMs) slice.endSample.toInt() else mp.duration

                    // Aplicar volumen y loop
                    val vol = slice.volume.coerceIn(0f, 2f)
                    mp.setVolume(vol, vol)
                    mp.isLooping = track.isLooping

                    // Aplicar Pitch si la versión de Android lo soporta (API 23+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            val pitch = slice.pitchShift.coerceIn(0.25f, 4.0f)
                            mp.playbackParams = mp.playbackParams.setPitch(pitch).setSpeed(pitch)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Salto milimétrico preciso asegurando que seekTo termine antes de iniciar
                    if (startMs > 0 && startMs < mp.duration) {
                        mp.setOnSeekCompleteListener { seekMp ->
                            seekMp.start()
                            scheduleStop(seekMp, startMs, endMs)
                        }
                        mp.seekTo(startMs)
                    } else {
                        mp.start()
                        scheduleStop(mp, startMs, endMs)
                    }
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleStop(mp: MediaPlayer, startMs: Int, endMs: Int) {
        val durationToPlay = (endMs - startMs).toLong()
        if (durationToPlay > 0) {
            stopJob = viewModelScope.launch {
                delay(durationToPlay)
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 🎛️ Modificar milisegundos de inicio y fin de un Pad individual
    fun updateSliceRange(sliceId: Int, newStart: Long, newEnd: Long) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val updatedSlices = track.slices.map { slice ->
                if (slice.id == sliceId) {
                    val validStart = newStart.coerceAtLeast(0L)
                    val validEnd = newEnd.coerceAtMost(track.totalDurationMs)
                    if (validStart < validEnd) {
                        slice.copy(startSample = validStart, endSample = validEnd)
                    } else {
                        slice
                    }
                } else {
                    slice
                }
            }
            currentState.copy(currentTrack = track.copy(slices = updatedSlices))
        }
    }

    // 💾 Guardar / Exportar el Sampler actual clonando el archivo físico a la carpeta 'exports' y escaneándolo
    fun exportSamplerAsWav(outputPath: String): Boolean {
        return try {
            val track = _uiState.value.currentTrack
            if (track.filePath.isNullOrEmpty() || track.slices.isEmpty()) return false
            
            val destFile = File(outputPath)
            destFile.parentFile?.mkdirs()

            if (track.filePath!!.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(track.filePath))?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return false
            } else {
                val sourceFile = File(track.filePath!!)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destFile, overwrite = true)
                } else {
                    return false
                }
            }

            // 🔍 Registrar el archivo en el MediaScanner para indexación instantánea en la Biblioteca
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("audio/wav", "audio/mpeg", "audio/mp3", "audio/flac")
            ) { _, _ -> }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ⚡ Función Diferencial: Auto Chop Inteligente distribuido sobre la duración real
    fun executeAutoChop(type: AutoChopType) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val duration = if (track.totalDurationMs > 0) track.totalDurationMs else 5000L
            val sliceCount = when (type) {
                AutoChopType.TRANSIENTS -> 8
                AutoChopType.SILENCES -> 6
                AutoChopType.ENERGY -> 4
                AutoChopType.MEASURES -> 16
            }
            
            val sliceDuration = duration / sliceCount
            val newSlices = List(sliceCount) { index ->
                AudioSlice(
                    id = index,
                    startSample = index * sliceDuration,
                    endSample = (index + 1) * sliceDuration
                )
            }

            currentState.copy(
                currentTrack = track.copy(
                    autoChopMode = type,
                    slices = newSlices
                )
            )
        }
    }

    // 🎛️ Modificar puntos globales de inicio y final de la muestra
    fun updateStartEndPoints(start: Long, end: Long) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(
                currentTrack = track.copy(
                    startPoint = start.coerceAtLeast(0L),
                    endPoint = end.coerceAtMost(if (track.totalDurationMs > 0) track.totalDurationMs else end)
                )
            )
        }
    }

    // 🎚️ Ajuste de Envolvente ADSR Completa
    fun updateSamplerAdsr(attack: Float, decay: Float, sustain: Float, release: Float) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(
                currentTrack = track.copy(
                    attack = attack.coerceIn(0.001f, 1f),
                    decay = decay.coerceIn(0.01f, 2f),
                    sustain = sustain.coerceIn(0f, 1f),
                    release = release.coerceIn(0.01f, 5f)
                )
            )
        }
    }

    // 🎛️ Ajuste de Fade In y Fade Out
    fun updateFadeInFadeOut(fadeIn: Long, fadeOut: Long) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(
                currentTrack = track.copy(
                    fadeInMs = fadeIn.coerceAtLeast(0L),
                    fadeOutMs = fadeOut.coerceAtLeast(0L)
                )
            )
        }
    }

    // 🔄 Efectos y Normalización (Loop, Normalization)
    fun toggleLoop() {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(currentTrack = track.copy(isLooping = !track.isLooping))
        }
    }

    fun toggleNormalization() {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(currentTrack = track.copy(isNormalized = !track.isNormalized))
        }
    }

    // 🎛️ Ajustes de Slice Individual (Pitch, Volumen y Reverse por pad)
    fun updateSlicePitch(sliceId: Int, pitch: Float) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val updatedSlices = track.slices.map { slice ->
                if (slice.id == sliceId) slice.copy(pitchShift = pitch.coerceIn(0.25f, 4.0f)) else slice
            }
            currentState.copy(currentTrack = track.copy(slices = updatedSlices))
        }
    }

    fun updateSliceVolume(sliceId: Int, volume: Float) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val updatedSlices = track.slices.map { slice ->
                if (slice.id == sliceId) slice.copy(volume = volume.coerceIn(0f, 2.0f)) else slice
            }
            currentState.copy(currentTrack = track.copy(slices = updatedSlices))
        }
    }

    fun toggleSliceReverse(sliceId: Int) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val updatedSlices = track.slices.map { slice ->
                if (slice.id == sliceId) slice.copy(isReversed = !slice.isReversed) else slice
            }
            currentState.copy(currentTrack = track.copy(slices = updatedSlices))
        }
    }

    // ▶️ Reproducción del sample principal
    fun togglePlaySample() {
        _uiState.update { it.copy(isPlayingSample = !it.isPlayingSample) }
    }

    override fun onCleared() {
        super.onCleared()
        stopJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
