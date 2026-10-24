package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentArtwork by viewModel.currentArtwork.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Caja mágica del vinilo con gestos de DJ, modulación de pitch y brazo integrado
        Box(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // 1. Al apoyar el dedo: iniciamos el scratch visual y de audio
                            playerManager.startScratch()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 40L 
                            val currentPos = playerManager.currentPosition.value
                            val newPosition = currentPos + (dragAmount.x * sensitivity).toLong()
                            
                            // 2. Al mover el dedo: actualizamos la posición y modulamos velocidad/pitch con dragAmount.x
                            playerManager.scratchTo(newPosition, dragAmount.x)
                        },
                        onDragEnd = {
                            // 3. Al levantar el dedo: restauramos la velocidad normal y reanudamos
                            playerManager.endScratch()
                        },
                        onDragCancel = {
                            playerManager.endScratch()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // El disco de vinilo giratorio con efectos de scratch visuales
            VinylRecord(
                artwork = currentArtwork,
                isPlaying = isPlaying,
                isScratching = isScratching,
                size = 220.dp
            )

            // El brazo de la aguja (Tonearm) que reacciona al estado de reproducción
            VinylTonearm(
                isPlaying = isPlaying,
                size = 220.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = currentTitle ?: "Ninguna canción seleccionada",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (duration > 0) currentPosition.toFloat() else 0f,
                onValueChange = { newValue ->
                    playerManager.seekTo(newValue.toLong())
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Modo aleatorio",
                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                val (icon, tint) = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, MaterialTheme.colorScheme.primary)
                    Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, MaterialTheme.colorScheme.primary)
                    else -> Pair(Icons.Default.Repeat, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                Icon(imageVector = icon, contentDescription = "Modo repetición", tint = tint)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.skipToPrevious() }, modifier = Modifier.size(44.dp)) {
                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = { playerManager.seekRewind() }, modifier = Modifier.size(44.dp)) {
                Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Retroceder", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }

            FilledIconButton(
                onClick = { playerManager.togglePlayPause() },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pausa",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { playerManager.seekForward() }, modifier = Modifier.size(44.dp)) {
                Icon(imageVector = Icons.Default.FastForward, contentDescription = "Adelantar", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = { viewModel.skipToNext() }, modifier = Modifier.size(44.dp)) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
