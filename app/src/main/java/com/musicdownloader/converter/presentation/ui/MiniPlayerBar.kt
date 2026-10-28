package com.musicdownloader.converter.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayerBar(
    currentSongName: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit
) {
    // Si no hay ninguna canción reproduciéndose o seleccionada, se oculta el mini-player
    if (currentSongName.isNullOrEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onBarClick() },
        color = Color(0xFF1E1E2C),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSongName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = if (isPlaying) "Reproduciendo..." else "En pausa",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            // Botón de Play / Pausa rápido
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pausa",
                    tint = Color(0xFFFFD700)
                )
            }
        }
    }
}
