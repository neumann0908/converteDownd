package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AutomationUiState(
    val selectedParameter: AutomationParameterType = AutomationParameterType.VOLUME,
    // Mantenemos un mapa con los carriles de cada parámetro utilizando entries para máxima compatibilidad y rendimiento
    val lanesMap: Map<AutomationParameterType, AutomationLane> = AutomationParameterType.entries.associateWith { AutomationLane(it) }
) {
    val currentLane: AutomationLane
        get() = lanesMap[selectedParameter] ?: AutomationLane(selectedParameter)
}

@HiltViewModel
class AutomationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    // Cambiar entre Volumen, Reverb, Filtro, etc., preservando sus puntos respectivos
    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { it.copy(selectedParameter = parameter) }
    }

    // Agregar o mover un punto de automatización con inmutabilidad estricta para forzar emisión en StateFlow
    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            // Filtramos los puntos cercanos y creamos una nueva lista ordenada
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - timeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(timeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            // Asignamos una nueva copia del carril con la nueva lista para forzar la actualización en UI
            updatedMap[state.selectedParameter] = currentLane.copy(points = newPoints.toMutableList())
            
            state.copy(lanesMap = updatedMap)
        }
    }

    // Borrar toda la curva del parámetro seleccionado actualmente de forma inmutable
    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            // Creamos una copia con la lista limpia
            updatedMap[state.selectedParameter] = currentLane.copy(points = mutableListOf())
            
            state.copy(lanesMap = updatedMap)
        }
    }
}
