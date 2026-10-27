package com.musicdownloader.converter.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.engine.DjAudioEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class DjTrack(
    val name: String,
    val path: String,
    val bpm: Int = 120,
    val musicalKey: String = "4A"
)

@HiltViewModel
class DjViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : AndroidViewModel(context as Application) {

    private val djAudioEngine = DjAudioEngine(context)

    private val _isPlayingDeckA = MutableStateFlow(false)
    val isPlayingDeckA: StateFlow<Boolean> = _isPlayingDeckA.asStateFlow()

    private val _isPlayingDeckB = MutableStateFlow(false)
    val isPlayingDeckB: StateFlow<Boolean> = _isPlayingDeckB.asStateFlow()

    private val _currentTrackA = MutableStateFlow<DjTrack?>(null)
    val currentTrackA: StateFlow<DjTrack?> = _currentTrackA.asStateFlow()

    private val _currentTrackB = MutableStateFlow<DjTrack?>(null)
    val currentTrackB: StateFlow<DjTrack?> = _currentTrackB.asStateFlow()

    private val _libraryTracks = MutableStateFlow<List<DjTrack>>(emptyList())
    val libraryTracks: StateFlow<List<DjTrack>> = _libraryTracks.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // --- ⚡ Modo Automix States ---
    private val _isAutomixActive = MutableStateFlow(false)
    val isAutomixActive: StateFlow<Boolean> = _isAutomixActive.asStateFlow()
    private var automixJob: Job? = null
    private var automixQueueIndex = 0

    private var currentSpeedA: Float = 1.0f
    private var currentSpeedB: Float = 1.0f

    private var cuePointA1: Int? = null
    private var cuePointA2: Int? = null
    private var cuePointB1: Int? = null
    private var cuePointB2: Int? = null

    init {
        loadLibraryTracks()
    }

