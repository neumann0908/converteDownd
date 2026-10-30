package com.musicdownloader.converter.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import com.musicdownloader.converter.util.AudioTrimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // --- ESTADO PARA EL INDICADOR DE CARGA ---
    private val _isTrimming = MutableStateFlow(false)
    val isTrimming: StateFlow<Boolean> = _isTrimming.asStateFlow()

    // --- CONTROLADOR DE PREVISUALIZACIÓN ---
    private var previewJob: Job? = null

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        loadDownloadedFiles()
    }

    fun loadDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val audioItemList = mutableListOf<AudioFileItem>()

            // 1. Escanear carpeta de descargas externas (MusicDownloads)
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.forEach { file ->
                    val sizeMb = String.format("%.2f MB", file.length().toDouble() / (1024 * 1024))
                    audioItemList.add(
                        AudioFileItem(
                            name = file.name,
                            path = file.absolutePath,
                            sizeMb = sizeMb
                        )
                    )
                }
            }

            // 2. Escanear carpeta interna de Beats Exportados (Beat Studio Pro)
            val exportDir = File(context.filesDir, "exports")
            if (exportDir.exists() && exportDir.isDirectory) {
                exportDir.listFiles { file ->
                    file.isFile && file.extension.equals("wav", ignoreCase = true)
                }?.forEach { file ->
                    val sizeMb = String.format("%.2f MB", file.length().toDouble() / (1024 * 1024))
                    audioItemList.add(
                        AudioFileItem(
                            name = file.name,
                            path = file.absolutePath,
                            sizeMb = sizeMb
                        )
                    )
                }
            }

            // Ordenar todos los archivos alfabéticamente o por fecha más reciente
            val sortedList = audioItemList.sortedBy { it.name }

            _allAudioList.clear()
            _allAudioList.addAll(sortedList)
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

    // --- FUNCIONES DE PREVISUALIZACIÓN ---
    fun previewAudioSegment(audio: AudioFileItem, startSeconds: Int, endSeconds: Int) {
        previewJob?.cancel()

        if (startSeconds >= endSeconds) {
            _errorMessage.value = "El segundo de inicio debe ser menor al segundo de fin."
            return
        }

        val startMs = startSeconds * 1000L
        val durationMs = (endSeconds - startSeconds) * 1000L

        try {
            playerManager.playFile(audio.path, audio.name)
            playerManager.seekTo(startMs)

            previewJob = viewModelScope.launch(Dispatchers.Main) {
                delay(durationMs)
                stopPreview()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Error al previsualizar: ${e.localizedMessage}"
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        try {
            playerManager.seekTo(0L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FUNCIÓN DE RECORTE CON LIMPIEZA DE ARCHIVOS TEMPORALES ANTE FALLOS ---
    fun trimAudio(
        audio: AudioFileItem, 
        startSeconds: Int, 
        endSeconds: Int, 
        customName: String = "", 
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isTrimming.value = true
            var dstFile: File? = null // Referencia para controlar el archivo de salida
            try {
                val srcFile = File(audio.path)
                if (!srcFile.exists() || startSeconds >= endSeconds) {
                    withContext(Dispatchers.Main) {
                        _isTrimming.value = false
                        _errorMessage.value = "El archivo no existe o los tiempos de recorte no son válidos."
                        onResult(false)
                    }
                    return@launch
                }
                val downloadDir = srcFile.parentFile ?: File(context.getExternalFilesDir(null), "MusicDownloads")
                
                // Determinamos el nombre del archivo de salida según lo ingresado por el usuario
                val trimmedFileName = if (customName.isNotBlank()) {
                    val cleanName = customName.trim().removeSuffix(".mp3").removeSuffix(".m4a").removeSuffix(".webm").removeSuffix(".wav")
                    val extension = if (audio.path.endsWith(".webm", ignoreCase = true)) "webm" else if (audio.path.endsWith(".wav", ignoreCase = true)) "wav" else "m4a"
                    "$cleanName.$extension"
                } else {
                    val baseName = srcFile.nameWithoutExtension
                    val extension = if (audio.path.endsWith(".webm", ignoreCase = true)) "webm" else if (audio.path.endsWith(".wav", ignoreCase = true)) "wav" else "m4a"
                    "${baseName}_recortado.$extension"
                }

                dstFile = File(downloadDir, trimmedFileName)

                AudioTrimmer.trim(
                    srcPath = audio.path,
                    dstPath = dstFile.absolutePath,
                    startMs = startSeconds * 1000L,
                    endMs = endSeconds * 1000L
                )
                
                withContext(Dispatchers.Main) {
                    _isTrimming.value = false
                    loadDownloadedFiles()
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                
                // --- LIMPIEZA AUTOMÁTICA DE ARCHIVOS PARCIALES SI OCURRE UN ERROR ---
                if (dstFile != null && dstFile.exists()) {
                    try {
                        dstFile.delete()
                    } catch (deleteEx: Exception) {
                        deleteEx.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    _isTrimming.value = false
                    _errorMessage.value = "Detalle: ${e.localizedMessage ?: e.toString()}"
                    onResult(false)
                }
            }
        }
    }
}
