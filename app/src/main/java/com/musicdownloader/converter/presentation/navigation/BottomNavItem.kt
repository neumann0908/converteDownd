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
}
