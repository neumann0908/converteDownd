package com.musicdownloader.converter.presentation.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

@Composable
fun VinylRecord(
    artwork: Bitmap?,
    isPlaying: Boolean,
    size: Dp = 220.dp
) {
    // Estado animado para la rotación fluida y precisa
    val rotationState = remember { Animatable(0f) }

    // Controlamos la rotación continua solo cuando está reproduciendo
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            try {
                while (true) {
                    rotationState.animateTo(
                        targetValue = rotationState.value + 360f,
                        animationSpec = tween(durationMillis = 4000, easing = LinearEasing) // 4 segundos por vuelta
                    )
                }
            } catch (e: CancellationException) {
                // Se cancela de forma limpia al pausar o hacer scratch
            }
        }
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Cuerpo principal del vinilo (disco negro con rotación aplicada)
        Card(
            modifier = Modifier
                .size(size)
                .rotate(rotationState.value),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                // Dibujo de surcos concéntricos con Canvas
                Canvas(modifier = Modifier.size(size)) {
                    val center = androidx.compose.ui.geometry.Offset(size.toPx() / 2f, size.toPx() / 2f)
                    val maxRadius = size.toPx() / 2f

                    for (i in 5..15) {
                        val radius = maxRadius * (i / 15f)
                        drawCircle(
                            color = Color(0xFF222222),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }
                    
                    // Efecto de brillo reflejado en el vinilo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius,
                        center = center
                    )
                }

                // Etiqueta central del vinilo (Carátula del álbum)
                Card(
                    modifier = Modifier.size(size * 0.38f),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        modifier = Modifier.size(size * 0.38f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = "Carátula del vinilo",
                                modifier = Modifier
                                    .size(size * 0.38f)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Agujero central del vinilo
                Card(
                    modifier = Modifier.size(14.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {}
            }
        }
    }
}
