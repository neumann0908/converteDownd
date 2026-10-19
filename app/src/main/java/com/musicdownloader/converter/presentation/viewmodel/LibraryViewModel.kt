package com.musicdownloader.converter.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _audioList = MutableStateFlow<List<AudioFileItem>>(emptyList())
    val audioList: StateFlow<List<AudioFileItem>> = _audioList.asStateFlow()

    init {
        loadDownloadedFiles()
    }

    fun loadDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val files = downloadDir.listFiles { file ->
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

                _audioList.value = files
            } else {
                _audioList.value = emptyList()
            }
        }
    }
}
