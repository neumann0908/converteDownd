Package com.musicdownloader.converter.presentation.navigation

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

// 🎙️ 8. Importaciones de nuestro Recording Engine Pro (Motor 8)
import com.musicdownloader.converter.beatstudio.presentation.recording.RecordingScreen
import com.musicdownloader.converter.beatstudio.presentation.recording.RecordingViewModel

// 📈 9. Importaciones de nuestro Automation Engine Pro (Motor 9)
import com.musicdownloader.converter.beatstudio.presentation.automation.AutomationScreen
import com.musicdownloader.converter.beatstudio.presentation.automation.AutomationViewModel

// 🚀 10. Importaciones de nuestro Master Engine Pro (Motor 10)
import com.musicdownloader.converter.beatstudio.presentation.master.MasterScreen
import com.musicdownloader.converter.beatstudio.presentation.master.MasterViewModel

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
        composable(Screen.BeatStudio.route) {
            val drumStudioViewModel: DrumStudioViewModel = hiltViewModel()
            BeatStudioScreen(viewModel = drumStudioViewModel)
        }

        // 🎙️ FASE 2: Integración Quirúrgica del Nuevo Sampler Studio Pro (Motor 2)
        composable(Screen.SamplerStudio.route) {
            val samplerStudioViewModel: SamplerStudioViewModel = hiltViewModel()
            SamplerStudioScreen(viewModel = samplerStudioViewModel)
        }

        // 🎹 FASE 3: Integración Quirúrgica del Nuevo Sequencer Studio Pro (Motor 3)
        composable(Screen.SequencerStudio.route) {
            val sequencerStudioViewModel: SequencerStudioViewModel = hiltViewModel()
            SequencerStudioScreen(viewModel = sequencerStudioViewModel)
        }

        // 🎹 FASE 5: Integración Quirúrgica del Nuevo Synth Studio Pro (Motor 5)
        composable(Screen.SynthStudio.route) {
            val synthViewModel: SynthViewModel = hiltViewModel()
            SynthStudioScreen(viewModel = synthViewModel)
        }

        // 🎙️ FASE 8: Integración Quirúrgica del Nuevo Recording Engine (Motor 8)
        composable(Screen.RecordingStudio.route) {
            val recordingViewModel: RecordingViewModel = hiltViewModel()
            RecordingScreen(viewModel = recordingViewModel)
        }

        // 📈 FASE 9: Integración Quirúrgica del Nuevo Automation Engine (Motor 9)
        composable(Screen.AutomationStudio.route) {
            val automationViewModel: AutomationViewModel = hiltViewModel()
            AutomationScreen(viewModel = automationViewModel)
        }

        // 🚀 FASE 10: Integración Quirúrgica del Nuevo Master Engine (Motor 10)
        composable(Screen.MasterStudio.route) {
            val masterViewModel: MasterViewModel = hiltViewModel()
            MasterScreen(viewModel = masterViewModel)
        }
    }
}
