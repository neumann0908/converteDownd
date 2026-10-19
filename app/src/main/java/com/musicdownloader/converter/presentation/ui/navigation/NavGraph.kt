package com.musicdownloader.converter.presentation.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicdownloader.converter.presentation.navigation.BottomNavItem
import com.musicdownloader.converter.presentation.ui.screens.DownloadScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Download,
        BottomNavItem.Transform,
        BottomNavItem.LibraryMusic,
        BottomNavItem.PlayCircle,
        BottomNavItem.History
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
                            navController.navigate(item.route) {
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
            startDestination = BottomNavItem.Download.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Download.route) { DownloadScreen() }
            composable(BottomNavItem.Transform.route) { Text("Pantalla de Conversión") }
            composable(BottomNavItem.LibraryMusic.route) { Text("Pantalla de Biblioteca") }
            composable(BottomNavItem.PlayCircle.route) { Text("Pantalla de Reproductor") }
            composable(BottomNavItem.History.route) { Text("Pantalla de Historial") }
        }
    }
}
