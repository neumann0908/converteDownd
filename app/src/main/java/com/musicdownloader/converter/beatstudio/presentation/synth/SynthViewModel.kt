package com.musicdownloader.converter.beatstudio.presentation.synth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado visual del Synth Studio.
 *
 * Contiene los parámetros que la interfaz necesita
 * para representar y controlar el sintetizador.
 */
data class SynthUiState(

    // Instrumento y parámetros actuales.
    val currentPatch: SynthPatch = SynthPatch(
        instrumentType = SynthInstrumentType.SYNTH_808
    ),

    // Volumen general: rango 0.0 - 1.0.
    val masterVolume: Float = 1.0f,

    // Paneo: -1.0 izquierda, 0.0 centro, 1.0 derecha.
    val pan: Float = 0.0f
)

/**
 * ViewModel principal del Synth Studio.
 *
 * Responsabilidades:
 * 1. Mantener el estado de la interfaz.
 * 2. Actualizar los parámetros del sintetizador.
 * 3. Validar los valores recibidos.
 * 4. Enviar los parámetros al SynthEngineCore.
 */
@HiltViewModel
class SynthViewModel @Inject constructor() : ViewModel() {

    // -------------------------------------------------
    // 1. ESTADO INTERNO Y MEMORIA DE PORTAMENTO
    // -------------------------------------------------

    private val _uiState = MutableStateFlow(SynthUiState())

    /**
     * Estado observable desde Jetpack Compose.
     *
     * La interfaz puede leerlo, pero no modificarlo directamente.
     */
    val uiState: StateFlow<SynthUiState> = _uiState.asStateFlow()

    // Memoria de la última nota tocada para calcular el efecto Glide / Portamento
    private var lastFrequency: Float = 440.0f


    // -------------------------------------------------
    // 2. SELECCIÓN DE INSTRUMENTO
    // -------------------------------------------------

    /**
     * Selecciona un instrumento del catálogo.
     *
     * Al seleccionar otro instrumento se utiliza
     * su forma de onda predeterminada.
     */
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


    // -------------------------------------------------
    // 3. CONTROL DE FORMA DE ONDA
    // -------------------------------------------------

    /**
     * Permite cambiar la forma de onda manualmente.
     */
    fun updateWaveform(waveform: WaveformType) {

        _uiState.update { state ->

            state.copy(
                currentPatch = state.currentPatch.copy(
                    waveform = waveform
                )
            )
        }
    }


    // -------------------------------------------------
    // 4. CONTROL DEL FILTRO: CUTOFF
    // -------------------------------------------------

    /**
     * Modifica la frecuencia de corte del filtro.
     *
     * Rango permitido: 20 Hz - 20000 Hz.
     */
    fun updateCutoff(cutoff: Float) {

        val safeCutoff = if (cutoff.isFinite()) {
            cutoff.coerceIn(20f, 20000f)
        } else {
            5000f
        }

        _uiState.update { state ->

            state.copy(
                currentPatch = state.currentPatch.copy(
                    cutoffFreq = safeCutoff
                )
            )
        }
    }


    // -------------------------------------------------
    // 5. CONTROL DE RESONANCIA
    // -------------------------------------------------

    /**
     * Modifica la resonancia Q del filtro.
     *
     * Rango permitido: 0.1 - 10.0.
     */
    fun updateResonance(resonance: Float) {

        val safeResonance = if (resonance.isFinite()) {
            resonance.coerceIn(0.1f, 10f)
        } else {
            0.707f
        }

        _uiState.update { state ->

            state.copy(
                currentPatch = state.currentPatch.copy(
                    resonance = safeResonance
                )
            )
        }
    }


    // -------------------------------------------------
    // 6. CONTROL DE ENVOLVENTE ADSR
    // -------------------------------------------------

