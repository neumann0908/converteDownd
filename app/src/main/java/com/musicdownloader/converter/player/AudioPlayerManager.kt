package com.musicdownloader.converter.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackTitle = MutableStateFlow<String?>("Ninguna canción seleccionada")
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }
            })
        }
    }

    fun playFile(path: String, title: String) {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(path)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _currentTrackTitle.value = title
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else if (player.mediaItemCount > 0) {
                player.play()
            }
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
