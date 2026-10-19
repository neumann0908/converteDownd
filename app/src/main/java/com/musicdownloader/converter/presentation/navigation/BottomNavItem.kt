package com.musicdownloader.converter.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
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
    object LibraryMusic : BottomNavItem("library", "Biblioteca", Icons.Default.LibraryMusic)
    object PlayCircle : BottomNavItem("player", "Reproductor", Icons.Default.PlayCircle)
    object History : BottomNavItem("history", "Historial", Icons.Default.History)
}
