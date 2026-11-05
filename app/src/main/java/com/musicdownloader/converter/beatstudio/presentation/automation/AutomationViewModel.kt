package com.musicdownloader.converter.beatstudio.presentation.automation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class AutomationUiState(
    val selectedParameter: AutomationParameterType = AutomationParameterType.VOLUME,
    val lanesMap: Map<AutomationParameterType, AutomationLane> = AutomationParameterType.entries.associateWith { AutomationLane(it) },
    val playbackTimeMs: Long = 0L,
    val currentAutomationValue: Float = 0.8f
) {
    val currentLane: AutomationLane
        get() = lanesMap[selectedParameter] ?: AutomationLane(selectedParameter)
}

@HiltViewModel
class AutomationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    private var automationJob: Job? = null

    init {
        startHighFrequencySync()
    }

    // --- BUCLE DE ALTA FRECUENCIA (~30 FPS) ---
    // Consulta la posición del reproductor en tiempo real y aplica la automatización al audio de forma continua
    private fun startHighFrequencySync() {
        automationJob?.cancel()
        automationJob = viewModelScope.launch {
            while (true) {
                if (playerManager.isPlaying.value) {
                    val currentPos = playerManager.getCurrentPositionDirect()
                    updatePlaybackPosition(currentPos)
                }
                delay(33L) // Actualización ultrafluida cada 33ms para un control DSP milimétrico
            }
        }
    }

    // Cambiar de parámetro activo y aplicar instantáneamente al reproductor
    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { state ->
            val lane = state.lanesMap[parameter] ?: AutomationLane(parameter)
            val value = lane.getValueAtTime(state.playbackTimeMs)
            
            playerManager.applyAutomation(parameter, value)

            state.copy(
                selectedParameter = parameter,
                currentAutomationValue = value
            )
        }
    }

    // --- Alternar modo de interpolación (LINEAL vs. STEP) ---
    fun toggleInterpolationMode() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val newMode = if (currentLane.interpolationMode == InterpolationMode.LINEAR) {
                InterpolationMode.STEP
            } else {
                InterpolationMode.LINEAR
            }
            
            val newLane = currentLane.copy(interpolationMode = newMode)
            updatedMap[state.selectedParameter] = newLane
            
            val calculatedValue = newLane.getValueAtTime(state.playbackTimeMs)
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = calculatedValue
            )
        }
    }

    // --- Alternar Snap-to-Grid (Ajuste magnético a la cuadrícula) ---
    fun toggleSnapToGrid() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val newLane = currentLane.copy(isSnapToGridEnabled = !currentLane.isSnapToGridEnabled)
            updatedMap[state.selectedParameter] = newLane

            state.copy(lanesMap = updatedMap)
        }
    }

    // --- Deshacer acción (Undo) ---
    fun undo() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val lane = updatedMap[state.selectedParameter] ?: return@update state
            if (lane.undoStack.isEmpty()) return@update state

            val previousPoints = lane.undoStack.last()
            val newUndoStack = lane.undoStack.dropLast(1)
            val newRedoStack = listOf(lane.points.toList()) + lane.redoStack

            val newLane = lane.copy(
                points = previousPoints.toMutableList(),
                undoStack = newUndoStack,
                redoStack = newRedoStack
            )
            updatedMap[state.selectedParameter] = newLane

            val calculatedValue = newLane.getValueAtTime(state.playbackTimeMs)
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(lanesMap = updatedMap, currentAutomationValue = calculatedValue)
        }
    }

    // --- Rehacer acción (Redo) ---
    fun redo() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val lane = updatedMap[state.selectedParameter] ?: return@update state
            if (lane.redoStack.isEmpty()) return@update state

            val nextPoints = lane.redoStack.first()
            val newRedoStack = lane.redoStack.drop(1)
            val newUndoStack = lane.undoStack + listOf(lane.points.toList())

            val newLane = lane.copy(
                points = nextPoints.toMutableList(),
                undoStack = newUndoStack,
                redoStack = newRedoStack
            )
            updatedMap[state.selectedParameter] = newLane

            val calculatedValue = newLane.getValueAtTime(state.playbackTimeMs)
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(lanesMap = updatedMap, currentAutomationValue = calculatedValue)
        }
    }

    // Agregar o actualizar puntos aplicando imantación a la cuadrícula y respaldo en Undo
    fun addOrUpdatePoint(timeMs: Long, canvasWidth: Float, totalDurationMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            // --- CÁLCULO DE IMANTACIÓN QUIRÚRGICA A LA CUADRÍCULA ---
            val finalTimeMs = if (currentLane.isSnapToGridEnabled && canvasWidth > 0f) {
                val gridDivisions = 6 // Coincide con las 6 divisiones verticales de la pantalla
                val gridStepMs = totalDurationMs.toFloat() / gridDivisions
                val nearestIndex = kotlin.math.round(timeMs.toFloat() / gridStepMs).toInt().coerceIn(0, gridDivisions)
                (nearestIndex * gridStepMs).toLong()
            } else {
                timeMs
            }

            val currentPointsSnapshot = currentLane.points.toList()
            
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - finalTimeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(finalTimeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            val newLane = currentLane.copy(
                points = newPoints.toMutableList(),
                undoStack = currentLane.undoStack + listOf(currentPointsSnapshot),
                redoStack = emptyList()
            )
            updatedMap[state.selectedParameter] = newLane
            
            val calculatedValue = newLane.getValueAtTime(state.playbackTimeMs)
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = calculatedValue
            )
        }
    }

    // Limpiar todos los puntos del carril actual registrando el snapshot en Undo
    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            if (currentLane.points.isEmpty()) return@update state
            
            val currentPointsSnapshot = currentLane.points.toList()
            
            val clearedLane = currentLane.copy(
                points = mutableListOf(),
                undoStack = currentLane.undoStack + listOf(currentPointsSnapshot),
                redoStack = emptyList()
            )
            updatedMap[state.selectedParameter] = clearedLane
            
            val calculatedValue = clearedLane.getValueAtTime(state.playbackTimeMs)
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = calculatedValue
            )
        }
    }

    // Sincronizar la posición del reproductor en tiempo real
    fun updatePlaybackPosition(timeMs: Long) {
        _uiState.update { state ->
            val value = state.currentLane.getValueAtTime(timeMs)
            playerManager.applyAutomation(state.selectedParameter, value)

            state.copy(
                playbackTimeMs = timeMs,
                currentAutomationValue = value
            )
        }
    }

    // --- GUARDAR LA MEZCLA O AUDIO ACTUAL DIRECTAMENTE HACIA LA BIBLIOTECA ---
    fun saveMixToLibrary(customName: String, onComplete: (String) -> Unit) {
        playerManager.exportCurrentAudioToLibrary(customName) { savedFile ->
            onComplete(savedFile.name)
        }
    }

    // --- NUEVO: SISTEMA DE PRESETS JSON ---

    // Guardar el estado actual de los carriles en un archivo JSON interno
    fun savePreset(presetName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val presetDir = File(context.filesDir, "presets").apply { if (!exists()) mkdirs() }
                val sanitizedName = presetName.ifEmpty { "Preset_${System.currentTimeMillis()}" }
                val file = File(presetDir, "$sanitizedName.json")

                val rootObject = JSONObject()
                val lanesArray = JSONArray()

                uiState.value.lanesMap.forEach { (param, lane) ->
                    val laneObj = JSONObject().apply {
                        put("parameter", param.name)
                        put("interpolationMode", lane.interpolationMode.name)
                        put("isSnapToGridEnabled", lane.isSnapToGridEnabled)

                        val pointsArray = JSONArray()
                        lane.points.forEach { point ->
                            val pObj = JSONObject().apply {
                                put("timeMs", point.timeMs)
                                put("value", point.value.toDouble())
                            }
                            pointsArray.put(pObj)
                        }
                        put("points", pointsArray)
                    }
                    lanesArray.put(laneObj)
                }
                rootObject.put("lanes", lanesArray)

                file.writeText(rootObject.toString())
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    // Obtener la lista de nombres de presets disponibles
    fun getSavedPresetsList(): List<String> {
        val presetDir = File(context.filesDir, "presets")
        if (!presetDir.exists() || !presetDir.isDirectory) return emptyList()
        return presetDir.listFiles { file -> file.isFile && file.extension.equals("json", true) }
            ?.map { it.nameWithoutExtension }?.sorted() ?: emptyList()
    }

    // Cargar un preset guardado desde su archivo JSON
    fun loadPreset(presetName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(File(context.filesDir, "presets"), "$presetName.json")
                if (!file.exists()) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }

                val jsonString = file.readText()
                val rootObject = JSONObject(jsonString)
                val lanesArray = rootObject.getJSONArray("lanes")

                val newLanesMap = mutableMapOf<AutomationParameterType, AutomationLane>()

                for (i in 0 until lanesArray.length()) {
                    val laneObj = lanesArray.getJSONObject(i)
                    val paramName = laneObj.getString("parameter")
                    val param = AutomationParameterType.valueOf(paramName)

                    val modeName = laneObj.getString("interpolationMode")
                    val mode = InterpolationMode.valueOf(modeName)
                    val snap = laneObj.getBoolean("isSnapToGridEnabled")

                    val pointsArray = laneObj.getJSONArray("points")
                    val points = mutableListOf<AutomationPoint>()
                    for (j in 0 until pointsArray.length()) {
                        val pObj = pointsArray.getJSONObject(j)
                        points.add(
                            AutomationPoint(
                                timeMs = pObj.getLong("timeMs"),
                                value = pObj.getDouble("value").toFloat()
                            )
                        )
                    }

                    newLanesMap[param] = AutomationLane(
                        parameter = param,
                        points = points,
                        interpolationMode = mode,
                        isSnapToGridEnabled = snap
                    )
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        val currentLane = newLanesMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
                        val calcValue = currentLane.getValueAtTime(state.playbackTimeMs)
                        playerManager.applyAutomation(state.selectedParameter, calcValue)

                        state.copy(
                            lanesMap = newLanesMap,
                            currentAutomationValue = calcValue
                        )
                    }
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        automationJob?.cancel()
    }
}
