package com.musicdownloader.converter.beatstudio.presentation.synth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SynthUiState(
    val currentPatch: SynthPatch = SynthPatch(instrumentType = SynthInstrumentType.SYNTH_808),
    val masterVolume: Float = 1.0f,
    val pan: Float = 0.0f
)

@HiltViewModel
class SynthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SynthUiState())
    val uiState: StateFlow<SynthUiState> = _uiState.asStateFlow()

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
            state.copy(currentPatch = state.currentPatch.copy(waveform = waveform))
        }
    }

    // 🎛️ Actualizar el corte de frecuencia del filtro (Cutoff)
    fun updateCutoff(cutoff: Float) {
        _uiState.update { state ->
            state.copy(currentPatch = state.currentPatch.copy(cutoffFreq = cutoff))
        }
    }

    fun updateAdsr(attack: Float, decay: Float, sustain: Float, release: Float) {
        _uiState.update { state ->
            val newAdsr = ADSRState(attackMs = attack, decayMs = decay, sustainLevel = sustain, releaseMs = release)
            state.copy(currentPatch = state.currentPatch.copy(adsr = newAdsr))
        }
    }

    fun playTestNote(frequency: Float) {
        val patch = _uiState.value.currentPatch
        val volume = _uiState.value.masterVolume
        val pan = _uiState.value.pan

        SynthEngineCore.playTone(
            frequency = frequency,
            durationMs = 600L,
            patch = patch,
            volume = volume,
            pan = pan
        )
    }
}
