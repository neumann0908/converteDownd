package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val totalDurationMs = 15000L // Supongamos que nuestra pista dura 15 segundos

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- CABEZAL DEL MOTOR 9 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "MOTOR 9: AUTOMATION ENGINE", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Editando parámetro: ${state.selectedParameter.name}", color = Color.White, fontSize = 12.sp)
                }
            }

            // --- BOTONES PARA ELEGIR QUÉ QUEREMOS AUTOMATIZAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { viewModel.selectParameter(AutomationParameterType.VOLUME) }) {
                    Text("Volumen", fontSize = 10.sp)
                }
                Button(onClick = { viewModel.selectParameter(AutomationParameterType.REVERB) }) {
                    Text("Reverb", fontSize = 10.sp)
                }
                Button(onClick = { viewModel.selectParameter(AutomationParameterType.CUTOFF) }) {
                    Text("Filtro", fontSize = 10.sp)
                }
            }

            // --- LIENZO TÁCTIL PARA DIBUJAR LAS CURVAS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // Convertimos el toque de tu dedo en un tiempo (ms) y un valor (0 a 1)
                                val timeMs = (offset.x / size.width) * totalDurationMs
                                val value = 1f - (offset.y / size.height) // Invertimos Y para que arriba sea 1.0
                                viewModel.addOrUpdatePoint(timeMs.toLong(), value)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Dibujar líneas de cuadrícula para que luzca como un DAW real
                        for (i in 0..4) {
                            val y = (height / 4) * i
                            drawLine(
                                color = Color(0xFF3B3B4F),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Dibujar las líneas de la curva que unen tus puntos
                        val points = state.currentLane.points
                        if (points.isNotEmpty()) {
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]

                                val x1 = (p1.timeMs.toFloat() / totalDurationMs) * width
                                val y1 = (1f - p1.value) * height
                                val x2 = (p2.timeMs.toFloat() / totalDurationMs) * width
                                val y2 = (1f - p2.value) * height

                                drawLine(
                                    color = Color(0xFFFFD700), // Línea dorada brillante
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 3f
                                )
                            }

                            // Dibujar puntitos celestes en cada nodo que tocaste
                            for (p in points) {
                                val x = (p.timeMs.toFloat() / totalDurationMs) * width
                                val y = (1f - p.value) * height
                                drawCircle(
                                    color = Color.Cyan,
                                    radius = 7f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTÓN DE BORRAR ---
            Button(
                onClick = { viewModel.clearPoints() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "🗑️ Borrar Línea de Automatización", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
