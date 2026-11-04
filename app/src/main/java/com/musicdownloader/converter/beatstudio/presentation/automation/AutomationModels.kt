package com.musicdownloader.converter.beatstudio.presentation.automation

// --- NUEVO: Modos de interpolación para las curvas (Rampa suave vs. Escalón rítmico) ---
enum class InterpolationMode {
    LINEAR, STEP
}

// Todos los parámetros profesionales con su respectivo displayName para la interfaz
enum class AutomationParameterType(val displayName: String) {
    VOLUME("Volumen"),
    PAN("Panorama"),
    CUTOFF("Filtro Corte"),
    RESONANCE("Resonancia"),
    REVERB("Reverb"),
    DELAY("Delay"),
    PITCH("Pitch"),
    SYNTH("Sintetizador"),
    EFFECTS("Efectos")
}

// Un punto clave en el tiempo donde tocamos la perilla virtual
data class AutomationPoint(
    val timeMs: Long,
    val value: Float // Un valor limpio de 0.0 (mínimo) a 1.0 (máximo)
)

// La línea de tiempo completa para un parámetro con soporte de interpolación configurable
data class AutomationLane(
    val parameter: AutomationParameterType,
    val points: MutableList<AutomationPoint> = mutableListOf(),
    val interpolationMode: InterpolationMode = InterpolationMode.LINEAR // <--- Modo activo por defecto
) {
    // Función matemática que calcula el valor exacto en cualquier milisegundo según el modo de interpolación
    fun getValueAtTime(timeMs: Long): Float {
        if (points.isEmpty()) return getDefaultValue(parameter)
        if (timeMs <= points.first().timeMs) return points.first().value
        if (timeMs >= points.last().timeMs) return points.last().value

        val nextIndex = points.indexOfFirst { it.timeMs >= timeMs }
        if (nextIndex <= 0) return points.first().value
        val prev = points[nextIndex - 1]
        val next = points[nextIndex]

        // Protección quirúrgica contra división por cero si dos puntos comparten timestamp
        if (next.timeMs == prev.timeMs) return prev.value

        return when (interpolationMode) {
            InterpolationMode.LINEAR -> {
                // Interpolación lineal (crear una rampa suave entre punto y punto)
                val progress = (timeMs - prev.timeMs).toFloat() / (next.timeMs - prev.timeMs).toFloat()
                prev.value + (next.value - prev.value) * progress
            }
            InterpolationMode.STEP -> {
                // En modo escalón, mantiene el valor plano del nodo anterior hasta llegar al salto exacto del siguiente
                prev.value
            }
        }
    }

    private fun getDefaultValue(param: AutomationParameterType): Float {
        return when (param) {
            AutomationParameterType.VOLUME -> 0.8f
            AutomationParameterType.PAN -> 0.5f
            AutomationParameterType.CUTOFF -> 1.0f
            AutomationParameterType.RESONANCE -> 0.5f
            AutomationParameterType.REVERB -> 0.0f
            AutomationParameterType.DELAY -> 0.0f
            AutomationParameterType.PITCH -> 0.5f
            AutomationParameterType.SYNTH -> 0.5f
            AutomationParameterType.EFFECTS -> 0.0f
        }
    }
}
