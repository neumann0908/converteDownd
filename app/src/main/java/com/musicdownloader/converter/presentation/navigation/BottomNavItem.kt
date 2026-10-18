package com.musicdownloader.converter.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Transform
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val screen: Screen, val title: String, val icon: ImageVector) {
    object Downloader : BottomNavItem(Screen.Downloader, "Descargar", Icons.Default.Download)
    object Converter : BottomNavItem(Screen.Converter, "Convertir", Icons.Default.Transform)
    object Library : BottomNavItem(Screen.Library, "Biblioteca", Icons.Default.LibraryMusic)
    object Player : BottomNavItem(Screen.Player, "Reproductor", Icons.Default.PlayCircle)
    object History : BottomNavItem(Screen.History, "Historial", Icons.Default.History)
}
