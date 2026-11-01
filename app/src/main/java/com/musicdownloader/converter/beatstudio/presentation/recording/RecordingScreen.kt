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
            Toast.makeText(context, "⏺ Grabación iniciada...", Toast.LENGTH_SHORT).show()
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

        // --- VÚMETRO DE ENTRADA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "MEDIDOR DE NIVEL (VU METER)", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { state.currentMeterLevel },
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    color = if (state.currentMeterLevel > 0.85f) Color(0xFFFF5252) else Color(0xFF00E676),
                    trackColor = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Nivel RMS: ${(state.currentMeterLevel * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
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
                    Toast.makeText(context, "⏹ Grabación finalizada y guardada", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRecording) Color(0xFFFF5252) else Color(0xFF00E676)
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (state.isRecording) "⏹ DETENER GRABACIÓN" else "⏺ GRABAR AUDIO / VOZ",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
