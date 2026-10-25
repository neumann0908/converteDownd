package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.viewmodel.PlaylistsViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    var showAddSongDialog by remember { mutableStateOf(false) }
    var targetPlaylistIdForSong by remember { mutableStateOf<Long?>(null) }
    var songNameInput by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(pageCount = { playlists.size })

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
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mis Listas",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, bottom = 12.dp)
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
                            modifier = Modifier.size(64.dp)
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
                // 🎡 CARRUSEL 3D ESTILO PINTEREST (Cover Flow con tarjetas verticales estilizadas)
                HorizontalPager(
                    state = pagerState,
                    // Corregido para usar PaddingValues compatible con todas las versiones
                    contentPadding = PaddingValues(start = 56.dp, end = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val playlist = playlists.getOrNull(page) ?: return@HorizontalPager
                    val songs by viewModel.getSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())

                    // Cálculo matemático preciso para el efecto de escala y profundidad 3D
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.2f)
                    val alpha = 1f - (pageOffset * 0.35f).coerceIn(0f, 0.45f)

                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(0.92f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (pageOffset == 0f) 16.dp else 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF352F44)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF433C56),
                                            Color(0xFF2E273D)
                                        )
                                    )
                                )
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cabecera con nombre de la lista y acciones rápidas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = playlist.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            targetPlaylistIdForSong = playlist.id
                                            songNameInput = ""
                                            showAddSongDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Añadir canción",
                                            tint = Color(0xFFE8C547)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deletePlaylist(playlist.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar lista",
                                            tint = Color(0xFFFF5252)
                                        )
                                    }
                                }
                            }

                            // 🖼️ ICONO / ARTE CENTRAL DE LA PLAYLIST
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color(0xFF231F2E), RoundedCornerShape(20.dp))
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1B1724), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color(0xFFE8C547),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${songs.size} canciones",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )

                            Divider(
                                color = Color.White.copy(alpha = 0.15f),
                                thickness = 1.dp
                            )

                            // Listado interno de canciones con scroll vertical interno
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF231F2E).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                if (songs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Esta lista está vacía.",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        songs.forEach { song ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2A2438), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                        modifier = Modifier.size(14.dp),
                                                        tint = Color(0xFFE8C547)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = song.songName,
                                                        fontSize = 12.sp,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.removeSongFromPlaylist(playlist.id, song.songPath)
                                                    },
                                                    modifier = Modifier.size(20.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Botón inferior "+ Nueva Lista" con el tono dorado exacto
            Button(
                onClick = { 
                    newPlaylistName = ""
                    showCreateDialog = true 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD5AB28)),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .height(50.dp)
                    .width(210.dp),
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
