package com.musicdownloader.converter.beatstudio.presentation.master

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    val advancedState by viewModel.advancedState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFileName by remember { mutableStateOf("MiPista_Master") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)) // Fondo DAW oscuro profesional estilo FL Studio / Bitwig
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- 1. CABEZAL DEL MOTOR 10 ---
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
                        Text(text = "MOTOR 10: MASTER CONSOLE", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Analog Mixing & Stems", color = Color.Gray, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Toca cualquier canal para reproducirlo y controlarlo con las perillas", color = Color.White, fontSize = 11.sp)
                }
            }

            // --- 2. VENTANA FLOTANTE OSCILOSCOPIO / "WAVE CANDY" GENERAL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE161B22)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
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
                            Text(text = "Wave Candy (Master Output Bus)", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                                drawLine(Color(0xFF1F2937), Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
                                drawLine(Color(0xFF1F2937), Offset(width / 2, 0f), Offset(width / 2, height), strokeWidth = 1f)

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

            // --- 3. 📁 LISTA APILADA DINÁMICAMENTE DE STEMS (CON REPRODUCCIÓN AL TOCAR) ---
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
                        Text(text = "📁 Canales Inyectados de la Biblioteca", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.loadAvailableStems() }) {
                            Text("Escanear 🔄", color = Color.Cyan, fontSize = 10.sp)
                        }
                    }

                    val activeStems = advancedState.activeStems
                    if (activeStems.isEmpty()) {
                        Text(
                            text = "No se encontraron audios o grabaciones en la biblioteca.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        activeStems.forEach { stem ->
                            val isSelected = stem.id == advancedState.selectedStemId
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF2A2A3D) else Color(0xFF0F141C)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 👈 AQUÍ CONECTAMOS LA REPRODUCCIÓN INSTANTÁNEA AL TOCAR CUALQUIER CANAL
                                    .clickable { viewModel.selectAndPlayStem(stem.id) }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) Color(0xFFFFD700) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically, 
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(modifier = Modifier.size(8.dp).background(if (isSelected) Color(0xFFFFD700) else Color(0xFF00E676), RoundedCornerShape(2.dp)))
                                            Text(
                                                text = stem.name, 
                                                color = Color.White, 
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Vol: ${String.format(java.util.Locale.US, "%.1f", stem.volume)}x", color = Color.Cyan, fontSize = 9.sp)
                                            IconButton(
                                                onClick = { viewModel.toggleStemMute(stem.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text(
                                                    text = if (stem.isMuted) "M" else "S",
                                                    color = if (stem.isMuted) Color.Red else Color.Green,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Mini Wave Candy propio de este canal
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .background(Color(0xFF06080C), RoundedCornerShape(4.dp))
                                            .padding(2.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val width = size.width
                                            val height = size.height
                                            val points = 50
                                            val stepX = width / points
                                            val pathPoints = mutableListOf<Offset>()
                                            for (i in 0..points) {
                                                val x = i * stepX
                                                val y = (height / 2f) + kotlin.math.sin(i * 0.3f + stem.hashCode() + System.currentTimeMillis() * 0.003f) * (height * 0.4f) * (if (stem.isMuted) 0.1f else stem.volume)
                                                pathPoints.add(Offset(x, y))
                                            }
                                            for (i in 0 until pathPoints.size - 1) {
                                                drawLine(
                                                    color = if (stem.isMuted) Color.DarkGray else Color(0xFF00FFCC),
                                                    start = pathPoints[i],
                                                    end = pathPoints[i + 1],
                                                    strokeWidth = 2f
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. 🎛️ SECCIÓN UNIFICADA DE CONSOLA ANALÓGICA CON PERILLAS ---
            val selectedStem = advancedState.activeStems.find { it.id == advancedState.selectedStemId }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎛️ Consola de Canal: ${selectedStem?.name ?: "Ningún canal seleccionado"}", 
                            color = Color(0xFFFFD700), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(text = "ANALOG KNOBS", color = Color.Gray, fontSize = 9.sp)
                    }

                    if (selectedStem != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Perilla de Volumen / Ganancia del Stem seleccionado
                            ConsoleKnob(
                                label = "GAIN / VOL",
                                value = selectedStem.volume,
                                valueRange = 0.0f..2.0f,
                                unit = "x",
                                onValueChanged = { viewModel.updateSelectedStemVolume(it) }
                            )

                            // Perilla de Panorama (Pan L/R) del Stem seleccionado
                            ConsoleKnob(
                                label = "PAN",
                                value = selectedStem.pan,
                                valueRange = -1.0f..1.0f,
                                unit = "",
                                onValueChanged = { viewModel.updateSelectedStemPan(it) }
                            )

                            // Perilla Global de Input Drive
                            ConsoleKnob(
                                label = "DRIVE",
                                value = advancedState.inputDriveDb,
                                valueRange = -6.0f..12.0f,
                                unit = "dB",
                                onValueChanged = { viewModel.updateInputDrive(it) }
                            )

                            // Perilla Global de Limitador Ceiling
                            ConsoleKnob(
                                label = "CEILING",
                                value = advancedState.limiterCeilingDb,
                                valueRange = -3.0f..0.0f,
                                unit = "dB",
                                onValueChanged = { viewModel.updateLimiterCeiling(it) }
                            )
                        }
                    } else {
                        Text(
                            text = "Selecciona un canal en la lista superior para ajustar sus perillas.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // --- 5. ⚡ PRESETS DE EXPORTACIÓN ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "⚡ Presets de Exportación (Destino)", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    ExportPreset.entries.forEach { preset ->
                        val isSelected = advancedState.selectedPreset == preset
                        Button(
                            onClick = { viewModel.applyExportPreset(preset) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF2A2A3D)),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = preset.displayName, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isSelected) "ACTIVO" else "", color = Color.DarkGray, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // --- 6. ✂️ EXPORTACIÓN POR REGIONES ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "✂️ Exportación por Regiones", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Renderizar solo un rango de tiempo específico", color = Color.Gray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = advancedState.isRegionExportEnabled,
                            onCheckedChange = { viewModel.toggleRegionExport(it) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    if (advancedState.isRegionExportEnabled) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Inicio: ${advancedState.regionStartSec.toInt()}s", color = Color.LightGray, fontSize = 10.sp)
                                Slider(
                                    value = advancedState.regionStartSec,
                                    onValueChange = { viewModel.updateExportRegion(it, advancedState.regionEndSec) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Fin: ${advancedState.regionEndSec.toInt()}s", color = Color.LightGray, fontSize = 10.sp)
                                Slider(
                                    value = advancedState.regionEndSec,
                                    onValueChange = { viewModel.updateExportRegion(advancedState.regionStartSec, it) },
                                    valueRange = 0f..300f,
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 7. PANEL DE DIAGNÓSTICO INTELIGENTE ---
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

            // --- 8. CONFIGURACIÓN DE FORMATO Y CALIDAD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🎚️ Parámetros de Renderizado", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)

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

            // --- 9. PROCESADORES DINÁMICOS Y STEMS ---
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

            // --- 10. BOTÓN PRINCIPAL DE EXPORTACIÓN ---
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
                    Text(text = "🚀 Renderizar y Exportar Master Unificado", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // --- DIÁLOGO DE EXPORTACIÓN ---
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
