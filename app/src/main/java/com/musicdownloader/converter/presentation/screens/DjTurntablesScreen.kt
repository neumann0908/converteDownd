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
                    track = trackA,
                    isPlaying = isPlayingA,
                    rotationAngle = if (isPlayingA) angleA else 0f,
                    onTogglePlay = { viewModel.toggleDeckA() },
                    onTrackClick = {
                        viewModel.loadLibraryTracks()
                        selectingForDeck = "A"
                    },
                    onEqClick = { eqDialogDeck = "A" },
                    showSyncButton = false
                )

                DeckCard(
                    modifier = Modifier.weight(1f),
                    title = "DECK B (Sync)",
                    statusColor = Color.Red,
                    track = trackB,
                    isPlaying = isPlayingB,
                    rotationAngle = if (isPlayingB) angleB else 0f,
                    onTogglePlay = { viewModel.toggleDeckB() },
                    onTrackClick = {
                        viewModel.loadLibraryTracks()
                        selectingForDeck = "B"
                    },
                    onEqClick = { eqDialogDeck = "B" },
                    showSyncButton = true,
                    onSyncClick = { viewModel.autoSyncDeckB() }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = track.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${track.bpm} BPM | ${track.musicalKey}",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.sp
                                        )
                                    }
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

        if (eqDialogDeck != null) {
            DeckControlDialog(
                deckName = eqDialogDeck!!,
                viewModel = viewModel,
                onDismiss = { eqDialogDeck = null }
            )
        }
    }
}

@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    title: String,
    statusColor: Color,
    track: DjTrack?,
    isPlaying: Boolean,
    rotationAngle: Float,
    onTogglePlay: () -> Unit,
    onTrackClick: () -> Unit,
    onEqClick: () -> Unit,
    showSyncButton: Boolean = false,
    onSyncClick: () -> Unit = {}
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
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = "FX & Stems", tint = Color(0xFFFFD700))
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
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track?.name ?: "Toca para cargar pista",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    if (track != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "BPM: ${track.bpm} | Key: ${track.musicalKey}",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            if (showSyncButton) {
                Button(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SYNC (AUTO)", fontSize = 10.sp, color = Color.Black)
                }
            }

            Box(
                modifier = Modifier.size(100.dp),
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
    viewModel: DjViewModel,
    onDismiss: () -> Unit
) {
    var low by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var high by remember { mutableFloatStateOf(0f) }
    var tempo by remember { mutableFloatStateOf(1.0f) }

    var vocalsMuted by remember { mutableStateOf(false) }
    var drumsMuted by remember { mutableStateOf(false) }
    var bassMuted by remember { mutableStateOf(false) }

    var echoActive by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("OFF") }

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
                // ⚡ FX Pad (Echo, Filter, Brake)
                Text(text = "Efectos en Vivo (FX Pad)", color = Color(0xFFFFD700), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { 
                            echoActive = !echoActive
                            if (deckName == "A") viewModel.toggleEchoA(echoActive)
                            else viewModel.toggleEchoB(echoActive)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (echoActive) Color(0xFF00E676) else Color(0xFF3B3B4F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (echoActive) "Echo ON" else "Echo", fontSize = 9.sp, color = if (echoActive) Color.Black else Color.White)
                    }

                    Button(
                        onClick = { 
                            filterMode = if (filterMode == "OFF") "LP" else if (filterMode == "LP") "HP" else "OFF"
                            if (deckName == "A") viewModel.toggleFilterA(filterMode)
                            else viewModel.toggleFilterB(filterMode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (filterMode != "OFF") Color(0xFFFF9800) else Color(0xFF3B3B4F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Filter: $filterMode", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            if (deckName == "A") viewModel.triggerBrakeA()
                            else viewModel.triggerBrakeB()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Brake", fontSize = 9.sp, color = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // ⚡ Bucles Automáticos (Auto-Loops)
                Text(text = "Bucles Automáticos (Auto-Loops)", color = Color(0xFFFFD700), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var activeLoopBeats by remember { mutableStateOf<Int?>(null) }
                    val currentBpm = if (deckName == "A") viewModel.currentTrackA.value?.bpm ?: 120 else viewModel.currentTrackB.value?.bpm ?: 120

                    listOf(1, 2, 4, 8).forEach { beats ->
                        val isActive = activeLoopBeats == beats
                        Button(
                            onClick = {
                                val newActiveState = if (isActive) null else beats
                                activeLoopBeats = newActiveState
                                val enabled = newActiveState != null
                                if (deckName == "A") {
                                    viewModel.toggleAutoLoopA(beats, currentBpm, enabled)
                                } else {
                                    viewModel.toggleAutoLoopB(beats, currentBpm, enabled)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF00E676) else Color(0xFF3B3B4F)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "${beats}B",
                                fontSize = 10.sp,
                                color = if (isActive) Color.Black else Color.White
                            )
                        }
                    }
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // 🎛️ Neural Mix / Stem Isolation
                Text(text = "Aislamiento de Stems (Neural Mix)", color = Color(0xFFFFD700), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { 
                            vocalsMuted = !vocalsMuted
                            if (deckName == "A") viewModel.toggleStemVocalsA(vocalsMuted)
                            else viewModel.toggleStemVocalsB(vocalsMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (vocalsMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (vocalsMuted) "Vocals OFF" else "Vocals", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            drumsMuted = !drumsMuted
                            if (deckName == "A") viewModel.toggleStemDrumsA(drumsMuted)
                            else viewModel.toggleStemDrumsB(drumsMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (drumsMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (drumsMuted) "Drums OFF" else "Drums", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            bassMuted = !bassMuted
                            if (deckName == "A") viewModel.toggleStemBassA(bassMuted)
                            else viewModel.toggleStemBassB(bassMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (bassMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (bassMuted) "Bass OFF" else "Bass", fontSize = 9.sp, color = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // Hot Cues
                Text(text = "Puntos Hot Cue", color = Color(0xFFFFD700), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { if (deckName == "A") viewModel.setHotCueA(1) else viewModel.setHotCueB(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set C1", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { if (deckName == "A") viewModel.jumpHotCueA(1) else viewModel.jumpHotCueB(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jump C1", fontSize = 10.sp, color = Color.White)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { if (deckName == "A") viewModel.setHotCueA(2) else viewModel.setHotCueB(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set C2", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { if (deckName == "A") viewModel.jumpHotCueA(2) else viewModel.jumpHotCueB(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jump C2", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // Tempo
                Column {
                    Text(text = "Velocidad (Tempo): ${String.format("%.2f", tempo)}x", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempo,
                        onValueChange = { 
                            tempo = it
                            if (deckName == "A") viewModel.setTempoDeckA(tempo)
                            else viewModel.setTempoDeckB(tempo)
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }

                Divider(color = Color.DarkGray, thickness = 1.dp)

                // EQ de 3 Bandas
                Column {
                    Text(text = "Graves (Low)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = low,
                        onValueChange = { low = it; if (deckName == "A") viewModel.setEqualizerA(low, mid, high) else viewModel.setEqualizerB(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Medios (Mid)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = mid,
                        onValueChange = { mid = it; if (deckName == "A") viewModel.setEqualizerA(low, mid, high) else viewModel.setEqualizerB(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Agudos (High)", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = high,
                        onValueChange = { high = it; if (deckName == "A") viewModel.setEqualizerA(low, mid, high) else viewModel.setEqualizerB(low, mid, high) },
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
