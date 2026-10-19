package com.musicdownloader.converter.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
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
            } else {
                if (player.mediaItemCount > 0) {
                    player.play()
                } else {
                    // Si no hay canción cargada, buscar la primera disponible en la carpeta de descargas
                    try {
                        val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
                        if (downloadDir.exists() && downloadDir.isDirectory) {
                            val firstFile = downloadDir.listFiles { file ->
                                file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                                        file.extension.equals("m4a", ignoreCase = true) ||
                                        file.extension.equals("webm", ignoreCase = true) ||
                                        file.extension.equals("wav", ignoreCase = true))
                            }?.sortedBy { it.name }?.firstOrNull()

                            if (firstFile != null) {
                                playFile(firstFile.absolutePath, firstFile.name)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
