package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerManager: AudioPlayerManager
) : ViewModel() {

    // Exponemos el estado de la canción actual desde el manager
    val currentSong = playerManager.currentSong

    // Exponemos el estado de reproducción (si está sonando o pausado)
    val isPlaying = playerManager.isPlaying

    // Función para alternar entre reproducir y pausar
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }
}
