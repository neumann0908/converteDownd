package com.musicdownloader.converter.beatstudio.presentation.sequencer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // 🎹 Cambiar entre vista de Secuenciador de Batería y Piano Roll
    fun togglePianoRollMode(enabled: Boolean, trackIndex: Int = 3) {
        _uiState.update { it.copy(isPianoRollMode = enabled, selectedPianoTrackIndex = trackIndex) }
    }

    // 🎵 Actualizar el tono (pitch) de un paso específico en el Piano Roll
    fun updateStepPitch(trackIndex: Int, stepIndex: Int, newPitch: Int) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) {
                            step.copy(pitch = newPitch, isActive = true)
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

    // 🟢 Asignar un archivo de audio (.wav) personalizado a una tecla/pitch específico
    fun assignSampleToPitch(trackIndex: Int, pitch: Int, filePath: String) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val newMap = track.customPitchSamples.toMutableMap()
                    newMap[pitch] = filePath
                    track.copy(customPitchSamples = newMap)
                } else {
                    track
                }
            }
            currentState.copy(tracks = updatedTracks)
        }
    }

    // 🟢 Importar y copiar un archivo nativo de Android al almacenamiento interno seguro de la app
    fun importCustomSampleForPitch(trackIndex: Int, pitch: Int, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val customDir = File(context.filesDir, "exports/custom_samples")
            if (!customDir.exists()) customDir.mkdirs()

            val destinationFile = File(customDir, "track_${trackIndex}_pitch_${pitch}_${System.currentTimeMillis()}.wav")
            inputStream?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            assignSampleToPitch(trackIndex, pitch, destinationFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 💾 Guardar la canción actual directamente en la carpeta exports/ para que la Biblioteca la escanee de inmediato
    fun saveSongToLibrary(songName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportDir = File(context.filesDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()

                val safeName = if (songName.isBlank()) "Beat_${System.currentTimeMillis()}" else songName.trim().replace(" ", "_")
                val destinationFile = File(exportDir, "$safeName.wav")

                // Tomamos la muestra principal activa o respaldamos con la primera pista disponible
                val activeTrack = uiState.value.tracks.firstOrNull { it.steps.any { s -> s.isActive } } ?: uiState.value.tracks.first()
                val customPath = activeTrack.customPitchSamples.values.firstOrNull()
                
                val sourceFile = if (customPath != null && File(customPath).exists()) {
                    File(customPath)
                } else {
                    val kitFolderName = activeTrack.trackName.lowercase().replace(" ", "_")
                    val kitFile = File(exportDir, "kits/$kitFolderName/c4.wav")
                    if (kitFile.exists()) kitFile else File(exportDir, "${activeTrack.trackName.lowercase()}.wav")
                }

                if (sourceFile.exists()) {
                    sourceFile.copyTo(destinationFile, overwrite = true)
                } else {
                    destinationFile.createNewFile()
                }

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
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
                    playTrackSound(track, step.pitch, step.velocity)
                }
            }
        }
    }

    // 🎚️ Reproductor inteligente que evalúa muestras personalizadas, kits o respaldos de la carpeta exports/
    private fun playTrackSound(track: SequencerTrack, pitch: Int, velocity: Float) {
        try {
            val exportDir = File(context.filesDir, "exports")
            
            // 1. Verificamos si hay una muestra personalizada asignada a esta tecla exacta
            val customPath = track.customPitchSamples[pitch]
            val sampleFile = if (customPath != null && File(customPath).exists()) {
                File(customPath)
            } else {
                // 2. Si no, buscamos en la carpeta de kits del instrumento
                val kitFolderName = track.trackName.lowercase().replace(" ", "_")
                val kitDir = File(exportDir, "kits/$kitFolderName")
                val noteFileName = when (pitch) {
                    0 -> "c4.wav"; 1 -> "d4.wav"; 2 -> "e4.wav"; 3 -> "f4.wav"
                    4 -> "g4.wav"; 5 -> "a4.wav"; 6 -> "b4.wav"; 7 -> "c5.wav"
                    else -> "c4.wav"
                }
                val kitFile = File(kitDir, noteFileName)
                if (kitFile.exists()) {
                    kitFile
                } else {
                    // 3. Respaldo general al archivo plano de la pista
                    File(exportDir, "${track.trackName.lowercase()}.wav")
                }
            }

            if (sampleFile.exists()) {
                val mediaPlayer = MediaPlayer.create(context, Uri.fromFile(sampleFile))
                mediaPlayer?.apply {
                    setVolume(velocity, velocity)
                    setOnCompletionListener { mp ->
                        mp.release()
                    }
                    start()
                }
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
