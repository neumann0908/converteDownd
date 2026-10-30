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

    // Memoria temporal para copiar y pegar patrones completos
    private var copiedTracksMemory: List<SequencerTrack>? = null

    // 🟢 Función auxiliar para actualizar las pistas del patrón activo de forma segura
    private fun updateCurrentPatternTracks(updateAction: (List<SequencerTrack>) -> List<SequencerTrack>) {
        _uiState.update { currentState ->
            val currentPattern = currentState.patterns[currentState.currentPatternIndex]
            val updatedTracks = updateAction(currentPattern.tracks)
            val updatedPatterns = currentState.patterns.mapIndexed { idx, pattern ->
                if (idx == currentState.currentPatternIndex) {
                    pattern.copy(tracks = updatedTracks)
                } else {
                    pattern
                }
            }
            currentState.copy(patterns = updatedPatterns)
        }
    }

    // 🎵 Activar o desactivar un cuadrito (paso) cuando lo tocas con el dedo en el patrón activo
    fun toggleStep(trackIndex: Int, stepIndex: Int) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
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
        }
    }

    // 🎚️ Actualizar la velocity (intensidad/volumen) de un paso específico en el patrón activo
    fun updateStepVelocity(trackIndex: Int, stepIndex: Int, newVelocity: Float) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
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
        }
    }

    // ⏱️ Actualizar la duración exacta (durationMs) de un paso específico
    fun updateStepDuration(trackIndex: Int, stepIndex: Int, newDuration: Long) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) step.copy(durationMs = newDuration.coerceIn(20L, 2000L)) else step
                    }
                    track.copy(steps = updatedSteps)
                } else track
            }
        }
    }

    // ⇄ Actualizar el desplazamiento temporal (Offset/Shift) de un paso específico
    fun updateStepOffset(trackIndex: Int, stepIndex: Int, newOffset: Long) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) step.copy(offsetMs = newOffset.coerceIn(-100L, 100L)) else step
                    }
                    track.copy(steps = updatedSteps)
                } else track
            }
        }
    }

    // 🎲 Actualizar la probabilidad de disparo de un paso específico
    fun updateStepProbability(trackIndex: Int, stepIndex: Int, newProb: Float) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val updatedSteps = track.steps.mapIndexed { sIdx, step ->
                        if (sIdx == stepIndex) step.copy(probability = newProb.coerceIn(0.0f, 1.0f)) else step
                    }
                    track.copy(steps = updatedSteps)
                } else track
            }
        }
    }

    // 🎚️ Actualizar el volumen general de una pista específica
    fun updateTrackVolume(trackIndex: Int, newVolume: Float) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(volume = newVolume.coerceIn(0.0f, 1.0f)) else track
            }
        }
    }

    // 🎧 Actualizar el paneo estéreo de una pista (-1.0 Izquierda a 1.0 Derecha)
    fun updateTrackPan(trackIndex: Int, newPan: Float) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(pan = newPan.coerceIn(-1.0f, 1.0f)) else track
            }
        }
    }

    // 🔇 Alternar el estado Mute (Silenciar) de una pista
    fun toggleTrackMute(trackIndex: Int) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(isMuted = !track.isMuted) else track
            }
        }
    }

    // 🎧 Alternar el estado Solo (Aislar) de una pista
    fun toggleTrackSolo(trackIndex: Int) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(isSolo = !track.isSolo) else track
            }
        }
    }

    // 🎹 Cambiar entre vista de Secuenciador de Batería y Piano Roll
    fun togglePianoRollMode(enabled: Boolean, trackIndex: Int = 3) {
        _uiState.update { it.copy(isPianoRollMode = enabled, selectedPianoTrackIndex = trackIndex) }
    }

    // 🎵 Actualizar el tono (pitch) de un paso específico en el Piano Roll del patrón activo
    fun updateStepPitch(trackIndex: Int, stepIndex: Int, newPitch: Int) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
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
        }
    }

    // 🟢 Asignar un archivo de audio (.wav) personalizado a una tecla/pitch específico en el patrón activo
    fun assignSampleToPitch(trackIndex: Int, pitch: Int, filePath: String) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { tIdx, track ->
                if (tIdx == trackIndex) {
                    val newMap = track.customPitchSamples.toMutableMap()
                    newMap[pitch] = filePath
                    track.copy(customPitchSamples = newMap)
                } else {
                    track
                }
            }
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

    // 🎛️ Cambiar el patrón activo (Patrón A, B, C)
    fun selectPattern(patternIndex: Int) {
        _uiState.update { currentState ->
            if (patternIndex in currentState.patterns.indices) {
                currentState.copy(currentPatternIndex = patternIndex)
            } else {
                currentState
            }
        }
    }

    // 🎛️ Duplicar el patrón actual hacia otro slot de patrón
    fun duplicatePattern(targetIndex: Int) {
        _uiState.update { currentState ->
            val currentPattern = currentState.patterns[currentState.currentPatternIndex]
            val updatedPatterns = currentState.patterns.mapIndexed { idx, pattern ->
                if (idx == targetIndex) {
                    pattern.copy(
                        tracks = currentPattern.tracks.map { track ->
                            track.copy(
                                steps = track.steps.map { it.copy() },
                                customPitchSamples = track.customPitchSamples.toMap()
                            )
                        }
                    )
                } else {
                    pattern
                }
            }
            currentState.copy(patterns = updatedPatterns)
        }
    }

    // 📋 Copiar el patrón activo actual a memoria temporal
    fun copyCurrentPattern() {
        val currentPattern = _uiState.value.patterns[_uiState.value.currentPatternIndex]
        copiedTracksMemory = currentPattern.tracks.map { track ->
            track.copy(
                steps = track.steps.map { it.copy() },
                customPitchSamples = track.customPitchSamples.toMap()
            )
        }
    }

    // 📋 Pegar los pasos/pistas copiados en el patrón activo actual
    fun pasteToCurrentPattern() {
        copiedTracksMemory?.let { memoryTracks ->
            updateCurrentPatternTracks { _ ->
                memoryTracks.map { track ->
                    track.copy(
                        steps = track.steps.map { it.copy() },
                        customPitchSamples = track.customPitchSamples.toMap()
                    )
                }
            }
        }
    }

    // 🎛️ Actualizar el nivel de Swing Global (Groove)
    fun updateSwing(newSwing: Float) {
        _uiState.update { it.copy(swing = newSwing.coerceIn(0.0f, 1.0f)) }
    }

    // 🎛️ Actualizar el Swing Individual de una pista específica
    fun updateTrackSwing(trackIndex: Int, newSwing: Float) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(trackSwing = newSwing.coerceIn(0.0f, 1.0f)) else track
            }
        }
    }

    // 🎛️ Alternar la humanización orgánica en una pista
    fun toggleTrackHumanize(trackIndex: Int) {
        updateCurrentPatternTracks { tracks ->
            tracks.mapIndexed { idx, track ->
                if (idx == trackIndex) track.copy(humanizeEnabled = !track.humanizeEnabled) else track
            }
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

    // ▶️ Iniciar el reloj maestro adaptado al sistema de arreglos musicales y Swing / Groove
    fun startPlayback() {
        if (_uiState.value.isPlaying) return
        
        _uiState.update { it.copy(isPlaying = true, currentSectionIndex = 0, currentLoopInverses = 0, currentStepIndex = -1) }
        
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val totalSteps = 16
            while (_uiState.value.isPlaying) {
                val state = _uiState.value
                val currentBpm = state.bpm
                val baseStepDelayMs = (60000f / currentBpm / 4f).toLong()

                val nextStep = (_uiState.value.currentStepIndex + 1) % totalSteps
                
                // Cálculo dinámico de Swing Global (micro-retraso en pasos impares / off-beats)
                val swingFactor = state.swing
                val stepDelayMs = if (nextStep % 2 != 0) {
                    (baseStepDelayMs * (1.0f + (swingFactor * 0.4f))).toLong()
                } else {
                    (baseStepDelayMs * (1.0f - (swingFactor * 0.4f))).toLong()
                }

                _uiState.update { currentState ->
                    val evaluatedNextStep = currentState.currentStepIndex + 1

                    if (evaluatedNextStep >= totalSteps) {
                        val arrangement = currentState.arrangement
                        if (arrangement.isNotEmpty()) {
                            val currentSec = arrangement.getOrNull(currentState.currentSectionIndex) ?: arrangement.first()
                            val loopsSoFar = currentState.currentLoopInverses + 1

                            if (loopsSoFar >= currentSec.repeatCount) {
                                val nextSecIndex = (currentState.currentSectionIndex + 1) % arrangement.size
                                playActiveStepsForColumn(0)
                                currentState.copy(
                                    currentStepIndex = 0,
                                    currentSectionIndex = nextSecIndex,
                                    currentLoopInverses = 0
                                )
                            } else {
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
                        playActiveStepsForColumn(evaluatedNextStep)
                        currentState.copy(currentStepIndex = evaluatedNextStep)
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

    // ➕ Añadir una nueva sección al arreglo
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

    // 🔊 Dispara los sonidos aplicando reglas de Solo/Mute, Probabilidad, Offset y Duración
    private fun playActiveStepsForColumn(stepIndex: Int) {
        val state = _uiState.value
        
        if (state.isMetronomeEnabled && stepIndex % 4 == 0) {
            playMetronomeClick()
        }

        val tracks = state.tracks
        val anySoloActive = tracks.any { it.isSolo }

        for (track in tracks) {
            // Regla DAW: Si hay pistas en Solo, solo suenan las que tengan Solo activo. Si no, se respeta Mute.
            val shouldPlay = if (anySoloActive) track.isSolo else !track.isMuted
            
            if (shouldPlay) {
                val step = track.steps.getOrNull(stepIndex)
                if (step != null && step.isActive) {
                    // 🎲 Evaluación de probabilidad de disparo
                    val passesProbability = step.probability >= 1.0f || (Math.random() <= step.probability)
                    if (passesProbability) {
                        val totalDelay = (step.offsetMs).coerceAtLeast(0L)
                        if (totalDelay > 0L) {
                            viewModelScope.launch {
                                delay(totalDelay)
                                playTrackSound(track, step.pitch, step.velocity, step.durationMs)
                            }
                        } else {
                            playTrackSound(track, step.pitch, step.velocity, step.durationMs)
                        }
                    }
                }
            }
        }
    }

    // 🎚️ Reproductor inteligente adaptado con Paneo Estéreo (-1.0 a 1.0) y Volumen por Pista
    private fun playTrackSound(track: SequencerTrack, pitch: Int, velocity: Float, durationMs: Long) {
        try {
            val exportDir = File(context.filesDir, "exports")
            
            val finalVelocity = if (track.humanizeEnabled) {
                val jitter = (0.85f + (Math.random() * 0.30f)).toFloat()
                (velocity * jitter).coerceIn(0.1f, 1.0f)
            } else {
                velocity
            }

            val customPath = track.customPitchSamples[pitch]
            val sampleFile = if (customPath != null && File(customPath).exists()) {
                File(customPath)
            } else {
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
                    File(exportDir, "${track.trackName.lowercase()}.wav")
                }
            }

            if (sampleFile.exists()) {
                val mediaPlayer = MediaPlayer.create(context, Uri.fromFile(sampleFile))
                mediaPlayer?.apply {
                    // 🎧 Cálculo de Paneo Estéreo y Volumen Real
                    val baseVol = track.volume * finalVelocity
                    val pan = track.pan.coerceIn(-1.0f, 1.0f)
                    val leftVol = baseVol * (1.0f - pan.coerceAtLeast(0.0f))
                    val rightVol = baseVol * (1.0f + pan.coerceAtMost(0.0f))

                    setVolume(leftVol, rightVol)
                    start()
                    viewModelScope.launch {
                        delay(durationMs)
                        try {
                            if (isPlaying) {
                                stop()
                            }
                            release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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

    // 🎚️ Cambiar el compás
    fun updateTimeSignature(signature: String) {
        _uiState.update { it.copy(timeSignature = signature) }
    }

    // 🎛️ Cambiar la resolución
    fun updateResolution(res: String) {
        _uiState.update { it.copy(resolution = res) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }
}
