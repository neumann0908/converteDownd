package com.musicdownloader.converter.beatstudio.presentation.sequencer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SequencerStudioScreen(
    viewModel: SequencerStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // 🟢 Estado local para controlar qué paso se está editando mediante pulsación larga (Velocity)
    var stepToEdit by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (trackIndex, stepIndex)

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
                    
                    // Botones rápidos para añadir bloques al arreglo
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

                // Fila horizontal de bloques de secciones (toca una sección para eliminarla)
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

        // --- REJILLA DEL SECUENCIADOR (STEP SEQUENCER GRID) ---
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
                        // Nombre del Instrumento / Pista
                        Text(
                            text = track.trackName,
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // Fila de 16 pasos
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
                                                step.isActive && isCurrentStepPlaying -> Color.White // Activado y sonando ahora mismo
                                                step.isActive -> Color(0xFF00E676).copy(alpha = 0.5f + (step.velocity * 0.5f)) // Brillo dinámico por velocity
                                                isCurrentStepPlaying -> Color(0xFF3B3B4F) // Paso actual inactivo
                                                else -> Color(0xFF28283D) // Apagado normal
                                            }
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    viewModel.toggleStep(trackIndex, stepIndex)
                                                },
                                                onLongPress = {
                                                    // 🟢 Mantener presionado abre el diálogo flotante de Velocity
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
}
