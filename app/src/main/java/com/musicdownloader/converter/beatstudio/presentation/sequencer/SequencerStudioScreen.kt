package com.musicdownloader.converter.beatstudio.presentation.sequencer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun SequencerStudioScreen(
    viewModel: SequencerStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Estado local para controlar qué paso se está editando mediante pulsación larga (Velocity)
    var stepToEdit by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (trackIndex, stepIndex)
    
    // 🟢 Estado local para controlar qué tecla/pitch se está configurando con muestras personalizadas
    var pitchToConfigure by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)) // Fondo DAW Pro Oscuro
            .padding(10.dp)
    ) {
        // --- CABEZAL DE CONTROLES PRINCIPALES (BPM y Play/Stop) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2C))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SEQUENCER ENGINE PRO",
                    color = Color(0xFFFFD700), // Dorado Pro
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BPM: ${state.bpm.toInt()} | Compás: ${state.timeSignature} | Res: ${state.resolution}",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }

            // Botón Reproducir / Detener
            IconButton(
                onClick = {
                    if (state.isPlaying) {
                        viewModel.stopPlayback()
                    } else {
                        viewModel.startPlayback()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (state.isPlaying) Color.Red else Color(0xFF00E676), CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Play/Stop",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CONTROLADOR DE VELOCIDAD (BPM SLIDER) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "TEMPO (BPM): ${state.bpm.toInt()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = state.bpm,
                    onValueChange = { viewModel.updateBpm(it) },
                    valueRange = 60f..200f,
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 🎛️ CONTROLES AVANZADOS (Compás, Resolución y Metrónomo) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Selector de Compás
            Button(
                onClick = {
                    val nextSig = when (state.timeSignature) {
                        "4/4" -> "3/4"
                        "3/4" -> "6/8"
                        else -> "4/4"
                    }
                    viewModel.updateTimeSignature(nextSig)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Compás: ${state.timeSignature}", color = Color.White, fontSize = 9.sp)
            }

            // Selector de Resolución
            Button(
                onClick = {
                    val nextRes = when (state.resolution) {
                        "1/16" -> "1/8"
                        "1/8" -> "1/4"
                        else -> "1/16"
                    }
                    viewModel.updateResolution(nextRes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Res: ${state.resolution}", color = Color.White, fontSize = 9.sp)
            }

            // Botón Metrónomo On/Off
            Button(
                onClick = { viewModel.toggleMetronome() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isMetronomeEnabled) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = if (state.isMetronomeEnabled) "Click: ON" else "Click: OFF",
                    color = if (state.isMetronomeEnabled) Color.Black else Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 🎹 CONMUTADOR DE VISTA: STEP SEQUENCER vs PIANO ROLL ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.togglePianoRollMode(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!state.isPianoRollMode) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = "Batería (Steps)",
                    color = if (!state.isPianoRollMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.togglePianoRollMode(true, 3) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isPianoRollMode) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = "Piano Roll (Melodía)",
                    color = if (state.isPianoRollMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 🎵 SISTEMA DE ARREGLOS (Línea de tiempo de la canción) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARREGLO DE CANCIÓN",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.addSongSection(SongSectionType.VERSO) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("+ Verso", color = Color.Black, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.addSongSection(SongSectionType.ESTRIBILLO) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("+ Coro", color = Color.Black, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.arrangement.forEachIndexed { index, section ->
                        val isPlayingThisSection = state.isPlaying && state.currentSectionIndex == index
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPlayingThisSection) Color(0xFF00E676) else Color(0xFF28283D))
                                .border(
                                    width = if (isPlayingThisSection) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.removeSongSection(section.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = section.name,
                                    color = if (isPlayingThisSection) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${section.repeatCount}x",
                                    color = if (isPlayingThisSection) Color.DarkGray else Color.Gray,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 🎛️ CONTENEDOR PRINCIPAL: STEP SEQUENCER VS PIANO ROLL ---
        if (!state.isPianoRollMode) {
            // REJILLA DEL SECUENCIADOR (STEP SEQUENCER GRID)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181822))
                    .border(1.dp, Color(0xFF28283D), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.tracks) { trackIndex, track ->
                        Column {
                            Text(
                                text = track.trackName,
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                track.steps.forEachIndexed { stepIndex, step ->
                                    val isCurrentStepPlaying = (state.currentStepIndex == stepIndex)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    step.isActive && isCurrentStepPlaying -> Color.White 
                                                    step.isActive -> Color(0xFF00E676).copy(alpha = 0.5f + (step.velocity * 0.5f))
                                                    isCurrentStepPlaying -> Color(0xFF3B3B4F) 
                                                    else -> Color(0xFF28283D) 
                                                }
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = {
                                                        viewModel.toggleStep(trackIndex, stepIndex)
                                                    },
                                                    onLongPress = {
                                                        stepToEdit = Pair(trackIndex, stepIndex)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${stepIndex + 1}",
                                            color = if (step.isActive) Color.Black else Color.Gray,
                                            fontSize = 7.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 🎹 VISTA PIANO ROLL PROFESIONAL CON BOTÓN DE CONFIGURACIÓN POR TECLA
            val activeTrack = state.tracks.getOrNull(state.selectedPianoTrackIndex) ?: state.tracks.last()
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "PIANO ROLL: ${activeTrack.trackName} (Toca ⚙️ para asignar muestra)",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val pitchNames = listOf("C5", "B4", "A4", "G4", "F4", "E4", "D4", "C4")
                        
                        items(pitchNames.size) { pitchIndex ->
                            val actualPitch = 7 - pitchIndex
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 🟢 Etiqueta de la tecla y botón de configuración de tono
                                Row(
                                    modifier = Modifier.width(44.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = pitchNames[pitchIndex],
                                        color = if (activeTrack.customPitchSamples.containsKey(actualPitch)) Color(0xFF00E676) else Color.LightGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Configurar Muestra",
                                        tint = if (activeTrack.customPitchSamples.containsKey(actualPitch)) Color(0xFF00E676) else Color.Gray,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable { pitchToConfigure = actualPitch }
                                    )
                                }

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    activeTrack.steps.forEachIndexed { stepIndex, step ->
                                        val hasNoteAtThisPitch = step.isActive && step.pitch == actualPitch
                                        val isCurrentStepPlaying = (state.currentStepIndex == stepIndex)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    when {
                                                        hasNoteAtThisPitch && isCurrentStepPlaying -> Color.White
                                                        hasNoteAtThisPitch -> Color(0xFF9C27B0) // Morado Pro para notas melódicas
                                                        isCurrentStepPlaying -> Color(0xFF3B3B4F)
                                                        else -> Color(0xFF22222F)
                                                    }
                                                )
                                                .clickable {
                                                    if (hasNoteAtThisPitch) {
                                                        viewModel.toggleStep(state.selectedPianoTrackIndex, stepIndex)
                                                    } else {
                                                        viewModel.updateStepPitch(state.selectedPianoTrackIndex, stepIndex, actualPitch)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 🎚️ DIÁLOGO FLOTANTE PARA EDITAR LA VELOCITY DE UN PASO ---
    if (stepToEdit != null) {
        val (tIdx, sIdx) = stepToEdit!!
        val currentStep = state.tracks.getOrNull(tIdx)?.steps?.getOrNull(sIdx)
        var tempVelocity by remember(currentStep?.velocity) { mutableStateOf(currentStep?.velocity ?: 1.0f) }

        AlertDialog(
            onDismissRequest = { stepToEdit = null },
            title = { 
                Text(
                    text = "Editar Velocity (Volumen)", 
                    color = Color(0xFFFFD700), 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Intensidad del golpe: ${(tempVelocity * 100).toInt()}%", 
                        color = Color.White, 
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = tempVelocity,
                        onValueChange = { tempVelocity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E676),
                            activeTrackColor = Color(0xFF00E676)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStepVelocity(tIdx, sIdx, tempVelocity)
                        stepToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stepToEdit = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E2C)
        )
    }

    // --- 📂 DIÁLOGO FLOTANTE PARA ASIGNAR MUESTRA PERSONALIZADA A UNA TECLA DEL PIANO ROLL ---
    if (pitchToConfigure != null) {
        val pitch = pitchToConfigure!!
        val activeTrack = state.tracks[state.selectedPianoTrackIndex]

        AlertDialog(
            onDismissRequest = { pitchToConfigure = null },
            title = { 
                Text(
                    text = "Asignar Muestra a Tono (Pitch: $pitch)", 
                    color = Color(0xFFFFD700), 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Elige un archivo .wav de la carpeta de kits para esta tecla:", 
                        color = Color.White, 
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val kitName = activeTrack.trackName.lowercase().replace(" ", "_")
                    val kitDir = File(context.filesDir, "exports/kits/$kitName")
                    val wavFiles = if (kitDir.exists() && kitDir.isDirectory) {
                        kitDir.listFiles { file -> file.extension.lowercase() == "wav" }?.toList() ?: emptyList()
                    } else {
                        emptyList()
                    }

                    if (wavFiles.isEmpty()) {
                        Text(
                            text = "No se encontraron archivos .wav en /exports/kits/$kitName/", 
                            color = Color.Gray, 
                            fontSize = 11.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(140.dp)) {
                            items(wavFiles.size) { index ->
                                val file = wavFiles[index]
                                TextButton(
                                    onClick = {
                                        viewModel.assignSampleToPitch(state.selectedPianoTrackIndex, pitch, file.absolutePath)
                                        pitchToConfigure = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📁 ${file.name}", 
                                        color = Color(0xFF00E676), 
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pitchToConfigure = null }) {
                    Text("Cerrar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E2C)
        )
    }
}
