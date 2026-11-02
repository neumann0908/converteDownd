package com.musicdownloader.converter.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn // Ícono para la cuadrícula del secuenciador (Motor 1)
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.GraphicEq // 🎙️ Ícono de ondas/ecualizador para el Sampler Studio (Motor 2)
import androidx.compose.material.icons.filled.Tune // 🎹 Ícono de controles/secuenciador para el Sequencer Studio (Motor 3)
import androidx.compose.material.icons.filled.Piano // 🎹 Ícono de piano para el Synth Studio (Motor 5)
import androidx.compose.material.icons.filled.Mic // 🎙️ Ícono de micrófono para el Recording Engine (Motor 8)
import androidx.compose.material.icons.filled.Timeline // 📈 Ícono de línea de tiempo para el Automation Engine (Motor 9)
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Download : BottomNavItem("download", "Descargar", Icons.Default.Download)
    object Transform : BottomNavItem("convert", "Convertir", Icons.Default.Transform)
    object Library : BottomNavItem("library", "Biblioteca", Icons.Default.LibraryMusic)
    object Player : BottomNavItem("player", "Player", Icons.Default.PlayCircle)
    object History : BottomNavItem("history", "Historial", Icons.Default.History)
    
    // 🎧 Consola DJ Pro existente
    object DjTurntables : BottomNavItem("dj_turntables", "DJ Pro", Icons.Default.MusicNote)

    // 🎛️ FASE 1: Beat Studio Pro (Motor 1 - Drum Engine)
    object BeatStudio : BottomNavItem("beat_studio", "Beat Studio", Icons.Default.GridOn)

    // 🎙️ FASE 2: Sampler Studio Pro (Motor 2 - Sampler Engine)
    object SamplerStudio : BottomNavItem("sampler_studio", "Sampler", Icons.Default.GraphicEq)

    // 🎹 FASE 3: Sequencer Studio Pro (Motor 3 - Sequencer Engine)
    object SequencerStudio : BottomNavItem("sequencer_studio", "Sequencer", Icons.Default.Tune)

    // 🎹 FASE 5: Synth Studio Pro (Motor 5 - Synth Engine)
    object SynthStudio : BottomNavItem("synth_studio", "Synth", Icons.Default.Piano)

    // 🎙️ FASE 8: Recording Engine (Motor 8 - Grabación y Estudio de Voz)
    object RecordingStudio : BottomNavItem("recording_studio", "Record", Icons.Default.Mic)

    // 📈 FASE 9: Automation Engine (Motor 9 - Curvas de automatización)
    object AutomationStudio : BottomNavItem("automation_studio", "Automation", Icons.Default.Timeline)
}
