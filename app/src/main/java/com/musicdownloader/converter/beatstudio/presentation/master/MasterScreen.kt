package com.musicdownloader.converter.beatstudio.presentation.master

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun MasterScreen(
    viewModel: MasterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFileName by remember { mutableStateOf("MiPista_Master") }
    
    // Controles de hardware virtual (Sliders de consola)
    var inputDrive by remember { mutableStateOf(0.0f) }
    var limiterCeiling by remember { mutableStateOf(-0.3f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)) // Fondo DAW oscuro profesional estilo FL Studio
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- CABEZAL DEL MOTOR 10 ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "MOTOR 10: MASTER ENGINE", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "FL-Studio Pro Style", color = Color.Gray, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Exportación profesional, Mastering asistido y Stems", color = Color.White, fontSize = 12.sp)
                }
            }

            // --- VENTANA FLOTANTE OSCILOSCOPIO / "WAVE CANDY" ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE161B22)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Wave Candy (Master Output)", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "REAL-TIME DSP", color = Color.Gray, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF080C10), RoundedCornerShape(6.dp))
                                .padding(4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                // Cuadrícula de referencia central
                                drawLine(Color(0xFF1F2937), Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
                                drawLine(Color(0xFF1F2937), Offset(width / 2, 0f), Offset(width / 2, height), strokeWidth = 1f)

                                // Onda simulada en tiempo real tipo Wave Candy
                                val points = 80
                                val stepX = width / points
                                val pathPoints = mutableListOf<Offset>()
                                for (i in 0..points) {
                                    val x = i * stepX
                                    val angle = (i.toFloat() / points) * 4f * Math.PI.toFloat()
                                    val y = (height / 2) + kotlin.math.sin(angle + System.currentTimeMillis() * 0.006f) * (height * 0.35f) * (1f - (i % 5) * 0.05f)
                                    pathPoints.add(Offset(x, y))
                                }

                                for (i in 0 until pathPoints.size - 1) {
                                    drawLine(
                                        color = Color(0xFF00FFCC),
                                        start = pathPoints[i],
                                        end = pathPoints[i + 1],
                                        strokeWidth = 2.5f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 🎚️ INTERFAZ DE HARDWARE: MEDIDORES LED ESTÉREO Y SLIDERS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎚️ Master Output LED Meters", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "PEAK / RMS", color = Color.Gray, fontSize = 10.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Canal L
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "CH L", color = Color.Gray, fontSize = 9.sp)
                                Text(text = "${state.diagnostics.truePeakDb} dB", color = Color.Cyan, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFF0B0E14), RoundedCornerShape(3.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.75f)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E676), Color(0xFFFFEB3B), Color(0xFFFF5252))
                                            ),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                        // Canal R
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "CH R", color = Color.Gray, fontSize = 9.sp)
                                Text(text = "${state.diagnostics.truePeakDb} dB", color = Color.Cyan, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFF0B0E14), RoundedCornerShape(3.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.72f)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E676), Color(0xFFFFEB3B), Color(0xFFFF5252))
                                            ),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(text = "Input Drive (Gain): ${String.format(java.util.Locale.US, "%.1f", inputDrive)} dB", color = Color.LightGray, fontSize = 11.sp)
                    Slider(
                        value = inputDrive,
                        onValueChange = { inputDrive = it },
                        valueRange = -6.0f..12.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = Color(0xFF2A2A3D)
                        ),
                        modifier = Modifier.height(20.dp)
                    )

                    Text(text = "Limiter Ceiling: ${String.format(java.util.Locale.US, "%.1f", limiterCeiling)} dB", color = Color.LightGray, fontSize = 11.sp)
                    Slider(
                        value = limiterCeiling,
                        onValueChange = { limiterCeiling = it },
                        valueRange = -3.0f..0.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = Color(0xFF2A2A3D)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }
            }

            // --- VISTA PREVIA DE MULTI-PISTAS (PLAYLIST / STEMS) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "📁 Playlist & Stems Activos", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    val tracks = listOf(
                        Pair("Moon Halo vocal", Color(0xFFE57373)),
                        Pair("Piano & Strings Bus", Color(0xFF81C784)),
                        Pair("Guitar & Melody", Color(0xFFFFB74D)),
                        Pair("Bass & SubBass Bus", Color(0xFFBA68C8))
                    )
                    
                    tracks.forEach { (trackName, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13131A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                                Text(text = trackName, color = Color.White, fontSize = 10.sp)
                            }
                            Text(text = "Stereo • 24bit", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }

            // --- PANEL DE DIAGNÓSTICO INTELIGENTE ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🤖 Diagnóstico de Mastering", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = { viewModel.runMasteringAnalysis() }) {
                            Text("Reanalizar 🔄", color = Color.Cyan, fontSize = 11.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "LUFS Integrados", color = Color.Gray, fontSize = 10.sp)
                            Text(text = "${state.diagnostics.lufs} LUFS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "True Peak", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                text = "${state.diagnostics.truePeakDb} dB", 
                                color = if (state.diagnostics.hasClipping) Color(0xFFFF5252) else Color(0xFF00E676), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                        }
                    }

                    Divider(color = Color(0xFF3B3B4F), thickness = 1.dp)
                    Text(text = state.diagnostics.analysisMessage, color = Color.LightGray, fontSize = 11.sp)
                }
            }

            // --- CONFIGURACIÓN DE FORMATO Y CALIDAD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🎚️ Parámetros de Renderizado", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Formato
                    Text(text = "Formato de Exportación:", color = Color.Gray, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExportFormat.entries.forEach { fmt ->
                            val isSelected = state.selectedFormat == fmt
                            Button(
                                onClick = { viewModel.setFormat(fmt) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2A2A3D)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = fmt.name, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Sample Rate
                    Text(text = "Frecuencia de Muestreo (Sample Rate):", color = Color.Gray, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(SampleRate.SR_44100, SampleRate.SR_48000, SampleRate.SR_96000).forEach { sr ->
                            val isSelected = state.selectedSampleRate == sr
                            Button(
                                onClick = { viewModel.setSampleRate(sr) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2A2A3D)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "${sr.hz / 1000} kHz", color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bit Depth
                    Text(text = "Profundidad de Bits (Bit Depth):", color = Color.Gray, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(BitDepth.BIT_16, BitDepth.BIT_24, BitDepth.BIT_32).forEach { bd ->
                            val isSelected = state.selectedBitDepth == bd
                            Button(
                                onClick = { viewModel.setBitDepth(bd) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2A2A3D)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = bd.displayName, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- PROCESADORES DINÁMICOS Y STEMS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "⚙️ Procesadores y Opciones", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Normalización Automática", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = state.isNormalizationEnabled,
                            onCheckedChange = { viewModel.toggleNormalization(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Limitador Final de Picos (Brickwall)", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = state.isLimiterEnabled,
                            onCheckedChange = { viewModel.toggleLimiter(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Renderizado de Pistas Individuales (Stems)", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = state.isStemExportEnabled,
                            onCheckedChange = { viewModel.toggleStemExport(it) }
                        )
                    }
                }
            }

            // --- BOTÓN PRINCIPAL DE EXPORTACIÓN ---
            Button(
                onClick = { showExportDialog = true },
                enabled = !state.isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Text(text = "🚀 Renderizar y Exportar Master", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // --- DIÁLOGO DE NOMBRE DE EXPORTACIÓN ---
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text(text = "Exportar Master Audio", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Nombre del archivo final:", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = exportFileName,
                            onValueChange = { exportFileName = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        viewModel.executeMasteringAndExport(exportFileName) { savedFileName ->
                            coroutineScope.launch {
                                val msg = if (savedFileName.isNotBlank()) "¡Master exportado con éxito: $savedFileName!" else "Error al exportar"
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }) {
                        Text("Exportar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2C)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
