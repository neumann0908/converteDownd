package com.musicdownloader.converter.beatstudio.presentation.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
            .background(Color(0xFF090D14)) // Fondo DAW Obsidian de Élite
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- 1. TOP DASHBOARD (ESTILO RACK HARDWARE) ---
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFFFD700), RoundedCornerShape(2.dp)))
                        Text(text = "DAW RACK // MOTOR 10 PRO", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { viewModel.loadAvailableStems() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Escanear Stems 🔄", color = Color.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- 2. RACK DE MONITOREO ANALÓGICO ("WAVE CANDY DOCK") ---
            Surface(
                color = Color(0xFF05070B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF17202F)),
                modifier = Modifier.fillMaxWidth().height(105.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp).fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "LIVE SPECTRUM ANALYZER (MASTER BUS)", color = Color(0xFF00FFCC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "DSP: 44.1kHz / 24-BIT", color = Color.Gray, fontSize = 8.sp)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF020305), RoundedCornerShape(4.dp))
                            .padding(2.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            drawLine(Color(0xFF0C131D), Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
                            val points = 60
                            val stepX = width / points
                            for (i in 0 until points) {
                                val x1 = i * stepX
                                val y1 = (height / 2f) + kotlin.math.sin(i * 0.4f + System.currentTimeMillis() * 0.005f) * (height * 0.38f)
                                val x2 = (i + 1) * stepX
                                val y2 = (height / 2f) + kotlin.math.sin((i + 1) * 0.4f + System.currentTimeMillis() * 0.005f) * (height * 0.38f)
                                drawLine(Color(0xFF00FFCC), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.5f)
                            }
                        }
                    }
                }
            }

            // --- 3. BANDEJA DE CANALES HORIZONTAL (MIXER STRIPS) ---
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "MIXER STRIPS (STEMS & LIBRARY)", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    val activeStems = advancedState.activeStems
                    if (activeStems.isEmpty()) {
                        Text(text = "No hay pistas detectadas en el sistema.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeStems.forEach { stem ->
                                val isSelected = stem.id == advancedState.selectedStemId
                                Surface(
                                    color = if (isSelected) Color(0xFF1F2B3E) else Color(0xFF070B10),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.5.dp, if (isSelected) Color(0xFFFFD700) else Color(0xFF1A2332)),
                                    modifier = Modifier
                                        .width(125.dp)
                                        .height(90.dp)
                                        .clickable { viewModel.selectAndPlayStem(stem.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(5.dp).background(if (isSelected) Color(0xFFFFD700) else Color(0xFF00E676), RoundedCornerShape(2.dp)))
                                            IconButton(
                                                onClick = { viewModel.toggleStemMute(stem.id) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Text(
                                                    text = if (stem.isMuted) "M" else "S",
                                                    color = if (stem.isMuted) Color.Red else Color.Green,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(text = stem.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "${String.format(java.util.Locale.US, "%.1f", stem.volume)}x", color = Color.Cyan, fontSize = 8.sp)
                                            Text(text = stem.sizeMb, color = Color.Gray, fontSize = 7.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. RACK DE PERILLAS VST / HARDWARE ANALÓGICO ---
            val selectedStem = advancedState.activeStems.find { it.id == advancedState.selectedStemId }
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VIRTUAL RACK: ${selectedStem?.name ?: "Ningún canal seleccionado"}",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(text = "KNOBS CONTROLLER", color = Color.Gray, fontSize = 8.sp)
                    }

                    if (selectedStem != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ConsoleKnob(
                                label = "GAIN",
                                value = selectedStem.volume,
                                valueRange = 0.0f..2.0f,
                                unit = "x",
                                onValueChanged = { viewModel.updateSelectedStemVolume(it) }
                            )
                            ConsoleKnob(
                                label = "PAN",
                                value = selectedStem.pan,
                                valueRange = -1.0f..1.0f,
                                unit = "",
                                onValueChanged = { viewModel.updateSelectedStemPan(it) }
                            )
                            ConsoleKnob(
                                label = "DRIVE",
                                value = advancedState.inputDriveDb,
                                valueRange = -6.0f..12.0f,
                                unit = "dB",
                                onValueChanged = { viewModel.updateInputDrive(it) }
                            )
                            ConsoleKnob(
                                label = "CEILING",
                                value = advancedState.limiterCeilingDb,
                                valueRange = -3.0f..0.0f,
                                unit = "dB",
                                onValueChanged = { viewModel.updateLimiterCeiling(it) }
                            )
                        }
                    } else {
                        Text(text = "Selecciona un canal en el mixer superior para activar sus potenciómetros.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    }
                }
            }

            // --- 5. PRESETS Y FORMATOS EN BLOQUES COMPACTOS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = Color(0xFF111722),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "⚡ PRESETS DESTINO", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        ExportPreset.entries.forEach { preset ->
                            val isSelected = advancedState.selectedPreset == preset
                            Button(
                                onClick = { viewModel.applyExportPreset(preset) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF1A2332)),
                                modifier = Modifier.fillMaxWidth().height(26.dp),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = preset.displayName.substringBefore(" "),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xFF111722),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "🎚️ FORMATO OUTPUT", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            ExportFormat.entries.forEach { fmt ->
                                val isSelected = state.selectedFormat == fmt
                                Button(
                                    onClick = { viewModel.setFormat(fmt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF1A2332)),
                                    modifier = Modifier.weight(1f).height(26.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = fmt.name, color = if (isSelected) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "LIMITADOR BRICKWALL", color = Color.Gray, fontSize = 8.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Activar", color = Color.White, fontSize = 8.sp)
                            Switch(
                                checked = state.isLimiterEnabled,
                                onCheckedChange = { viewModel.toggleLimiter(it) },
                                modifier = Modifier.scale(0.5f)
                            )
                        }
                    }
                }
            }

            // --- 6. PANEL DE DIAGNÓSTICO IA ---
            Surface(
                color = Color(0xFF0B0F17),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF17202F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🤖 DIAGNÓSTICO IA MASTER", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.runMasteringAnalysis() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(18.dp)) {
                            Text("Reanalizar 🔄", color = Color.Cyan, fontSize = 8.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Text(text = "LUFS: ${state.diagnostics.lufs} dB", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "True Peak: ${state.diagnostics.truePeakDb} dB", color = if (state.diagnostics.hasClipping) Color(0xFFFF5252) else Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = state.diagnostics.analysisMessage, color = Color.LightGray, fontSize = 8.sp)
                }
            }

            // --- 7. BOTÓN DE EXPORTACIÓN FINAL ---
            Button(
                onClick = { showExportDialog = true },
                enabled = !state.isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                } else {
                    Text(text = "🚀 Renderizar y Exportar Master Definitivo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // --- DIÁLOGO MODAL DE EXPORTACIÓN ---
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text(text = "Exportar Master Audio", color = Color.White, fontSize = 13.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Nombre del archivo final:", color = Color.Gray, fontSize = 10.sp)
                        OutlinedTextField(
                            value = exportFileName,
                            onValueChange = { exportFileName = it },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        viewModel.executeMasteringAndExport(exportFileName) { saved ->
                            coroutineScope.launch { snackbarHostState.showSnackbar("¡Master exportado con éxito: $saved!") }
                        }
                    }) {
                        Text("Exportar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) { Text("Cancelar", color = Color.Gray) }
                },
                containerColor = Color(0xFF111722)
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
