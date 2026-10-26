package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.engine.DjAudioEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DjTrack(
    val name: String,
    val path: String
)

@HiltViewModel
class DjViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : AndroidViewModel(context as Application) {

    private val djAudioEngine = DjAudioEngine(context)

    private val _isPlayingDeckA = MutableStateFlow(false)
    val isPlayingDeckA: StateFlow<Boolean> = _isPlayingDeckA.asStateFlow()

    private val _isPlayingDeckB = MutableStateFlow(false)
    val isPlayingDeckB: StateFlow<Boolean> = _isPlayingDeckB.asStateFlow()

    private val _currentTrackA = MutableStateFlow<DjTrack?>(null)
    val currentTrackA: StateFlow<DjTrack?> = _currentTrackA.asStateFlow()

    private val _currentTrackB = MutableStateFlow<DjTrack?>(null)
    val currentTrackB: StateFlow<DjTrack?> = _currentTrackB.asStateFlow()

    private val _libraryTracks = MutableStateFlow<List<DjTrack>>(emptyList())
    val libraryTracks: StateFlow<List<DjTrack>> = _libraryTracks.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    init {
        loadLibraryTracks()
    }

    fun loadLibraryTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            val tracks = if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.map { DjTrack(name = it.name, path = it.absolutePath) }?.sortedBy { it.name } ?: emptyList()
            } else {
                emptyList()
            }
            _libraryTracks.value = tracks
        }
    }

    fun setTrackForDeckA(track: DjTrack) {
        _currentTrackA.value = track
        djAudioEngine.loadTrackA(track.path)
        _isPlayingDeckA.value = false
    }

    fun setTrackForDeckB(track: DjTrack) {
        _currentTrackB.value = track
        djAudioEngine.loadTrackB(track.path)
        _isPlayingDeckB.value = false
    }

    fun toggleDeckA() {
        val trackPath = _currentTrackA.value?.path ?: return
        val isPlaying = djAudioEngine.togglePlayA(trackPath)
        _isPlayingDeckA.value = isPlaying
    }

    fun toggleDeckB() {
        val trackPath = _currentTrackB.value?.path ?: return
        val isPlaying = djAudioEngine.togglePlayB(trackPath)
        _isPlayingDeckB.value = isPlaying
    }

    fun updateCrossfader(value: Float) {
        djAudioEngine.updateCrossfader(value)
    }

    // --- Conexión directa del Ecualizador con el motor ---
    fun setEqualizerA(low: Float, mid: Float, high: Float) {
        djAudioEngine.setEqualizerA(low, mid, high)
    }

    fun setEqualizerB(low: Float, mid: Float, high: Float) {
        djAudioEngine.setEqualizerB(low, mid, high)
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            djAudioEngine.stopRecording()
            _isRecording.value = false
            loadLibraryTracks()
        } else {
            val started = djAudioEngine.startRecording()
            _isRecording.value = started
        }
    }

    override fun onCleared() {
        super.onCleared()
        djAudioEngine.release()
    }
}
