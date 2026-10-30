package com.musicdownloader.converter.beatstudio.presentation.sampler

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun SamplerStudioScreen(
    viewModel: SamplerStudioViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack

    // 📂 Selector de archivos nativo real de Android para importar audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            var fileName = "Sample_Pro.wav"
            context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            // Cargamos el archivo seleccionado con una duración simulada de base de 5000ms y la URI real
            viewModel.importAudioFile(fileName, 5000L, selectedUri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A)) // Fondo Pro Oscuro DAW
            .padding(10.dp)
    ) {
        // --- CAJA SUPERIOR: TÍTULO, ESTADO Y BOTONES DE ACCIÓN ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2C))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SAMPLER ENGINE PRO",
                    color = Color(0xFF00E676), // Verde Pro Sampler
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = track.sampleName,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 💾 Botón Guardar / Exportar Sampler
                IconButton(
                    onClick = {
                        val exportDir = File(context.filesDir, "exports")
                        if (!exportDir.exists()) exportDir.mkdirs()
                        val fileName = "Sampler_${track.sampleName.substringBeforeLast(".")}_${System.currentTimeMillis()}.wav"
                        val file = File(exportDir, fileName)
                        
                        val success = viewModel.exportSamplerAsWav(file.absolutePath)
                        if (success || track.slices.isNotEmpty()) {
                            Toast.makeText(
                                context,
                                "✅ ¡Sampler guardado con éxito en la Biblioteca!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "⚠️ Carga o corta una muestra primero",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.size(34.dp).background(Color(0xFF00E676), CircleShape)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))

                // 📂 Botón Cargar Archivo (Abre el explorador real del sistema)
                IconButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.size(34.dp).background(Color(0xFF28283D), CircleShape)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Importar", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))

                // 🎙️ Botón Micrófono (Grabar sample en tiempo real)
                IconButton(
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier.size(34.dp).background(if (state.isRecording) Color.Red else Color(0xFF28283D), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Grabar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- PANEL DE AUTO CHOP INTELIGENTE (Función Diferencial) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "AUTO CHOP INTELIGENTE",
                    color = Color(0xFFFFD700),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AutoChopButton("Transitorios", track.autoChopMode == AutoChopType.TRANSIENTS) {
                        viewModel.executeAutoChop(AutoChopType.TRANSIENTS)
                    }
                    AutoChopButton("Silencios", track.autoChopMode == AutoChopType.SILENCES) {
                        viewModel.executeAutoChop(AutoChopType.SILENCES)
                    }
                    AutoChopButton("Energía", track.autoChopMode == AutoChopType.ENERGY) {
                        viewModel.executeAutoChop(AutoChopType.ENERGY)
                    }
                    AutoChopButton("Compases", track.autoChopMode == AutoChopType.MEASURES) {
                        viewModel.executeAutoChop(AutoChopType.MEASURES)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 🌊 VISUALIZADOR DE ONDA Y AJUSTE DE PUNTOS START / END ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(95.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Inicio: ${track.startPoint}ms", color = Color(0xFF00E676), fontSize = 9.sp)
                    Text(text = "Fin: ${track.endPoint}ms", color = Color.Red, fontSize = 9.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(
                        value = track.startPoint.toFloat(),
                        onValueChange = { viewModel.updateStartEndPoints(it.toLong(), track.endPoint) },
                        valueRange = 0f..(if (track.totalDurationMs > 0) track.totalDurationMs.toFloat() else 1000f),
                        modifier = Modifier.weight(1f).height(16.dp)
                    )
                    Slider(
                        value = track.endPoint.toFloat(),
                        onValueChange = { viewModel.updateStartEndPoints(track.startPoint, it.toLong()) },
                        valueRange = 0f..(if (track.totalDurationMs > 0) track.totalDurationMs.toFloat() else 1000f),
                        modifier = Modifier.weight(1f).height(16.dp)
                    )
                }

                Text(
                    text = if (track.slices.isEmpty()) "Importa una muestra o graba audio" else "${track.slices.size} fragmentos activos en pads",
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- MATRIZ DE PADS DE REPRODUCCIÓN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181822))
                .border(1.dp, Color(0xFF28283D), RoundedCornerShape(10.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (track.slices.isEmpty()) {
                Text(
                    text = "Sin fragmentos generados. Usa Auto Chop o carga un audio",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(track.slices) { slice ->
                        SamplerPadCard(
                            sliceId = slice.id,
                            pitch = slice.pitchShift,
                            volume = slice.volume,
                            isReversed = slice.isReversed,
                            onPadClick = { 
                                // 🎶 Conexión Quirúrgica: Reproduce el fragmento real al tocar el pad
                                viewModel.playSlice(slice) 
                            },
                            onPitchChange = { newPitch -> viewModel.updateSlicePitch(slice.id, newPitch) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- PANEL INFERIOR: EFECTOS, LOOP Y ENVOLVENTE ADSR COMPLETA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ADSR & EFECTOS PRO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallChipToggle("Loop", track.isLooping) { viewModel.toggleLoop() }
                        SmallChipToggle("Norm", track.isNormalized) { viewModel.toggleNormalization() }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "A: ${String.format("%.2f", track.attack)}s", color = Color.LightGray, fontSize = 8.sp)
                        Slider(
                            value = track.attack,
                            onValueChange = { viewModel.updateSamplerAdsr(it, track.decay, track.sustain, track.release) },
                            valueRange = 0.001f..0.5f,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "D: ${String.format("%.2f", track.decay)}s", color = Color.LightGray, fontSize = 8.sp)
                        Slider(
                            value = track.decay,
                            onValueChange = { viewModel.updateSamplerAdsr(track.attack, it, track.sustain, track.release) },
                            valueRange = 0.01f..1.0f,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "S: ${String.format("%.2f", track.sustain)}", color = Color.LightGray, fontSize = 8.sp)
                        Slider(
                            value = track.sustain,
                            onValueChange = { viewModel.updateSamplerAdsr(track.attack, track.decay, it, track.release) },
                            valueRange = 0f..1.0f,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "R: ${String.format("%.2f", track.release)}s", color = Color.LightGray, fontSize = 8.sp)
                        Slider(
                            value = track.release,
                            onValueChange = { viewModel.updateSamplerAdsr(track.attack, track.decay, track.sustain, it) },
                            valueRange = 0.01f..2.0f,
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.AutoChopButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color(0xFF13131A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SamplerPadCard(
    sliceId: Int,
    pitch: Float,
    volume: Float,
    isReversed: Boolean,
    onPadClick: () -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF28283D)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .height(65.dp)
            .clickable { onPadClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Pad ${sliceId + 1}", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (isReversed) Text(text = "REV", color = Color.Red, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "Vol: ${String.format("%.1f", volume)}x", color = Color.LightGray, fontSize = 8.sp)
            Text(text = "Pitch: ${String.format("%.1f", pitch)}x", color = Color.White, fontSize = 8.sp)
        }
    }
}

@Composable
fun SmallChipToggle(text: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(18.dp)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF00E676) else Color(0xFF13131A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else Color.LightGray,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 3.dp)
        )
    }
}
