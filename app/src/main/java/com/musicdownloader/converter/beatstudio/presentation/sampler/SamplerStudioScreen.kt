package com.musicdownloader.converter.beatstudio.presentation.sampler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FolderOpen
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
fun SamplerStudioScreen(
    viewModel: SamplerStudioViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)) // Fondo Pro Oscuro DAW
            .padding(12.dp)
    ) {
        // --- CAJA SUPERIOR: TÍTULO Y ESTADO DE MUESTRA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2C))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SAMPLER ENGINE PRO",
                    color = Color(0xFF00E676), // Verde Pro Sampler
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = track.sampleName,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Cargar Archivo (WAV, MP3, FLAC)[span_0](start_span)[span_0](end_span)
                IconButton(
                    onClick = { viewModel.importAudioFile("DrumLoop_Pro.wav", 4000L) },
                    modifier = Modifier.size(36.dp).background(Color(0xFF28283D), CircleShape)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Importar", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Botón Micrófono (Grabar sample)[span_1](start_span)[span_1](end_span)
                IconButton(
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier.size(36.dp).background(if (state.isRecording) Color.Red else Color(0xFF28283D), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Grabar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- PANEL DE AUTO CHOP INTELIGENTE (Función Diferencial) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "AUTO CHOP INTELIGENTE",
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AutoChopButton("Transitorios", track.autoChopMode == AutoChopType.TRANSIENTS) {
                        viewModel.executeAutoChop(AutoChopType.TRANSIENTS)
                    }
                    AutoChopButton("Silencios", track.autoChopMode == AutoChopType.SILENCES) {
                        viewModel.executeAutoChop(AutoChopType.SILENCES)
                    }
                    AutoChopButton("Energía", track.autoChopMode == AutoChopType.ENERGY) {
                        viewModel.executeAutoChop(AutoChopType.ENERGY)
                    }
                    AutoChopButton("Compases", track.autoChopMode == AutoChopType.MEASURES) {
                        viewModel.executeAutoChop(AutoChopType.MEASURES)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- VISUALIZADOR DE ONDA Y PADS DE REPRODUCCIÓN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF181822))
                .border(1.dp, Color(0xFF28283D), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (track.slices.isEmpty()) {
                Text(
                    text = "Importa un archivo o graba para ver la forma de onda",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            } else {
                // Matriz de Pads para disparar cada fragmento (*slice*)[span_2](start_span)[span_2](end_span)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(track.slices) { slice ->
                        SamplerPadCard(
                            sliceId = slice.id,
                            pitch = slice.pitchShift,
                            isReversed = slice.isReversed,
                            onPadClick = { viewModel.toggleSliceReverse(slice.id) },
                            onPitchChange = { newPitch -> viewModel.updateSlicePitch(slice.id, newPitch) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- PANEL INFERIOR: EFECTOS, LOOP Y ENVOLVENTE ADSR ---
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
                    Text(text = "EFECTOS & ADSR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallChipToggle("Loop", track.isLooping) { viewModel.toggleLoop() }
                        SmallChipToggle("Norm", track.isNormalized) { viewModel.toggleNormalization() }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Ataque: ${String.format("%.2f", track.attack)}s", color = Color.LightGray, fontSize = 9.sp)
                        Slider(
                            value = track.attack,
                            onValueChange = { viewModel.updateSamplerAdsr(it, track.decay, track.sustain, track.release) },
                            valueRange = 0.001f..0.5f,
                            modifier = Modifier.height(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Decay: ${String.format("%.2f", track.decay)}s", color = Color.LightGray, fontSize = 9.sp)
                        Slider(
                            value = track.decay,
                            onValueChange = { viewModel.updateSamplerAdsr(track.attack, it, track.sustain, track.release) },
                            valueRange = 0.01f..1.0f,
                            modifier = Modifier.height(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// 🛠️ ¡CORREGIDO AQUÍ AGREGANDO LA PALABRA CLAVE "fun"!
@Composable
fun RowScope.AutoChopButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color(0xFF13131A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SamplerPadCard(
    sliceId: Int,
    pitch: Float,
    isReversed: Boolean,
    onPadClick: () -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF28283D)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(75.dp)
            .clickable { onPadClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Pad ${sliceId + 1}", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (isReversed) Text(text = "REV", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "Pitch: ${String.format("%.1f", pitch)}x", color = Color.White, fontSize = 9.sp)
        }
    }
}

@Composable
fun SmallChipToggle(text: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF00E676) else Color(0xFF13131A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else Color.LightGray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
