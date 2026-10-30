package com.musicdownloader.converter.beatstudio.presentation.sequencer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SequencerStudioScreen(
    viewModel: SequencerStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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
                                                step.isActive -> Color(0xFF00E676) // Activado normal
                                                isCurrentStepPlaying -> Color(0xFF3B3B4F) // Paso actual inactivo
                                                else -> Color(0xFF28283D) // Apagado normal
                                            }
                                        )
                                        .clickable {
                                            viewModel.toggleStep(trackIndex, stepIndex)
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
}
