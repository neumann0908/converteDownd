package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DjViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPlayingDeckA = MutableStateFlow(false)
    val isPlayingDeckA: StateFlow<Boolean> = _isPlayingDeckA.asStateFlow()

    private val _isPlayingDeckB = MutableStateFlow(false)
    val isPlayingDeckB: StateFlow<Boolean> = _isPlayingDeckB.asStateFlow()

    // Pistas actuales en los Decks
    private val _currentTrackA = MutableStateFlow("Electronic Cyber Beat")
    val currentTrackA: StateFlow<String> = _currentTrackA.asStateFlow()

    private val _currentTrackB = MutableStateFlow("Toca para cargar pista")
    val currentTrackB: StateFlow<String> = _currentTrackB.asStateFlow()

    // Lista de canciones simulada de la biblioteca (puedes conectarla con tu Room DB o lista real más adelante)
    private val _libraryTracks = MutableStateFlow(listOf(
        "Electronic Cyber Beat",
        "Cyberpunk Synthwave",
        "Bass Drop Remix",
        "Lofi Chillhop",
        "Night Drive Theme",
        "Retro Synth Pop"
    ))
    val libraryTracks: StateFlow<List<String>> = _libraryTracks.asStateFlow()

    fun toggleDeckA() {
        _isPlayingDeckA.value = !_isPlayingDeckA.value
    }

    fun toggleDeckB() {
        _isPlayingDeckB.value = !_isPlayingDeckB.value
    }

    fun setTrackForDeckA(trackName: String) {
        _currentTrackA.value = trackName
    }

    fun setTrackForDeckB(trackName: String) {
        _currentTrackB.value = trackName
    }
}
