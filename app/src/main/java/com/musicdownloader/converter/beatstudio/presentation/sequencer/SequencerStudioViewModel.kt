package com.musicdownloader.converter.beatstudio.presentation.sequencer

import android.content.Context
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

@HiltViewModel
class SequencerStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SequencerStudioState())
    val uiState: StateFlow<SequencerStudioState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    // 🎵 Activar o desactivar un cuadrito (paso) cuando lo tocas con el dedo
    fun toggleStep(trackIndex: Int, stepIndex: Int) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) {
                            step.copy(isActive = !step.isActive)
                        } else {
                            step
                        }
                    }
                    track.copy(steps = updatedSteps)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // ▶️ Iniciar el reloj maestro para reproducir el ritmo paso a paso
    fun startPlayback() {
        if (_uiState.value.isPlaying) return
        
        _uiState.update { it.copy(isPlaying = true) }
        
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val totalSteps = 16
            while (_uiState.value.isPlaying) {
                val currentBpm = _uiState.value.bpm
                // Calculamos el tiempo exacto de espera entre cada paso según los BPM
                val stepDelayMs = (60000f / currentBpm / 4f).toLong()

                _uiState.update { state ->
                    val nextStep = (state.currentStepIndex + 1) % totalSteps
                    playActiveStepsForColumn(nextStep)
                    state.copy(currentStepIndex = nextStep)
                }

                delay(stepDelayMs)
            }
        }
    }

    // ⏹️ Detener la reproducción del secuenciador
    fun stopPlayback() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = false, currentStepIndex = -1) }
    }

    // 🔊 Dispara los sonidos que están activos en la columna actual
    private fun playActiveStepsForColumn(stepIndex: Int) {
        val tracks = _uiState.value.tracks
        for (track in tracks) {
            if (!track.isMuted) {
                val step = track.steps.getOrNull(stepIndex)
                if (step != null && step.isActive) {
                    // Aquí conectaremos la reproducción de audio de cada pad/pista
                }
            }
        }
    }

    // 🎛️ Cambiar la velocidad de la música (BPM)
    fun updateBpm(newBpm: Float) {
        _uiState.update { it.copy(bpm = newBpm.coerceIn(40f, 240f)) }
    }

    // 🎚️ Cambiar el compás (Ej: "4/4", "3/4", "6/8")
    fun updateTimeSignature(signature: String) {
        _uiState.update { it.copy(timeSignature = signature) }
    }

    // 🎛️ Cambiar la resolución (Ej: "1/16", "1/8")
    fun updateResolution(res: String) {
        _uiState.update { it.copy(resolution = res) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }
}
