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

    // 🥁 Encender o apagar una casilla de ritmo en el secuenciador
    fun toggleStep(trackId: String, stepIndex: Int) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    val newSteps = track.steps.toMutableList()
                    if (stepIndex < newSteps.size) {
                        newSteps[stepIndex] = !newSteps[stepIndex]
                    }
                    track.copy(steps = newSteps)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 📐 Cambiar el número de pads/pasos (16, 32 o 64 pasos)[span_29](start_span)[span_29](end_span)
    fun setStepCount(count: Int) {
        val validCount = when (count) {
            16, 32, 64 -> count
            else -> 16
        }
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                val newSteps = track.steps.toMutableList().apply {
                    while (size < validCount) add(false)
                    if (size > validCount) subList(validCount, size).clear()
                }
                val newVelocities = track.velocities.toMutableList().apply {
                    while (size < validCount) add(0.8f)
                    if (size > validCount) subList(validCount, size).clear()
                }
                val newProbabilities = track.probabilities.toMutableList().apply {
                    while (size < validCount) add(1.0f)
                    if (size > validCount) subList(validCount, size).clear()
                }
                val newAccents = track.accents.toMutableList().apply {
                    while (size < validCount) add(false)
                    if (size > validCount) subList(validCount, size).clear()
                }
                track.copy(
                    steps = newSteps,
                    velocities = newVelocities,
                    probabilities = newProbabilities,
                    accents = newAccents
                )
            }
            currentState.copy(stepCount = validCount, tracks = updatedTracks)
        }
    }

    // 🎚️ Control de Velocity (fuerza) por golpe[span_30](start_span)[span_30](end_span)[span_31](start_span)[span_31](end_span)
    fun updateStepVelocity(trackId: String, stepIndex: Int, velocity: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    val newVel = track.velocities.toMutableList()
                    if (stepIndex < newVel.size) {
                        newVel[stepIndex] = velocity.coerceIn(0.1f, 1.0f)
                    }
                    track.copy(velocities = newVel)
                } else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🎲 Probabilidad de disparo por paso (Trigger Probability)[span_32](start_span)[span_32](end_span)
    fun updateStepProbability(trackId: String, stepIndex: Int, probability: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    val newProb = track.probabilities.toMutableList()
                    if (stepIndex < newProb.size) {
                        newProb[stepIndex] = probability.coerceIn(0.0f, 1.0f)
                    }
                    track.copy(probabilities = newProb)
                } else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // ⭐ Activar/Desactivar acento en un paso[span_33](start_span)[span_33](end_span)
    fun toggleStepAccent(trackId: String, stepIndex: Int) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    val newAccents = track.accents.toMutableList()
                    if (stepIndex < newAccents.size) {
                        newAccents[stepIndex] = !newAccents[stepIndex]
                    }
                    track.copy(accents = newAccents)
                } else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 📋 Copiar y duplicar patrones rítmicos[span_34](start_span)[span_34](end_span)
    fun duplicatePattern() {
        _uiState.update { currentState ->
            val currentSize = currentState.stepCount
            val updatedTracks = currentState.tracks.map { track ->
                val doubledSteps = (track.steps + track.steps).take(currentSize * 2)
                val doubledVel = (track.velocities + track.velocities).take(currentSize * 2)
                track.copy(steps = doubledSteps, velocities = doubledVel)
            }
            currentState.copy(
                stepCount = (currentSize * 2).coerceAtMost(64),
                tracks = updatedTracks
            )
        }
    }

    // 🎛️ Función diferencial: Control de Envolvente ADSR (Ataque, Caída, Sostenimiento, Liberación)[span_35](start_span)[span_35](end_span)
    fun updateAdsr(trackId: String, attack: Float, decay: Float, sustain: Float, release: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(
                        attack = attack.coerceIn(0.001f, 1f),
                        decay = decay.coerceIn(0.01f, 2f),
                        sustain = sustain.coerceIn(0f, 1f),
                        release = release.coerceIn(0.01f, 3f)
                    )
                } else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🎛️ Función diferencial: Efectos independientes por pad (Reverb / Delay)[span_36](start_span)[span_36](end_span)
    fun updateEffects(trackId: String, reverb: Float, delay: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(
                        reverb = reverb.coerceIn(0f, 1f),
                        delay = delay.coerceIn(0f, 1f)
                    )
                } else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🎹 Cambiar Kits de batería intercambiables[span_37](start_span)[span_37](end_span)[span_38](start_span)[span_38](end_span)
    fun switchKit(kitName: String) {
        _uiState.update { it.copy(currentKit = kitName) }
    }

    // 🎷 Groove: Control de Swing y Humanización[span_39](start_span)[span_39](end_span)[span_40](start_span)[span_40](end_span)
    fun updateSwing(newSwing: Float) {
        _uiState.update { it.copy(swing = newSwing.coerceIn(0f, 1f)) }
    }

    fun updateHumanization(newHumanization: Float) {
        _uiState.update { it.copy(humanization = newHumanization.coerceIn(0f, 1f)) }
    }

    // ▶️ Controles generales de reproducción y mezcla
    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun updateBpm(newBpm: Int) {
        _uiState.update { it.copy(bpm = newBpm.coerceIn(40, 300)) }
    }

    fun selectTrack(index: Int) {
        _uiState.update { it.copy(selectedTrackIndex = index) }
    }

    fun toggleMute(trackId: String) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(isMuted = !track.isMuted) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    fun toggleSolo(trackId: String) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(isSolo = !track.isSolo) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    fun updateVolume(trackId: String, newVolume: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(volume = newVolume.coerceIn(0f, 1f)) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    fun updatePitch(trackId: String, newPitch: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(pitch = newPitch.coerceIn(0.25f, 4.0f)) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    fun updatePan(trackId: String, newPan: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.map { track ->
                if (track.id == trackId) track.copy(pan = newPan.coerceIn(-1.0f, 1.0f)) else track
            }
            currentState.copy(tracks = updatedTracks)
        }
    }
}
