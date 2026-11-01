package com.musicdownloader.converter.beatstudio.presentation.synth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 * 5. Gestionar Presets, Secuenciador y Exportación de Melodías hacia la Biblioteca.
 */
@HiltViewModel
class SynthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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
    // 7. CONTROL DE LFO & MATRIZ DE MODULACIÓN EXPANDIDA
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

    /**
     * Actualiza el parámetro destino al que afectará el LFO (Cutoff, Pitch o Pan).
     */
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
    // 9. CONTROL DE UNISON & DETUNE (SUPERSAW)
    // -------------------------------------------------

    /**
     * Actualiza la cantidad de voces Unison (1 a 5).
     */
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

    /**
     * Actualiza la intensidad del microdesfase de afinación Detune (0.0 a 1.0).
     */
    fun updateDetune(detune: Float) {
        val safeDetune = if (detune.isFinite()) {
            detune.coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }
        _uiState.update { state ->
            state.copy(
                currentPatch = state.currentPatch.copy(
                    detuneAmount = safeDetune
                )
            )
        }
    }


    // -------------------------------------------------
    // 10. CONTROL DEL RACK DE EFECTOS (FX RACK)
    // -------------------------------------------------

    /**
     * Actualiza los parámetros del Rack de Efectos Nativos (Reverb, Delay y Distorsión).
     * Cada parámetro maneja un rango normalizado de 0.0 a 1.0.
     */
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


    // -------------------------------------------------
    // 11. VOLUMEN MAESTRO
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
    // 12. PANEO ESTÉREO
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
    // 13. SISTEMA DE PRESETS Y EXPORTACIÓN A BIBLIOTECA
    // -------------------------------------------------

    private val _savedPresets = MutableStateFlow>(emptyMap())
    val savedPresets: StateFlow> = _savedPresets.asStateFlow()

    private val _currentPresetName = MutableStateFlow("Default Patch")
    val currentPresetName: StateFlow<String> = _currentPresetName.asStateFlow()

    // Secuenciador en memoria para registrar notas interpretadas en vivo
    private val recordedNotes = mutableListOf<NoteEvent>()
    private var recordingStartTime = 0L
    private var isRecordingActive = false

    fun saveCurrentPreset(name: String) {
        if (name.isBlank()) return
        val patchToSave = _uiState.value.currentPatch
        _savedPresets.update { currentMap ->
            currentMap + (name to patchToSave)
        }
        _currentPresetName.value = name
    }

    fun loadPreset(name: String) {
        val preset = _savedPresets.value[name] ?: return
        _uiState.update { state ->
            state.copy(currentPatch = preset)
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

    /**
     * Renderiza la melodía y exporta un archivo .wav físico dentro de la carpeta 'exports'
     * para que sea escaneada y listada automáticamente por la Biblioteca.
     */
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

                // Exportamos un buffer de audio renderizado de 4 segundos con el parche activo
                SynthEngineCore.exportSynthToWav(
                    outputFile = wavFile,
                    frequency = 440.0f,
                    durationMs = 4000L,
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


    // -------------------------------------------------
    // 14. REPRODUCCIÓN DE NOTA DE PRUEBA (CON SOPORTE PORTAMENTO Y GRABACIÓN)
    // -------------------------------------------------

    /**
     * Envía una nota de prueba al motor de síntesis.
     *
     * Calcula la frecuencia inicial de deslizamiento basándose en la nota anterior
     * si el portamento está activado y registra la nota si la grabación está activa.
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

        // Si estamos grabando, registramos el evento de nota
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
