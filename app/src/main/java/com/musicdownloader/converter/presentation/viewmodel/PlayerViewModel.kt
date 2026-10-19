package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerManager: AudioPlayerManager
) : ViewModel()
