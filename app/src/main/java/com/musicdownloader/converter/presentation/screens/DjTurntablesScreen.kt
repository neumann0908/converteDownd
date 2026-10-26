package com.musicdownloader.converter.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdownloader.converter.presentation.viewmodel.DjViewModel
import com.musicdownloader.converter.presentation.viewmodel.DjTrack

@Composable
fun DjTurntablesScreen(viewModel: DjViewModel) {
    val isPlayingA by viewModel.isPlayingDeckA.collectAsState()
    val isPlayingB by viewModel.isPlayingDeckB.collectAsState()
    val trackA by viewModel.currentTrackA.collectAsState()
    val trackB by viewModel.currentTrackB.collectAsState()
    val libraryTracks by viewModel.libraryTracks.collectAsState()

    var crossfaderValue by remember { mutableStateOf(0.5f) }
    var selectingForDeck by remember { mutableStateOf<String?>(null) }

    val infiniteTransitionA = rememberInfiniteTransition(label = "deckA")
    val angleA by infiniteTransitionA.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotationA"
    )

    val infiniteTransitionB = rememberInfiniteTransition(label = "deckB")
    val angleB by infiniteTransitionB.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotationB"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CONSOLA DJ PRO - DUAL DECK",
                color = Color(0xFFFFD700),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeckCard(
                    modifier = Modifier.weight(1f),
                    title = "DECK A (Master)",
                    statusColor = Color.Green,
                    trackName = trackA?.name ?: "Toca para cargar pista",
                    isPlaying = isPlayingA,
                    rotationAngle = if (isPlayingA) angleA else 0f,
                    onTogglePlay = { viewModel.toggleDeckA() },
                    onTrackClick = {
                        viewModel.loadLibraryTracks() // 👈 Refresca al instante al hacer clic
                        selectingForDeck = "A"
                    }
                )

                DeckCard(
                    modifier = Modifier.weight(1f),
                    title = "DECK B (Scratch)",
                    statusColor = Color.Red,
                    trackName = trackB?.name ?: "Toca para cargar pista",
                    isPlaying = isPlayingB,
                    rotationAngle = if (isPlayingB) angleB else 0f,
                    onTogglePlay = { viewModel.toggleDeckB() },
                    onTrackClick = {
                        viewModel.loadLibraryTracks() // 👈 Refresca al instante al hacer clic
                        selectingForDeck = "B"
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CROSSFADER", color = Color.White, fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = crossfaderValue,
                        onValueChange = { 
                            crossfaderValue = it
                            viewModel.updateCrossfader(it)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        if (selectingForDeck != null) {
            AlertDialog(
                onDismissRequest = { selectingForDeck = null },
                containerColor = Color(0xFF1E1E2C),
                title = {
                    Text(text = "Seleccionar Pista - Deck $selectingForDeck", color = Color(0xFFFFD700), fontSize = 14.sp)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (libraryTracks.isEmpty()) {
                            Text(text = "No hay canciones descargadas", color = Color.LightGray, fontSize = 12.sp)
                        } else {
                            libraryTracks.forEach { track ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectingForDeck == "A") {
                                                viewModel.setTrackForDeckA(track)
                                            } else {
                                                viewModel.setTrackForDeckB(track)
                                            }
                                            selectingForDeck = null
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = track.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectingForDeck = null }) {
                        Text("Cerrar", color = Color(0xFFFFD700))
                    }
                }
            )
        }
    }
}

@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    title: String,
    statusColor: Color,
    trackName: String,
    isPlaying: Boolean,
    rotationAngle: Float,
    onTogglePlay: () -> Unit,
    onTrackClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.LightGray, fontSize = 11.sp)
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B2B3D))
                    .clickable { onTrackClick() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = trackName, color = Color(0xFFFFD700), fontSize = 10.sp, maxLines = 1)
            }

            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotationAngle)) {
                    drawCircle(color = Color.Black)
                    drawCircle(color = Color(0xFF252525), radius = size.minDimension / 2.3f)
                    drawCircle(color = Color(0xFF151515), radius = size.minDimension / 3f)
                    drawCircle(color = Color(0xFFFFD700), radius = size.minDimension / 6f)
                    drawCircle(color = Color.Black, radius = size.minDimension / 18f)
                }
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF2B2B3D))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFFFFD700)
                )
            }
        }
    }
}
