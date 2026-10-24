package com.musicdownloader.converter.presentation.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
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
    isScratching: Boolean = false,
    size: Dp = 220.dp
) {
    val resolvedPrimaryColor = MaterialTheme.colorScheme.primary

    // Estado animado para la rotación fluida y precisa
    val rotationState = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            try {
                while (true) {
                    rotationState.animateTo(
                        targetValue = rotationState.value + 360f,
                        animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                    )
                }
            } catch (e: CancellationException) {}
        }
    }

    val scratchTransition = rememberInfiniteTransition(label = "ScratchEffect")
    val scratchAlpha by scratchTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScratchAlpha"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Cuerpo principal del vinilo con rotación aplicada
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
                // 1. IMAGEN DE EXTREMO A EXTREMO (Picture Disc full cover dinámico)
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "Picture Disc Artwork",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // 2. Canvas para surcos, brillo y el borde degradado mate 3D
                Canvas(modifier = Modifier.size(size)) {
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    val maxRadius = size.toPx() / 2f

                    // Surcos translúcidos del vinilo superpuestos
                    for (i in 5..15) {
                        val radius = maxRadius * (i / 15f)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1f)
                        )
                    }
                    
                    // Efecto de brillo reflejado
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius,
                        center = center
                    )

                    // Borde fino negro mate con degradado radial (Efecto Picture Disc 3D)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius - 3f,
                        center = center,
                        style = Stroke(width = 12f)
                    )

                    // ⚡ EFECTO VISUAL DE SCRATCH
                    if (isScratching) {
                        drawCircle(
                            color = Color.White.copy(alpha = scratchAlpha),
                            radius = maxRadius * 0.72f,
                            center = center,
                            style = Stroke(width = 6f)
                        )
                        drawCircle(
                            color = resolvedPrimaryColor.copy(alpha = scratchAlpha),
                            radius = maxRadius * 0.48f,
                            center = center,
                            style = Stroke(width = 4f)
                        )
                    }
                }

                // 3. Eje central clásico del disco (Perno metálico central)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray.copy(alpha = 0.85f))
                        .border(1.5f, Color.LightGray.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}
