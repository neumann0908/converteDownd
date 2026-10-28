package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import com.musicdownloader.converter.presentation.viewmodel.PlayerViewModel
import java.util.Locale

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerManager = viewModel.playerManager
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isScratching by playerManager.isScratching.collectAsState()
    val currentTitle by playerManager.currentTrackTitle.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val sleepTimerRemaining by playerManager.sleepTimerRemaining.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    
    // Estados de los efectos de audio avanzados
    val isBassBoostEnabled by playerManager.isBassBoostEnabled.collectAsState()
    val isReverbEnabled by playerManager.isReverbEnabled.collectAsState()
    
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentArtwork by viewModel.currentArtwork.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Caja mágica del vinilo Picture Disc con gestos de DJ y brazo integrado
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                playerManager.startScratch()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val sensitivity = 40L 
                                val currentPos = playerManager.currentPosition.value
                                val newPosition = currentPos + (dragAmount.x * sensitivity).toLong()
                                
                                playerManager.scratchTo(newPosition, dragAmount.x)
                            },
                            onDragEnd = {
                                playerManager.endScratch()
                            },
                            onDragCancel = {
                                playerManager.endScratch()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                VinylRecord(
                    artwork = currentArtwork,
                    isPlaying = isPlaying,
                    isScratching = isScratching,
                    size = 220.dp
                )

                VinylTonearm(
                    isPlaying = isPlaying,
                    size = 220.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- VÚMETRO ANALÓGICO ESTÉREO EN VIVO ---
            AnalogVuMeter(isPlaying = isPlaying)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentTitle ?: "Ninguna canción seleccionada",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Indicador visual del tiempo restante del Temporizador de Apagado
            if (sleepTimerRemaining != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apagado automático en: ${formatTime(sleepTimerRemaining!!)}",
                    fontSize = 13.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { newValue ->
                        playerManager.seekTo(newValue.toLong())
                    },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD700),
                        activeTrackColor = Color(0xFFFFD700),
                        inactiveTrackColor = Color(0xFF3B3B4F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = formatTime(duration),
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de controles secundarios (Shuffle, Velocidad, Ecualizador, Bass Boost, Reverb, Sleep Timer, Repeat)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Modo aleatorio",
                        tint = if (isShuffleEnabled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                // Botón de Velocidad de Reproducción
                IconButton(onClick = { showSpeedDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Velocidad de reproducción",
                        tint = if (playbackSpeed != 1.0f) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                // Botón para abrir el Ecualizador Gráfico
                IconButton(onClick = { showEqualizerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Ecualizador Gráfico",
                        tint = Color(0xFFFFD700)
                    )
                }

                // Botón de Bass Boost (Refuerzo de graves)
                IconButton(onClick = { playerManager.toggleBassBoost() }) {
                    Icon(
                        imageVector = Icons.Default.SurroundSound,
                        contentDescription = "Refuerzo de graves (Bass Boost)",
                        tint = if (isBassBoostEnabled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                // Botón de Reverb (Acústica ambiental)
                IconButton(onClick = { playerManager.toggleReverb() }) {
                    Icon(
                        imageVector = Icons.Default.Speaker,
                        contentDescription = "Efecto Reverb / Acústica",
                        tint = if (isReverbEnabled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                // Botón del Temporizador de Apagado
                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = "Temporizador de apagado",
                        tint = if (sleepTimerRemaining != null) Color(0xFFFF9800) else Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    val (icon, tint) = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, Color(0xFFFFD700))
                        Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, Color(0xFFFFD700))
                        else -> Pair(Icons.Default.Repeat, Color.LightGray.copy(alpha = 0.5f))
                    }
                    Icon(imageVector = icon, contentDescription = "Modo repetición", tint = tint)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.skipToPrevious() }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(28.dp), tint = Color(0xFFFFD700))
                }

                IconButton(onClick = { playerManager.seekRewind() }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Retroceder", modifier = Modifier.size(24.dp), tint = Color(0xFFFFD700))
                }

                FilledIconButton(
                    onClick = { playerManager.togglePlayPause() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pausa",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { playerManager.seekForward() }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = "Adelantar", modifier = Modifier.size(24.dp), tint = Color(0xFFFFD700))
                }

                IconButton(onClick = { viewModel.skipToNext() }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(28.dp), tint = Color(0xFFFFD700))
                }
            }
        }
    }

    // --- VENTANA EMERGENTE (DIÁLOGO) DEL ECUALIZADOR GRÁFICO ---
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            containerColor = Color(0xFF1E1E2C),
            title = { Text("🎛️ Ecualizador Gráfico Pro", color = Color(0xFFFFD700)) },
            text = {
                val numBands = playerManager.getNumberOfBands()
                if (numBands > 0) {
                    val range = playerManager.getBandLevelRange()
                    val minLevel = range[0].toFloat()
                    val maxLevel = range[1].toFloat()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        for (i in 0 until numBands) {
                            val band = i.toShort()
                            val centerFreqHz = playerManager.getCenterFreq(band)
                            val freqLabel = if (centerFreqHz >= 1000000) {
                                "${centerFreqHz / 1000000} MHz"
                            } else if (centerFreqHz >= 1000) {
                                "${centerFreqHz / 1000} kHz"
                            } else {
                                "$centerFreqHz Hz"
                            }

                            var currentLevel by remember {
                                mutableStateOf(playerManager.getBandLevel(band).toFloat())
                            }

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = freqLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "${(currentLevel / 100).toInt()} dB", fontSize = 12.sp, color = Color(0xFFFFD700))
                                }
                                Slider(
                                    value = currentLevel,
                                    onValueChange = { newVal ->
                                        currentLevel = newVal
                                        playerManager.setBandLevel(band, newVal.toInt().toShort())
                                    },
                                    valueRange = minLevel..maxLevel,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700),
                                        inactiveTrackColor = Color(0xFF3B3B4F)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Text("Reproduce una música para activar el ecualizador.", color = Color.LightGray)
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) {
                    Text("¡Listo!", color = Color(0xFFFFD700))
                }
            }
        )
    }

    // Ventana emergente (Diálogo) para configurar el Temporizador de Apagado
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            containerColor = Color(0xFF1E1E2C),
            title = { Text("Temporizador de Apagado", color = Color(0xFFFFD700)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15L, 30L, 45L, 60L).forEach { mins ->
                        Button(
                            onClick = {
                                playerManager.startSleepTimer(mins)
                                showTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B3D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("$mins minutos", color = Color.White) }
                    }

                    if (sleepTimerRemaining != null) {
                        Button(
                            onClick = {
                                playerManager.cancelSleepTimer()
                                showTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Desactivar temporizador", color = Color.White) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Cerrar", color = Color(0xFFFFD700))
                }
            }
        )
    }

    // Ventana emergente (Diálogo) para configurar la Velocidad de Reproducción
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Color(0xFF1E1E2C),
            title = { Text("Velocidad de Reproducción", color = Color(0xFFFFD700)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        Button(
                            onClick = {
                                playerManager.setPlaybackSpeed(speed)
                                showSpeedDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2B2B3D)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x"
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cerrar", color = Color(0xFFFFD700))
                }
            }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
