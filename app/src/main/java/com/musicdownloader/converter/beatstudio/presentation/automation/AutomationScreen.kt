package com.musicdownloader.converter.beatstudio.presentation.automation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    // Obtenemos la duración real directamente del reproductor, con respaldo de 3 minutos si está vacía
    val playerDuration by viewModel.playerManager.duration.collectAsState()
    val totalDurationMs = if (playerDuration > 0L) playerDuration else 180000L

    // Extracción limpia del estado gestionado por el bucle de alta frecuencia del ViewModel
    val playbackTimeMs = state.playbackTimeMs
    val currentLane = state.currentLane
    val selectedParameter = state.selectedParameter
    val currentAutomationValue = state.currentAutomationValue

    // --- ESTADOS PARA DIÁLOGOS DE AUDIO Y PRESETS JSON ---
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showViewPresetsDialog by remember { mutableStateOf(false) }
    
    var presetNameInput by remember { mutableStateOf("Mix_Motor9_${System.currentTimeMillis().toString().takeLast(4)}") }
    var presetNameToSave by remember { mutableStateOf("Preset_${System.currentTimeMillis().toString().takeLast(4)}") }
    var savedPresetsList by remember { mutableStateOf(listOf<String>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- ESTADO DE SCROLL VERTICAL FLUIDO ---
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
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
                    Text(
                        text = "Editando parámetro: ${selectedParameter.displayName} (Valor actual: ${String.format("%.2f", currentAutomationValue)})", 
                        color = Color.White, 
                        fontSize = 12.sp
                    )
                }
            }

            // --- SELECTOR DE PARÁMETROS PROFESIONALES ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(AutomationParameterType.entries) { param ->
                    val isSelected = selectedParameter == param
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

            // --- LIENZO TÁCTIL Y DE RENDERIZADO ---
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
                        .pointerInput(selectedParameter) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()

                                if (canvasWidth > 0f && canvasHeight > 0f) {
                                    val timeMs = (offset.x / canvasWidth) * totalDurationMs
                                    val value = 1f - (offset.y / canvasHeight)
                                    viewModel.addOrUpdatePoint(timeMs.toLong(), canvasWidth, totalDurationMs, value.coerceIn(0f, 1f))
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Cuadrícula horizontal
                        for (i in 0..4) {
                            val y = (height / 4) * i
                            drawLine(
                                color = Color(0xFF3B3B4F),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Cuadrícula vertical (6 divisiones / compases)
                        for (j in 0..6) {
                            val x = (width / 6) * j
                            drawLine(
                                color = Color(0xFF3B3B4F),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f
                            )
                        }

                        // Curvas y puntos del carril actual (con soporte Linear y Step)
                        val points = currentLane.points
                        if (points.isNotEmpty()) {
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]

                                val x1 = (p1.timeMs.toFloat() / totalDurationMs) * width
                                val y1 = (1f - p1.value) * height
                                val x2 = (p2.timeMs.toFloat() / totalDurationMs) * width
                                val y2 = (1f - p2.value) * height

                                if (currentLane.interpolationMode == InterpolationMode.LINEAR) {
                                    drawLine(
                                        color = Color(0xFFFFD700),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = 3f
                                    )
                                } else {
                                    drawLine(
                                        color = Color(0xFFFFD700),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y1),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color(0xFFFFD700),
                                        start = Offset(x2, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = 3f
                                    )
                                }
                            }

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

                        // --- CABEZAL DE REPRODUCCIÓN (PLAYHEAD) ADAPTATIVO ---
                        val playheadX = (playbackTimeMs.toFloat() / totalDurationMs) * width
                        drawLine(
                            color = Color.White,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, height),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            // --- FILA DE BOTONES DE HISTORIAL (UNDO / REDO) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val canUndo = currentLane.undoStack.isNotEmpty()
                Button(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A3D),
                        disabledContainerColor = Color(0xFF1B1B26)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(
                        text = "↶ Deshacer", 
                        color = if (canUndo) Color(0xFFFFD700) else Color.Gray, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 11.sp
                    )
                }

                val canRedo = currentLane.redoStack.isNotEmpty()
                Button(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A3D),
                        disabledContainerColor = Color(0xFF1B1B26)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(
                        text = "↷ Rehacer", 
                        color = if (canRedo) Color(0xFFFFD700) else Color.Gray, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 11.sp
                    )
                }
            }

            // --- BOTÓN DE ALTERNANCIA DE SNAP-TO-GRID (AJUSTE MAGNÉTICO) ---
            val snapText = if (currentLane.isSnapToGridEnabled) {
                "Ajuste Magnético (Snap): Activado 🧲"
            } else {
                "Ajuste Magnético (Snap): Desactivado 🔓"
            }
            
            Button(
                onClick = { viewModel.toggleSnapToGrid() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3D)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text(text = snapText, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            // --- BOTÓN DE ALTERNANCIA DE MODO (LINEAL / STEP) ---
            val modeText = if (currentLane.interpolationMode == InterpolationMode.LINEAR) {
                "Modo: Rampa Lineal 📈"
            } else {
                "Modo: Escalón (Step) 🧱"
            }
            
            Button(
                onClick = { viewModel.toggleInterpolationMode() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3D)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text(text = modeText, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            // --- NUEVO: FILA CON LOS DOS BOTONES DE PRESETS JSON ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSavePresetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(text = "💾 Guardar Preset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { 
                        savedPresetsList = viewModel.getSavedPresetsList()
                        showViewPresetsDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(text = "👁️ Ver Presets", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // --- BOTÓN DE GUARDAR AUDIO EN BIBLIOTECA ---
            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(text = "💾 Guardar Audio en Biblioteca", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

            // --- ESPACIADOR INFERIOR PARA EVITAR SOLAPAMIENTO CON LA BARRA DE NAVEGACIÓN ---
            Spacer(modifier = Modifier.height(48.dp))
        }

        // --- DIÁLOGO 1: GUARDAR AUDIO EN BIBLIOTECA ---
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(text = "Guardar en Biblioteca", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Introduce el nombre para tu archivo de audio:", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = presetNameInput,
                            onValueChange = { presetNameInput = it },
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
                        showSaveDialog = false
                        viewModel.saveMixToLibrary(presetNameInput) { fileName ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡Audio guardado exitosamente: $fileName!")
                            }
                        }
                    }) {
                        Text("Guardar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2C)
            )
        }

        // --- DIÁLOGO 2: GUARDAR PRESET JSON ---
        if (showSavePresetDialog) {
            AlertDialog(
                onDismissRequest = { showSavePresetDialog = false },
                title = { Text(text = "Guardar Preset de Automatización", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Introduce el nombre del preset:", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = presetNameToSave,
                            onValueChange = { presetNameToSave = it },
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
                        showSavePresetDialog = false
                        viewModel.savePreset(presetNameToSave) { success ->
                            coroutineScope.launch {
                                val msg = if (success) "¡Preset guardado con éxito!" else "Error al guardar preset"
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }) {
                        Text("Guardar", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSavePresetDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2C)
            )
        }

        // --- DIÁLOGO 3: VISUALIZAR Y CARGAR PRESETS JSON GUARDADOS ---
        if (showViewPresetsDialog) {
            AlertDialog(
                onDismissRequest = { showViewPresetsDialog = false },
                title = { Text(text = "Presets Guardados", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedPresetsList.isEmpty()) {
                            Text(text = "No hay presets guardados todavía.", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Text(text = "Pulsa sobre un preset para cargarlo:", color = Color.Gray, fontSize = 11.sp)
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(savedPresetsList) { presetName ->
                                    Surface(
                                        onClick = {
                                            showViewPresetsDialog = false
                                            viewModel.loadPreset(presetName) { success ->
                                                coroutineScope.launch {
                                                    val msg = if (success) "¡Preset '$presetName' cargado!" else "Error al cargar"
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        },
                                        color = Color(0xFF2A2A3D),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📄 $presetName",
                                            color = Color(0xFFFFD700),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showViewPresetsDialog = false }) {
                        Text("Cerrar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2C)
            )
        }

        // --- HOST DE SNACKBAR PARA NOTIFICACIONES ---
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
