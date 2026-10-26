package com.musicdownloader.converter.presentation.navigation

sealed class Screen(val route: String) {
    object Downloader : Screen("downloader_screen")
    object Converter : Screen("converter_screen")
    object Library : Screen("library_screen")
    object Player : Screen("player_screen")
    object History : Screen("history_screen")
    
    // Nueva ruta añadida para la consola DJ Pro
    object DjTurntables : Screen("dj_turntables")
}
