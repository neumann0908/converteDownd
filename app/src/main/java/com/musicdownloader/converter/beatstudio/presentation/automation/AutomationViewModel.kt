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
    val playbackTimeMs: Long = 0L
) {
    val currentLane: AutomationLane
        get() = lanesMap[selectedParameter] ?: AutomationLane(selectedParameter)
}

@HiltViewModel
class AutomationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { it.copy(selectedParameter = parameter) }
    }

    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - timeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(timeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            updatedMap[state.selectedParameter] = currentLane.copy(points = newPoints.toMutableList())
            state.copy(lanesMap = updatedMap)
        }
    }

    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            updatedMap[state.selectedParameter] = currentLane.copy(points = mutableListOf())
            state.copy(lanesMap = updatedMap)
        }
    }

    fun updatePlaybackPosition(timeMs: Long) {
        _uiState.update { it.copy(playbackTimeMs = timeMs) }
    }
}