    /**
     * Actualiza Attack, Decay, Sustain y Release.
     *
     * Attack, Decay y Release se expresan en milisegundos.
     * Sustain utiliza un valor normalizado entre 0 y 1.
     */
    fun updateAdsr(
        attack: Float,
        decay: Float,
        sustain: Float,
        release: Float
    ) {

        val safeAttack = if (attack.isFinite()) {
            attack.coerceIn(0f, 10000f)
        } else {
            15f
        }

        val safeDecay = if (decay.isFinite()) {
            decay.coerceIn(0f, 10000f)
        } else {
            200f
        }

        val safeSustain = if (sustain.isFinite()) {
            sustain.coerceIn(0f, 1f)
        } else {
            0.7f
        }

        val safeRelease = if (release.isFinite()) {
            release.coerceIn(0f, 10000f)
        } else {
            300f
        }

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


    // -------------------------------------------------
    // 7. CONTROL DE LFO
    // -------------------------------------------------

    /**
     * Actualiza la velocidad (Rate) y la profundidad (Depth) del LFO.
     */
    fun updateLfo(rateHz: Float, depth: Float) {
        val safeRate = if (rateHz.isFinite()) {
            rateHz.coerceIn(0.1f, 20.0f)
        } else {
            2.0f
        }

        val safeDepth = if (depth.isFinite()) {
            depth.coerceIn(0.0f, 1.0f)
        } else {
            0.3f
        }

        val newLfo = LFOState(
            rateHz = safeRate,
            depth = safeDepth
        )

        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    lfo = newLfo
                )
            )
        }
    }


    // -------------------------------------------------
    // 8. CONTROL DE PORTAMENTO (GLIDE)
    // -------------------------------------------------

    /**
     * Actualiza el tiempo de Portamento en milisegundos.
     * Rango permitido: 0 ms - 1000 ms.
     */
    fun updatePortamento(portamentoMs: Float) {
        val safePortamento = if (portamentoMs.isFinite()) {
            portamentoMs.coerceIn(0f, 1000f)
        } else {
            0f
        }

        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    portamentoMs = safePortamento
                )
            )
        }
    }


    // -------------------------------------------------
    // 9. VOLUMEN MAESTRO
    // -------------------------------------------------

    /**
     * Controla el volumen general del sintetizador.
     *
     * Rango: 0.0 - 1.0.
     */
    fun updateMasterVolume(volume: Float) {

        val safeVolume = if (volume.isFinite()) {
            volume.coerceIn(0f, 1f)
        } else {
            1.0f
        }

        _uiState.update { state ->

            state.copy(
                masterVolume = safeVolume
            )
        }
    }


    // -------------------------------------------------
    // 10. PANEO ESTÉREO
    // -------------------------------------------------

    /**
     * Controla la posición estéreo.
     *
     * -1.0 = izquierda.
     *  0.0 = centro.
     *  1.0 = derecha.
     */
    fun updatePan(pan: Float) {

        val safePan = if (pan.isFinite()) {
            pan.coerceIn(-1f, 1f)
        } else {
            0f
        }

        _uiState.update { state ->

            state.copy(
                pan = safePan
            )
        }
    }


    // -------------------------------------------------
    // 11. REPRODUCCIÓN DE NOTA DE PRUEBA (CON SOPORTE PORTAMENTO)
    // -------------------------------------------------

    /**
     * Envía una nota de prueba al motor de síntesis.
     *
     * Calcula la frecuencia inicial de deslizamiento basándose en la nota anterior
     * si el portamento está activado.
     */
    fun playTestNote(frequency: Float) {

        // Capturamos el estado actual una sola vez.
        val state = _uiState.value

        // Validamos la frecuencia.
        if (!frequency.isFinite() || frequency <= 0f) {
            return
        }

        val safeFrequency = frequency.coerceIn(
            20f,
            20000f
        )

        // Extraemos los parámetros actuales.
        val patch = state.currentPatch
        val volume = state.masterVolume
        val currentPan = state.pan

        // Calculamos frecuencia inicial y de destino para el Glide
        val startFreq = if (patch.portamentoMs > 0f) lastFrequency else safeFrequency
        lastFrequency = safeFrequency

        // Enviamos la nota al motor con los parámetros de portamento.
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
