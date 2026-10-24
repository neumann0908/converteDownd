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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.cos
import kotlin.math.sin

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

    // Contenedor principal que simula la tornamesa Technics Gold Edition
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. CHASIS DE LA TORNAMESA (Estilo Technics SL-1200 Gold Edition)
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD5AB28)) // Dorado Pro
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Canvas para el plato de aluminio, puntos estroboscópicos y luz LED roja
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.toPx()
                    val canvasHeight = size.toPx()
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val platterRadius = canvasWidth * 0.44f

                    // Borde exterior del plato de aluminio
                    drawCircle(
                        color = Color(0xFFCCCCCC),
                        radius = platterRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF4A4A4A),
                        radius = platterRadius,
                        center = center,
                        style = Stroke(width = 3f)
                    )

                    // Anillo de puntos estroboscópicos (Estilo Technics)
                    val strobeRadius = platterRadius - 8f
                    val dotCount = 48
                    for (i in 0 until dotCount) {
                        val angle = (i * 360f / dotCount) * (Math.PI / 180f)
                        val x = center.x + (strobeRadius * cos(angle)).toFloat()
                        val y = center.y + (strobeRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = Color.Black,
                            radius = 2f,
                            center = Offset(x, y)
                        )
                    }

                    // Luz estroboscópica roja clásica en la esquina inferior izquierda del plato
                    drawCircle(
                        color = Color(0xFFFF3333),
                        radius = 4f,
                        center = Offset(center.x - platterRadius * 0.72f, center.y + platterRadius * 0.72f)
                    )
                    drawCircle(
                        color = Color(0xFFFF9999),
                        radius = 2f,
                        center = Offset(center.x - platterRadius * 0.72f, center.y + platterRadius * 0.72f)
                    )
                }
            }
        }

        // 2. DISCO DE VINILO GIRATORIO (Con Picture Disc, surcos y spindle)
        val vinylSize = size * 0.82f
        Box(
            modifier = Modifier.size(vinylSize),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(vinylSize)
                    .rotate(rotationState.value),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Box(
                    modifier = Modifier.size(vinylSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Imagen de extremo a extremo (Picture Disc / Artwork)
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
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Surcos, brillo y borde 3D
                    Canvas(modifier = Modifier.size(vinylSize)) {
                        val vSize = vinylSize.toPx()
                        val center = Offset(vSize / 2f, vSize / 2f)
                        val maxRadius = vSize / 2f

                        for (i in 4..12) {
                            val radius = maxRadius * (i / 12f)
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.15f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                                center = center,
                                radius = maxRadius
                            ),
                            radius = maxRadius,
                            center = center
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                center = center,
                                radius = maxRadius
                            ),
                            radius = maxRadius - 2f,
                            center = center,
                            style = Stroke(width = 10f)
                        )

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

                    // Eje central clásico (Perno metálico)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray.copy(alpha = 0.85f))
                            .border(1.5.dp, Color.LightGray.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                    }
                }
            }
        }
    }
}
