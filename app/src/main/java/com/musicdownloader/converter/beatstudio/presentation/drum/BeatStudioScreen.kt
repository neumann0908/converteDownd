package com.musicdownloader.converter.beatstudio.presentation.drum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

@Composable
fun BeatStudioScreen(
    viewModel: DrumStudioViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val selectedTrack = state.tracks.getOrNull(state.selectedTrackIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)) // Fondo Pro Oscuro
            .padding(16.dp)
    ) {
        // --- CAJA SUPERIOR: TÍTULO, BPM Y PLAY ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2C))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BEAT STUDIO PRO",
                    color = Color(0xFFFFD700), // Dorado Pro
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kit: ${state.currentKit}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${state.bpm} BPM", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- BARRA DE CONFIGURACIÓN AVANZADA (PASOS Y KITS) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selector de Pasos (16, 32, 64)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(16, 32, 64).forEach { count ->
                    SmallChipButton(
                        text = "${count}p",
                        isActive = state.stepCount == count,
                        activeColor = Color(0xFFFFD700),
                        onClick = { viewModel.setStepCount(count) }
                    )
                }
            }

            // Selector Rápido de Kits
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Standard 808", "Trap", "Lo-Fi").forEach { kit ->
                    SmallChipButton(
                        text = kit.take(4),
                        isActive = state.currentKit.contains(kit),
                        activeColor = Color(0xFF00E676),
                        onClick = { viewModel.switchKit("Kit $kit") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- CUADRÍCULA DE RITMOS (TRACKS Y PASOS) ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.tracks) { trackIndex, track ->
                TrackRow(
                    track = track,
                    currentStep = if (state.isPlaying) state.currentStep else -1,
                    stepCount = state.stepCount,
                    onStepClick = { stepIndex ->
                        viewModel.toggleStep(track.id, stepIndex)
                    },
                    isSelected = state.selectedTrackIndex == trackIndex,
                    onTrackSelect = { viewModel.selectTrack(trackIndex) },
                    onMuteClick = { viewModel.toggleMute(track.id) },
                    onSoloClick = { viewModel.toggleSolo(track.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- PANEL DE FUNCIÓN DIFERENCIAL (CONTROL DE PISTA SELECCIONADA) ---
        if (selectedTrack != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "EDICIÓN DIFERENCIAL: ${selectedTrack.name}",
                        color = Color(selectedTrack.colorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Controles deslizantes de Volumen, Pitch y Pan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volumen
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Vol: ${(selectedTrack.volume * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
                            Slider(
                                value = selectedTrack.volume,
                                onValueChange = { viewModel.updateVolume(selectedTrack.id, it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = Color(selectedTrack.colorHex), activeTrackColor = Color(selectedTrack.colorHex))
                            )
                        }
                        // Pitch (Tono)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Pitch: ${String.format("%.2f", selectedTrack.pitch)}x", color = Color.White, fontSize = 10.sp)
                            Slider(
                                value = selectedTrack.pitch,
                                onValueChange = { viewModel.updatePitch(selectedTrack.id, it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(selectedTrack.colorHex), activeTrackColor = Color(selectedTrack.colorHex))
                            )
                        }
                        // Pan (Panorama)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Pan: ${String.format("%.1f", selectedTrack.pan)}", color = Color.White, fontSize = 10.sp)
                            Slider(
                                value = selectedTrack.pan,
                                onValueChange = { viewModel.updatePan(selectedTrack.id, it) },
                                valueRange = -1.0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(selectedTrack.colorHex), activeTrackColor = Color(selectedTrack.colorHex))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    track: DrumTrack,
    currentStep: Int,
    stepCount: Int,
    onStepClick: (Int) -> Unit,
    isSelected: Boolean,
    onTrackSelect: () -> Unit,
    onMuteClick: () -> Unit,
    onSoloClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF28283D) else Color(0xFF1E1E2C)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackSelect() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nombre del instrumento y controles Mute/Solo compactos
            Column(
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = track.name,
                    color = Color(track.colorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmallChipButton(
                        text = "M",
                        isActive = track.isMuted,
                        activeColor = Color.Red,
                        onClick = onMuteClick
                    )
                    SmallChipButton(
                        text = "S",
                        isActive = track.isSolo,
                        activeColor = Color(0xFFFFD700),
                        onClick = onSoloClick
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Botoncitos de la secuencia en fila (dinámicos según stepCount)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stepCount) { stepIndex ->
                    val isActive = track.steps.getOrElse(stepIndex) { false }
                    val isPlayingStep = currentStep == stepIndex

                    Box(
                        modifier = Modifier
                            .size(width = if (stepCount > 32) 16.dp else if (stepCount > 16) 20.dp else 24.dp, height = 32.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    isActive -> Color(track.colorHex)
                                    isPlayingStep -> Color.White.copy(alpha = 0.3f)
                                    (stepIndex / 4) % 2 == 0 -> Color(0xFF13131A)
                                    else -> Color(0xFF181822)
                                }
                            )
                            .border(
                                width = if (isPlayingStep) 1.5.dp else 0.5.dp,
                                color = if (isPlayingStep) Color.White else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .clickable { onStepClick(stepIndex) }
                    )
                }
            }
        }
    }
}

// Botón auxiliar compacto para chips y selectores
@Composable
fun SmallChipButton(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeColor else Color(0xFF13131A))
            .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else Color.LightGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
