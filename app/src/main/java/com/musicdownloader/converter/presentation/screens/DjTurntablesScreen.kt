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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val isAutomix by viewModel.isAutomixActive.collectAsState()
    
    // Estados Headphone Cue (Punto 12)
    val isSplitAudio by viewModel.isSplitAudioActive.collectAsState()
    val cueA by viewModel.cueDeckA.collectAsState()
    val cueB by viewModel.cueDeckB.collectAsState()

    var crossfaderValue by remember { mutableStateOf(0.5f) }
    var selectingForDeck by remember { mutableStateOf<String?>(null) }
    var eqDialogDeck by remember { mutableStateOf<String?>(null) }
    var showSamplerDialog by remember { mutableStateOf(false) }

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
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🎛️ Cabecera Pro Rediseñada en Dos Filas Simétricas
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSOLA DJ PRO",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 13.sp
                    )
                }

                // Fila de Botones Superiores Distribuidos Uniformemente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.toggleSplitAudio() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSplitAudio) Color(0xFFFF9800) else Color(0xFF3B3B4F)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text(
                            text = if (isSplitAudio) "SPLIT ON" else "CUE CPL",
                            fontSize = 9.sp,
                            color = if (isSplitAudio) Color.Black else Color.White
                        )
                    }

                    Button(
                        onClick = { showSamplerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text(text = "PADS", fontSize = 10.sp, color = Color(0xFFFFD700))
                    }

                    Button(
                        onClick = { viewModel.toggleAutomix() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAutomix) Color(0xFF00E676) else Color(0xFF3B3B4F)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text(
                            text = if (isAutomix) "AUTO ON" else "AUTOMIX",
                            fontSize = 9.sp,
                            color = if (isAutomix) Color.Black else Color.White
                        )
                    }

                    Button(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                viewModel.toggleRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else Color(0xFF2B2B3D)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.White else Color.Red)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "REC", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Decks A y B con Alineación Absoluta y Simétrica
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeckCard(
                    modifier = Modifier.weight(1f),
                    title = "DECK A",
                    subtitle = "(Master)",
                    statusColor = Color.Green,
                    track = trackA,
                    isPlaying = isPlayingA,
                    rotationAngle = if (isPlayingA) angleA else 0f,
                    isCueActive = cueA,
                    onCueClick = { viewModel.toggleCueDeckA() },
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
                    title = "DECK B",
                    subtitle = "(Sync)",
                    statusColor = Color.Red,
                    track = trackB,
                    isPlaying = isPlayingB,
                    rotationAngle = if (isPlayingB) angleB else 0f,
                    isCueActive = cueB,
                    onCueClick = { viewModel.toggleCueDeckB() },
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

            // Crossfader Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CROSSFADER", color = Color.White, fontSize = 12.sp)
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    }
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
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = track.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${track.bpm}BPM | ${track.musicalKey}",
                                            color = Color(0xFFFFD700),
                                            fontSize = 10.sp
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

        if (showSamplerDialog) {
            SamplerPadDialog(
                viewModel = viewModel,
                onDismiss = { showSamplerDialog = false }
            )
        }
    }
}

