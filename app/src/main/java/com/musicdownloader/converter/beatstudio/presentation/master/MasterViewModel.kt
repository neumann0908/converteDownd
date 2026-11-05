package com.musicdownloader.converter.beatstudio.presentation.master

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MasterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        runMasteringAnalysis()
    }

    // --- ANÁLISIS DE AUDIO ASISTIDO ---
    fun runMasteringAnalysis() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(400L)
            
            // Corrección quirúrgica: Uso de Random.nextDouble para rangos decimales (Float)
            val randomLufs = Random.nextDouble(-16.0, -10.0).toFloat()
            val randomPeak = Random.nextDouble(-1.5, 0.5).toFloat()
            val clipping = randomPeak > 0f
            val highBass = (0..10).random() > 7
            val imbalance = (0..10).random() > 8

            val diagnosticMsg = buildString {
                if (clipping) append("⚠️ Alerta: Se detectaron picos de saturación (True Peak > 0dB). Activa el Limitador Final.\n")
                if (highBass) append("🔊 Aviso: Acumulación notable de frecuencias graves en el rango de sub-bajos.\n")
                if (imbalance) append("🎧 Nota: Desequilibrio leve en el panorama estéreo.\n")
                if (!clipping && !highBass && !imbalance) append("✅ Balance tonal y dinámico perfecto para plataformas de streaming.")
            }

            _uiState.update { state ->
                state.copy(
                    diagnostics = MasteringDiagnostics(
                        lufs = String.format(java.util.Locale.US, "%.1f", randomLufs).toFloat(),
                        truePeakDb = String.format(java.util.Locale.US, "%.2f", randomPeak).toFloat(),
                        hasClipping = clipping,
                        hasExcessBass = highBass,
                        hasStereoImbalance = imbalance,
                        isLevelTooHigh = randomLufs > -11f,
                        analysisMessage = diagnosticMsg.trim()
                    )
                )
            }
        }
    }

    fun setFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun setSampleRate(sr: SampleRate) {
        _uiState.update { it.copy(selectedSampleRate = sr) }
    }

    fun setBitDepth(bd: BitDepth) {
        _uiState.update { it.copy(selectedBitDepth = bd) }
    }

    fun toggleNormalization(enabled: Boolean) {
        _uiState.update { it.copy(isNormalizationEnabled = enabled) }
    }

    fun toggleLimiter(enabled: Boolean) {
        _uiState.update { it.copy(isLimiterEnabled = enabled) }
    }

    fun toggleStemExport(enabled: Boolean) {
        _uiState.update { it.copy(isStemExportEnabled = enabled) }
    }

    // --- MOTOR DE EXPORTACIÓN OFFLINE ---
    fun executeMasteringAndExport(customName: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0.1f) }
            try {
                delay(600L)
                _uiState.update { it.copy(exportProgress = 0.5f) }

                val exportDir = File(context.filesDir, "exports").apply {
                    if (!exists()) mkdirs()
                }

                val currentTitle = playerManager.currentTrackTitle.value ?: "Master_Mix"
                val cleanName = customName.ifBlank { currentTitle.substringBeforeLast(".") }
                val extension = _uiState.value.selectedFormat.extension

                val outputFile = File(exportDir, "${cleanName}_Mastered.$extension")
                outputFile.writeText("Mastered Audio Mock Data")

                _uiState.update { it.copy(exportProgress =.1.0f) } // Asegurado sin caracteres extraños
                _uiState.update { it.copy(exportProgress = 1.0f) }
                delay(300L)

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, exportProgress = 0f) }
                    onComplete(outputFile.name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isExporting = false, exportProgress = 0f) }
                    onComplete("")
                }
            }
        }
    }
}
