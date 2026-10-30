package com.musicdownloader.converter.presentation.screens.library

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.viewmodel.AudioFileItem
import com.musicdownloader.converter.presentation.viewmodel.LibraryViewModel
import com.musicdownloader.converter.presentation.viewmodel.PlaylistsViewModel
import java.io.File

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

    var audioToTrim by remember { mutableStateOf<AudioFileItem?>(null) }
    var startSecStr by remember { mutableStateOf("0") }
    var endSecStr by remember { mutableStateOf("30") }
    var customNameStr by remember { mutableStateOf("") }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var songNameToAdd by remember { mutableStateOf("") }
    var songPathToAdd by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadDownloadedFiles()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13131A))
    ) {
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
                    text = "MI BIBLIOTECA PRO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                IconButton(onClick = { viewModel.loadDownloadedFiles() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color(0xFFFFD700))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Buscar canción o beat...", color = Color.LightGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFFD700)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E2C),
                    unfocusedContainerColor = Color(0xFF1E1E2C),
                    disabledContainerColor = Color(0xFF1E1E2C),
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF3B3B4F),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (audioList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron audios o beats exportados.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(audioList) { audio ->
                        AudioItemRow(
                            audio = audio,
                            onPlayClick = {
                                viewModel.playerManager.playFile(audio.path, audio.name)
                            },
                            onShareClick = {
                                try {
                                    val file = File(audio.path)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Compartir Audio/Beat"))
                                    } else {
                                        Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTrimClick = {
                                audioToTrim = audio
                                startSecStr = "0"
                                endSecStr = "30"
                                customNameStr = audio.name.substringBeforeLast(".")
                            },
                            onAddToPlaylistClick = {
                                songNameToAdd = audio.name
                                songPathToAdd = audio.path
                                showPlaylistDialog = true
                            },
                            onDeleteClick = {
                                viewModel.deleteAudio(audio.path)
                            }
                        )
                    }
                }
            }
        }

        if (showPlaylistDialog) {
            AddSongToPlaylistDialog(
                songName = songNameToAdd,
                songPath = songPathToAdd,
                onDismiss = { showPlaylistDialog = false }
            )
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                containerColor = Color(0xFF1E1E2C),
                title = { Text("Error al Recortar", color = Color(0xFFFFD700)) },
                text = { Text(errorMessage ?: "", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Aceptar", color = Color(0xFFFFD700))
                    }
                }
            )
        }

        if (audioToTrim != null) {
            AlertDialog(
                onDismissRequest = { 
                    viewModel.stopPreview()
                    audioToTrim = null 
                },
                containerColor = Color(0xFF1E1E2C),
                title = { Text("Recortar Audio", color = Color(0xFFFFD700)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = audioToTrim!!.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedTextField(
                            value = customNameStr,
                            onValueChange = { customNameStr = it },
                            label = { Text("Nombre del archivo", color = Color.LightGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2B2B3D),
                                unfocusedContainerColor = Color(0xFF2B2B3D),
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF3B3B4F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFD700),
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        OutlinedTextField(
                            value = startSecStr,
                            onValueChange = { startSecStr = it },
                            label = { Text("Segundo de inicio", color = Color.LightGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2B2B3D),
                                unfocusedContainerColor = Color(0xFF2B2B3D),
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF3B3B4F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFD700),
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        OutlinedTextField(
                            value = endSecStr,
                            onValueChange = { endSecStr = it },
                            label = { Text("Segundo de fin", color = Color.LightGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2B2B3D),
                                unfocusedContainerColor = Color(0xFF2B2B3D),
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF3B3B4F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFD700),
                                unfocusedLabelColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val start = startSecStr.toIntOrNull() ?: 0
                                val end = endSecStr.toIntOrNull() ?: 30
                                audioToTrim?.let { audio ->
                                    viewModel.previewAudioSegment(audio, start, end)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B3B4F)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "▶ Previsualizar Fragmento",
                                color = Color(0xFFFFD700)
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
                        Text("Recortar", color = Color(0xFFFFD700))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        viewModel.stopPreview()
                        audioToTrim = null 
                    }) {
                        Text("Cancelar", color = Color.LightGray)
                    }
                }
            )
        }

        if (isTrimming) {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E2C)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recortando audio...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Por favor espera un momento",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
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
    onShareClick: () -> Unit,
    onTrimClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2C)
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
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = audio.name,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audio.sizeMb,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = Color(0xFFFFD700)
                )
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = Color(0xFF00E676)
                )
            }

            IconButton(onClick = onTrimClick) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Recortar",
                    tint = Color(0xFFFF9800)
                )
            }

            IconButton(onClick = onAddToPlaylistClick) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Añadir a lista",
                    tint = Color(0xFF00E676)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFE53935)
                )
            }
        }
    }
}

@Composable
fun AddSongToPlaylistDialog(
    songName: String,
    songPath: String,
    viewModel: PlaylistsViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        title = { Text("Añadir a una lista", color = Color(0xFFFFD700)) },
        text = {
            if (playlists.isEmpty()) {
                Text("No tienes listas creadas. ¡Crea una primero en la pestaña Listas!", color = Color.LightGray)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addSongToPlaylist(
                                        playlistId = playlist.id,
                                        songPath = songPath,
                                        songName = songName
                                    )
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2B2B3D)
                            )
                        ) {
                            Text(
                                text = playlist.name,
                                modifier = Modifier.padding(16.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFFFFD700))
            }
        }
    )
}
