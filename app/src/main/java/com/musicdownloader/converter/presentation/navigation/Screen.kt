package com.musicdownloader.converter.presentation.navigation

sealed class Screen(val route: String) {
    object Downloader : Screen("downloader_screen")
    object Converter : Screen("converter_screen")
    object Library : Screen("library_screen")
    object Player : Screen("player_screen")
    object History : Screen("history_screen")
    
    // Consola DJ Pro existente
    object DjTurntables : Screen("dj_turntables")

    // 🎛️ FASE 1: Nueva ruta añadida quirúrgicamente para el Beat Studio Pro
    object BeatStudio : Screen("beat_studio")
}