@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    statusColor: Color,
    track: DjTrack?,
    isPlaying: Boolean,
    rotationAngle: Float,
    isCueActive: Boolean,
    onCueClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onTrackClick: () -> Unit,
    onEqClick: () -> Unit,
    showSyncButton: Boolean = false,
    onSyncClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🎛️ Cabecera de Deck Alineada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, color = Color.White, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                    Text(text = subtitle, color = Color.LightGray, fontSize = 8.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Button(
                        onClick = onCueClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCueActive) Color(0xFFFF9800) else Color(0xFF3B3B4F)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(22.dp).width(36.dp)
                    ) {
                        Text(text = "CUE", fontSize = 8.sp, color = if (isCueActive) Color.Black else Color.White)
                    }

                    IconButton(onClick = onEqClick, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    }

                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                }
            }

            // Selector de Pista Estándar y Centrado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2B2B3D))
                    .clickable { onTrackClick() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track?.name ?: "Cargar pista",
                        color = Color(0xFFFFD700),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    if (track != null) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "${track.bpm}BPM | ${track.musicalKey}",
                            color = Color.LightGray,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // 🎯 BLOQUE DE SYNC / ESPACIADOR SIMÉTRICO ESTÁNDAR
            if (showSyncButton) {
                Button(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SYNC (AUTO)", fontSize = 9.sp, color = Color.Black)
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 🎶 Vinilo Centrado y Proporcional (110.dp)
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

            // Botón Play/Pause Centrado
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2B3D))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SamplerPadDialog(
    viewModel: DjViewModel,
    onDismiss: () -> Unit
) {
    val samples = listOf(
        "Air Horn" to Color(0xFFFF9800),
        "Scratch" to Color(0xFF00E676),
        "Laser" to Color(0xFF00B0FF),
        "Kick" to Color(0xFFE53935),
        "Snare" to Color(0xFF9C27B0),
        "Vocals" to Color(0xFFFFD700),
        "BASS DROP" to Color(0xFFE91E63),
        "Siren" to Color(0xFF3F51B5)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        title = {
            Text(text = "Banco de Samples", color = Color(0xFFFFD700), fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                samples.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (name, color) ->
                            Button(
                                onClick = { viewModel.playSample(name) },
                                colors = ButtonDefaults.buttonColors(containerColor = color),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
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
    var keyShift by remember { mutableFloatStateOf(0f) }
    var masterTempoActive by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ⚡ Key Shift & Master Tempo (Punto 13)
                Text(text = "Tonalidad (Key Shift & Master Tempo)", color = Color(0xFFFFD700), fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Key Shift: ${if (keyShift > 0) "+$keyShift" else keyShift.toInt()} semitonos",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Button(
                        onClick = {
                            masterTempoActive = !masterTempoActive
                            if (deckName == "A") viewModel.toggleMasterTempoA(masterTempoActive)
                            else viewModel.toggleMasterTempoB(masterTempoActive)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (masterTempoActive) Color(0xFF00E676) else Color(0xFF3B3B4F)
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (masterTempoActive) "PITCH LOCK ON" else "PITCH LOCK",
                            fontSize = 9.sp,
                            color = if (masterTempoActive) Color.Black else Color.White
                        )
                    }
                }
                Slider(
                    value = keyShift,
                    onValueChange = { 
                        keyShift = it
                        if (deckName == "A") viewModel.setKeyShiftA(keyShift)
                        else viewModel.setKeyShiftB(keyShift)
                    },
                    valueRange = -6f..6f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                )

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                // Efectos FX
                Text(text = "Efectos en Vivo (FX Pad)", color = Color(0xFFFFD700), fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { 
                            echoActive = !echoActive
                            if (deckName == "A") viewModel.toggleEchoA(echoActive)
                            else viewModel.toggleEchoB(echoActive)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (echoActive) Color(0xFF00E676) else Color(0xFF3B3B4F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text("Filter: $filterMode", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            if (deckName == "A") viewModel.triggerBrakeA()
                            else viewModel.triggerBrakeB()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text("Brake", fontSize = 9.sp, color = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                // Auto-Loops
                Text(text = "Bucles Automáticos (Auto-Loops)", color = Color(0xFFFFD700), fontSize = 11.sp)
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
                                activeLoopBeats = if (isActive) null else beats
                                val enabled = activeLoopBeats != null
                                if (deckName == "A") viewModel.toggleAutoLoopA(beats, currentBpm, enabled)
                                else viewModel.toggleAutoLoopB(beats, currentBpm, enabled)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF00E676) else Color(0xFF3B3B4F)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("${beats}B", fontSize = 9.sp, color = if (isActive) Color.Black else Color.White)
                        }
                    }
                }

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                // Stems
                Text(text = "Aislamiento de Stems (Neural Mix)", color = Color(0xFFFFD700), fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { 
                            vocalsMuted = !vocalsMuted
                            if (deckName == "A") viewModel.toggleStemVocalsA(vocalsMuted)
                            else viewModel.toggleStemVocalsB(vocalsMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (vocalsMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text(if (vocalsMuted) "Vocals OFF" else "Vocals", fontSize = 8.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            drumsMuted = !drumsMuted
                            if (deckName == "A") viewModel.toggleStemDrumsA(drumsMuted)
                            else viewModel.toggleStemDrumsB(drumsMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (drumsMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text(if (drumsMuted) "Drums OFF" else "Drums", fontSize = 8.sp, color = Color.White)
                    }

                    Button(
                        onClick = { 
                            bassMuted = !bassMuted
                            if (deckName == "A") viewModel.toggleStemBassA(bassMuted)
                            else viewModel.toggleStemBassB(bassMuted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (bassMuted) Color.Red else Color(0xFF3B3B4F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text(if (bassMuted) "Bass OFF" else "Bass", fontSize = 8.sp, color = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                // Hot Cues
                Text(text = "Puntos Hot Cue", color = Color(0xFFFFD700), fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { if (deckName == "A") viewModel.setHotCueA(1) else viewModel.setHotCueB(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            Text("Set C1", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { if (deckName == "A") viewModel.jumpHotCueA(1) else viewModel.jumpHotCueB(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            Text("Jump C1", fontSize = 9.sp, color = Color.White)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { if (deckName == "A") viewModel.setHotCueA(2) else viewModel.setHotCueB(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            Text("Set C2", fontSize = 9.sp, color = Color.White)
                        }
                        Button(
                            onClick = { if (deckName == "A") viewModel.jumpHotCueA(2) else viewModel.jumpHotCueB(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            Text("Jump C2", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                // Tempo
                Column {
                    Text(text = "Velocidad (Tempo): ${String.format("%.2f", tempo)}x", color = Color.White, fontSize = 10.sp)
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

                // EQ de 3 Bandas
                Column {
                    Text(text = "Graves (Low)", color = Color.White, fontSize = 10.sp)
                    Slider(
                        value = low,
                        onValueChange = { low = it; if (deckName == "A") viewModel.setEqualizerA(low, mid, high) else viewModel.setEqualizerB(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Medios (Mid)", color = Color(0xFFFFD700), fontSize = 10.sp)
                    Slider(
                        value = mid,
                        onValueChange = { mid = it; if (deckName == "A") viewModel.setEqualizerA(low, mid, high) else viewModel.setEqualizerB(low, mid, high) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                }
                Column {
                    Text(text = "Agudos (High)", color = Color.White, fontSize = 10.sp)
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
