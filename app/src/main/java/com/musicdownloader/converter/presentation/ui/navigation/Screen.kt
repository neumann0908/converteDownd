package com.musicdownloader.converter.presentation.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Downloader : Screen("downloader", "Descargar")
    object Converter : Screen("converter", "Convertir")
    object Library : Screen("library", "Biblioteca")
}
