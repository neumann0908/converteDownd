package com.musicdownloader.converter.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn // <-- Ícono para la cuadrícula del secuenciador
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Transform
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

    // 🎛️ FASE 1: Nuevo ítem añadido quirúrgicamente para el Beat Studio Pro
    object BeatStudio : BottomNavItem("beat_studio", "Beat Studio", Icons.Default.GridOn)
}
