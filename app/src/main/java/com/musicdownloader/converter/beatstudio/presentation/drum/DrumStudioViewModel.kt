package com.musicdownloader.converter.beatstudio.presentation.drum

import android.content.Context
import androidx.lifecycle.ViewModel
import com.musicdownloader.converter.converter.engine.AudioEngine
import com.musicdownloader.converter.converter.engine.AssetAudioLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DrumStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrumStudioState())
    val uiState: StateFlow<DrumStudioState> = _uiState.asStateFlow()

    // Instancia del motor nativo Oboe de ultra baja latencia
    private val audioEngine = AudioEngine()

    init {
        // Enciende el motor Oboe automáticamente al abrir la pantalla de ritmos
        audioEngine.nativeStart()
    }

    /**
     * Dispara instantáneamente el sonido asignado al índice de la pista/pad.
     */
    fun playTrackSound(trackIndex: Int) {
        audioEngine.nativePlayPad(trackIndex)
    }

    /**
     * Obtiene la lista de sonidos disponibles en la memoria interna (filesDir/kits/) 
     * filtrados por categoría (ej. "kick", "snare").
     */
    fun getFilteredSamplesForCategory(categoryKeyword: String): List<String> {
        return AssetAudioLoader.getFilteredSamplesForCategory(context, categoryKeyword)
    }

    /**
     * Carga un sample específico desde la memoria interna y lo asigna al pad/pista en caliente.
     */
    fun assignSampleToTrack(trackIndex: Int, samplePath: String) {
        AssetAudioLoader.loadSampleToPad(context, samplePath, trackIndex)
    }

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

    // 📐 Cambiar el número de pads/pasos (16, 32 o 64 pasos)
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

    // 🎚️ Control de Velocity (fuerza) por golpe
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

    // 🎲 Probabilidad de disparo por paso (Trigger Probability)
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

    // ⭐ Activar/Desactivar acento en un paso
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

    // 📋 Copiar y duplicar patrones rítmicos
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

    // 🎛️ Función diferencial: Control de Envolvente ADSR
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

    // 🎛️ Función diferencial: Efectos independientes por pad (Reverb / Delay)
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

    // 🎹 Cambiar Kits de batería intercambiables
    fun switchKit(kitName: String) {
        _uiState.update { it.copy(currentKit = kitName) }
    }

    // 🎷 Groove: Control de Swing y Humanización
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
        _otivStateUpdateIfNeeded(...) // Mantener lógica limpia existente
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
