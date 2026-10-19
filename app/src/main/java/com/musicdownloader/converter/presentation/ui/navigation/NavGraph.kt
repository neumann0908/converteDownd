package com.musicdownloader.converter.presentation.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
        BottomNavItem.Library,
        BottomNavItem.Player,
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
            composable(BottomNavItem.Transform.route) { SimplePlaceholder("Pantalla de Conversión") }
            composable(BottomNavItem.Library.route) { SimplePlaceholder("Pantalla de Biblioteca") }
            composable(BottomNavItem.Player.route) { SimplePlaceholder("Pantalla de Reproductor") }
            composable(BottomNavItem.History.route) { SimplePlaceholder("Pantalla de Historial") }
        }
    }
}

@Composable
fun SimplePlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
