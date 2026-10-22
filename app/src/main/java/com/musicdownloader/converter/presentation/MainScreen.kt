package com.musicdownloader.converter.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicdownloader.converter.presentation.ui.screens.DownloadScreen
import com.musicdownloader.converter.presentation.screens.library.LibraryScreen
import com.musicdownloader.converter.presentation.ui.screens.PlayerScreen
import com.musicdownloader.converter.presentation.ui.screens.PlaylistsScreen
import com.musicdownloader.converter.presentation.ui.MiniPlayerBar // <-- Importamos nuestro nuevo componente

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Download : Screen("download", "Descargar", { Icon(Icons.Default.Download, contentDescription = null) })
    object Library : Screen("library", "Biblioteca", { Icon(Icons.Default.LibraryMusic, contentDescription = null) })
    object Playlists : Screen("playlists", "Listas", { Icon(Icons.Default.QueueMusic, contentDescription = null) })
    object Player : Screen("player", "Player", { Icon(Icons.Default.PlayCircle, contentDescription = null) })
}

@Composable
fun MainScreen(
    // Si manejas tu reproductor mediante un ViewModel o gestor inyectado/pasado aquí, 
    // puedes recibirlo o instanciarlo. Por ejemplo: playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    // TODO: Reemplaza estas variables de prueba con los estados reales de tu PlayerViewModel o PlayerManager
    // Ejemplo: val currentSong = playerViewModel.currentSong.collectAsState().value
    //          val isPlaying = playerViewModel.isPlaying.collectAsState().value
    val currentSongName: String? = null // Se ocultará automáticamente si es nulo o vacío
    val isPlaying = false

    val items = listOf(
        Screen.Download,
        Screen.Library,
        Screen.Playlists,
        Screen.Player
    )

    Scaffold(
        bottomBar = {
            Column {
                // 1. El Mini-Player flotando de forma persistente sobre la barra de navegación
                MiniPlayerBar(
                    currentSongName = currentSongName,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        // TODO: Llama aquí a la función de play/pausa de tu reproductor
                        // playerViewModel.togglePlayPause()
                    },
                    onBarClick = {
                        // Al hacer clic en la barra, navega a la pantalla completa del Player
                        navController.navigate(Screen.Player.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 2. Tu barra de navegación inferior de siempre
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Download.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Download.route) { DownloadScreen() }
            composable(Screen.Library.route) { LibraryScreen() }
            composable(Screen.Playlists.route) { PlaylistsScreen() }
            composable(Screen.Player.route) { PlayerScreen() }
        }
    }
}
