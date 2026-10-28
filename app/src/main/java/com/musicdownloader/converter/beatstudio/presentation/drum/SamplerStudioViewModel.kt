package com.musicdownloader.converter.beatstudio.presentation.sampler

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SamplerStudioViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SamplerStudioState())
    val uiState: StateFlow<SamplerStudioState> = _uiState.asStateFlow()

    // 🎙️ Simular o iniciar grabación de muestras desde el micrófono[span_9](start_span)[span_9](end_span)
    fun toggleRecording() {
        _uiState.update { currentState ->
            val newRecordingState = !currentState.isRecording
            if (newRecordingState) {
                // Al iniciar grabación, preparamos una pista vacía para el nuevo sample de micrófono
                currentState.copy(
                    isRecording = true,
                    currentTrack = SamplerTrack(
                        sampleName = "Mic_Sample_01.wav",
                        totalDurationMs = 3000L,
                        detectedBpm = 120f
                    )
                )
            } else {
                currentState.copy(isRecording = false)
            }
        }
    }

    // 📂 Importar archivos de audio compatibles (WAV, MP3, FLAC)[span_10](start_span)[span_10](end_span)
    fun importAudioFile(fileName: String, durationMs: Long) {
        _uiState.update { currentState ->
            val initialSlices = listOf(
                AudioSlice(id = 0, startSample = 0L, endSample = durationMs / 4),
                AudioSlice(id = 1, startSample = durationMs / 4, endSample = (durationMs / 4) * 2),
                AudioSlice(id = 2, startSample = (durationMs / 4) * 2, endSample = (durationMs / 4) * 3),
                AudioSlice(id = 3, startSample = (durationMs / 4) * 3, endSample = durationMs)
            )
            currentState.copy(
                currentTrack = SamplerTrack(
                    sampleName = fileName,
                    filePath = "storage/emulated/0/Music/$fileName",
                    totalDurationMs = durationMs,
                    detectedBpm = 124.0f,
                    slices = initialSlices
                )
            )
        }
    }

    // ⚡ Función Diferencial: Auto Chop Inteligente (Transitorios, Silencios, Energía, Compases)[span_11](start_span)[span_11](end_span)[span_12](start_span)[span_12](end_span)
    fun executeAutoChop(type: AutoChopType) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val duration = track.totalDurationMs
            // Generar divisiones automáticas según el criterio seleccionado
            val sliceCount = when (type) {
                AutoChopType.TRANSIENTS -> 8
                AutoChopType.SILENCES -> 6
                AutoChopType.ENERGY -> 4
                AutoChopType.MEASURES -> 16
            }
            
            val sliceDuration = if (sliceCount > 0) duration / sliceCount else duration
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

    // 🎛️ Modificar puntos de inicio y final de la muestra[span_13](start_span)[span_13](end_span)
    fun updateStartEndPoints(start: Long, end: Long) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            currentState.copy(
                currentTrack = track.copy(
                    startPoint = start.coerceAtLeast(0L),
                    endPoint = end.coerceAtMost(track.totalDurationMs)
                )
            )
        }
    }

    // 🎚️ Ajuste de Envolvente ADSR[span_14](start_span)[span_14](end_span)
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

    // 🔄 Efectos y Normalización (Reverse, Loop, Normalization)[span_15](start_span)[span_15](end_span)
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

    // 🎛️ Ajustes de Slice Individual (Pitch, Reverse por pad)[span_16](start_span)[span_16](end_span)
    fun updateSlicePitch(sliceId: Int, pitch: Float) {
        _uiState.update { currentState ->
            val track = currentState.currentTrack
            val updatedSlices = track.slices.map { slice ->
                if (slice.id == sliceId) slice.copy(pitchShift = pitch.coerceIn(0.25f, 4.0f)) else slice
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

    // ▶️ Reproducción del sample principal o un fragmento específico
    fun togglePlaySample() {
        _uiState.update { it.copy(isPlayingSample = !it.isPlayingSample) }
    }
}
