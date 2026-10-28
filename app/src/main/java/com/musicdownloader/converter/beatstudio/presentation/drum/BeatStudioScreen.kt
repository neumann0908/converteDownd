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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)) // Fondo Pro Oscuro
            .padding(16.dp)
    ) {
        // --- LA CAJA SUPERIOR: TÍTULO Y BOTÓN PLAY ---
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
                    text = "Drum Engine / Step Sequencer",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${state.bpm} BPM", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))

                // Botón mágico de Play y Stop
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

        Spacer(modifier = Modifier.height(16.dp))

        // --- LA CUADRÍCULA DE RITMOS (LOS 16 PASOS) ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.tracks) { trackIndex, track ->
                TrackRow(
                    track = track,
                    currentStep = if (state.isPlaying) state.currentStep else -1,
                    onStepClick = { stepIndex ->
                        viewModel.toggleStep(track.id, stepIndex)
                    },
                    isSelected = state.selectedTrackIndex == trackIndex,
                    onTrackSelect = { viewModel.selectTrack(trackIndex) }
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    track: DrumTrack,
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    isSelected: Boolean,
    onTrackSelect: () -> Unit
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
            // Nombre del instrumento (Ej: Kick 808)
            Text(
                text = track.name,
                color = Color(track.colorHex),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.width(90.dp)
            )

            // Los 16 botoncitos en fila
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(16) { stepIndex ->
                    val isActive = track.steps[stepIndex]
                    val isPlayingStep = currentStep == stepIndex

                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 36.dp)
                            .clip(RoundedCornerShape(4.dp))
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
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onStepClick(stepIndex) }
                    )
                }
            }
        }
    }
}
