package com.musicdownloader.converter.beatstudio.presentation.drum

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DrumStudioViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DrumStudioState())
    val uiState: StateFlow<DrumStudioState> = _uiState.asStateFlow()

    // Sirve para encender o apagar una casilla cuando la tocas con el dedo
    fun toggleStep(trackId: String, stepIndex: Int) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    val newSteps = track.steps.toMutableList()
                    newSteps[stepIndex] = !newSteps[stepIndex]
                    track.copy(steps = newSteps)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // Arranca o pausa la musiquita
    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    // Cambia la velocidad del beat
    fun updateBpm(newBpm: Int) {
        _uiState.update { it.copy(bpm = newBpm.coerceIn(60, 200)) }
    }

    // Selecciona una pista de instrumento
    fun selectTrack(index: Int) {
        _uiState.update { it.copy(selectedTrackIndex = index) }
    }

    // Silencia o prende una pista individual (Mute)
    fun toggleMute(trackId: String) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(isMuted = !track.isMuted)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // Aísla la pista para destacar solo su sonido (Solo)
    fun toggleSolo(trackId: String) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(isSolo = !track.isSolo)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // Ajusta de forma independiente el volumen de una pista específica
    fun updateVolume(trackId: String, newVolume: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(volume = newVolume.coerceIn(0f, 1f))
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🎛️ FASE PRO: Ajusta el Swing global del secuenciador[span_13](start_span)[span_13](end_span)
    fun updateSwing(newSwing: Float) {
        _uiState.update { it.copy(swing = newSwing.coerceIn(0f, 1f)) }
    }

    // 🎛️ FASE PRO (Función diferencial): Modifica el tono (Pitch) independiente por pad[span_14](start_span)[span_14](end_span)
    fun updatePitch(trackId: String, newPitch: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(pitch = newPitch.coerceIn(0.5f, 2.0f)) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🎛️ FASE PRO (Función diferencial): Modifica el panorama (Pan) independiente por pad[span_15](start_span)[span_15](end_span)
    fun updatePan(trackId: String, newPan: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(pan = newPan.coerceIn(-1.0f, 1.0f)) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }
}
