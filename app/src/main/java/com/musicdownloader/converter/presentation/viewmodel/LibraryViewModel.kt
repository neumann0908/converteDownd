package com.musicdownloader.converter.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import com.musicdownloader.converter.util.AudioTrimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AudioFileItem(
    val name: String,
    val path: String,
    val sizeMb: String
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val playerManager: AudioPlayerManager
) : ViewModel() {

    private val _allAudioList = mutableListOf<AudioFileItem>()

    private val _audioList = MutableStateFlow<List<AudioFileItem>>(emptyList())
    val audioList: StateFlow<List<AudioFileItem>> = _audioList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- NUEVO ESTADO PARA EL INDICADOR DE CARGA ---
    private val _isTrimming = MutableStateFlow(false)
    val isTrimming: StateFlow<Boolean> = _isTrimming.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        loadDownloadedFiles()
    }

    fun loadDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            val files = if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.map { file ->
                    val sizeMb = String.format("%.2f MB", file.length().toDouble() / (1024 * 1024))
                    AudioFileItem(
                        name = file.name,
                        path = file.absolutePath,
                        sizeMb = sizeMb
                    )
                }?.sortedBy { it.name } ?: emptyList()
            } else {
                emptyList()
            }

            _allAudioList.clear()
            _allAudioList.addAll(files)
            filterAudios(_searchQuery.value)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterAudios(query)
    }

    private fun filterAudios(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            if (query.isBlank()) {
                _audioList.value = _allAudioList.toList()
            } else {
                _audioList.value = _allAudioList.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun deleteAudio(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            loadDownloadedFiles()
        }
    }

    fun trimAudio(audio: AudioFileItem, startSeconds: Int, endSeconds: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Activamos el indicador visual de carga al inicio del proceso
            _isTrimming.value = true
            try {
                val srcFile = File(audio.path)
                if (!srcFile.exists() || startSeconds >= endSeconds) {
                    withContext(Dispatchers.Main) {
                        _isTrimming.value = false // Ocultar indicador si falla la validación
                        _errorMessage.value = "El archivo no existe o los tiempos de recorte no son válidos."
                        onResult(false)
                    }
                    return@launch
                }
                val downloadDir = srcFile.parentFile ?: File(context.getExternalFilesDir(null), "MusicDownloads")
                val baseName = srcFile.nameWithoutExtension
                
                val extension = if (audio.path.endsWith(".webm", ignoreCase = true)) "webm" else "m4a"
                val dstFile = File(downloadDir, "${baseName}_recortado.$extension")

                AudioTrimmer.trim(
                    srcPath = audio.path,
                    dstPath = dstFile.absolutePath,
                    startMs = startSeconds * 1000L,
                    endMs = endSeconds * 1000L
                )
                
                withContext(Dispatchers.Main) {
                    _isTrimming.value = false // Desactivar indicador al terminar con éxito
                    loadDownloadedFiles()
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isTrimming.value = false // Desactivar indicador si ocurre una excepción
                    _errorMessage.value = "Detalle: ${e.localizedMessage ?: e.toString()}"
                    onResult(false)
                }
            }
        }
    }
}
