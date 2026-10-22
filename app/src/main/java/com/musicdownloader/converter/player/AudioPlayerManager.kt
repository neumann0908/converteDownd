package com.musicdownloader.converter.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackTitle = MutableStateFlow<String?>("Ninguna canción seleccionada")
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressTracking()
                    } else {
                        stopProgressTracking()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = duration.coerceAtLeast(0L)
                    }
                }

                // Detecta automáticamente el cambio de pista en la cola secuencial
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let { item ->
                        val title = item.mediaId.ifEmpty { item.localConfiguration?.uri?.lastPathSegment ?: "Pista desconocida" }
                        _currentTrackTitle.value = title
                    }
                }
            })
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    // Carga toda la lista de la carpeta de descargas como una playlist en ExoPlayer
    fun loadAndPlayPlaylist() {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val audioFiles = downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.sortedBy { it.name }

                if (!audioFiles.isNullOrEmpty()) {
                    exoPlayer?.let { player ->
                        player.stop()
                        player.clearMediaItems()

                        val mediaItems = audioFiles.map { file ->
                            MediaItem.Builder()
                                .setUri(file.absolutePath)
                                .setMediaId(file.name)
                                .build()
                        }

                        player.setMediaItems(mediaItems)
                        player.prepare()
                        player.play()
                        _currentTrackTitle.value = audioFiles.first().name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // CORREGIDO: Carga toda la playlist y se posiciona en el archivo que el usuario seleccionó
    fun playFile(path: String, title: String) {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val audioFiles = downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.sortedBy { it.name }

                if (!audioFiles.isNullOrEmpty()) {
                    exoPlayer?.let { player ->
                        player.stop()
                        player.clearMediaItems()

                        val mediaItems = audioFiles.map { file ->
                            MediaItem.Builder()
                                .setUri(file.absolutePath)
                                .setMediaId(file.name)
                                .build()
                        }

                        player.setMediaItems(mediaItems)
                        player.prepare()

                        // Buscar el índice exacto de la canción seleccionada en la lista
                        val targetIndex = audioFiles.indexOfFirst { it.absolutePath == path }
                        if (targetIndex != -1) {
                            player.seekTo(targetIndex, 0L)
                        }

                        player.play()
                        _currentTrackTitle.value = title
                    }
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback por si la carpeta no está disponible
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.Builder()
                .setUri(path)
                .setMediaId(title)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _currentTrackTitle.value = title
            _duration.value = player.duration.coerceAtLeast(0L)
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
                    loadAndPlayPlaylist()
                }
            }
        }
    }

    // Funciones para saltar entre pistas manualmente
    fun skipToNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.play()
            }
        }
    }

    fun skipToPrevious() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    // CORREGIDO: Se eliminó la duplicidad y quedó limpia una sola función
    fun seekForward() {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
            player.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    fun seekRewind() {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition - 10000).coerceAtLeast(0L)
            player.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    fun release() {
        stopProgressTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}
