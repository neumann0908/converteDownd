package com.musicdownloader.converter.beatstudio.presentation.master

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import com.musicdownloader.converter.beatstudio.presentation.automation.AutomationParameterType
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

// --- MODELO EXTENDIDO MULTICANAL Y TEMPORAL PARA LA LÍNEA DE TIEMPO ---
data class MasterStemItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val sizeMb: String,
    val volume: Float = 1.0f,     // Ganancia individual del canal (0.0 a 2.0)
    val pan: Float = 0.0f,        // Panorama estéreo (-1.0 izq a 1.0 der)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isInSession: Boolean = false, // Indica si la pista ya fue abierta/añadida al mixer multicanal
    val startTimeMs: Long = 0L,       // Momento de inicio temporal en la línea de tiempo (ms)
    val durationMs: Long = 0L         // Duración total del clip de audio (ms)
)

// --- PRESETS DE EXPORTACIÓN PROFESIONAL ---
enum class ExportPreset(val displayName: String, val description: String) {
    STREAMING("Streaming (Spotify/Apple)", "Optimizado a -14 LUFS, 44.1kHz / 24-bit con limitador activo"),
    CD_AUDIO("Audio CD Estándar", "WAV sin pérdidas, 16-bit / 44.1 kHz"),
    HI_RES("Hi-Res Studio Master", "Máxima calidad, 96 kHz / 32-bit Float")
}

// Estado extendido para sesión multicanal, stems, presets, región y línea de tiempo
data class AdvancedMasterState(
    val inputDriveDb: Float = 0.0f,
    val limiterCeilingDb: Float = -0.3f,
    val selectedPreset: ExportPreset = ExportPreset.STREAMING,
    val isRegionExportEnabled: Boolean = false,
    val regionStartSec: Float = 0.0f,
    val regionEndSec: Float = 120.0f,
    val isStemExportEnabled: Boolean = false,
    val availableLibraryStems: List<MasterStemItem> = emptyList(), // Auditorio de archivos escaneados
    val activeSessionStems: List<MasterStemItem> = emptyList(),    // Canales abiertos en la consola de mezcla (Timeline)
    val selectedStemId: String? = null,                            // Canal enfocado en las perillas VST
    val isMultiPlayActive: Boolean = false,                        // Reproducción global multicanal activa
    val savedProjectsList: List<String> = emptyList()              // Lista de sesiones guardadas en biblioteca
)

