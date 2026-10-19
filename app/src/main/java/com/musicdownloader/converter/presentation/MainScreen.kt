package com.musicdownloader.converter.presentation

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
import com.musicdownloader.converter.presentation.ui.screens.LibraryScreen
import com.musicdownloader.converter.presentation.ui.screens.PlayerScreen
import com.musicdownloader.converter.presentation.ui.screens.PlaylistsScreen

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Download : Screen("download", "Descargar", { Icon(Icons.Default.Download, contentDescription = null) })
    object Library : Screen("library", "Biblioteca", { Icon(Icons.Default.LibraryMusic, contentDescription = null) })
    object Playlists : Screen("playlists", "Listas", { Icon(Icons.Default.QueueMusic, contentDescription = null) })
    object Player : Screen("player", "Player", { Icon(Icons.Default.PlayCircle, contentDescription = null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Download,
        Screen.Library,
        Screen.Playlists,
        Screen.Player
    )

    Scaffold(
        bottomBar = {
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
