package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.lifecycle.ViewModel
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    // Agregar o actualizar puntos de automatización con inmutabilidad estricta
    fun addOrUpdatePoint(timeMs: Long, value: Float) {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val filteredPoints = currentLane.points.filter { kotlin.math.abs(it.timeMs - timeMs) >= 200L }
            val newPoints = (filteredPoints + AutomationPoint(timeMs, value.coerceIn(0f, 1f))).sortedBy { it.timeMs }
            
            val newLane = currentLane.copy(points = newPoints.toMutableList())
            updatedMap[state.selectedParameter] = newLane
            
            val calculatedValue = newLane.getValueAtTime(state.playbackTimeMs)
            
            // Aplicar inmediatamente al reproductor al modificar la curva
            playerManager.applyAutomation(state.selectedParameter, calculatedValue)

            state.copy(
                lanesMap = updatedMap,
                currentAutomationValue = calculatedValue
            )
        }
    }

    // Limpiar todos los puntos del carril actual
    fun clearPoints() {
        _uiState.update { state ->
            val updatedMap = state.lanesMap.toMutableMap()
            val currentLane = updatedMap[state.selectedParameter] ?: AutomationLane(state.selectedParameter)
            
            val clearedLane = currentLane.copy(points = mutableListOf())
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
            
            // --- CONEXIÓN QUIRÚRGICA CRUCIAL ---
            // Envía el valor interpolado de la curva (Volumen, Pitch, Reverb, etc.) al reproductor en tiempo real
            playerManager.applyAutomation(state.selectedParameter, value)

            state.copy(
                playbackTimeMs = timeMs,
                currentAutomationValue = value
            )
        }
    }
}
