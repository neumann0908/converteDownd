package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConverterViewModel : ViewModel() {
    private val _selectedFormat = MutableStateFlow("MP3")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    fun setFormat(format: String) {
        _selectedFormat.value = format
    }
}
