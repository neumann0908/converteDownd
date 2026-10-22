package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerManager: AudioPlayerManager
) : ViewModel() {

    // Exponemos el título de la pista actual
    val currentTrackTitle = playerManager.currentTrackTitle

    // Exponemos el estado de reproducción
    val isPlaying = playerManager.isPlaying

    // NUEVO: Exponemos los estados de Shuffle y Repeat
    val isShuffleEnabled = playerManager.isShuffleEnabled
    val repeatMode = playerManager.repeatMode

    // Función para alternar reproducción y pausa
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    // Función para saltar a la siguiente canción en la cola
    fun skipToNext() {
        playerManager.skipToNext()
    }

    // Función para regresar a la canción anterior en la cola
    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    // NUEVO: Función para alternar el modo aleatorio
    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    // NUEVO: Función para cambiar el modo de repetición (Off -> All -> One)
    fun cycleRepeatMode() {
        playerManager.cycleRepeatMode()
    }
}
