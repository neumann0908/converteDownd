package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

data class DjTrack(
    val name: String,
    val path: String
)

@HiltViewModel
class DjViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : AndroidViewModel(context as Application) {

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

    // Dos reproductores independientes para la mezcla dual
    private var mediaPlayerA: MediaPlayer? = null
    private var mediaPlayerB: MediaPlayer? = null

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
        try {
            mediaPlayerA?.release()
            mediaPlayerA = MediaPlayer().apply {
                setDataSource(track.path)
                prepare()
                isLooping = true
            }
            _isPlayingDeckA.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTrackForDeckB(track: DjTrack) {
        _currentTrackB.value = track
        try {
            mediaPlayerB?.release()
            mediaPlayerB = MediaPlayer().apply {
                setDataSource(track.path)
                prepare()
                isLooping = true
            }
            _isPlayingDeckB.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDeckA() {
        val track = _currentTrackA.value ?: return
        if (mediaPlayerA == null) {
            setTrackForDeckA(track)
        }
        try {
            mediaPlayerA?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlayingDeckA.value = false
                } else {
                    player.start()
                    _isPlayingDeckA.value = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDeckB() {
        val track = _currentTrackB.value ?: return
        if (mediaPlayerB == null) {
            setTrackForDeckB(track)
        }
        try {
            mediaPlayerB?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlayingDeckB.value = false
                } else {
                    player.start()
                    _isPlayingDeckB.value = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Control del Crossfader (0.0 = Todo Deck A, 1.0 = Todo Deck B)
    fun updateCrossfader(value: Float) {
        val position = value.coerceIn(0f, 1f)
        
        // Curva de crossfade de potencia igual (Equal-power)
        val angleA = (1.0f - position) * (Math.PI / 2.0)
        val angleB = position * (Math.PI / 2.0)
        
        val volA = cos(angleA).toFloat()
        val volB = sin(angleB).toFloat()

        try {
            mediaPlayerA?.setVolume(volA, volA)
            mediaPlayerB?.setVolume(volB, volB)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerA?.release()
        mediaPlayerB?.release()
        mediaPlayerA = null
        mediaPlayerB = null
    }
}
