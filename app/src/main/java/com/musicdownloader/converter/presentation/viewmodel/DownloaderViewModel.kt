package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloaderViewModel : ViewModel() {
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun startDownload() {
        // Lógica de activación de descarga mediante motores y WorkManager
    }
}
