package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ¡Aquí estaba el detalle: es "data class" con espacio, no con punto!
data class Track(val name: String, val path: String)

class DjViewModel(application: Application) : AndroidViewModel(application) {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isPlayingDeckA = MutableStateFlow(false)
    val isPlayingDeckA: StateFlow<Boolean> = _isPlayingDeckA.asStateFlow()

    private val _isPlayingDeckB = MutableStateFlow(false)
    val isPlayingDeckB: StateFlow<Boolean> = _isPlayingDeckB.asStateFlow()

    init {
        loadMusicTracks()
    }

    fun loadMusicTracks() {
        viewModelScope.launch {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val musicFiles = mutableListOf<Track>()
            
            musicDir?.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension.equals("mp3", true) || file.extension.equals("wav", true))) {
                    musicFiles.add(Track(file.name, file.absolutePath))
                }
            }
            _tracks.value = musicFiles
        }
    }

    fun toggleDeckA() {
        _isPlayingDeckA.value = !_isPlayingDeckA.value
    }

    fun toggleDeckB() {
        _isPlayingDeckB.value = !_isPlayingDeckB.value
    }
}
