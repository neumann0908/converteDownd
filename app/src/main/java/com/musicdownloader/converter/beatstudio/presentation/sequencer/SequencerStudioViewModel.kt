package com.musicdownloader.converter.beatstudio.presentation.sequencer

import android.content.Context
import android.media.MediaPlayer
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
import java.io.File
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

    // 🎚️ Actualizar la velocity (intensidad/volumen) de un paso específico
    fun updateStepVelocity(trackIndex: Int, stepIndex: Int, newVelocity: Float) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) {
                            step.copy(velocity = newVelocity.coerceIn(0.1f, 1.0f))
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

    // ▶️ Iniciar el reloj maestro adaptado al sistema de arreglos musicales
    fun startPlayback() {
        if (_uiState.value.isPlaying) return
        
        _uiState.update { it.copy(isPlaying = true, currentSectionIndex = 0, currentLoopInverses = 0, currentStepIndex = -1) }
        
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val totalSteps = 16
            while (_uiState.value.isPlaying) {
                val state = _uiState.value
                val currentBpm = state.bpm
                // Calculamos el tiempo exacto de espera entre cada paso según los BPM
                val stepDelayMs = (60000f / currentBpm / 4f).toLong()

                _uiState.update { currentState ->
                    val nextStep = currentState.currentStepIndex + 1

                    if (nextStep >= totalSteps) {
                        // Fin de un loop de 16 pasos, evaluamos el bloque de arreglo actual
                        val arrangement = currentState.arrangement
                        if (arrangement.isNotEmpty()) {
                            val currentSec = arrangement.getOrNull(currentState.currentSectionIndex) ?: arrangement.first()
                            val loopsSoFar = currentState.currentLoopInverses + 1

                            if (loopsSoFar >= currentSec.repeatCount) {
                                // Pasar a la siguiente sección del arreglo de manera circular
                                val nextSecIndex = (currentState.currentSectionIndex + 1) % arrangement.size
                                playActiveStepsForColumn(0)
                                currentState.copy(
                                    currentStepIndex = 0,
                                    currentSectionIndex = nextSecIndex,
                                    currentLoopInverses = 0
                                )
                            } else {
                                // Repetir la misma sección actual
                                playActiveStepsForColumn(0)
                                currentState.copy(
                                    currentStepIndex = 0,
                                    currentLoopInverses = loopsSoFar
                                )
                            }
                        } else {
                            playActiveStepsForColumn(0)
                            currentState.copy(currentStepIndex = 0)
                        }
                    } else {
                        playActiveStepsForColumn(nextStep)
                        currentState.copy(currentStepIndex = nextStep)
                    }
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

    // 🟢 Activar o desactivar el metrónomo
    fun toggleMetronome() {
        _uiState.update { it.copy(isMetronomeEnabled = !it.isMetronomeEnabled) }
    }

    // ➕ Añadir una nueva sección al arreglo (Intro, Verso, Estribillo, etc.)
    fun addSongSection(type: SongSectionType) {
        val namePrefix = when (type) {
            SongSectionType.INTRO -> "Intro"
            SongSectionType.VERSO -> "Verso"
            SongSectionType.ESTRIBILLO -> "Estribillo"
            SongSectionType.PUENTE -> "Puente"
            SongSectionType.OUTRO -> "Outro"
        }
        val newSection = SongSection(
            id = System.currentTimeMillis().toString(),
            type = type,
            name = "$namePrefix ${(_uiState.value.arrangement.size + 1)}",
            repeatCount = 2
        )
        _uiState.update { it.copy(arrangement = it.arrangement + newSection) }
    }

    // ❌ Eliminar una sección del arreglo por su ID
    fun removeSongSection(sectionId: String) {
        _uiState.update { it.copy(arrangement = it.arrangement.filter { sec -> sec.id != sectionId }) }
    }

    // 🔊 Dispara los sonidos que están activos en la columna actual y el metrónomo
    private fun playActiveStepsForColumn(stepIndex: Int) {
        val state = _uiState.value
        
        // Si el metrónomo está activo, reproducimos un clic en cada tiempo principal (ej. cada 4 pasos)
        if (state.isMetronomeEnabled && stepIndex % 4 == 0) {
            playMetronomeClick()
        }

        for (track in state.tracks) {
            if (!track.isMuted) {
                val step = track.steps.getOrNull(stepIndex)
                if (step != null && step.isActive) {
                    playTrackSound(track.trackName, step.velocity)
                }
            }
        }
    }

    // 🎚️ Reproductor individual para cada pista conectado a la carpeta exports/
    private fun playTrackSound(trackName: String, velocity: Float) {
        try {
            val exportDir = File(context.filesDir, "exports")
            val sampleFile = File(exportDir, "${trackName.lowercase()}.wav")

            val mediaPlayer = if (sampleFile.exists()) {
                MediaPlayer.create(context, Uri.fromFile(sampleFile))
            } else {
                null
            }

            mediaPlayer?.apply {
                setVolume(velocity, velocity)
                setOnCompletionListener { mp ->
                    mp.release()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔊 Reproductor para el clic del metrónomo
    private fun playMetronomeClick() {
        try {
            val exportDir = File(context.filesDir, "exports")
            val clickFile = File(exportDir, "metronome.wav")
            if (clickFile.exists()) {
                val mediaPlayer = MediaPlayer.create(context, Uri.fromFile(clickFile))
                mediaPlayer?.apply {
                    setOnCompletionListener { it.release() }
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
