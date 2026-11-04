package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // Cambiar de parámetro activo y aplicar instantáneamente su valor actual al reproductor
    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { state ->
            val lane = state.lanesMap[parameter] ?: AutomationLane(parameter)
            val value = lane.getValueAtTime(state.playbackTimeMs)
            
            // Aplicar de inmediato al motor de audio al cambiar de carril
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

    // --- NUEVO: Deshacer acción (Undo) ---
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

    // --- NUEVO: Rehacer acción (Redo) ---
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

    // Agregar o actualizar puntos de automatización registrando el snapshot en la pila de Undo
    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            // Guardamos el estado actual antes del cambio en la pila de Undo
            val currentPointsSnapshot = currentLane.points.toList()
            
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - timeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(timeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            val newLane = currentLane.copy(
                points = newPoints.toMutableList(),
                undoStack = currentLane.undoStack + listOf(currentPointsSnapshot),
                redoStack = emptyList() // Limpiamos Redo al realizar una nueva modificación
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

    // Sincronizar la posición del reproductor en tiempo real y enviar el valor DSP al AudioPlayerManager
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

    override fun onCleared() {
        super.onCleared()
        automationJob?.cancel()
    }
}
