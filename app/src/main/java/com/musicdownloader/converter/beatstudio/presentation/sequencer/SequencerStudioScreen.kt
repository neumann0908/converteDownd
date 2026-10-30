package com.musicdownloader.converter.beatstudio.presentation.sequencer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.beatstudio.presentation.midi.ChordType
import com.musicdownloader.converter.beatstudio.presentation.midi.MusicalScaleType
import java.io.File

@Composable
fun SequencerStudioScreen(
    viewModel: SequencerStudioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState() // 🟢 Desplazamiento global fluido para toda la pantalla

    // Estado local para editar la velocity, duración, offset, probabilidad, pitch bend y modulación mediante pulsación larga
    var stepToEdit by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (trackIndex, stepIndex)
    
    // Estado unificado para configurar kits: Pair(trackIndex, pitch)
    var activeConfigTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Estado unificado para el selector de archivos nativos: Pair(trackIndex, pitch)
    var targetPickerTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Estado para controlar el diálogo de guardado en la biblioteca
    var showSaveDialog by remember { mutableStateOf(false) }
    var songNameInput by remember { mutableStateOf("Mi_Nuevo_Beat") }

    // Lanzador exclusivo para cargar archivos de audio nativos de Android
    val nativeFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            targetPickerTarget?.let { (tIdx, pitch) ->
                viewModel.importCustomSampleForPitch(tIdx, pitch, selectedUri)
                targetPickerTarget = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
            .padding(10.dp)
            .verticalScroll(scrollState) // 🟢 Habilitamos scroll vertical en toda la vista para cero amontonamiento
    ) {
        // --- CABEZAL DE CONTROLES PRINCIPALES CON BOTÓN DE GUARDADO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2C))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SEQUENCER ENGINE PRO",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BPM: ${state.bpm.toInt()} | Compás: ${state.timeSignature} | Res: ${state.resolution}",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // 💾 Botón de Guardado en Biblioteca
                IconButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Guardar en Biblioteca",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botón Reproducir / Detener
                IconButton(
                    onClick = {
                        if (state.isPlaying) viewModel.stopPlayback() else viewModel.startPlayback()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (state.isPlaying) Color.Red else Color(0xFF00E676), CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CONTROLADOR TEMPO (BPM) Y SWING GLOBAL (GROOVE) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEMPO: ${state.bpm.toInt()} BPM",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SWING GROOVE: ${(state.swing * 100).toInt()}%",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Slider(
                    value = state.bpm,
                    onValueChange = { viewModel.updateBpm(it) },
                    valueRange = 60f..200f,
                    modifier = Modifier.height(18.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Slider(
                    value = state.swing,
                    onValueChange = { viewModel.updateSwing(it) },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.height(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CONTROLES AVANZADOS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val nextSig = when (state.timeSignature) {
                        "4/4" -> "3/4"
                        "3/4" -> "6/8"
                        else -> "4/4"
                    }
                    viewModel.updateTimeSignature(nextSig)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Compás: ${state.timeSignature}", color = Color.White, fontSize = 9.sp)
            }

            Button(
                onClick = {
                    val nextRes = when (state.resolution) {
                        "1/16" -> "1/8"
                        "1/8" -> "1/4"
                        else -> "1/16"
                    }
                    viewModel.updateResolution(nextRes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Res: ${state.resolution}", color = Color.White, fontSize = 9.sp)
            }

            Button(
                onClick = { viewModel.toggleMetronome() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isMetronomeEnabled) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = if (state.isMetronomeEnabled) "Click: ON" else "Click: OFF",
                    color = if (state.isMetronomeEnabled) Color.Black else Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CONMUTADOR DE VISTA ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.togglePianoRollMode(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!state.isPianoRollMode) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = "Batería (Steps)",
                    color = if (!state.isPianoRollMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.togglePianoRollMode(true, 3) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isPianoRollMode) Color(0xFFFFD700) else Color(0xFF1E1E2C)
                ),
                modifier = Modifier.weight(1f).height(32.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(
                    text = "Piano Roll (Melodía)",
                    color = if (state.isPianoRollMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SISTEMA DE ARREGLOS ---
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
                    Text(
                        text = "ARREGLO DE CANCIÓN",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.addSongSection(SongSectionType.VERSO) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("+ Verso", color = Color.Black, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.addSongSection(SongSectionType.ESTRIBILLO) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("+ Coro", color = Color.Black, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.arrangement.forEachIndexed { index, section ->
                        val isPlayingThisSection = state.isPlaying && state.currentSectionIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPlayingThisSection) Color(0xFF00E676) else Color(0xFF28283D))
                                .border(
                                    width = if (isPlayingThisSection) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.removeSongSection(section.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = section.name,
                                    color = if (isPlayingThisSection) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${section.repeatCount}x",
                                    color = if (isPlayingThisSection) Color.DarkGray else Color.Gray,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 🎛️ GESTOR DE PATRONES INDEPENDIENTES (A, B, C) Y COPIAR/PEGAR/DUPLICAR ---
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
                    Text(
                        text = "PATRONES Y EDICIÓN",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Herramientas Copiar / Pegar / Duplicar
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.copyCurrentPattern() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283D)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Copiar", color = Color.White, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.pasteToCurrentPattern() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283D)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Pegar", color = Color.White, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { 
                                val nextIndex = (state.currentPatternIndex + 1) % state.patterns.size
                                viewModel.duplicatePattern(nextIndex)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Duplicar", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selector visual de Patrones (A, B, C)
                Row(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.patterns.forEachIndexed { index, pattern ->
                        val isSelected = state.currentPatternIndex == index
                        Button(
                            onClick = { viewModel.selectPattern(index) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF00E676) else Color(0xFF28283D)
                            ),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(
                                text = pattern.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CONTENEDOR PRINCIPAL: STEP SEQUENCER VS PIANO ROLL ---
        if (!state.isPianoRollMode) {
            // 🟢 REJILLA DE BATERÍA CON CONTROLES AVANZADOS DE PISTA Y ALTURA CONTROLADA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp) // Altura fija ideal para desplazamiento suave dentro del scroll general
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181822))
                    .border(1.dp, Color(0xFF28283D), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.tracks) { trackIndex, track ->
                        val hasCustomSample = track.customPitchSamples.containsKey(0)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                // Encabezado de Pista con Mute, Solo, Hum, Archivos y Kits
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = track.trackName,
                                            color = Color(0xFF00E676),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // 🔇 Botón Mute
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (track.isMuted) Color.Red else Color(0xFF28283D))
                                                .clickable { viewModel.toggleTrackMute(trackIndex) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "M", color = if (track.isMuted) Color.White else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // 🎧 Botón Solo
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (track.isSolo) Color(0xFFFFD700) else Color(0xFF28283D))
                                                .clickable { viewModel.toggleTrackSolo(trackIndex) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "S", color = if (track.isSolo) Color.Black else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // 🟢 Botón de Humanización individual (Hum)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (track.humanizeEnabled) Color(0xFFFFD700) else Color(0xFF28283D))
                                                .clickable { viewModel.toggleTrackHumanize(trackIndex) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = "Hum", color = if (track.humanizeEnabled) Color.Black else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Cargar Archivo Nativo",
                                            tint = if (hasCustomSample) Color(0xFF00E676) else Color.Gray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    targetPickerTarget = Pair(trackIndex, 0)
                                                    nativeFilePickerLauncher.launch("audio/*")
                                                }
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Configurar Kit",
                                            tint = if (hasCustomSample) Color(0xFFFFD700) else Color.Gray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    activeConfigTarget = Pair(trackIndex, 0)
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Sliders de Volumen y Paneo Estéreo por Pista
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Vol: ${(track.volume * 100).toInt()}%", color = Color.LightGray, fontSize = 8.sp)
                                    Slider(
                                        value = track.volume,
                                        onValueChange = { viewModel.updateTrackVolume(trackIndex, it) },
                                        valueRange = 0.0f..1.0f,
                                        modifier = Modifier.weight(1f).height(16.dp)
                                    )

                                    Text(text = "Pan: ${if (track.pan < 0) "L${(-track.pan * 100).toInt()}" else if (track.pan > 0) "R${(track.pan * 100).toInt()}" else "C"}", color = Color.LightGray, fontSize = 8.sp)
                                    Slider(
                                        value = track.pan,
                                        onValueChange = { viewModel.updateTrackPan(trackIndex, it) },
                                        valueRange = -1.0f..1.0f,
                                        modifier = Modifier.weight(1f).height(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Pasos de la pista (Step Sequencer)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    track.steps.forEachIndexed { stepIndex, step ->
                                        val isCurrentStepPlaying = (state.currentStepIndex == stepIndex)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when {
                                                        step.isActive && isCurrentStepPlaying -> Color.White 
                                                        step.isActive -> Color(0xFF00E676).copy(alpha = 0.5f + (step.velocity * 0.5f))
                                                        isCurrentStepPlaying -> Color(0xFF3B3B4F) 
                                                        else -> Color(0xFF28283D) 
                                                    }
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onTap = { viewModel.toggleStep(trackIndex, stepIndex) },
                                                        onLongPress = { stepToEdit = Pair(trackIndex, stepIndex) }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${stepIndex + 1}",
                                                color = if (step.isActive) Color.Black else Color.Gray,
                                                fontSize = 7.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- 🎹 VISTA PIANO ROLL PROFESIONAL CON BARRA DEL MOTOR 4 ---
            val activeTrack = state.tracks.getOrNull(state.selectedPianoTrackIndex) ?: state.tracks.last()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
            ) {
                // 🎛️ BARRA DE CONTROL DEL MOTOR 4 (ESCALAS Y ACORDES)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "MOTOR 4: ESCALAS Y ACORDES",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Selector de Escalas Musicales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MusicalScaleType.entries.forEach { scale ->
                                val isSelected = state.selectedScale == scale
                                Button(
                                    onClick = { viewModel.updateMusicalScale(scale) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF00E676) else Color(0xFF28283D)
                                    ),
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        text = scale.name.take(6),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Botones de Acordes Automáticos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ChordType.entries.take(3).forEach { chord ->
                                Button(
                                    onClick = { 
                                        viewModel.applyChordToPattern(state.rootKey, chord, state.selectedPianoTrackIndex, 0)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283D)),
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        text = "+ ${chord.name.take(8)}",
                                        color = Color(0xFFFFD700),
                                        fontSize = 7.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Tarjeta del Piano Roll
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181822)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "PIANO ROLL: ${activeTrack.trackName} | Escala: ${state.selectedScale.displayName}",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val pitchNames = listOf("C5", "B4", "A4", "G4", "F4", "E4", "D4", "C4")
                            
                            items(pitchNames.size) { pitchIndex ->
                                val actualPitch = 7 - pitchIndex
                                val hasKeySample = activeTrack.customPitchSamples.containsKey(actualPitch)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.width(56.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = pitchNames[pitchIndex],
                                            color = if (hasKeySample) Color(0xFF00E676) else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = "Cargar Archivo Nativo",
                                                tint = Color(0xFF00E676),
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clickable {
                                                        targetPickerTarget = Pair(state.selectedPianoTrackIndex, actualPitch)
                                                        nativeFilePickerLauncher.launch("audio/*")
                                                    }
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Configurar Kit",
                                                tint = if (hasKeySample) Color(0xFFFFD700) else Color.Gray,
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .clickable { activeConfigTarget = Pair(state.selectedPianoTrackIndex, actualPitch) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        activeTrack.steps.forEachIndexed { stepIndex, step ->
                                            val hasNoteAtThisPitch = step.isActive && step.pitch == actualPitch
                                            val isCurrentStepPlaying = (state.currentStepIndex == stepIndex)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(24.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        when {
                                                            hasNoteAtThisPitch && isCurrentStepPlaying -> Color.White
                                                            hasNoteAtThisPitch -> Color(0xFF9C27B0)
                                                            isCurrentStepPlaying -> Color(0xFF3B3B4F)
                                                            else -> Color(0xFF22222F)
                                                        }
                                                    )
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                if (hasNoteAtThisPitch) {
                                                                    viewModel.toggleStep(state.selectedPianoTrackIndex, stepIndex)
                                                                } else {
                                                                    viewModel.updateStepPitch(state.selectedPianoTrackIndex, stepIndex, actualPitch)
                                                                }
                                                            },
                                                            onLongPress = {
                                                                stepToEdit = Pair(state.selectedPianoTrackIndex, stepIndex)
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA GUARDAR EN BIBLIOTECA ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Beat en Biblioteca", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Introduce un nombre para tu creación musical:", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = songNameInput,
                        onValueChange = { songNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSongToLibrary(songNameInput) { success ->
                            if (success) {
                                Toast.makeText(context, "¡Guardado en Biblioteca con éxito!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al guardar el beat", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
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

    // --- 🎚️ DIÁLOGO AVANZADO DE EDICIÓN DE PASO (CON PITCH BEND Y MODULACIÓN PRO) ---
    if (stepToEdit != null) {
        val (tIdx, sIdx) = stepToEdit!!
        val currentStep = state.tracks.getOrNull(tIdx)?.steps?.getOrNull(sIdx)
        var tempVelocity by remember(currentStep?.velocity) { mutableStateOf(currentStep?.velocity ?: 1.0f) }
        var tempDuration by remember(currentStep?.durationMs) { mutableStateOf(currentStep?.durationMs ?: 125L) }
        var tempOffset by remember(currentStep?.offsetMs) { mutableStateOf(currentStep?.offsetMs ?: 0L) }
        var tempProbability by remember(currentStep?.probability) { mutableStateOf(currentStep?.probability ?: 1.0f) }
        var tempPitchBend by remember(currentStep?.pitchBend) { mutableStateOf(currentStep?.pitchBend ?: 0.0f) }
        var tempModulation by remember(currentStep?.modulation) { mutableStateOf(currentStep?.modulation ?: 0.0f) }

        AlertDialog(
            onDismissRequest = { stepToEdit = null },
            title = { Text("Editor Pro de Expresividad y Paso", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Intensidad (Velocity): ${(tempVelocity * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempVelocity,
                        onValueChange = { tempVelocity = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Duración nota: ${tempDuration} ms", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempDuration.toFloat(),
                        onValueChange = { tempDuration = it.toLong() },
                        valueRange = 20f..1000f,
                        modifier = Modifier.height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Desplazamiento (Shift): ${tempOffset} ms", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempOffset.toFloat(),
                        onValueChange = { tempOffset = it.toLong() },
                        valueRange = -50f..50f,
                        modifier = Modifier.height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Probabilidad: ${(tempProbability * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = tempProbability,
                        onValueChange = { tempProbability = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // 🎚️ Control de Pitch Bend (-1.0 a 1.0)
                    Text("Pitch Bend: ${String.format("%.2f", tempPitchBend)}", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = tempPitchBend,
                        onValueChange = { tempPitchBend = it },
                        valueRange = -1.0f..1.0f,
                        modifier = Modifier.height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // 🎛️ Control de Modulation (0.0 a 1.0)
                    Text("Modulation (Vibrato): ${(tempModulation * 100).toInt()}%", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = tempModulation,
                        onValueChange = { tempModulation = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.height(22.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStepVelocity(tIdx, sIdx, tempVelocity)
                        viewModel.updateStepDuration(tIdx, sIdx, tempDuration)
                        viewModel.updateStepOffset(tIdx, sIdx, tempOffset)
                        viewModel.updateStepProbability(tIdx, sIdx, tempProbability)
                        viewModel.updateStepPitchBend(tIdx, sIdx, tempPitchBend)
                        viewModel.updateStepModulation(tIdx, sIdx, tempModulation)
                        stepToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stepToEdit = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E2C)
        )
    }

    // --- DIÁLOGO DE KITS ---
    if (activeConfigTarget != null) {
        val (tIdx, pitch) = activeConfigTarget!!
        val targetTrack = state.tracks.getOrNull(tIdx)

        AlertDialog(
            onDismissRequest = { activeConfigTarget = null },
            title = { Text("Kit para ${targetTrack?.trackName ?: "Pista"} (Pitch: $pitch)", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Elige un archivo de la carpeta de kits internos:", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val kitName = targetTrack?.trackName?.lowercase()?.replace(" ", "_") ?: "kick"
                    val kitDir = File(context.filesDir, "exports/kits/$kitName")
                    val wavFiles = if (kitDir.exists() && kitDir.isDirectory) {
                        kitDir.listFiles { file -> file.extension.lowercase() == "wav" }?.toList() ?: emptyList()
                    } else {
                        emptyList()
                    }

                    if (wavFiles.isEmpty()) {
                        Text("No hay archivos en /exports/kits/$kitName/", color = Color.Gray, fontSize = 10.sp)
                    } else {
                        LazyColumn(modifier = Modifier.height(140.dp)) {
                            items(wavFiles.size) { index ->
                                val file = wavFiles[index]
                                TextButton(
                                    onClick = {
                                        viewModel.assignSampleToPitch(tIdx, pitch, file.absolutePath)
                                        activeConfigTarget = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📁 ${file.name}", color = Color(0xFFFFD700), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeConfigTarget = null }) { Text("Cerrar", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E1E2C)
        )
    }
}
