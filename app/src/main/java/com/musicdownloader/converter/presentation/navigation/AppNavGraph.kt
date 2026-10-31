package com.musicdownloader.converter.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musicdownloader.converter.presentation.screens.DjTurntablesScreen
import com.musicdownloader.converter.presentation.screens.converter.ConverterScreen
import com.musicdownloader.converter.presentation.screens.downloader.DownloaderScreen
import com.musicdownloader.converter.presentation.screens.history.HistoryScreen
import com.musicdownloader.converter.presentation.screens.library.LibraryScreen
import com.musicdownloader.converter.presentation.screens.player.PlayerScreen
import com.musicdownloader.converter.presentation.viewmodel.DjViewModel

// 1. Importaciones de nuestro Beat Studio Pro (Motor 1)
import com.musicdownloader.converter.beatstudio.presentation.drum.BeatStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.drum.DrumStudioViewModel

// 🎙️ 2. Importaciones de nuestro Sampler Studio Pro (Motor 2)
import com.musicdownloader.converter.beatstudio.presentation.sampler.SamplerStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.sampler.SamplerStudioViewModel

// 🎹 3. Importaciones de nuestro Sequencer Studio Pro (Motor 3)
import com.musicdownloader.converter.beatstudio.presentation.sequencer.SequencerStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.sequencer.SequencerStudioViewModel

// 🎹 5. Importaciones de nuestro Synth Studio Pro (Motor 5)
import com.musicdownloader.converter.beatstudio.presentation.synth.SynthStudioScreen
import com.musicdownloader.converter.beatstudio.presentation.synth.SynthViewModel

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Downloader.route,
        modifier = modifier
    ) {
        composable(Screen.Downloader.route) { DownloaderScreen() }
        composable(Screen.Converter.route) { ConverterScreen() }
        composable(Screen.Library.route) { LibraryScreen() }
        composable(Screen.Player.route) { PlayerScreen() }
        composable(Screen.History.route) { HistoryScreen() }
        
        // Ruta para la consola DJ Pro existente
        composable("dj_turntables") {
            val djViewModel: DjViewModel = viewModel()
            DjTurntablesScreen(viewModel = djViewModel)
        }

        // 🛠️ FASE 1: Integración Quirúrgica del Nuevo Beat Studio Pro (Motor 1)
        composable("beat_studio") {
            val drumStudioViewModel: DrumStudioViewModel = hiltViewModel()
            BeatStudioScreen(viewModel = drumStudioViewModel)
        }

        // 🎙️ FASE 2: Integración Quirúrgica del Nuevo Sampler Studio Pro (Motor 2)
        composable("sampler_studio") {
            val samplerStudioViewModel: SamplerStudioViewModel = hiltViewModel()
            SamplerStudioScreen(viewModel = samplerStudioViewModel)
        }

        // 🎹 FASE 3: Integración Quirúrgica del Nuevo Sequencer Studio Pro (Motor 3)
        composable("sequencer_studio") {
            val sequencerStudioViewModel: SequencerStudioViewModel = hiltViewModel()
            SequencerStudioScreen(viewModel = sequencerStudioViewModel)
        }

        // 🎹 FASE 5: Integración Quirúrgica del Nuevo Synth Studio Pro (Motor 5)
        composable("synth_studio") {
            val synthViewModel: SynthViewModel = hiltViewModel()
            SynthStudioScreen(viewModel = synthViewModel)
        }
    }
}
