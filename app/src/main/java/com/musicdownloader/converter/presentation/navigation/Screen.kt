package com.musicdownloader.converter.presentation.navigation

sealed class Screen(val route: String) {
    object Downloader : Screen("downloader_screen")
    object Converter : Screen("converter_screen")
    object Library : Screen("library_screen")
    object Player : Screen("player_screen")
    object History : Screen("history_screen")
    
    // Consola DJ Pro existente
    object DjTurntables : Screen("dj_turntables")

    // 🎛️ FASE 1: Beat Studio Pro (Motor 1 - Drum Engine)
    object BeatStudio : Screen("beat_studio")

    // 🎙️ FASE 2: Sampler Studio Pro (Motor 2 - Sampler Engine)
    object SamplerStudio : Screen("sampler_studio")
}
