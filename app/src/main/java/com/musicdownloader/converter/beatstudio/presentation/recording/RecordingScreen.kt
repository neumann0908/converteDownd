package com.musicdownloader.converter.beatstudio.presentation.recording

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
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
                // Control deslizante para ajustar los BPM en vivo
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

                // Barras de frecuencias simuladas con efecto neón basadas en el RMS actual
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barCount = 12
                    for (i in 0 until barCount) {
                        // Generar alturas dinámicas basadas en el nivel de entrada y un seno armónico
                        val dynamicFactor = if (state.isRecording || state.isMetronomeOn) {
                            ((sin((i + System.currentTimeMillis() * 0.01) * 0.5) + 1.0) * 0.5).toFloat()
                        } else 0.05f
                        
                        val barHeightFraction = (state.currentMeterLevel * 1.5f + dynamicFactor * 0.6f).coerceIn(0.1f, 1.0f)
                        
                        val barColor = when {
                            i > 9 -> Color(0xFFFF5252) // Rojo pico
                            i > 6 -> Color(0xFFFFEB3B) // Amarillo medio
                            else -> Color(0xFF00E676)  // Verde seguro / Cian neón
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
                    Toast.makeText(context, "⏹ Grabación finalizada y guardada en Biblioteca", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRecording) Color(0xFFFF5252) else Color(0xFF00E676)
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (state.isRecording) "⏹ DETENER GRABACIÓN" else "⏺ GRABAR AUDIO / VOZ (CON TEMPO)",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
