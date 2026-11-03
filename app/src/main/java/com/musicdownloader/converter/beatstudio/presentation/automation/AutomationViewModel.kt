package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AutomationUiState(
    val selectedParameter: AutomationParameterType = AutomationParameterType.VOLUME,
    val lanesMap: Map<AutomationParameterType, AutomationLane> = AutomationParameterType.entries.associateWith { AutomationLane(it) },
    val playbackTimeMs: Long = 0L,
    val currentAutomationValue: Float = 0.8f // Valor interpolado actual para efectos o volumen
) {
    val currentLane: AutomationLane
        get() = lanesMap[selectedParameter] ?: AutomationLane(selectedParameter)
}

@HiltViewModel
class AutomationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { 
            val lane = it.lanesMap[parameter] ?: AutomationLane(parameter)
            it.copy(
                selectedParameter = parameter,
                currentAutomationValue = lane.getValueAtTime(it.playbackTimeMs)
            ) 
        }
    }

    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - timeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(timeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            val newLane = currentLane.copy(points = newPoints.toMutableList())
            updatedMap[state.selectedParameter] = newLane
            
            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = newLane.getValueAtTime(state.playbackTimeMs)
            )
        }
    }

    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val clearedLane = currentLane.copy(points = mutableListOf())
            updatedMap[state.selectedParameter] = clearedLane
            
            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = clearedLane.getValueAtTime(state.playbackTimeMs)
            )
        }
    }

    // Sincroniza el tiempo del reproductor y calcula el valor de la curva al vuelo
    fun updatePlaybackPosition(timeMs: Long) {
        _uiState.update { state ->
            val value = state.currentLane.getValueAtTime(timeMs)
            state.copy(
                playbackTimeMs = timeMs,
                currentAutomationValue = value
            )
        }
    }
}
