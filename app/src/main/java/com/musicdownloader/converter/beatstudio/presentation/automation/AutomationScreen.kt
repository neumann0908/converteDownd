package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val totalDurationMs = 15000L // Duración total de la pista (15 segundos)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- CABEZAL DEL MOTOR 9 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "MOTOR 9: AUTOMATION ENGINE", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Editando parámetro: ${state.selectedParameter.displayName}", color = Color.White, fontSize = 12.sp)
                }
            }

            // --- SELECTOR DESLIZANTE DE PARÁMETROS PROFESIONALES ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(AutomationParameterType.entries) { param ->
                    val isSelected = state.selectedParameter == param
                    val backgroundColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                    val textColor = if (isSelected) Color.Black else Color.White

                    Surface(
                        onClick = { viewModel.selectParameter(param) },
                        shape = RoundedCornerShape(8.dp),
                        color = backgroundColor,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = param.displayName,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // --- LIENZO TÁCTIL PARA DIBUJAR LAS CURVAS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .pointerInput(state.selectedParameter) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()

                                if (canvasWidth > 0f && canvasHeight > 0f) {
                                    val timeMs = (offset.x / canvasWidth) * totalDurationMs
                                    val value = 1f - (offset.y / canvasHeight) // Invertir eje Y para que arriba sea 1.0 y abajo 0.0
                                    viewModel.addOrUpdatePoint(timeMs.toLong(), value.coerceIn(0f, 1f))
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Dibujar líneas de cuadrícula horizontal para estilo DAW real
                        for (i in 0..4) {
                            val y = (height / 4) * i
                            drawLine(
                                color = Color(0xFF3B3B4F),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Dibujar líneas de cuadrícula vertical
                        for (j in 0..6) {
                            val x = (width / 6) * j
                            drawLine(
                                color = Color(0xFF3B3B4F),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f
                            )
                        }

                        // Dibujar las líneas de la curva que unen los puntos del carril actual
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
                                    color = Color(0xFFFFD700), // Línea dorada brillante de automatización
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 3f
                                )
                            }

                            // Dibujar puntitos celestes en cada nodo interactivo
                            for (p in points) {
                                val x = (p.timeMs.toFloat() / totalDurationMs) * width
                                val y = (1f - p.value) * height
                                drawCircle(
                                    color = Color.Cyan,
                                    radius = 8f,
                                    center = Offset(x, y)
                                )
                            }
                        }

                        // --- CABEZAL DE REPRODUCCIÓN (PLAYHEAD) ---
                        val playheadX = (state.playbackTimeMs.toFloat() / totalDurationMs) * width
                        drawLine(
                            color = Color.White, // Línea blanca vertical del playhead
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, height),
                            strokeWidth = 2f
                        )
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
                Text(text = "🗑️ Borrar Curva de Automatización", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
