package com.musicdownloader.converter.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
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
    private var cracklePlayer: ExoPlayer? = null // Reproductor para el chisporroteo continuo
    private var scratchPlayer: ExoPlayer? = null // Reproductor exclusivo para el sonido de scratch
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // Estado para controlar el efecto visual de rasguño (scratch) en el vinilo
    private val _isScratching = MutableStateFlow(false)
    val isScratching: StateFlow<Boolean> = _isScratching

    private val _currentTrackTitle = MutableStateFlow<String?>("Ninguna canción seleccionada")
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _currentArtwork = MutableStateFlow<Bitmap?>(null)
    val currentArtwork: StateFlow<Bitmap?> = _currentArtwork

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressTracking()
                        startCrackle()
                    } else {
                        stopProgressTracking()
                        stopCrackle()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = duration.coerceAtLeast(0L)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let { item ->
                        val title = item.mediaId.ifEmpty { item.localConfiguration?.uri?.lastPathSegment ?: "Pista desconocida" }
                        _currentTrackTitle.value = title
                        val filePath = item.localConfiguration?.uri?.path
                        extractAlbumArt(filePath)
                    }
                }
            })
        }

        cracklePlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0.2f 
        }

        scratchPlayer = ExoPlayer.Builder(context).build().apply {
            volume = 0.8f
        }
    }

    private fun startCrackle() {
        try {
            if (cracklePlayer?.mediaItemCount == 0) {
                val uri = Uri.parse("android.resource://${context.packageName}/raw/crackle")
                val mediaItem = MediaItem.fromUri(uri)
                cracklePlayer?.setMediaItem(mediaItem)
                cracklePlayer?.prepare()
            }
            cracklePlayer?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopCrackle() {
        try {
            cracklePlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playScratchSound() {
        try {
            val uri = Uri.parse("android.resource://${context.packageName}/raw/scratch")
            val mediaItem = MediaItem.fromUri(uri)
            scratchPlayer?.setMediaItem(mediaItem)
            scratchPlayer?.prepare()
            scratchPlayer?.seekTo(0L)
            scratchPlayer?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SUPERPODERES DE DJ CON PITCH/SPEED MODULATION ---
    
    // 1. Al tocar el disco: Activamos el estado visual y disparamos el rasguño
    fun startScratch() {
        _isScratching.value = true
        playScratchSound()
    }

    // 2. Al mover el dedo: Movemos la canción al vuelo y modulamos velocidad/pitch dinámicamente
    fun scratchTo(positionMs: Long, dragDeltaX: Float) {
        exoPlayer?.let { player ->
            val targetPos = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(targetPos)
            _currentPosition.value = targetPos

            // Calculamos la velocidad en función del movimiento horizontal del dedo
            val speed = (1.0f + (dragDeltaX / 15f)).coerceIn(0.1f, 2.5f)
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    // 3. Al soltar el dedo: Desactivamos el estado visual, restauramos velocidad normal y reanudamos la música
    fun endScratch() {
        _isScratching.value = false
        scratchPlayer?.pause()
        scratchPlayer?.seekTo(0L)
        
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(1.0f)
            player.play()
        }
    }

    // ---------------------------

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

    private fun extractAlbumArt(filePath: String?) {
        if (filePath.isNullOrEmpty()) {
            _currentArtwork.value = null
            return
        }

        val retriever = MediaMetadataRetriever()
        try {
            val file = File(filePath)
            if (file.exists()) {
                retriever.setDataSource(file.absolutePath)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    _currentArtwork.value = bitmap
                    retriever.release()
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }

        try {
            val audioFile = File(filePath)
            val parentDir = audioFile.parentFile
            if (parentDir != null && parentDir.exists()) {
                val baseName = audioFile.nameWithoutExtension
                val matchingImage = parentDir.listFiles { f ->
                    f.isFile && (f.name.equals("$baseName.jpg", true) || f.name.equals("$baseName.png", true))
                }?.firstOrNull()

                if (matchingImage != null && matchingImage.exists()) {
                    val bitmap = BitmapFactory.decodeFile(matchingImage.absolutePath)
                    if (bitmap != null) {
                        _currentArtwork.value = bitmap
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _currentArtwork.value = null
    }

    fun loadAndPlayPlaylist() {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val audioFiles = downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", true) || file.extension.equals("wav", true))
                }?.sortedBy { it.name }

                if (!audioFiles.isNullOrEmpty()) {
                    exoPlayer?.let { player ->
                        player.stop()
                        player.clearMediaItems()
                        val mediaItems = audioFiles.map { MediaItem.Builder().setUri(it.absolutePath).setMediaId(it.name).build() }
                        player.setMediaItems(mediaItems, 0, 0L)
                        player.prepare()
                        player.play()
                        _currentTrackTitle.value = audioFiles.first().name
                        extractAlbumArt(audioFiles.first().absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playFile(path: String, title: String) {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.Builder().setUri(path).setMediaId(title).build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _currentTrackTitle.value = title
            _duration.value = player.duration.coerceAtLeast(0L)
            extractAlbumArt(path)
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause()
            else {
                if (player.mediaItemCount > 0) player.play()
                else loadAndPlayPlaylist()
            }
        }
    }

    fun skipToNext() {
        exoPlayer?.let { if (it.hasNextMediaItem()) { it.seekToNextMediaItem(); it.play() } }
    }

    fun skipToPrevious() {
        exoPlayer?.let { if (it.hasPreviousMediaItem()) { it.seekToPreviousMediaItem(); it.play() } }
    }

    fun toggleShuffle() {
        exoPlayer?.let {
            val newState = !it.shuffleModeEnabled
            it.shuffleModeEnabled = newState
            _isShuffleEnabled.value = newState
        }
    }

    fun cycleRepeatMode() {
        exoPlayer?.let { player ->
            val nextMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            player.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward() {
        exoPlayer?.let {
            val newPos = (it.currentPosition + 10000).coerceAtMost(it.duration)
            it.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    fun seekRewind() {
        exoPlayer?.let {
            val newPos = (it.currentPosition - 10000).coerceAtLeast(0L)
            it.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    fun release() {
        stopProgressTracking()
        exoPlayer?.release()
        exoPlayer = null
        cracklePlayer?.release()
        cracklePlayer = null
        scratchPlayer?.release()
        scratchPlayer = null
    }
}
