package com.musicdownloader.converter.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val context = LocalContext.current

    var audioToTrim by remember { mutableStateOf<AudioFileItem?>(null) }
    var startSecStr by remember { mutableStateOf("0") }
    var endSecStr by remember { mutableStateOf("30") }

    LaunchedEffect(Unit) {
        viewModel.loadDownloadedFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                        },
                        onDeleteClick = {
                            viewModel.deleteAudio(audio.path)
                        }
                    )
                }
            }
        }
    }

    // Diálogo para ingresar los segundos de recorte
    if (audioToTrim != null) {
        AlertDialog(
            onDismissRequest = { audioToTrim = null },
            title = { Text("Recortar Audio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = audioToTrim!!.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedTextField(
                        value = startSecStr,
                        onValueChange = { startSecStr = it },
                        label = { Text("Segundo de inicio") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endSecStr,
                        onValueChange = { endSecStr = it },
                        label = { Text("Segundo de fin") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val start = startSecStr.toIntOrNull() ?: 0
                    val end = endSecStr.toIntOrNull() ?: 30
                    audioToTrim?.let { audio ->
                        viewModel.trimAudio(audio, start, end) { success ->
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
                TextButton(onClick = { audioToTrim = null }) {
                    Text("Cancelar")
                }
            }
        )
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
