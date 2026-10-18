package com.musicdownloader.converter.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musicdownloader.converter.presentation.screens.converter.ConverterScreen
import com.musicdownloader.converter.presentation.screens.downloader.DownloaderScreen
import com.musicdownloader.converter.presentation.screens.history.HistoryScreen
import com.musicdownloader.converter.presentation.screens.library.LibraryScreen
import com.musicdownloader.converter.presentation.screens.player.PlayerScreen

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
    }
}