    fun loadLibraryTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
            val tracks = if (downloadDir.exists() && downloadDir.isDirectory) {
                downloadDir.listFiles { file ->
                    file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                            file.extension.equals("m4a", ignoreCase = true) ||
                            file.extension.equals("webm", ignoreCase = true) ||
                            file.extension.equals("wav", ignoreCase = true))
                }?.map { 
                    val randomBpm = 118 + (it.name.hashCode() % 15).let { if (it < 0) -it else it }
                    val keys = listOf("1A", "2A", "3B", "4A", "5B", "8A", "9B", "11A")
                    val randomKey = keys[(it.name.hashCode().let { if (it < 0) -it else it }) % keys.size]
                    
                    DjTrack(name = it.name, path = it.absolutePath, bpm = randomBpm, musicalKey = randomKey)
                }?.sortedBy { it.name } ?: emptyList()
            } else {
                emptyList()
            }
            _libraryTracks.value = tracks
        }
    }

    fun setTrackForDeckA(track: DjTrack) {
        _currentTrackA.value = track
        djAudioEngine.loadTrackA(track.path)
        _isPlayingDeckA.value = false
        currentSpeedA = 1.0f
        cuePointA1 = null
        cuePointA2 = null
    }

    fun setTrackForDeckB(track: DjTrack) {
        _currentTrackB.value = track
        djAudioEngine.loadTrackB(track.path)
        _isPlayingDeckB.value = false
        currentSpeedB = 1.0f
        cuePointB1 = null
        cuePointB2 = null
    }

    fun toggleDeckA() {
        val trackPath = _currentTrackA.value?.path ?: return
        val isPlaying = djAudioEngine.togglePlayA(trackPath)
        _isPlayingDeckA.value = isPlaying
    }

    fun toggleDeckB() {
        val trackPath = _currentTrackB.value?.path ?: return
        val isPlaying = djAudioEngine.togglePlayB(trackPath)
        _isPlayingDeckB.value = isPlaying
    }

    fun updateCrossfader(value: Float) {
        djAudioEngine.updateCrossfader(value)
    }

    fun setEqualizerA(low: Float, mid: Float, high: Float) {
        djAudioEngine.setEqualizerA(low, mid, high)
    }

    fun setEqualizerB(low: Float, mid: Float, high: Float) {
        djAudioEngine.setEqualizerB(low, mid, high)
    }

    // --- 🎛️ Métodos de Stems (Neural Mix) ---
    fun toggleStemVocalsA(muted: Boolean) { djAudioEngine.toggleStemVocalsA(muted) }
    fun toggleStemDrumsA(muted: Boolean) { djAudioEngine.toggleStemDrumsA(muted) }
    fun toggleStemBassA(muted: Boolean) { djAudioEngine.toggleStemBassA(muted) }

    fun toggleStemVocalsB(muted: Boolean) { djAudioEngine.toggleStemVocalsB(muted) }
    fun toggleStemDrumsB(muted: Boolean) { djAudioEngine.toggleStemDrumsB(muted) }
    fun toggleStemBassB(muted: Boolean) { djAudioEngine.toggleStemBassB(muted) }

    // --- 🎛️ FX Pad Methods (Echo, Filter, Brake) ---
    fun toggleEchoA(enabled: Boolean) { djAudioEngine.toggleEchoA(enabled) }
    fun toggleFilterA(mode: String) { djAudioEngine.toggleFilterA(mode) }
    fun triggerBrakeA() {
        djAudioEngine.triggerBrakeA()
        _isPlayingDeckA.value = false
    }

    fun toggleEchoB(enabled: Boolean) { djAudioEngine.toggleEchoB(enabled) }
    fun toggleFilterB(mode: String) { djAudioEngine.toggleFilterB(mode) }
    fun triggerBrakeB() {
        djAudioEngine.triggerBrakeB()
        _isPlayingDeckB.value = false
    }

    // --- ⚡ Sampler Pad (Sintetizador de Alta Fidelidad con Modelado Físico y Saturación tanh) ---
    fun playSample(sampleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val durationMs = when (sampleName) {
                    "Air Horn" -> 350
                    "Scratch" -> 220
                    "Laser" -> 250
                    "Kick" -> 320
                    "Snare" -> 260
                    "Vocals" -> 400
                    "BASS DROP" -> 750
                    "Siren" -> 500
                    else -> 300
                }

                val numSamples = (sampleRate * durationMs) / 1000
                val sample = ByteArray(numSamples * 2)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val envelope = 1.0 - progress

                    var wave = when (sampleName) {
                        "Kick" -> {
                            val freq = 42.0 + 98.0 * kotlin.math.exp(-progress * 14.0)
                            val transient = if (i < (sampleRate * 0.006)) (Random.nextDouble() * 2.0 - 1.0) * kotlin.math.exp(-i / 40.0) else 0.0
                            val body = sin(2.0 * PI * freq * t)
                            (body * (envelope * envelope)) + (transient * 0.45)
                        }
                        "Snare" -> {
                            val tone1 = sin(2.0 * PI * 190.0 * t) * kotlin.math.exp(-progress * 12.0)
                            val tone2 = sin(2.0 * PI * 340.0 * t) * kotlin.math.exp(-progress * 18.0)
                            val noise = (Random.nextDouble() * 2.0 - 1.0)
                            val noiseEnv = kotlin.math.exp(-progress * 9.0)
                            ((tone1 + tone2) * 0.35 + noise * 0.65 * noiseEnv) * envelope
                        }
                        "BASS DROP" -> {
                            val freq = 26.0 + 74.0 * (1.0 - progress)
                            sin(2.0 * PI * freq * t) * (envelope * 1.3)
                        }
                        "Scratch" -> {
                            val freq = 220.0 + sin(t * 80.0) * 450.0 * (1.0 - progress)
                            sin(2.0 * PI * freq * t) * envelope
                        }
                        "Air Horn" -> {
                            (sin(2.0 * PI * 466.16 * t) + sin(2.0 * PI * 587.33 * t) + sin(2.0 * PI * 698.46 * t)) / 3.0 * envelope
                        }
                        "Laser" -> {
                            val freq = 1500.0 * (1.0 - progress * 1.3)
                            sin(2.0 * PI * freq * t) * envelope
                        }
                        "Siren" -> {
                            val freq = 420.0 + sin(t * 30.0) * 200.0
                            sin(2.0 * PI * freq * t) * envelope
                        }
                        else -> {
                            sin(2.0 * PI * 440.0 * t) * envelope
                        }
                    }

                    // Saturación analógica soft-clipping con tanh para calidez y pegada
                    wave = kotlin.math.tanh(wave * 1.6)

                    val valShort = (wave * 32767).toInt().toShort()
                    sample[2 * i] = (valShort.toInt() and 0x00ff).toByte()
                    sample[2 * i + 1] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sample.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(sample, 0, sample.size)
                audioTrack.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- ⚡ Auto-Loops Methods ---
    fun toggleAutoLoopA(beats: Int, bpm: Int, active: Boolean) {
        if (active) djAudioEngine.startAutoLoopA(beats, bpm)
        else djAudioEngine.stopAutoLoopA()
    }

    fun toggleAutoLoopB(beats: Int, bpm: Int, active: Boolean) {
        if (active) djAudioEngine.startAutoLoopB(beats, bpm)
        else djAudioEngine.stopAutoLoopB()
    }

    // --- ⚡ Modo Automix (Mezcla Automática Inteligente) ---
    fun toggleAutomix() {
        val newState = !_isAutomixActive.value
        _isAutomixActive.value = newState
        if (newState) {
            loadLibraryTracks()
            startAutomixLoop()
        } else {
            stopAutomixLoop()
        }
    }

    private fun startAutomixLoop() {
        automixJob?.cancel()
        automixJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500L)
            var tracks = _libraryTracks.value
            
            if (tracks.isEmpty()) {
                val downloadDir = File(context.getExternalFilesDir(null), "MusicDownloads")
                if (downloadDir.exists() && downloadDir.isDirectory) {
                    tracks = downloadDir.listFiles { file ->
                        file.isFile && (file.extension.equals("mp3", ignoreCase = true) ||
                                file.extension.equals("m4a", ignoreCase = true) ||
                                file.extension.equals("webm", ignoreCase = true) ||
                                file.extension.equals("wav", ignoreCase = true))
                    }?.map { 
                        DjTrack(name = it.name, path = it.absolutePath, bpm = 120, musicalKey = "4A")
                    } ?: emptyList()
                    _libraryTracks.value = tracks
                }
            }

            if (tracks.isEmpty()) {
                _isAutomixActive.value = false
                return@launch
            }

            if (!_isPlayingDeckA.value && !_isPlayingDeckB.value) {
                automixQueueIndex = 0
                setTrackForDeckA(tracks[automixQueueIndex % tracks.size])
                toggleDeckA()
                automixQueueIndex++
            }

            while (_isAutomixActive.value) {
                delay(12000L)
                val currentTracks = _libraryTracks.value.ifEmpty { tracks }
                if (currentTracks.isEmpty()) continue

                if (_isPlayingDeckA.value && !_isPlayingDeckB.value) {
                    val nextTrack = currentTracks[automixQueueIndex % currentTracks.size]
                    automixQueueIndex++
                    
                    setTrackForDeckB(nextTrack)
                    toggleDeckB()

                    for (i in 1..10) {
                        val progress = i / 10f
                        updateCrossfader(progress)
                        delay(250L)
                    }
                    if (_isPlayingDeckA.value) toggleDeckA()
                } else if (_isPlayingDeckB.value && !_isPlayingDeckA.value) {
                    val nextTrack = currentTracks[automixQueueIndex % currentTracks.size]
                    automixQueueIndex++

                    setTrackForDeckA(nextTrack)
                    toggleDeckA()

                    for (i in 1..10) {
                        val progress = 1.0f - (i / 10f)
                        updateCrossfader(progress)
                        delay(250L)
                    }
                    if (_isPlayingDeckB.value) toggleDeckB()
                }
            }
        }
    }

    private fun stopAutomixLoop() {
        automixJob?.cancel()
        automixJob = null
    }

    fun setTempoDeckA(speed: Float) {
        currentSpeedA = speed
        djAudioEngine.setTempoDeckA(speed)
    }

    fun setTempoDeckB(speed: Float) {
        currentSpeedB = speed
        djAudioEngine.setTempoDeckB(speed)
    }

    fun autoSyncDeckB() {
        currentSpeedB = currentSpeedA
        djAudioEngine.setTempoDeckB(currentSpeedA)
        val masterPosition = djAudioEngine.getCurrentPositionA()
        djAudioEngine.seekToDeckB(masterPosition)
    }

    fun setHotCueA(cueIndex: Int) {
        val pos = djAudioEngine.getCurrentPositionA()
        if (cueIndex == 1) cuePointA1 = pos else if (cueIndex == 2) cuePointA2 = pos
    }

    fun jumpHotCueA(cueIndex: Int) {
        val pos = if (cueIndex == 1) cuePointA1 else cuePointA2
        pos?.let { djAudioEngine.seekToDeckA(it) }
    }

    fun setHotCueB(cueIndex: Int) {
        val pos = djAudioEngine.getCurrentPositionB()
        if (cueIndex == 1) cuePointB1 = pos else if (cueIndex == 2) cuePointB2 = pos
    }

    fun jumpHotCueB(cueIndex: Int) {
        val pos = if (cueIndex == 1) cuePointB1 else cuePointB2
        pos?.let { djAudioEngine.seekToDeckB(it) }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            djAudioEngine.stopRecording()
            _isRecording.value = false
            loadLibraryTracks()
        } else {
            val started = djAudioEngine.startRecording()
            _isRecording.value = started
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutomixLoop()
        djAudioEngine.release()
    }
}
