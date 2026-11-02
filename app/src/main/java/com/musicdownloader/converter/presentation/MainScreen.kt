package com.musicdownloader.converter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn // Cuadrícula para Beat Studio (Motor 1)
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.GraphicEq // 🎙️ Ícono de onda para el Sampler Studio (Motor 2)
import androidx.compose.material.icons.filled.Tune // 🎹 Ícono de controles/secuenciador para el Sequencer Engine (Motor 3)
import androidx.compose.material.icons.filled.Piano // 🎹 Ícono de piano para el Synth Studio (Motor 5)
import androidx.compose.material.icons.filled.Mic // 🎙️ Ícono de micrófono para el Recording Engine (Motor 8)
import androidx.compose.material.icons.filled.Timeline // 📈 Ícono de línea de tiempo para el Automation Engine (Motor 9)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// 🎹 Importaciones del Motor 5: Synth Studio (Synth Engine)
import com.musicdownloader.converter.beatstudio.presentation.synth.SynthStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.synth.SynthViewModel

// 🎙️ Importaciones del Motor 8: Recording Engine (Grabación)
import com.musicdownloader.converter.beatstudio.presentation.recording.RecordingScreen
import com.musicdownloader.converter.beatstudio.presentation.recording.RecordingViewModel

// 📈 Importaciones del Motor 9: Automation Engine (Automatización)
import com.musicdownloader.converter.beatstudio.presentation.automation.AutomationScreen
import com.musicdownloader.converter.beatstudio.presentation.automation.AutomationViewModel

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Download : Screen("download", "Descargar", { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp)) })
    object Library : Screen("library", "Biblioteca", { Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(20.dp)) })
    object Playlists : Screen("playlists", "Listas", { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) })
    object Player : Screen("player", "Player", { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp)) })
    object DjTurntables : Screen("dj_turntables", "DJ Pro", { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp)) })
    
    // 🥁 Motor 1: Drum Studio
    object BeatStudio : Screen("beat_studio", "Beat Studio", { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(20.dp)) })

    // 🎙️ Motor 2: Sampler Studio
    object SamplerStudio : Screen("sampler_studio", "Sampler", { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(20.dp)) })

    // 🎹 Motor 3: Sequencer Studio (Secuenciador profesional)
    object SequencerStudio : Screen("sequencer_studio", "Sequencer", { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp)) })

    // 🎹 Motor 5: Synth Studio (Sintetizador profesional)
    object SynthStudio : Screen("synth_studio", "Synth", { Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(20.dp)) })

    // 🎙️ Motor 8: Recording Engine (Estudio de grabación y tomas)
    object RecordingStudio : Screen("recording_studio", "Record", { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp)) })

    // 📈 Motor 9: Automation Engine (Curvas de automatización)
    object AutomationStudio : Screen("automation_studio", "Automation", { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(20.dp)) })
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
        Screen.SequencerStudio,
        Screen.SynthStudio,
        Screen.RecordingStudio,
        Screen.AutomationStudio
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

                // --- BARRA DE NAVEGACIÓN DESLIZANTE PROFESIONAL (ESTILO DAW) ---
                Surface(
                    color = Color(0xFF1E1E2C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(items) { screen ->
                            val isSelected = currentRoute == screen.route
                            val backgroundColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF28283D)
                            val contentColor = if (isSelected) Color.Black else Color.LightGray

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(backgroundColor)
                                    .clickable {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                                        screen.icon()
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title,
                                        color = contentColor,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
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

            // 🎹 Motor 5: Synth Studio (Conectado con su SynthViewModel de Hilt)
            composable(Screen.SynthStudio.route) {
                val synthViewModel: SynthViewModel = hiltViewModel()
                SynthStudioScreen(viewModel = synthViewModel)
            }

            // 🎙️ Motor 8: Recording Engine (Estudio de grabación integrado con Hilt)
            composable(Screen.RecordingStudio.route) {
                val recordingViewModel: RecordingViewModel = hiltViewModel()
                RecordingScreen(viewModel = recordingViewModel)
            }

            // 📈 Motor 9: Automation Engine (Estudio de automatización de parámetros)
            composable(Screen.AutomationStudio.route) {
                val automationViewModel: AutomationViewModel = hiltViewModel()
                AutomationScreen(viewModel = automationViewModel)
            }
        }
    }
}
