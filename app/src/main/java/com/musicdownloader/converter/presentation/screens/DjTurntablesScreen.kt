package com.musicdownloader.converter.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.musicdownloader.converter.presentation.viewmodel.DjViewModel
import com.musicdownloader.converter.presentation.viewmodel.DjTrack

@Composable
fun DjTurntablesScreen(viewModel: DjViewModel) {
    val context = LocalContext.current
    val isPlayingA by viewModel.isPlayingDeckA.collectAsState()
    val isPlayingB by viewModel.isPlayingDeckB.collectAsState()
    val trackA by viewModel.currentTrackA.collectAsState()
    val trackB by viewModel.currentTrackB.collectAsState()
    val libraryTracks by viewModel.libraryTracks.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    var crossfaderValue by remember { mutableStateOf(0.5f) }
    var selectingForDeck by remember { mutableStateOf<String?>(null) }
    var eqDialogDeck by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecording()
        }
    }

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
            // Fila superior: Título y Botón REC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSOLA DJ PRO - DUAL DECK",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.toggleRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRecording) Color.Red else Color(0xFF2B2B3D))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color.White else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "REC ON" else "REC",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Decks A y B
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
                        viewModel.loadLibraryTracks()
                        selectingForDeck = "A"
                    },
                    onEqClick = { eqDialogDeck = "A" }
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
                        viewModel.loadLibraryTracks()
                        selectingForDeck = "B"
                    },
                    onEqClick = { eqDialogDeck = "B" }
                )
            }

            // Control de Crossfader
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

        // Diálogo para seleccionar la pista de audio
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

        // Diálogo unificado con Hot Cues, Tempo y Ecualizador
        if (eqDialogDeck != null) {
            DeckControlDialog(
                deckName = eqDialogDeck!!,
                onDismiss = { eqDialogDeck = null },
                onApplyEq = { low, mid, high ->
                    if (eqDialogDeck == "A") viewModel.setEqualizerA(low, mid, high)
                    else viewModel.setEqualizerB(low, mid, high)
                },
                onApplyTempo = { speed ->
                    if (eqDialogDeck == "A") viewModel.setTempoDeckA(speed)
                    else viewModel.setTempoDeckB(speed)
                },
                onSetCue = { cueIndex ->
                    if (eqDialogDeck == "A") viewModel.setHotCueA(cueIndex)
                    else viewModel.setHotCueB(cueIndex)
                },
                onJumpCue = { cueIndex ->
                    if (eqDialogDeck == "A") viewModel.jumpHotCueA(cueIndex)
                    else viewModel.jumpHotCueB(cueIndex)
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
    onTrackClick: () -> Unit,
    onEqClick: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEqClick, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = "FX & Cues", tint = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                }
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

@Composable
fun DeckControlDialog(
    deckName: String,
    onDismiss: () -> Unit,
    onApplyEq: (Float, Float, Float) -> Unit,
    onApplyTempo: (Float) -> Unit,
    onSetCue: (Int) -> Unit,
    onJumpCue: (Int) -> Unit
) {
    var low by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var high by remember { mutableFloatStateOf(0f) }
    var tempo by remember { mutableFloatStateOf(1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        title = {
            Text(text = "Controles Pro - Deck $deckName", color = Color(0xFFFFD700), fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 🎯 Puntos Hot Cue (Cue 1 y Cue 2)
                Text(text = "Puntos Hot Cue", color = Color(0xFFFFD700), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cue 1
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onSetCue(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set C1", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { onJumpCue(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jump C1", fontSize = 10.sp, color = Color.White)
                        }
                    }
                    // Cue 2
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onSetCue(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set C2", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { onJumpCue(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jump C2", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // Control de Velocidad / Tempo
                Column {
                    Text(text = "Velocidad (Tempo): ${String.format("%.2f", tempo)}x", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempo,
                        onValueChange = { 
                            tempo = it
                            onApplyTempo(tempo)
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // Ecualizador de 3 Bandas
                Column {
                    Text(text = "Graves (Low)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = low,
                        onValueChange = { low = it; onApplyEq(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Medios (Mid)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = mid,
                        onValueChange = { mid = it; onApplyEq(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Agudos (High)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = high,
                        onValueChange = { high = it; onApplyEq(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFFFFD700))
            }
        }
    )
}
