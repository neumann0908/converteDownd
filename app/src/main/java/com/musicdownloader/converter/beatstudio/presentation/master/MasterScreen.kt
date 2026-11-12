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
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var exportFileName by remember { mutableStateOf("MiPista_Master") }
    var projectNameInput by remember { mutableStateOf("Sesion_Pro_1") }

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
            // --- 1. TOP DASHBOARD (ESTILO RACK HARDWARE & PROYECTOS) ---
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(Color(0xFFFFD700), RoundedCornerShape(2.dp)))
                            Text(text = "DAW RACK // ARRANGEMENT VIEW", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { viewModel.loadAvailableStems() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Escanear 🔄", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showSaveProjectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("💾 Guardar", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- 2. BIBLIOTECA DE AUDIOS ESCANEADOS ---
            Surface(
                color = Color(0xFF0E131D),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1A2332)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "📁 BIBLIOTECA DE AUDIOS (Toca para añadir pista al timeline)", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    val libraryStems = advancedState.availableLibraryStems
                    if (libraryStems.isEmpty()) {
                        Text(text = "Pulsa 'Escanear' arriba para buscar audios.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            libraryStems.forEach { stem ->
                                Surface(
                                    color = Color(0xFF161F2E),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2B3A52)),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(65.dp)
                                        .clickable { viewModel.addStemToSession(stem.id) }
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = stem.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = stem.sizeMb, color = Color.Gray, fontSize = 7.sp)
                                            Text(text = "➕ Añadir", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. EDITOR DE LÍNEA DE TIEMPO MULTITRACK VERTICAL (ARRANGEMENT VIEW) ---
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎚️ LÍNEA DE TIEMPO (PISTAS VERTICALES)", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        
                        Button(
                            onClick = { viewModel.togglePlayAllChannels() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (advancedState.isMultiPlayActive) Color(0xFFFF5252) else Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (advancedState.isMultiPlayActive) "⏹ Detener Sesión" else "▶ Reproducir Timeline",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Regleta de Tiempo Superior (Timeline Ruler)
                    Surface(
                        color = Color(0xFF070B10),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "0.0s (Inicio)", color = Color.Cyan, fontSize = 8.sp)
                            Text(text = "Pos: ${(state.currentPosition / 1000f).let { String.format(java.util.Locale.US, "%.1fs", it) }}", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(text = "60.0s+", color = Color.Gray, fontSize = 8.sp)
                        }
                    }

                    val sessionStems = advancedState.activeSessionStems
                    if (sessionStems.isEmpty()) {
                        Text(text = "No hay pistas en el timeline. Añade audios desde la biblioteca superior.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    } else {
                        // Apilado vertical de pistas (Vertical Multitrack Tracks)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            sessionStems.forEach { stem ->
                                val isSelected = stem.id == advancedState.selectedStemId
                                Surface(
                                    color = if (isSelected) Color(0xFF1F2B3E) else Color(0xFF070B10),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.5.dp, if (isSelected) Color(0xFFFFD700) else Color(0xFF1A2332)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectAndPlayStem(stem.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Cabecera de la pista
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(6.dp).background(if (isSelected) Color(0xFFFFD700) else Color(0xFF00E676), RoundedCornerShape(2.dp)))
                                                Text(text = stem.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.toggleStemMute(stem.id) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Text(
                                                        text = if (stem.isMuted) "M" else "S",
                                                        color = if (stem.isMuted) Color.Red else Color.Green,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeStemFromSession(stem.id) },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Text("❌", fontSize = 8.sp)
                                                }
                                            }
                                        }

                                        // Controles de volumen, panorama y desplazamiento temporal (Start Time)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Vol: ${String.format(java.util.Locale.US, "%.1f", stem.volume)}x", color = Color.Cyan, fontSize = 8.sp)
                                            Text(text = "Pan: ${String.format(java.util.Locale.US, "%.1f", stem.pan)}", color = Color.Gray, fontSize = 8.sp)
                                            Text(text = "Inicio: ${(stem.startTimeMs / 1000f).let { String.format(java.util.Locale.US, "%.1fs", it) }}", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Slider para posicionar el inicio del audio de forma milimétrica en el timeline (0s a 30s)
                                        Slider(
                                            value = stem.startTimeMs.toFloat(),
                                            onValueChange = { viewModel.updateStemStartTime(stem.id, it.toLong()) },
                                            valueRange = 0f..30000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFFFD700),
                                                activeTrackColor = Color(0xFFFFD700),
                                                inactiveTrackColor = Color(0xFF1A2332)
                                            ),
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. RACK DE PERILLAS VST / HARDWARE ANALÓGICO (CANAL SELECCIONADO) ---
            val selectedStem = advancedState.activeSessionStems.find { it.id == advancedState.selectedStemId }
            Surface(
                color = Color(0xFF111722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VIRTUAL RACK: ${selectedStem?.name ?: "Ningún canal enfocado"}",
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
                        Text(text = "Selecciona una pista vertical para manipular sus potenciómetros VST.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
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

        // --- DIÁLOGO MODAL DE GUARDAR SESIÓN / PROYECTO ---
        if (showSaveProjectDialog) {
            AlertDialog(
                onDismissRequest = { showSaveProjectDialog = false },
                title = { Text(text = "Guardar Sesión de Ingeniería", color = Color.White, fontSize = 13.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Nombre del proyecto DAW:", color = Color.Gray, fontSize = 10.sp)
                        OutlinedTextField(
                            value = projectNameInput,
                            onValueChange = { projectNameInput = it },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSaveProjectDialog = false
                        viewModel.saveStudioSessionProject(projectNameInput) { success ->
                            coroutineScope.launch {
                                val msg = if (success) "¡Sesión guardada en biblioteca!" else "Error al guardar sesión"
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }) {
                        Text("Guardar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveProjectDialog = false }) { Text("Cancelar", color = Color.Gray) }
                },
                containerColor = Color(0xFF111722)
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
