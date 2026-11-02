package com.musicdownloader.converter.beatstudio.presentation.recording

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.sin

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var startSecStr by remember { mutableStateOf("0") }
    var endSecStr by remember { mutableStateOf("15") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingSession("Toma_Voz")
            Toast.makeText(context, "⏺ Grabación iniciada con metrónomo...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "❌ Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- CABEZAL DEL MOTOR 8 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "MOTOR 8: RECORDING ENGINE", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Entrada: Micrófono Estéreo | Tomas: ${state.recordedTakesCount}", color = Color.White, fontSize = 12.sp)
                }
            }

            // --- CONTROL DE TEMPO Y METRONOMO ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "TEMPO: ${state.currentBpm} BPM", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Pulso Compás: Beat ${state.currentBeat}/4", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Metrónomo", color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = state.isMetronomeOn,
                                onCheckedChange = { viewModel.toggleMetronome(it) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = state.currentBpm.toFloat(),
                        onValueChange = { viewModel.updateBpm(it.toInt()) },
                        valueRange = 40f..240f,
                        steps = 199,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700)
                        )
                    )
                }
            }

            // --- ESPECTRÓGRAFO / VÚMETRO NEÓN ESTILO DAW ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "ESPECTRO NEÓN & VU METER", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barCount = 12
                        for (i in 0 until barCount) {
                            val dynamicFactor = if (state.isRecording || state.isMetronomeOn) {
                                ((sin((i + System.currentTimeMillis() * 0.01) * 0.5) + 1.0) * 0.5).toFloat()
                            } else 0.05f
                            
                            val barHeightFraction = (state.currentMeterLevel * 1.5f + dynamicFactor * 0.6f).coerceIn(0.1f, 1.0f)
                            
                            val barColor = when {
                                i > 9 -> Color(0xFFFF5252)
                                i > 6 -> Color(0xFFFFEB3B)
                                else -> Color(0xFF00E676)
                            }

                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(barHeightFraction)
                                    .background(barColor, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Nivel RMS: ${(state.currentMeterLevel * 100).toInt()}%", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- INTERRUPTORES DE MONITOREO Y ARMADO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "MONITOR LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Switch(
                            checked = state.isMonitorLiveOn,
                            onCheckedChange = { viewModel.toggleMonitorLive(it) }
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ARM RECORD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Switch(
                            checked = state.isArmRecordOn,
                            onCheckedChange = { viewModel.toggleArmRecord(it) }
                        )
                    }
                }
            }

            // --- BOTÓN PRINCIPAL DE GRABACIÓN ---
            Button(
                onClick = {
                    if (!state.isRecording) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.stopRecordingSession()
                        Toast.makeText(context, "⏹ Grabación finalizada. Abre el editor DAW.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRecording) Color(0xFFFF5252) else Color(0xFF00E676)
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (state.isRecording) "⏹ DETENER Y ABRIR DAW" else "⏺ GRABAR AUDIO / VOZ (CON TEMPO)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // --- DIÁLOGO DAW DE RECORTAR TOMA (ESTILO PRO TOOLS / MULTITRACK) ---
        if (state.pendingTrimFile != null) {
            val file = state.pendingTrimFile!!
            val totalDurationSec = 30

            AlertDialog(
                onDismissRequest = { viewModel.dismissTrim() },
                containerColor = Color(0xFF1E1E2C),
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎚️ ESTUDIO DAW: RECORTAR TOMA", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${totalDurationSec}s", color = Color.Cyan, fontSize = 11.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Archivo: ${file.name}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )

                        // --- VISUALIZADOR DE ONDA MULTITRACK / WAVEFORM DAW ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(Color(0xFF13131A), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val bars = 48
                                val barWidth = canvasWidth / bars

                                // Rejilla de compases estilo DAW
                                for (i in 0..6) {
                                    val x = (canvasWidth / 6) * i
                                    drawLine(
                                        color = Color(0xFF3B3B4F),
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                }

                                val startSecVal = startSecStr.toFloatOrNull() ?: 0f
                                val endSecVal = endSecStr.toFloatOrNull() ?: totalDurationSec.toFloat()

                                // Dibujar ondas de audio en bloques multicolores
                                for (i in 0 until bars) {
                                    val x = i * barWidth + barWidth / 2
                                    val waveFactor = ((sin(i * 0.35) + 1.0) * 0.45 + 0.15).toFloat()
                                    val barHeight = canvasHeight * waveFactor

                                    val progressRatio = i.toFloat() / bars
                                    val currentBarSec = progressRatio * totalDurationSec
                                    val isSelectedRange = currentBarSec in startSecVal..endSecVal

                                    val barColor = if (isSelectedRange) Color(0xFFFFD700) else Color(0xFF00E676).copy(alpha = 0.3f)

                                    drawLine(
                                        color = barColor,
                                        start = Offset(x, (canvasHeight - barHeight) / 2),
                                        end = Offset(x, (canvasHeight + barHeight) / 2),
                                        strokeWidth = barWidth * 0.75f
                                    )
                                }
                            }
                        }

                        // --- CAMPOS DE INICIO Y FIN ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = startSecStr,
                                onValueChange = { startSecStr = it },
                                label = { Text("Inicio (s)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF3B3B4F)
                                )
                            )
                            OutlinedTextField(
                                value = endSecStr,
                                onValueChange = { endSecStr = it },
                                label = { Text("Fin (s)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF3B3B4F)
                                )
                            )
                        }

                        // --- BOTÓN DE PREESCUCHA ---
                        Button(
                            onClick = {
                                val start = startSecStr.toIntOrNull() ?: 0
                                viewModel.playerManager.playFile(file.absolutePath, file.name)
                                viewModel.playerManager.seekTo(start * 1000L)
                                Toast.makeText(context, "▶ Preescuchando desde el segundo $start", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "▶ Preescuchar Fragmento Seleccionado", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = startSecStr.toIntOrNull() ?: 0
                        val end = endSecStr.toIntOrNull() ?: 15
                        viewModel.playerManager.stop()
                        viewModel.applyTrimToPending(start, end) { success ->
                            if (success) {
                                Toast.makeText(context, "✅ ¡Toma recortada y guardada en Biblioteca!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ Error al recortar el audio", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Confirmar y Guardar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.dismissTrim()
                    }) {
                        Text("Descartar", color = Color.LightGray)
                    }
                }
            )
        }
    }
}
