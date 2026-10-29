package com.musicdownloader.converter.beatstudio.presentation.drum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun BeatStudioScreen(
    viewModel: DrumStudioViewModel
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val selectedTrack = state.tracks.getOrNull(state.selectedTrackIndex)

    // 🌐 Observación reactiva de los estados de descarga y progreso (0% a 100%)
    val isDownloading by viewModel.isDownloading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()

    // 🚨 Observación reactiva de errores y mensajes del motor de audio
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Estados locales para el diálogo de selección de samples filtrados por categoría
    var showSampleDialog by remember { mutableStateOf(false) }
    var selectedTrackIndexForSample by remember { mutableStateOf(0) }
    var selectedTrackNameForSample by remember { mutableStateOf("") }
    var availableSamplesList by remember { mutableStateOf(listOf<String>()) }

    // Contenedor principal en formato Box para permitir capas flotantes (Overlay de descarga y mensajes)
    Box(modifier = Modifier.fillMaxSize()) {

        // --- CONTENIDO PRINCIPAL DE LA APP ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF13131A)) // Fondo Pro Oscuro
                .padding(12.dp)
        ) {
            // --- CAJA SUPERIOR: TÍTULO, BPM Y PLAY ---
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
                        text = "BEAT STUDIO PRO",
                        color = Color(0xFFFFD700), // Dorado Pro
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kit: ${state.currentKit}",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${state.bpm} BPM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(42.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            // --- BARRA DE CONFIGURACIÓN AVANZADA (PASOS, KITS Y DUPLICAR) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de Pasos (16, 32, 64)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(16, 32, 64).forEach { count ->
                        SmallChipButton(
                            text = "${count}p",
                            isActive = state.stepCount == count,
                            activeColor = Color(0xFFFFD700),
                            onClick = { viewModel.setStepCount(count) }
                        )
                    }
                }

                // Botón de Duplicar Patrón
                SmallChipButton(
                    text = "Duplicar",
                    isActive = false,
                    activeColor = Color(0xFF00E676),
                    onClick = { viewModel.duplicatePattern() }
                )

                // Selector Rápido de Kits
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Standard 808", "Trap").forEach { kit ->
                        SmallChipButton(
                            text = kit.take(4),
                            isActive = state.currentKit.contains(kit),
                            activeColor = Color(0xFF00E676),
                            onClick = { viewModel.switchKit("Kit $kit") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- CUADRÍCULA DE RITMOS (TRACKS Y PASOS) ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        onSoloClick = { viewModel.toggleSolo(track.id) },
                        onInstrumentClick = {
                            val nameLower = track.name.lowercase()
                            val keyword = when {
                                nameLower.contains("kick") -> "kick"
                                nameLower.contains("snare") -> "snare"
                                nameLower.contains("hat") || nameLower.contains("closed") || nameLower.contains("open") -> "hat"
                                nameLower.contains("clap") -> "clap"
                                nameLower.contains("perc") -> "perc"
                                nameLower.contains("tom") -> "tom"
                                nameLower.contains("rim") -> "rim"
                                nameLower.contains("shaker") -> "shaker"
                                else -> nameLower.take(4)
                            }
                            
                            availableSamplesList = viewModel.getFilteredSamplesForCategory(keyword)
                            selectedTrackIndexForSample = trackIndex
                            selectedTrackNameForSample = track.name
                            showSampleDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- PANEL DE FUNCIÓN DIFERENCIAL PROFUNDA (VOL, PITCH, PAN, ADSR, EFECTOS Y EXPORTACIÓN) ---
            if (selectedTrack != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EDICIÓN: ${selectedTrack.name}",
                                color = Color(selectedTrack.colorHex),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // 💾 Botón de Exportación WAV directo
                                SmallChipButton(
                                    text = "💾 Exportar WAV",
                                    isActive = false,
                                    activeColor = Color(0xFF00E676),
                                    onClick = {
                                        val exportDir = File(context.filesDir, "exports")
                                        if (!exportDir.exists()) exportDir.mkdirs()
                                        val file = File(exportDir, "${selectedTrack.name.replace(" ", "_")}_${System.currentTimeMillis()}.wav")
                                        viewModel.exportTrackAsWav(state.selectedTrackIndex, file.absolutePath)
                                    }
                                )
                                Text(
                                    text = "Swing: ${(state.swing * 100).toInt()}%",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Vol: ${(selectedTrack.volume * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.volume,
                                    onValueChange = { viewModel.updateVolume(selectedTrack.id, it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Pitch: ${String.format("%.2f", selectedTrack.pitch)}x", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.pitch,
                                    onValueChange = { viewModel.updatePitch(selectedTrack.id, it) },
                                    valueRange = 0.5f..2.0f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Pan: ${String.format("%.1f", selectedTrack.pan)}", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.pan,
                                    onValueChange = { viewModel.updatePan(selectedTrack.id, it) },
                                    valueRange = -1.0f..1.0f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Attack: ${String.format("%.2f", selectedTrack.attack)}s", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.attack,
                                    onValueChange = { viewModel.updateAdsr(selectedTrack.id, it, selectedTrack.decay, selectedTrack.sustain, selectedTrack.release) },
                                    valueRange = 0.001f..0.5f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Reverb: ${(selectedTrack.reverb * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.reverb,
                                    onValueChange = { viewModel.updateEffects(selectedTrack.id, it, selectedTrack.delay) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Delay: ${(selectedTrack.delay * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                                Slider(
                                    value = selectedTrack.delay,
                                    onValueChange = { viewModel.updateEffects(selectedTrack.id, selectedTrack.reverb, it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- VENTANA FLOTANTE DE PROGRESO DE DESCARGA (0% A 100%) ---
        if (isDownloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎛️ PREPARANDO BEAT STUDIO PRO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Descargando kits de audio: ${(progress * 100).toInt()}%",
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFFFFD700),
                            trackColor = Color.DarkGray,
                        )

                        Text(
                            text = "Por favor, mantén la app abierta y conectada.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- 🚨 DIÁLOGO FLOTANTE DE DIAGNÓSTICO DE ERRORES Y ÉXITO ---
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                containerColor = Color(0xFF1E1E2C),
                title = {
                    Text(
                        text = "🚨 Beat Studio Pro",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(text = "Entendido", color = Color(0xFFFFD700))
                    }
                }
            )
        }

        // --- DIÁLOGO FLOTANTE PARA SELECCIÓN DE SAMPLE FILTRADO ---
        if (showSampleDialog) {
            AlertDialog(
                onDismissRequest = { showSampleDialog = false },
                containerColor = Color(0xFF1E1E2C),
                title = {
                    Text(
                        text = "Sonidos para: $selectedTrackNameForSample",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    if (availableSamplesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron samples en la memoria interna (kits/)",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableSamplesList) { sampleName ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF28283D)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.assignSampleToTrack(selectedTrackIndexForSample, sampleName)
                                            showSampleDialog = false
                                        }
                                ) {
                                    Text(
                                        text = sampleName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSampleDialog = false }) {
                        Text(text = "Cerrar", color = Color(0xFFFFD700))
                    }
                }
            )
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
    onSoloClick: () -> Unit,
    onInstrumentClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF28283D) else Color(0xFF1E1E2C)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackSelect() }
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(90.dp)
                    .clickable { onInstrumentClick() }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = track.name,
                    color = Color(track.colorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = "Cambiar 🔍",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
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

            Spacer(modifier = Modifier.width(2.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stepCount) { stepIndex ->
                    val isActive = track.steps.getOrElse(stepIndex) { false }
                    val isPlayingStep = currentStep == stepIndex

                    Box(
                        modifier = Modifier
                            .size(width = if (stepCount > 32) 14.dp else if (stepCount > 16) 18.dp else 22.dp, height = 28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isActive -> Color(track.colorHex)
                                    isPlayingStep -> Color.White.copy(alpha = 0.3f)
                                    (stepIndex / 4) % 2 == 0 -> Color(0xFF13131A)
                                    else -> Color(0xFF181822)
                                }
                            )
                            .border(
                                width = if (isPlayingStep) 1.2.dp else 0.4.dp,
                                color = if (isPlayingStep) Color.White else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { onStepClick(stepIndex) }
                    )
                }
            }
        }
    }
}

@Composable
fun SmallChipButton(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeColor else Color(0xFF13131A))
            .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
