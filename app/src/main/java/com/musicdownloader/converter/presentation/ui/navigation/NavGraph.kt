package com.musicdownloader.converter.presentation.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicdownloader.converter.presentation.ui.screens.ConverterScreen
import com.musicdownloader.converter.presentation.ui.screens.DownloaderScreen
import com.musicdownloader.converter.presentation.ui.screens.LibraryScreen

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    object Download : NavigationItem(Screen.Downloader.route, "Descargar", Icons.Default.Download)
    object Converter : NavigationItem(Screen.Converter.route, "Convertir", Icons.Default.Sync)
    object Library : NavigationItem(Screen.Library.route, "Biblioteca", Icons.Default.LibraryMusic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenNavGraph() {
    val navController = rememberNavController()
    val items = listOf(
        NavigationItem.Download,
        NavigationItem.Converter,
        NavigationItem.Library
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = Screen.Downloader.route) {
                composable(Screen.Downloader.route) { DownloaderScreen() }
                composable(Screen.Converter.route) { ConverterScreen() }
                composable(Screen.Library.route) { LibraryScreen() }
            }
        }
    }
}
