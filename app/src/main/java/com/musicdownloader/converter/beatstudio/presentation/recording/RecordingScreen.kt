package com.musicdownloader.converter.beatstudio.presentation.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import com.musicdownloader.converter.util.AudioTrimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.abs

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isMonitorLiveOn: Boolean = false,
    val isArmRecordOn: Boolean = true,
    val isMetronomeOn: Boolean = false,
    val currentBpm: Int = 120,
    val currentBeat: Int = 1,
    val currentMeterLevel: Float = 0.0f,
    val recordedTakesCount: Int = 0,
    val pendingTrimFile: File? = null,
    val waveformPeaks: List<Float> = emptyList() // <--- Picos reales de la onda
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerManager: AudioPlayerManager
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

    // --- INICIAR SESIÓN CON O SIN TEMPO ---
    fun startRecordingSession(title: String, useTempo: Boolean) {
        _uiState.update { it.copy(isArmRecordOn = true, pendingTrimFile = null, waveformPeaks = emptyList()) }

        val cacheDir = context.cacheDir
        rawAudioFile = File(cacheDir, "raw_recording_${System.currentTimeMillis()}.pcm")

        rawAudioFile?.let { file ->
            _uiState.update { it.copy(isRecording = true) }
            
            if (useTempo) {
                toggleMetronome(true)
            } else {
                toggleMetronome(false)
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
                    val exportDir = File(context.filesDir, "exports")
                    if (!exportDir.exists()) {
                        exportDir.mkdirs()
                    }

                    val fileName = "${customTitle}_${System.currentTimeMillis()}.wav"
                    wavOutputFile = File(exportDir, fileName)

                    wavOutputFile?.let { wav ->
                        rawToWav(raw, wav, 44100, 2, 16)
                    }

                    raw.delete()

                    // Extraer picos reales de audio para el visor DAW
                    val peaks = wavOutputFile?.let { extractWaveformPeaks(it, 48) } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        _uiState.update { 
                            it.copy(
                                isRecording = false,
                                isMetronomeOn = false,
                                currentMeterLevel = 0.0f,
                                recordedTakesCount = it.recordedTakesCount + 1,
                                pendingTrimFile = wavOutputFile,
                                waveformPeaks = peaks
                            ) 
                        }
                    }
                }
            }
        }
    }

    // --- EXTRACCIÓN DE PICOS PCM PARA ONDA DINÁMICA ---
    private fun extractWaveformPeaks(wavFile: File, barCount: Int): List<Float> {
        val peaks = mutableListOf<Float>()
        try {
            FileInputStream(wavFile).use { fis ->
                val header = ByteArray(44)
                fis.read(header)

                val availableBytes = fis.available()
                val buffer = ByteArray(availableBytes)
                val bytesRead = fis.read(buffer)

                if (bytesRead <= 0) return List(barCount) { 0.2f }

                val totalSamples = bytesRead / 2
                if (totalSamples <= 0) return List(barCount) { 0.2f }

                val samplesPerBar = totalSamples / barCount
                if (samplesPerBar <= 0) return List(barCount) { 0.2f }

                for (i in 0 until barCount) {
                    var maxVal = 0f
                    val startSample = i * samplesPerBar
                    val endSample = minOf(startSample + samplesPerBar, totalSamples)

                    for (s in startSample until endSample) {
                        val byteIndex = s * 2
                        if (byteIndex + 1 < buffer.size) {
                            val low = buffer[byteIndex].toInt() and 0xFF
                            val high = buffer[byteIndex + 1].toInt()
                            val sample = (high shl 8) or low
                            val absVal = abs(sample.toFloat()) / 32768f
                            if (absVal > maxVal) maxVal = absVal
                        }
                    }
                    peaks.add(maxVal.coerceIn(0.15f, 1.0f))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return List(barCount) { 0.3f }
        }
        return if (peaks.isEmpty()) List(barCount) { 0.3f } else peaks
    }

    // --- FUNCIÓN DE RECORTAR TOMA (TRIMMING) ---
    fun applyTrimToPending(startSec: Int, endSec: Int, onFinished: (Boolean) -> Unit) {
        val fileToTrim = _uiState.value.pendingTrimFile ?: return
        if (startSec >= endSec) {
            onFinished(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parentDir = fileToTrim.parentFile ?: File(context.filesDir, "exports")
                val trimmedFile = File(parentDir, "Trimmed_${fileToTrim.name}")

                AudioTrimmer.trim(
                    srcPath = fileToTrim.absolutePath,
                    dstPath = trimmedFile.absolutePath,
                    startMs = startSec * 1000L,
                    endMs = endSec * 1000L
                )

                fileToTrim.delete()
                trimmedFile.renameTo(fileToTrim)

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pendingTrimFile = null, waveformPeaks = emptyList()) }
                    onFinished(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onFinished(false)
                }
            }
        }
    }

    fun dismissTrim() {
        _uiState.update { it.copy(pendingTrimFile = null, waveformPeaks = emptyList()) }
    }

    private fun rawToWav(rawFile: File, wavFile: File, sampleRate: Int, channels: Int, bitRate: Int) {
        val rawData = ByteArray(rawFile.length().toInt())
        FileInputStream(rawFile).use { it.read(rawData) }

        FileOutputStream(wavFile).use { fos ->
            val totalDataLen = rawData.size + 36
            val byteRate = sampleRate * channels * (bitRate / 8)

            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalDataLen), 0, 4)
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16), 0, 4)
            fos.write(shortToByteArray(1), 0, 2)
            fos.write(shortToByteArray(channels.toShort()), 0, 2)
            fos.write(intToByteArray(sampleRate), 0, 4)
            fos.write(intToByteArray(byteRate), 0, 4)
            fos.write(shortToByteArray((channels * (bitRate / 8)).toShort()), 0, 2)
            fos.write(shortToByteArray(bitRate.toShort()), 0, 2)
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
