package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.viewmodel.PlaylistsViewModel

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    var expandedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showAddSongDialog by remember { mutableStateOf(false) }
    var targetPlaylistIdForSong by remember { mutableStateOf<Long?>(null) }
    var songNameInput by remember { mutableStateOf("") }

    // Fondo atmosférico Hi-Fi en armonía con la tornamesa
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A2438),
                        Color(0xFF1A1625),
                        Color(0xFF0F0D14)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mis Listas",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tienes listas creadas aún.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(playlists) { playlist ->
                        val isExpanded = expandedPlaylistId == playlist.id
                        val songs by viewModel.getSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedPlaylistId = if (isExpanded) null else playlist.id
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF352F44)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFF231F2E), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = null,
                                                tint = Color(0xFFE8C547) // Acento dorado pro
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = playlist.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${songs.size} canciones",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            targetPlaylistIdForSong = playlist.id
                                            songNameInput = ""
                                            showAddSongDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = "Añadir canción",
                                                tint = Color(0xFFE8C547)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar lista",
                                                tint = Color(0xFFFF5252)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF2A2438).copy(alpha = 0.6f))
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            color = Color.White.copy(alpha = 0.15f)
                                        )
                                        
                                        if (songs.isEmpty()) {
                                            Text(
                                                text = "Esta lista está vacía.",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            songs.forEach { song ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MusicNote,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = Color(0xFFE8C547)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = song.songName,
                                                            fontSize = 14.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.removeSongFromPlaylist(playlist.id, song.songPath)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.RemoveCircleOutline,
                                                            contentDescription = "Quitar canción",
                                                            tint = Color(0xFFFF5252)
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
                }
            }
        }

        // Botón flotante "+ Nueva Lista" con acento dorado
        Button(
            onClick = { 
                newPlaylistName = ""
                showCreateDialog = true 
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD5AB28)),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp)
                .height(50.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Nueva Lista",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nueva Lista") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName)
                        showCreateDialog = false
                    }
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddSongDialog) {
        AlertDialog(
            onDismissRequest = { showAddSongDialog = false },
            title = { Text("Añadir Canción") },
            text = {
                OutlinedTextField(
                    value = songNameInput,
                    onValueChange = { songNameInput = it },
                    label = { Text("Nombre de la canción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (songNameInput.isNotBlank() && targetPlaylistIdForSong != null) {
                        viewModel.addSongToPlaylist(
                            playlistId = targetPlaylistIdForSong!!,
                            songPath = "path_${System.currentTimeMillis()}",
                            songName = songNameInput
                        )
                    }
                    showAddSongDialog = false
                    songNameInput = ""
                }) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
