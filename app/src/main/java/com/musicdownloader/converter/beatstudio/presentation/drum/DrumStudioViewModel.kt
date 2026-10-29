package com.musicdownloader.converter.beatstudio.presentation.drum

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.converter.engine.AudioEngine
import com.musicdownloader.converter.converter.engine.AssetAudioLoader
import com.musicdownloader.converter.converter.engine.AssetDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DrumStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrumStudioState())
    val uiState: StateFlow<DrumStudioState> = _uiState.asStateFlow()

    // 🌐 Estados expuestos desde AssetDownloader para gobernar la ventana de progreso (0% a 100%) en la UI
    val isDownloading = AssetDownloader.isDownloading
    val downloadProgress = AssetDownloader.downloadProgress

    // 🚨 Nuevo flujo para capturar errores de forma visual sin necesidad de Logcat
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    // Instancia del motor nativo Oboe de ultra baja latencia
    private val audioEngine = AudioEngine()

    init {
        // Enciende el motor Oboe automáticamente al abrir la pantalla de ritmos
        try {
            audioEngine.nativeStart()
        } catch (e: Exception) {
            _errorMessage.value = "⚠️ Error al iniciar el motor Oboe: ${e.message}"
        }

        // 🎹 Auto-asignar samples por defecto a los pads si ya están descargados los kits
        autoAssignDefaultSamplesIfNeeded()

        // 🥁 Bucle reactivo del secuenciador (Ticker basado en BPM)
        viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                if (state.isPlaying) {
                    // Cálculo de milisegundos por paso (dividido en semicorcheas por compás)
                    val intervalMs = (60_000L / state.bpm) / 4

                    _uiState.update { current ->
                        val nextStep = (current.currentStep + 1) % current.stepCount
                        
                        // Disparar de forma sincronizada los pads activos en este paso exacto
                        current.tracks.forEachIndexed { index, track ->
                            if (!track.isMuted && track.steps.getOrElse(nextStep) { false }) {
                                try {
                                    audioEngine.nativePlayPad(index)
                                } catch (e: Exception) {
                                    // Falla menor de disparo evitada para no congelar el loop
                                }
                            }
                        }
                        current.copy(currentStep = nextStep)
                    }
                    delay(intervalMs)
                } else {
                    delay(100) // Pausa eficiente cuando el secuenciador está detenido
                    if (_uiState.value.currentStep != -1) {
                        _uiState.update { it.copy(currentStep = -1) }
                    }
                }
            }
        }
    }

    /**
     * 🎹 Función inteligente: Escanea la memoria interna y asigna automáticamente
     * un sample por categoría a cada pista del secuenciador al arrancar la app.
     */
    private fun autoAssignDefaultSamplesIfNeeded() {
        try {
            val kitsDir = File(context.filesDir, "kits")
            if (kitsDir.exists() && kitsDir.listFiles()?.isNotEmpty() == true) {
                val currentState = _uiState.value
                currentState.tracks.forEachIndexed { index, track ->
                    val nameLower = track.name.lowercase()
                    val keyword = when {
                        nameLower.contains("kick") -> "kick"
                        nameLower.contains("snare") -> "snare"
                        nameLower.contains("hat") || nameLower.contains("closed") || nameLower.contains("open") -> "hat"
                        nameLower.contains("clap") -> "clap"
                        nameLower.contains("perc") -> "perc"
                        nameLower.contains("tom") -> "tom"
                        nameLower.contains("rim") -> "rim"
                        nameLower.contains("shaker") -> "shaker"
                        else -> nameLower.take(4)
                    }
                    
                    val matchedSamples = AssetAudioLoader.getFilteredSamplesForCategory(context, keyword)
                    if (matchedSamples.isNotEmpty()) {
                        AssetAudioLoader.loadSampleToPad(context, matchedSamples.first(), index)
                    }
                }
            }
        } catch (e: Exception) {
            // Manejo silencioso para no interrumpir el arranque
        }
    }

    /**
     * Dispara instantáneamente el sonido asignado al índice de la pista/pad.
     */
    fun playTrackSound(trackIndex: Int) {
        try {
            audioEngine.nativePlayPad(trackIndex)
        } catch (e: Exception) {
            _errorMessage.value = "⚠️ Error al reproducir pad $trackIndex: ${e.message}"
        }
    }

    /**
     * Obtiene la lista de sonidos disponibles en la memoria interna (filesDir/kits/) 
     * filtrados por categoría (ej. "kick", "snare").
     */
    fun getFilteredSamplesForCategory(categoryKeyword: String): List<String> {
        return try {
            AssetAudioLoader.getFilteredSamplesForCategory(context, categoryKeyword)
        } catch (e: Exception) {
            _errorMessage.value = "⚠️ Error al listar samples: ${e.message}"
            emptyList()
        }
    }

    /**
     * Carga un sample específico desde la memoria interna y lo asigna al pad/pista en caliente,
     * verificando que el motor C++ lo acepte correctamente.
     */
    fun assignSampleToTrack(trackIndex: Int, samplePath: String) {
        try {
            val success = AssetAudioLoader.loadSampleToPad(context, samplePath, trackIndex)
            if (!success) {
                _errorMessage.value = "⚠️ El motor C++ rechazó el audio en el pad $trackIndex. Posible formato .wav incompatible."
            } else {
                _errorMessage.value = null // Limpia cualquier error previo si cargó con éxito
            }
        } catch (e: Exception) {
            _errorMessage.value = "❌ Error al cargar sample en pad $trackIndex: ${e.message}"
        }
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

    // ▶️ Controles generales de reproducción y mezcla con validación de errores visuales
    fun togglePlayPause() {
        try {
            val currentState = _uiState.value
            val hasActiveSteps = currentState.tracks.any { track -> track.steps.any { it } }
            
            if (!currentState.isPlaying && !hasActiveSteps) {
                _errorMessage.value = "⚠️ Advertencia: No hay pasos marcados en el secuenciador para reproducir."
                return
            }

            audioEngine.nativeStart()
            _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        } catch (e: Exception) {
            _errorMessage.value = "❌ Error en reproducción: ${e.message}"
        }
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
