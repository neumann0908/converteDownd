package com.musicdownloader.converter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn // Cuadrícula para Beat Studio (Motor 1)
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq // 🎙️ Ícono de onda para el Sampler Studio (Motor 2)
import androidx.compose.material.icons.filled.Tune // 🎹 Ícono de controles/secuenciador para el Sequencer Engine (Motor 3)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicdownloader.converter.presentation.ui.screens.DownloadScreen
import com.musicdownloader.converter.presentation.screens.library.LibraryScreen
import com.musicdownloader.converter.presentation.ui.screens.PlayerScreen
import com.musicdownloader.converter.presentation.ui.screens.PlaylistsScreen
import com.musicdownloader.converter.presentation.screens.DjTurntablesScreen
import com.musicdownloader.converter.presentation.viewmodel.DjViewModel
import com.musicdownloader.converter.presentation.ui.MiniPlayerBar
import com.musicdownloader.converter.presentation.viewmodel.PlayerViewModel

// 🎛️ Importaciones del Motor 1: Beat Studio (Drum Engine)
import com.musicdownloader.converter.beatstudio.presentation.drum.BeatStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.drum.DrumStudioViewModel

// 🎙️ Importaciones del Motor 2: Sampler Studio (Sampler Engine)
import com.musicdownloader.converter.beatstudio.presentation.sampler.SamplerStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.sampler.SamplerStudioViewModel

// 🎹 Importaciones del Motor 3: Sequencer Studio (Sequencer Engine)
import com.musicdownloader.converter.beatstudio.presentation.sequencer.SequencerStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.sequencer.SequencerStudioViewModel

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Download : Screen("download", "Descargar", { Icon(Icons.Default.Download, contentDescription = null) })
    object Library : Screen("library", "Biblioteca", { Icon(Icons.Default.LibraryMusic, contentDescription = null) })
    object Playlists : Screen("playlists", "Listas", { Icon(Icons.Default.QueueMusic, contentDescription = null) })
    object Player : Screen("player", "Player", { Icon(Icons.Default.PlayCircle, contentDescription = null) })
    object DjTurntables : Screen("dj_turntables", "DJ Pro", { Icon(Icons.Default.MusicNote, contentDescription = null) })
    
    // 🥁 Motor 1: Drum Studio
    object BeatStudio : Screen("beat_studio", "Beat Studio", { Icon(Icons.Default.GridOn, contentDescription = null) })

    // 🎙️ Motor 2: Sampler Studio
    object SamplerStudio : Screen("sampler_studio", "Sampler", { Icon(Icons.Default.GraphicEq, contentDescription = null) })

    // 🎹 Motor 3: Sequencer Studio (Secuenciador profesional)
    object SequencerStudio : Screen("sequencer_studio", "Sequencer", { Icon(Icons.Default.Tune, contentDescription = null) })
}

@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    val currentTrackTitle by playerViewModel.currentTrackTitle.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    val items = listOf(
        Screen.Download,
        Screen.Library,
        Screen.Playlists,
        Screen.Player,
        Screen.DjTurntables,
        Screen.BeatStudio,
        Screen.SamplerStudio,
        Screen.SequencerStudio // <--- ¡Motor 3 añadido formalmente a la barra de navegación inferior!
    )

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.background(Color(0xFF13131A))
            ) {
                MiniPlayerBar(
                    currentSongName = currentTrackTitle, 
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        playerViewModel.togglePlayPause()
                    },
                    onBarClick = {
                        navController.navigate(Screen.Player.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBar(
                    containerColor = Color(0xFF1E1E2C),
                    contentColor = Color.White
                ) {
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
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                unselectedIconColor = Color.LightGray,
                                selectedTextColor = Color(0xFFFFD700),
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFFFFD700)
                            )
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
            
            // Consola DJ Pro
            composable(Screen.DjTurntables.route) {
                val djViewModel: DjViewModel = hiltViewModel()
                DjTurntablesScreen(viewModel = djViewModel)
            }

            // 🎛️ Motor 1: Beat Studio
            composable(Screen.BeatStudio.route) {
                val drumStudioViewModel: DrumStudioViewModel = hiltViewModel()
                BeatStudioScreen(viewModel = drumStudioViewModel)
            }

            // 🎙️ Motor 2: Sampler Studio
            composable(Screen.SamplerStudio.route) {
                val samplerStudioViewModel: SamplerStudioViewModel = hiltViewModel()
                SamplerStudioScreen(viewModel = samplerStudioViewModel)
            }

            // 🎹 Motor 3: Sequencer Studio (Conectado con su ViewModel de Hilt)
            composable(Screen.SequencerStudio.route) {
                val sequencerStudioViewModel: SequencerStudioViewModel = hiltViewModel()
                SequencerStudioScreen(viewModel = sequencerStudioViewModel)
            }
        }
    }
}
