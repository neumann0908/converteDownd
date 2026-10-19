package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.data.local.PlaylistEntity
import com.musicdownloader.converter.presentation.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var expandedPlaylistId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Título Centrado en el contenedor superior
            Text(
                text = "Mis Listas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes listas creadas aún.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 70.dp) // Espacio para el botón flotante inferior
                ) {
                    items(playlists) { playlist ->
                        val isExpanded = expandedPlaylistId == playlist.id
                        CustomPlaylistCard(
                            playlist = playlist,
                            isExpanded = isExpanded,
                            viewModel = viewModel,
                            onCardClick = {
                                expandedPlaylistId = if (isExpanded) null else playlist.id
                            },
                            onDeletePlaylist = {
                                viewModel.deletePlaylist(playlist.id)
                                if (isExpanded) expandedPlaylistId = null
                            }
                        )
                    }
                }
            }
        }

        // Botón "Nueva Lista" en la parte inferior izquierda (arriba de Player)
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = Color(0xFF1E88E5), // Azul moderno
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Lista")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nueva Lista",
                    fontWeight = FontWeight.Bold
                )
            }
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
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
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
}

@Composable
fun CustomPlaylistCard(
    playlist: PlaylistEntity,
    isExpanded: Boolean,
    viewModel: PlaylistViewModel,
    onCardClick: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    val songs by viewModel.getSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())

    // Definición de Colores para el Card Bicolor
    val headerBlue = Color(0xFF1565C0) // Azul para la parte superior
    val bodyGreen = Color(0xFF2E7D32)  // Verde para la parte inferior
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = cardShape)
            .clickable { onCardClick() },
        shape = cardShape,
        border = BorderStroke(2.dp, Color.Black) // Borde negro sólido
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // PARTE SUPERIOR (Encabezado - Color Azul)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBlue)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = playlist.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${songs.size} canciones",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDeletePlaylist) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar lista",
                            tint = Color(0xFFFF8A80) // Rojo claro visible en fondo azul
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir",
                        tint = Color.White
                    )
                }
            }

            // PARTE INFERIOR (Cuerpo con Canciones - Color Verde)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bodyGreen)
                        .padding(16.dp)
                ) {
                    Divider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))

                    if (songs.isEmpty()) {
                        Text(
                            text = "Esta lista está vacía.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 8.dp)
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.playerManager.playFile(song.path, song.title)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.removeSongFromPlaylist(song.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar canción",
                                        tint = Color(0xFFFF8A80)
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
