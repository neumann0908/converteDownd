package com.musicdownloader.converter.beatstudio.presentation.synth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun SynthStudioScreen(
    viewModel: SynthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(10.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- CABEZAL DEL SINTETIZADOR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "MOTOR 5: SYNTH STUDIO", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "Instrumento Activo: ${state.currentPatch.instrumentType.displayName}", color = Color.White, fontSize = 12.sp)
                Text(text = "Onda: ${state.currentPatch.waveform.name}", color = Color(0xFF00E676), fontSize = 10.sp)
            }
        }

        // --- CATÁLOGO DE INSTRUMENTOS (10 Recomendados) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = "CATÁLOGO DE INSTRUMENTOS", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SynthInstrumentType.entries) { instrument ->
                        val isSelected = state.currentPatch.instrumentType == instrument
                        Button(
                            onClick = { viewModel.selectInstrument(instrument) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFF00E676) else Color(0xFF28283D)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(text = instrument.displayName, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- SELECTOR DE FORMAS DE ONDA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = "OSCILADOR (FORMAS DE ONDA)", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WaveformType.entries.forEach { wave ->
                        val isSelected = state.currentPatch.waveform == wave
                        Button(
                            onClick = { viewModel.updateWaveform(wave) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF28283D)),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(text = wave.name, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- LABORATORIO ADSR (ENVOLVENTES) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "ENVOLVENTE ADSR", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val adsr = state.currentPatch.adsr

                // Attack
                Text(text = "Attack: ${adsr.attackMs.toInt()} ms", color = Color.White, fontSize = 10.sp)
                Slider(value = adsr.attackMs, onValueChange = { viewModel.updateAdsr(it, adsr.decayMs, adsr.sustainLevel, adsr.releaseMs) }, valueRange = 1f..500f, modifier = Modifier.height(20.dp))

                // Decay
                Text(text = "Decay: ${adsr.decayMs.toInt()} ms", color = Color.White, fontSize = 10.sp)
                Slider(value = adsr.decayMs, onValueChange = { viewModel.updateAdsr(adsr.attackMs, it, adsr.sustainLevel, adsr.releaseMs) }, valueRange = 10f..1000f, modifier = Modifier.height(20.dp))

                // Sustain
                Text(text = "Sustain: ${(adsr.sustainLevel * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
                Slider(value = adsr.sustainLevel, onValueChange = { viewModel.updateAdsr(adsr.attackMs, adsr.decayMs, it, adsr.releaseMs) }, valueRange = 0.0f..1.0f, modifier = Modifier.height(20.dp))

                // Release
                Text(text = "Release: ${adsr.releaseMs.toInt()} ms", color = Color.White, fontSize = 10.sp)
                Slider(value = adsr.releaseMs, onValueChange = { viewModel.updateAdsr(adsr.attackMs, adsr.decayMs, adsr.sustainLevel, it) }, valueRange = 10f..2000f, modifier = Modifier.height(20.dp))
            }
        }

        // --- TECLADO VIRTUAL TÁCTIL PARA PRUEBAS EN VIVO ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "TECLADO DE PRUEBA EN VIVO", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val notes = listOf(
                        Pair("C4", 261.63f),
                        Pair("D4", 293.66f),
                        Pair("E4", 329.63f),
                        Pair("F4", 349.23f),
                        Pair("G4", 392.00f),
                        Pair("A4", 440.00f),
                        Pair("B4", 493.88f),
                        Pair("C5", 523.25f)
                    )

                    notes.forEach { (noteName, freq) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0E0E0))
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                                .clickable { viewModel.playTestNote(freq) },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(text = noteName, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
