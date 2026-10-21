package com.musicdownloader.converter.presentation.screens.library

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.viewmodel.AudioFileItem
import com.musicdownloader.converter.presentation.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val audioList by viewModel.audioList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isTrimming by viewModel.isTrimming.collectAsState()
    val context = LocalContext.current

    // --- ESTADOS LOCALES PARA EL DIÁLOGO DE RECORTE ---
    var audioToTrim by remember { mutableStateOf<AudioFileItem?>(null) }
    var startSecStr by remember { mutableStateOf("0") }
    var endSecStr by remember { mutableStateOf("30") }
    var customNameStr by remember { mutableStateOf("") } // Nombre personalizado

    LaunchedEffect(Unit) {
        viewModel.loadDownloadedFiles()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Cabecera con título y botón de actualizar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mi Biblioteca",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.loadDownloadedFiles() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }

            // Campo de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Buscar canción...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Lista de canciones
            if (audioList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron canciones.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(audioList) { audio ->
                        AudioItemRow(
                            audio = audio,
                            onPlayClick = {
                                viewModel.playerManager.playFile(audio.path, audio.name)
                            },
                            onTrimClick = {
                                audioToTrim = audio
                                startSecStr = "0"
                                endSecStr = "30"
                                // Nombre por defecto sugiriendo el actual sin extensión
                                customNameStr = audio.name.substringBeforeLast(".")
                            },
                            onDeleteClick = {
                                viewModel.deleteAudio(audio.path)
                            }
                        )
                    }
                }
            }
        }

        // --- DIÁLOGO DE ERROR ---
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(errorMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        // --- DIÁLOGO PARA CONFIGURAR Y RECORTAR EL AUDIO ---
        if (audioToTrim != null) {
            AlertDialog(
                onDismissRequest = { 
                    viewModel.stopPreview()
                    audioToTrim = null 
                },
                title = { Text("Recortar Audio") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = audioToTrim!!.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Campo para el nombre personalizado
                        OutlinedTextField(
                            value = customNameStr,
                            onValueChange = { customNameStr = it },
                            label = { Text("Nombre del archivo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Segundo de Inicio
                        OutlinedTextField(
                            value = startSecStr,
                            onValueChange = { startSecStr = it },
                            label = { Text("Segundo de inicio") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Segundo de Fin
                        OutlinedTextField(
                            value = endSecStr,
                            onValueChange = { endSecStr = it },
                            label = { Text("Segundo de fin") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Botón de Previsualización
                        Button(
                            onClick = {
                                val start = startSecStr.toIntOrNull() ?: 0
                                val end = endSecStr.toIntOrNull() ?: 30
                                audioToTrim?.let { audio ->
                                    viewModel.previewAudioSegment(audio, start, end)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "▶ Previsualizar Fragmento",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = startSecStr.toIntOrNull() ?: 0
                        val end = endSecStr.toIntOrNull() ?: 30
                        viewModel.stopPreview()
                        
                        audioToTrim?.let { audio ->
                            // Invocación pasando el nombre personalizado customNameStr
                            viewModel.trimAudio(audio, start, end, customNameStr) { success ->
                                if (success) {
                                    Toast.makeText(context, "¡Audio recortado con éxito!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al recortar el audio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        audioToTrim = null
                    }) {
                        Text("Recortar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        viewModel.stopPreview()
                        audioToTrim = null 
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // --- DIÁLOGO DE CARGA DURANTE EL RECORTE ---
        if (isTrimming) {
            Dialog(
                onDismissRequest = { /* Bloquea interacciones externas */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recortando audio...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Por favor espera un momento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioItemRow(
    audio: AudioFileItem,
    onPlayClick: () -> Unit,
    onTrimClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = audio.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audio.sizeMb,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón Reproducir
            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Botón Recortar
            IconButton(onClick = onTrimClick) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Recortar",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            // Botón Eliminar
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
