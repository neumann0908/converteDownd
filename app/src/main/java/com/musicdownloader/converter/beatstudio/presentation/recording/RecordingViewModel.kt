package com.musicdownloader.converter.beatstudio.presentation.synth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Evento individual de nota para el secuenciador y grabación de melodías.
 */
data class NoteEvent(
    val frequency: Float,
    val startTimestampMs: Long,
    val durationMs: Long,
    val volume: Float
)

/**
 * Estructura de preset segura sin conflictos de corchetes angulares.
 */
data class SynthPreset(
    val name: String,
    val patch: SynthPatch
)

class SynthPresetList : ArrayList<SynthPreset>()

/**
 * Proyecto musical completo de Synth Studio (Parche + Secuencia de Melodía).
 */
data class SynthProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val creationDate: Long = System.currentTimeMillis(),
    val patch: SynthPatch,
    val notes: List<NoteEvent>
)

/**
 * Estado visual del Synth Studio.
 */
data class SynthUiState(
    val currentPatch: SynthPatch = SynthPatch(
        instrumentType = SynthInstrumentType.SYNTH_808
    ),
    val masterVolume: Float = 1.0f,
    val pan: Float = 0.0f
)

/**
 * ViewModel principal del Synth Studio.
 */
@HiltViewModel
class SynthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SynthUiState())
    val uiState = _uiState.asStateFlow()

    private var lastFrequency: Float = 440.0f

    fun selectInstrument(instrumentType: SynthInstrumentType) {
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    instrumentType = instrumentType,
                    waveform = instrumentType.defaultWaveform
                )
            )
        }
    }

    fun updateWaveform(waveform: WaveformType) {
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    waveform = waveform
                )
            )
        }
    }

    fun updateCutoff(cutoff: Float) {
        val safeCutoff = if (cutoff.isFinite()) cutoff.coerceIn(20f, 20000f) else 5000f
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    cutoffFreq = safeCutoff
                )
            )
        }
    }

    fun updateResonance(resonance: Float) {
        val safeResonance = if (resonance.isFinite()) resonance.coerceIn(0.1f, 10f) else 0.707f
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    resonance = safeResonance
                )
            )
        }
    }

    fun updateAdsr(
        attack: Float,
        decay: Float,
        sustain: Float,
        release: Float
    ) {
        val safeAttack = if (attack.isFinite()) attack.coerceIn(0f, 10000f) else 15f
        val safeDecay = if (decay.isFinite()) decay.coerceIn(0f, 10000f) else 200f
        val safeSustain = if (sustain.isFinite()) sustain.coerceIn(0f, 1f) else 0.7f
        val safeRelease = if (release.isFinite()) release.coerceIn(0f, 10000f) else 300f

        val newAdsr = ADSRState(
            attackMs = safeAttack,
            decayMs = safeDecay,
            sustainLevel = safeSustain,
            releaseMs = safeRelease
        )

        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    adsr = newAdsr
                )
            )
        }
    }

    fun updateLfo(rateHz: Float, depth: Float) {
        val safeRate = if (rateHz.isFinite()) rateHz.coerceIn(0.1f, 20.0f) else 2.0f
        val safeDepth = if (depth.isFinite()) depth.coerceIn(0.0f, 1.0f) else 0.3f

        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    lfo = state.currentPatch.lfo.copy(
                        rateHz = safeRate,
                        depth = safeDepth
                    )
                )
            )
        }
    }

    fun updateLfoDestination(destination: ModulationDestination) {
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    lfo = state.currentPatch.lfo.copy(
                        destination = destination
                    )
                )
            )
        }
    }

    fun updatePortamento(portamentoMs: Float) {
        val safePortamento = if (portamentoMs.isFinite()) portamentoMs.coerceIn(0f, 1000f) else 0f
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    portamentoMs = safePortamento
                )
            )
        }
    }

    fun updateUnison(voices: Int) {
        val safeVoices = voices.coerceIn(1, 5)
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    unisonVoices = safeVoices
                )
            )
        }
    }

    fun updateDetune(detune: Float) {
        val safeDetune = if (detune.isFinite()) detune.coerceIn(0.0f, 1.0f) else 0.0f
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    detuneAmount = safeDetune
                )
            )
        }
    }

    fun updateFX(reverbMix: Float, delayMix: Float, distortionDrive: Float) {
        val safeReverb = if (reverbMix.isFinite()) reverbMix.coerceIn(0.0f, 1.0f) else 0.0f
        val safeDelay = if (delayMix.isFinite()) delayMix.coerceIn(0.0f, 1.0f) else 0.0f
        val safeDistortion = if (distortionDrive.isFinite()) distortionDrive.coerceIn(0.0f, 1.0f) else 0.0f

        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    fx = FXState(
                        reverbMix = safeReverb,
                        delayMix = safeDelay,
                        distortionDrive = safeDistortion
                    )
                )
            )
        }
    }

    fun updateMasterVolume(volume: Float) {
        val safeVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1.0f
        _uiState.update { state ->
            state.copy(masterVolume = safeVolume)
        }
    }

    fun updatePan(pan: Float) {
        val safePan = if (pan.isFinite()) pan.coerceIn(-1f, 1f) else 0f
        _uiState.update { state ->
            state.copy(pan = safePan)
        }
    }

    // -------------------------------------------------
    // SISTEMA DE PRESETS Y EXPORTACIÓN A BIBLIOTECA
    // -------------------------------------------------

    private val _savedPresets = MutableStateFlow(SynthPresetList())
    val savedPresets = _savedPresets.asStateFlow()

    private val _currentPresetName = MutableStateFlow("Default Patch")
    val currentPresetName = _currentPresetName.asStateFlow()

    private val recordedNotes = mutableListOf<NoteEvent>()
    private var recordingStartTime = 0L
    private var isRecordingActive = false

    fun saveCurrentPreset(name: String) {
        if (name.isBlank()) return
        val patchToSave = _uiState.value.currentPatch
        val currentList = _savedPresets.value
        val newList = SynthPresetList().apply {
            addAll(currentList.filter { it.name != name })
            add(SynthPreset(name, patchToSave))
        }
        _savedPresets.value = newList
        _currentPresetName.value = name
    }

    fun loadPreset(name: String) {
        val preset = _savedPresets.value.find { it.name == name } ?: return
        _uiState.update { state ->
            state.copy(currentPatch = preset.patch)
        }
        _currentPresetName.value = name
    }

    fun startRecordingMelody() {
        recordedNotes.clear()
        recordingStartTime = System.currentTimeMillis()
        isRecordingActive = true
    }

    fun stopRecordingMelody(): List<NoteEvent> {
        isRecordingActive = false
        return recordedNotes.toList()
    }

    fun saveMelodyToLibrary(title: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanTitle = title.ifBlank { "Synth_Melodia_${System.currentTimeMillis()}" }
                val exportDir = File(context.filesDir, "exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val wavFile = File(exportDir, "$cleanTitle.wav")
                val patch = _uiState.value.currentPatch
                val volume = _uiState.value.masterVolume
                val pan = _uiState.value.pan

                // Exportación secuenciada nota por nota basada en la grabación real
                SynthEngineCore.exportSynthMelodyToWav(
                    outputFile = wavFile,
                    notes = recordedNotes.toList(),
                    patch = patch,
                    volume = volume,
                    pan = pan
                )

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

    fun playTestNote(frequency: Float) {
        val state = _uiState.value
        if (!frequency.isFinite() || frequency <= 0f) return

        val safeFrequency = frequency.coerceIn(20f, 20000f)
        val patch = state.currentPatch
        val volume = state.masterVolume
        val currentPan = state.pan

        if (isRecordingActive) {
            val timestamp = System.currentTimeMillis() - recordingStartTime
            recordedNotes.add(
                NoteEvent(
                    frequency = safeFrequency,
                    startTimestampMs = timestamp,
                    durationMs = 600L,
                    volume = volume
                )
            )
        }

        val startFreq = if (patch.portamentoMs > 0f) lastFrequency else safeFrequency
        lastFrequency = safeFrequency

        SynthEngineCore.playTone(
            frequency = safeFrequency,
            startFrequency = startFreq,
            portamentoMs = patch.portamentoMs,
            durationMs = 600L,
            patch = patch,
            volume = volume,
            pan = currentPan
        )
    }
}
