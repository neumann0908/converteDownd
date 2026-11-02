package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AutomationUiState(
    val selectedParameter: AutomationParameterType = AutomationParameterType.VOLUME,
    // Mantenemos un mapa con los carriles de cada parámetro para no perder las curvas al cambiar de pestaña
    val lanesMap: Map<AutomationParameterType, AutomationLane> = AutomationParameterType.values().associateWith { AutomationLane(it) }
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

    // Agregar o mover un punto de automatización tocando la pantalla en el parámetro activo
    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            // Si hay un punto muy cerca, lo quitamos para poner el nuevo
            currentLane.points.removeAll { kotlin.math.abs(it.timeMs - timeMs) < 200L }
            currentLane.points.add(AutomationPoint(timeMs, value.coerceIn(0f, 1f)))
            currentLane.points.sortBy { it.timeMs }
            
            updatedMap[state.selectedParameter] = currentLane
            state.copy(lanesMap = updatedMap)
        }
    }

    // Borrar toda la curva del parámetro seleccionado actualmente
    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter]
            currentLane?.points?.clear()
            
            if (currentLane != null) {
                updatedMap[state.selectedParameter] = currentLane
            }
            state.copy(lanesMap = updatedMap)
        }
    }
}