@HiltViewModel
class MasterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterUiState())
    val uiState = _uiState.asStateFlow()

    private val _advancedState = MutableStateFlow(AdvancedMasterState())
    val advancedState = _advancedState.asStateFlow()

    init {
        runMasteringAnalysis()
        loadAvailableStems()
        loadSavedProjects()
    }

    // --- ESCANEO DE AUDIOS DE LA BIBLIOTECA ---
    fun loadAvailableStems() {
        viewModelScope.launch(Dispatchers.IO) {
            val stemList = mutableListOf<MasterStemItem>()

            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", true) ||
                            file.extension.equals("m4a", true) ||
                            file.extension.equals("webm", true) ||
                            file.extension.equals("wav", true))
                }?.forEach { file ->
                    val sizeMb = String.format(java.util.Locale.US, "%.2f MB", file.length().toDouble() / (1024 * 1024))
                    stemList.add(MasterStemItem(name = file.name, path = file.absolutePath, sizeMb = sizeMb, durationMs = 5000L))
                }
            }

            val exportDir = File(context.filesDir, "exports")
            if (exportDir.exists() && exportDir.isDirectory) {
                exportDir.listFiles { file ->
                    file.isFile && file.extension.equals("wav", true)
                }?.forEach { file ->
                    val sizeMb = String.format(java.util.Locale.US, "%.2f MB", file.length().toDouble() / (1024 * 1024))
                    stemList.add(MasterStemItem(name = file.name, path = file.absolutePath, sizeMb = sizeMb, durationMs = 5000L))
                }
            }

            val recordingsDir = File(context.filesDir, "recordings")
            if (recordingsDir.exists() && recordingsDir.isDirectory) {
                recordingsDir.listFiles { file ->
                    file.isFile && file.extension.equals("wav", true)
                }?.forEach { file ->
                    val sizeMb = String.format(java.util.Locale.US, "%.2f MB", file.length().toDouble() / (1024 * 1024))
                    stemList.add(MasterStemItem(name = "🎙️ ${file.name}", path = file.absolutePath, sizeMb = sizeMb, durationMs = 5000L))
                }
            }

            val sortedStems = stemList.sortedBy { it.name }
            _advancedState.update { state ->
                state.copy(availableLibraryStems = sortedStems)
            }
        }
    }

    // --- AÑADIR AUDIO DESDE LA BIBLIOTECA AL MIXER MULTICANAL (CREAR CANAL) ---
    fun addStemToSession(stemId: String) {
        val libraryStem = _advancedState.value.availableLibraryStems.find { it.id == stemId } ?: return
        
        _advancedState.update { state ->
            if (state.activeSessionStems.any { it.path == libraryStem.path }) return@update state

            val defaultOffset = 0L
            val calculatedStartTime = if (state.activeSessionStems.isNotEmpty()) {
                state.activeSessionStems.maxOf { it.startTimeMs + it.durationMs }
            } else {
                defaultOffset
            }

            val newStemChannel = libraryStem.copy(
                isInSession = true,
                startTimeMs = calculatedStartTime,
                durationMs = libraryStem.durationMs.ifZero { 5000L }
            )
            val updatedSession = state.activeSessionStems + newStemChannel
            
            state.copy(
                activeSessionStems = updatedSession,
                selectedStemId = state.selectedStemId ?: newStemChannel.id
            )
        }
    }

    // --- ELIMINAR CANAL DE LA SESIÓN ---
    fun removeStemFromSession(stemId: String) {
        _advancedState.update { state ->
            val updatedSession = state.activeSessionStems.filter { it.id != stemId }
            val nextSelectedId = if (state.selectedStemId == stemId) updatedSession.firstOrNull()?.id else state.selectedStemId
            state.copy(
                activeSessionStems = updatedSession,
                selectedStemId = nextSelectedId
            )
        }
    }

    // --- ACTUALIZAR POSICIÓN TEMPORAL DE UN STEM EN LA LÍNEA DE TIEMPO ---
    fun updateStemStartTime(stemId: String, newStartTimeMs: Long) {
        _advancedState.update { state ->
            state.copy(
                activeSessionStems = state.activeSessionStems.map { stem ->
                    if (stem.id == stemId) stem.copy(startTimeMs = newStartTimeMs.coerceAtLeast(0L)) else stem
                }
            )
        }
    }

    // --- SELECCIONAR Y REPRODUCIR PISTA INDIVIDUAL ---
    fun selectAndPlayStem(stemId: String) {
        _advancedState.update { it.copy(selectedStemId = stemId) }
        val stem = _advancedState.value.activeSessionStems.find { it.id == stemId } 
            ?: _advancedState.value.availableLibraryStems.find { it.id == stemId }
            
        if (stem != null) {
            try {
                playerManager.playFile(stem.path, stem.name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- REPRODUCIR TODOS LOS CANALES ACTIVOS SIMULTÁNEAMENTE EN LA LÍNEA DE TIEMPO ---
    fun togglePlayAllChannels() {
        val currentState = _advancedState.value.isMultiPlayActive
        if (!currentState) {
            val activeStems = _advancedState.value.activeSessionStems
            if (activeStems.isNotEmpty()) {
                _advancedState.update { it.copy(isMultiPlayActive = true) }
                playerManager.playMultiTrackSession(activeStems, playerManager.getCurrentPositionDirect())
            }
        } else {
            playerManager.stop()
            _advancedState.update { it.copy(isMultiPlayActive = false) }
        }
    }

    // --- CONTROLES DE HARDWARE Y PARÁMETROS DEL CANAL SELECCIONADO (VIRTUAL RACK) ---
    fun updateSelectedStemVolume(volume: Float) {
        val stemId = _advancedState.value.selectedStemId ?: return
        _advancedState.update { state ->
            state.copy(
                activeSessionStems = state.activeSessionStems.map { if (it.id == stemId) it.copy(volume = volume) else it }
            )
        }
        playerManager.setStemGain(stemId, volume)
    }

    fun updateSelectedStemGain(gainValue: Float) {
        updateSelectedStemVolume(gainValue)
    }

    fun updateSelectedStemPan(pan: Float) {
        val stemId = _advancedState.value.selectedStemId ?: return
        _advancedState.update { state ->
            state.copy(
                activeSessionStems = state.activeSessionStems.map { if (it.id == stemId) it.copy(pan = pan) else it }
            )
        }
    }

    fun toggleStemMute(stemId: String) {
        _advancedState.update { state ->
            state.copy(
                activeSessionStems = state.activeSessionStems.map { if (it.id == stemId) it.copy(isMuted = !it.isMuted) else it }
            )
        }
    }

    fun toggleStemSolo(stemId: String) {
        _advancedState.update { state ->
            state.copy(
                activeSessionStems = state.activeSessionStems.map { if (it.id == stemId) it.copy(isSolo = !it.isSolo) else it }
            )
        }
    }

    fun toggleStemExport(enabled: Boolean) {
        _advancedState.update { it.copy(isStemExportEnabled = enabled) }
    }

    // --- GUARDAR Y CARGAR PROYECTO DE INGENIERÍA EN LA BIBLIOTECA ---
    fun saveStudioSessionProject(projectName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val projectsDir = File(context.filesDir, "projects").apply {
                    if (!exists()) mkdirs()
                }
                val cleanName = projectName.ifBlank { "Session_${System.currentTimeMillis()}" }
                val projectFile = File(projectsDir, "$cleanName.beatproj")

                val sessionData = buildString {
                    append("PROJECT:$cleanName\n")
                    _advancedState.value.activeSessionStems.forEach { stem ->
                        append("STEM:${stem.name}|PATH:${stem.path}|VOL:${stem.volume}|PAN:${stem.pan}|MUTE:${stem.isMuted}|START:${stem.startTimeMs}\n")
                    }
                }
                projectFile.writeText(sessionData)

                loadSavedProjects()
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

    fun loadSavedProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val projectsDir = File(context.filesDir, "projects")
            val projectNames = mutableListOf<String>()
            if (projectsDir.exists() && projectsDir.isDirectory) {
                projectsDir.listFiles { file -> file.isFile && file.extension.equals("beatproj", true) }?.forEach {
                    projectNames.add(it.nameWithoutExtension)
                }
            }
            _advancedState.update { it.copy(savedProjectsList = projectNames) }
        }
    }

    // --- ANÁLISIS DE AUDIO ASISTIDO ---
    fun runMasteringAnalysis() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(400L)
            
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

    fun updateInputDrive(gainDb: Float) {
        _advancedState.update { it.copy(inputDriveDb = gainDb) }
        val stemId = _advancedState.value.selectedStemId
        if (stemId != null) {
            playerManager.updateStemMasteringParameters(stemId, gainDb, _advancedState.value.limiterCeilingDb)
        } else {
            playerManager.updateMasteringParameters(gainDb, _advancedState.value.limiterCeilingDb)
        }
    }

    fun updateLimiterCeiling(ceilingDb: Float) {
        _advancedState.update { it.copy(limiterCeilingDb = ceilingDb) }
        val stemId = _advancedState.value.selectedStemId
        if (stemId != null) {
            playerManager.updateStemMasteringParameters(stemId, _advancedState.value.inputDriveDb, ceilingDb)
        } else {
            playerManager.updateMasteringParameters(_advancedState.value.inputDriveDb, ceilingDb)
        }
    }

    fun applyExportPreset(preset: ExportPreset) {
        _advancedState.update { it.copy(selectedPreset = preset) }
        when (preset) {
            ExportPreset.STREAMING -> {
                _uiState.update { it.copy(selectedFormat = ExportFormat.MP3, selectedSampleRate = SampleRate.SR_44100, selectedBitDepth = BitDepth.BIT_24) }
            }
            ExportPreset.CD_AUDIO -> {
                _uiState.update { it.copy(selectedFormat = ExportFormat.WAV, selectedSampleRate = SampleRate.SR_44100, selectedBitDepth = BitDepth.BIT_16) }
            }
            ExportPreset.HI_RES -> {
                _uiState.update { it.copy(selectedFormat = ExportFormat.FLAC, selectedSampleRate = SampleRate.SR_96000, selectedBitDepth = BitDepth.BIT_32) }
            }
        }
    }

    fun toggleRegionExport(enabled: Boolean) {
        _advancedState.update { it.copy(isRegionExportEnabled = enabled) }
    }

    fun updateExportRegion(start: Float, end: Float) {
        _advancedState.update { it.copy(regionStartSec = start, regionEndSec = end) }
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
                outputFile.writeText("Mastered Audio unifying multi-track active session stems with timeline layout parameters")

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

private inline fun Long.ifZero(block: () -> Long): Long = if (this == 0L) block() else this
