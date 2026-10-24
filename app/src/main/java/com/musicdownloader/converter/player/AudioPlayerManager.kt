package com.musicdownloader.converter.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicdownloader.converter.R 
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
    private var cracklePlayer: ExoPlayer? = null 
    private var scratchPlayer: ExoPlayer? = null 
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var fadeJob: Job? = null 

    // --- EFECTOS DE AUDIO AVANZADOS (BASS BOOST & REVERB) ---
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null

    private val _isBassBoostEnabled = MutableStateFlow(false)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled

    // --- CARRUSEL ALEATORIO DE IMÁGENES DESDE DRAWABLE ---
    private var artSlideshowJob: Job? = null
    
    private val drawableArts = listOf(
        R.drawable.disco1,
        R.drawable.disco2,
        R.drawable.disco3
    )

    // --- TEMPORIZADOR DE APAGADO (SLEEP TIMER) ---
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining
    private var sleepTimerJob: Job? = null

    // --- VELOCIDAD DE REPRODUCCIÓN ---
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

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
                        startArtSlideshow() 
                        
                        // Aseguramos que los efectos capturen la sesión activa
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupAudioEffects(audioSessionId)
                        }
                    } else {
                        stopProgressTracking()
                        stopCrackle()
                        stopArtSlideshow()  
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = duration.coerceAtLeast(0L)
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            setupAudioEffects(audioSessionId)
                        }
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    super.onAudioSessionIdChanged(audioSessionId)
                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        setupAudioEffects(audioSessionId)
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

    // --- CONFIGURACIÓN DE EFECTOS DE AUDIO (BASS BOOST & REVERB) ---

    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            bassBoost?.release()
            presetReverb?.release()

            // 1. Configurar Bass Boost forzando potencia máxima soportada
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isBassBoostEnabled.value
                if (enabled) {
                    val maxStrength = if (strengthSupported) 1000.toShort() else 500.toShort()
                    setStrength(maxStrength)
                }
            }

            // 2. Configurar Reverb ambiental
            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = _isReverbEnabled.value
                if (enabled) {
                    preset = PresetReverb.PRESET_SMALLROOM
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBassBoost() {
        val newState = !_isBassBoostEnabled.value
        _isBassBoostEnabled.value = newState
        try {
            bassBoost?.let {
                it.enabled = newState
                if (newState) {
                    val maxStrength = if (it.strengthSupported) 1000.toShort() else 500.toShort()
                    it.setStrength(maxStrength)
                }
            } ?: run {
                exoPlayer?.audioSessionId?.let { sessionId ->
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        setupAudioEffects(sessionId)
                        bassBoost?.enabled = newState
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleReverb() {
        val newState = !_isReverbEnabled.value
        _isReverbEnabled.value = newState
        try {
            presetReverb?.let {
                it.enabled = newState
                if (newState) {
                    it.preset = PresetReverb.PRESET_SMALLROOM
                }
            } ?: run {
                exoPlayer?.audioSessionId?.let { sessionId ->
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        setupAudioEffects(sessionId)
                        presetReverb?.enabled = newState
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------------

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

    private fun startArtSlideshow() {
        if (drawableArts.isEmpty()) return
        
        artSlideshowJob?.cancel()
        artSlideshowJob = scope.launch {
            while (true) {
                delay(7000L)
                if (drawableArts.isNotEmpty()) {
                    val randomResId = drawableArts.random()
                    try {
                        val bitmap = BitmapFactory.decodeResource(context.resources, randomResId)
                        if (bitmap != null) {
                            _currentArtwork.value = bitmap
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun stopArtSlideshow() {
        artSlideshowJob?.cancel()
        artSlideshowJob = null
    }

    fun startSleepTimer(minutes: Long) {
        sleepTimerJob?.cancel()
        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemaining.value = totalMs

        sleepTimerJob = scope.launch {
            var currentMs = totalMs
            while (currentMs > 0) {
                delay(1000L)
                currentMs -= 1000L
                _sleepTimerRemaining.value = currentMs.coerceAtLeast(0L)
            }
            fadeOut {
                exoPlayer?.pause()
                _sleepTimerRemaining.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    private fun fadeIn(durationMs: Long = 400L) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            exoPlayer?.volume = 0f
            val steps = 8
            val stepDelay = durationMs / steps
            for (i in 1..steps) {
                delay(stepDelay)
                exoPlayer?.volume = i.toFloat() / steps
            }
            exoPlayer?.volume = 1f
        }
    }

    private fun fadeOut(durationMs: Long = 300L, onFinished: () -> Unit) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val currentVol = exoPlayer?.volume ?: 1f
            val steps = 6
            val stepDelay = durationMs / steps
            for (i in steps downTo 1) {
                delay(stepDelay)
                exoPlayer?.volume = currentVol * (i.toFloat() / steps)
            }
            exoPlayer?.volume = 0f
            onFinished()
            exoPlayer?.volume = 1f 
        }
    }
    
    fun startScratch() {
        _isScratching.value = true
        playScratchSound()
    }

    fun scratchTo(positionMs: Long, dragDeltaX: Float) {
        exoPlayer?.let { player ->
            val targetPos = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(targetPos)
            _currentPosition.value = targetPos

            val speed = (1.0f + (dragDeltaX / 15f)).coerceIn(0.1f, 2.5f)
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    fun endScratch() {
        _isScratching.value = false
        scratchPlayer?.pause()
        scratchPlayer?.seekTo(0L)
        
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(_playbackSpeed.value)
            player.play()
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
                    playFile(audioFiles.first().absolutePath, audioFiles.first().name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playFile(path: String, title: String) {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            val audioFiles = if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", true) || file.extension.equals("wav", true))
                }?.sortedBy { it.name } ?: emptyList()
            } else {
                emptyList()
            }

            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()

                if (audioFiles.isNotEmpty()) {
                    val mediaItems = audioFiles.map { 
                        MediaItem.Builder()
                            .setUri(it.absolutePath)
                            .setMediaId(it.name)
                            .build() 
                    }
                    val targetIndex = audioFiles.indexOfFirst { it.absolutePath == path }.coerceAtLeast(0)
                    
                    player.setMediaItems(mediaItems, targetIndex, 0L)
                    player.prepare()
                    player.playbackParameters = PlaybackParameters(_playbackSpeed.value)
                    player.play()
                    fadeIn()
                    _currentTrackTitle.value = audioFiles[targetIndex].name
                    extractAlbumArt(audioFiles[targetIndex].absolutePath)
                } else {
                    val mediaItem = MediaItem.Builder().setUri(path).setMediaId(title).build()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playbackParameters = PlaybackParameters(_playbackSpeed.value)
                    player.play()
                    fadeIn()
                    _currentTrackTitle.value = title
                    _duration.value = player.duration.coerceAtLeast(0L)
                    extractAlbumArt(path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                fadeOut {
                    player.pause()
                }
            } else {
                if (player.mediaItemCount > 0) {
                    player.play()
                    fadeIn()
                } else {
                    loadAndPlayPlaylist()
                }
            }
        }
    }

    fun skipToNext() {
        exoPlayer?.let { 
            if (it.hasNextMediaItem()) { 
                it.seekToNextMediaItem() 
                it.play() 
                fadeIn() 
            } 
        }
    }

    fun skipToPrevious() {
        exoPlayer?.let { 
            if (it.hasPreviousMediaItem()) { 
                it.seekToPreviousMediaItem() 
                it.play() 
                fadeIn() 
            } 
        }
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
        cancelSleepTimer()
        stopArtSlideshow()
        
        bassBoost?.release()
        bassBoost = null
        presetReverb?.release()
        presetReverb = null

        exoPlayer?.release()
        exoPlayer = null
        cracklePlayer?.release()
        cracklePlayer = null
        scratchPlayer?.release()
        scratchPlayer = null
    }
}
