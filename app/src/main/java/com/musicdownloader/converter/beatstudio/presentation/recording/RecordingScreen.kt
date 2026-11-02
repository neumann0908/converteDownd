package com.musicdownloader.converter.beatstudio.presentation.recording

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var startSecStr by remember { mutableStateOf("0") }
    var endSecStr by remember { mutableStateOf("15") }
    var pendingUseTempo by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingSession("Toma_Voz", pendingUseTempo)
            val modeText = if (pendingUseTempo) "con tempo y metrónomo" else "sin tempo (libre)"
            Toast.makeText(context, "⏺ Grabación iniciada $modeText...", Toast.LENGTH_SHORT).show()
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

            // --- BOTONES DUALES DE GRABACIÓN & ACCESO A COMPING ---
            if (!state.isRecording) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingUseTempo = true
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "⏺ CON TEMPO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                pendingUseTempo = false
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "⏺ SIN TEMPO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Botón para desplegar el estudio de comping de tomas
                    Button(
                        onClick = { viewModel.openCompingStudio() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B3D)),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "🎚️ ABRIR ESTUDIO COMPING (FUSIONAR TOMAS)", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.stopRecordingSession()
                        Toast.makeText(context, "⏹ Grabación finalizada. Abriendo editor DAW...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "⏹ DETENER Y ABRIR DAW", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- DIÁLOGO 1: ESTUDIO DE COMPING (FUSIÓN DE TOMAS) ---
        if (state.isCompingOpen) {
            val selectedFiles = remember { mutableStateListOf<File>() }

            AlertDialog(
                onDismissRequest = { viewModel.closeCompingStudio() },
                containerColor = Color(0xFF1E1E2C),
                title = { Text(text = "🎚️ ESTUDIO DAW: COMPING DE TOMAS", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Selecciona las tomas que deseas apilar y fusionar en una pista maestra:", color = Color.LightGray, fontSize = 11.sp)
                        
                        if (state.availableTakes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No hay tomas grabadas aún.", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(state.availableTakes) { file ->
                                    val isSelected = selectedFiles.contains(file)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF2B3B3D) else Color(0xFF13131A)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) selectedFiles.remove(file) else selectedFiles.add(file)
                                            },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = file.name, color = if (isSelected) Color(0xFFFFD700) else Color.White, fontSize = 11.sp, maxLines = 1)
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedFiles.add(file) else selectedFiles.remove(file)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.executeCompingFusion(selectedFiles) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "✅ ¡Comping completado! Pista maestra guardada.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ Selecciona al menos una toma válida", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Fusionar en Pista Maestra", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeCompingStudio() }) {
                        Text("Cancelar", color = Color.LightGray)
                    }
                }
            )
        }

        // --- DIÁLOGO 2: ESTUDIO DAW DE RECORTAR TOMA (TRIMMING CON PLAYHEAD) ---
        if (state.pendingTrimFile != null) {
            val file = state.pendingTrimFile!!
            val totalDurationSec = 30

            var isPlayingPreview by remember { mutableStateOf(false) }
            var playbackProgress by remember { mutableStateOf(0f) }

            // Bucle de animación del cabezal de reproducción (Playhead)
            LaunchedEffect(isPlayingPreview) {
                if (isPlayingPreview) {
                    playbackProgress = 0f
                    val startSec = startSecStr.toFloatOrNull() ?: 0f
                    val endSec = endSecStr.toFloatOrNull() ?: totalDurationSec.toFloat()
                    val durationMs = ((endSec - startSec) * 1000f).toLong().coerceAtLeast(1000L)
                    
                    val startTime = System.currentTimeMillis()
                    while (isPlayingPreview) {
                        val elapsed = System.currentTimeMillis() - startTime
                        playbackProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                        if (playbackProgress >= 1f) {
                            isPlayingPreview = false
                            break
                        }
                        delay(16) // 60 FPS
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { 
                    isPlayingPreview = false
                    viewModel.dismissTrim() 
                },
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

                        // --- VISUALIZADOR DE ONDA DINÁMICO CON PLAYHEAD ---
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

                                // Dibujar ondas con picos reales PCM
                                for (i in 0 until bars) {
                                    val x = i * barWidth + barWidth / 2
                                    val waveFactor = state.waveformPeaks.getOrElse(i) { 0.3f }
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

                                // --- LÍNEA DE REPRODUCCIÓN (PLAYHEAD) ANIMADA ---
                                if (isPlayingPreview) {
                                    val startRatio = startSecVal / totalDurationSec
                                    val endRatio = endSecStr.toFloatOrNull()?.div(totalDurationSec) ?: 1f
                                    val currentPlayheadRatio = startRatio + (endRatio - startRatio) * playbackProgress
                                    val playheadX = canvasWidth * currentPlayheadRatio

                                    drawLine(
                                        color = Color.Cyan,
                                        start = Offset(playheadX, 0f),
                                        end = Offset(playheadX, canvasHeight),
                                        strokeWidth = 2.5f
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

                        // --- BOTÓN DE PREESCUCHA CON PLAYHEAD ACTIVADO ---
                        Button(
                            onClick = {
                                isPlayingPreview = true
                                viewModel.playerManager.playFile(file.absolutePath, file.name)
                                Toast.makeText(context, "▶ Preescuchando fragmento...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B4F)),
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "▶ Preescuchar Archivo", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        isPlayingPreview = false
                        val start = startSecStr.toIntOrNull() ?: 0
                        val end = endSecStr.toIntOrNull() ?: 15
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
                        isPlayingPreview = false
                        viewModel.dismissTrim()
                    }) {
                        Text("Descartar", color = Color.LightGray)
                    }
                }
            )
        }
    }
}
