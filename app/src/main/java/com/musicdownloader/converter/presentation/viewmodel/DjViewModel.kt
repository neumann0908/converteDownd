package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DjViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPlayingDeckA = MutableStateFlow(false)
    val isPlayingDeckA: StateFlow<Boolean> = _isPlayingDeckA.asStateFlow()

    private val _isPlayingDeckB = MutableStateFlow(false)
    val isPlayingDeckB: StateFlow<Boolean> = _isPlayingDeckB.asStateFlow()

    private val _currentTrackA = MutableStateFlow("Electronic Cyber Beat")
    val currentTrackA: StateFlow<String> = _currentTrackA.asStateFlow()

    private val _currentTrackB = MutableStateFlow("Toca para cargar pista")
    val currentTrackB: StateFlow<String> = _currentTrackB.asStateFlow()

    // Lista dinámica que almacenará los nombres de los archivos reales descargados
    private val _libraryTracks = MutableStateFlow<List<String>>(emptyList())
    val libraryTracks: StateFlow<List<String>> = _libraryTracks.asStateFlow()

    init {
        loadLibraryTracks()
    }

    // Escanea la misma carpeta "MusicDownloads" que usa la Biblioteca
    fun loadLibraryTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            val files = if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.map { it.name }?.sorted() ?: emptyList()
            } else {
                emptyList()
            }

            _libraryTracks.value = files
        }
    }

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
