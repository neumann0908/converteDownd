package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AutomationUiState(
    val selectedParameter: AutomationParameterType = AutomationParameterType.VOLUME,
    val currentLane: AutomationLane = AutomationLane(AutomationParameterType.VOLUME)
)

@HiltViewModel
class AutomationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState = _uiState.asStateFlow()

    // Cambiar entre Volumen, Reverb, Filtro, etc.
    fun selectParameter(parameter: AutomationParameterType) {
        _uiState.update { it.copy(selectedParameter = parameter, currentLane = AutomationLane(parameter)) }
    }

    // Agregar o mover un punto de automatización tocando la pantalla
    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val lane = state.currentLane
            // Si hay un punto muy cerca, lo quitamos para poner el nuevo
            lane.points.removeAll { kotlin.math.abs(it.timeMs - timeMs) < 200L }
            lane.points.add(AutomationPoint(timeMs, value.coerceIn(0f, 1f)))
            lane.points.sortBy { it.timeMs }
            state.copy(currentLane = lane)
        }
    }

    // Borrar toda la curva
    fun clearPoints() {
        _uiState.update { state ->
            state.currentLane.points.clear()
            state.copy(currentLane = state.currentLane)
        }
    }
}
