package com.musicdownloader.converter.beatstudio.presentation.recording

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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isMonitorLiveOn: Boolean = false,
    val isArmRecordOn: Boolean = true,
    val isMetronomeOn: Boolean = false,
    val currentBpm: Int = 120,
    val currentBeat: Int = 1,
    val currentMeterLevel: Float = 0.0f,
    val recordedTakesCount: Int = 0
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState = _uiState.asStateFlow()

    private var rawAudioFile: File? = null
    private var wavOutputFile: File? = null

    fun toggleMonitorLive(enabled: Boolean) {
        _uiState.update { it.copy(isMonitorLiveOn = enabled) }
    }

    fun toggleArmRecord(enabled: Boolean) {
        _uiState.update { it.copy(isArmRecordOn = enabled) }
    }

    // --- Control de BPM y Metrónomo ---
    fun updateBpm(newBpm: Int) {
        val clampedBpm = newBpm.coerceIn(40, 240)
        RecordingEngineCore.currentBpm = clampedBpm
        _uiState.update { it.copy(currentBpm = clampedBpm) }
    }

    fun toggleMetronome(enabled: Boolean) {
        _uiState.update { it.copy(isMetronomeOn = enabled) }
        if (enabled) {
            RecordingEngineCore.startMetronome { beat ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentBeat = beat) }
                }
            }
        } else {
            RecordingEngineCore.stopMetronome()
        }
    }

    fun startRecordingSession(title: String) {
        if (!_uiState.value.isArmRecordOn) return

        val cacheDir = context.cacheDir
        rawAudioFile = File(cacheDir, "raw_recording_${System.currentTimeMillis()}.pcm")

        rawAudioFile?.let { file ->
            _uiState.update { it.copy(isRecording = true) }
            
            // Si el metrónomo no estaba activo al grabar, lo iniciamos opcionalmente o aseguramos el pulso
            if (!_uiState.value.isMetronomeOn) {
                toggleMetronome(true)
            }

            RecordingEngineCore.startRecording(file) { rms ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentMeterLevel = rms) }
                }
            }
        }
    }

    fun stopRecordingSession(customTitle: String = "Toma_Voz") {
        RecordingEngineCore.stopRecording()
        RecordingEngineCore.stopMetronome()

        viewModelScope.launch(Dispatchers.IO) {
            rawAudioFile?.let { raw ->
                if (raw.exists()) {
                    // Directorio oficial de la biblioteca / archivos de la app
                    val libraryDir = File(context.filesDir, "recordings")
                    if (!libraryDir.exists()) libraryDir.mkdirs()

                    val fileName = "${customTitle}_${System.currentTimeMillis()}.wav"
                    wavOutputFile = File(libraryDir, fileName)

                    // Convertimos el PCM crudo a un archivo .wav válido para la biblioteca
                    wavOutputFile?.let { wav ->
                        rawToWav(raw, wav, 44100, 2, 16)
                    }

                    // Limpiamos el archivo temporal en crudo
                    raw.delete()
                }
            }

            viewModelScope.launch {
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        isMetronomeOn = false,
                        currentMeterLevel = 0.0f,
                        recordedTakesCount = it.recordedTakesCount + 1
                    ) 
                }
            }
        }
    }

    // Método utilitario para empaquetar PCM crudo en cabeceras WAV estándar
    private fun rawToWav(rawFile: File, wavFile: File, sampleRate: Int, channels: Int, bitRate: Int) {
        val rawData = ByteArray(rawFile.length().toInt())
        FileInputStream(rawFile).use { it.read(rawData) }

        FileOutputStream(wavFile).use { fos ->
            val totalDataLen = rawData.size + 36
            val byteRate = sampleRate * channels * (bitRate / 8)

            // Cabecera RIFF / WAV
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalDataLen), 0, 4)
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16), 0, 4) // SubChunk1Size (16 para PCM)
            fos.write(shortToByteArray(1), 0, 2)  // AudioFormat (1 = PCM)
            fos.write(shortToByteArray(channels.toShort()), 0, 2)
            fos.write(intToByteArray(sampleRate), 0, 4)
            fos.write(intToByteArray(byteRate), 0, 4)
            fos.write(shortToByteArray((channels * (bitRate / 8)).toShort()), 0, 2) // BlockAlign
            fos.write(shortToByteArray(bitRate.toShort()), 0, 2) // BitsPerSample
            fos.write("data".toByteArray())
            fos.write(intToByteArray(rawData.size), 0, 4)
            fos.write(rawData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteArray(4) { (value shr (it * 8)).toByte() }
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteArray(2) { (value.toInt() shr (it * 8)).toByte() }
    }
}
