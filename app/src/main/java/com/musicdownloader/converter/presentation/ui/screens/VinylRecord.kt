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
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun VinylRecord(
    artwork: Bitmap?,
    isPlaying: Boolean,
    isScratching: Boolean = false,
    size: Dp = 240.dp
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

    // Contenedor principal con sombra y relieve elevado
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. CHASIS PRO (Degradado metálico dorado y textura industrial de puntos perforados)
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE8C547), // Dorado luz superior
                                Color(0xFFD5AB28), // Dorado base Technics Pro
                                Color(0xFF9E7C13)  // Sombra metálica inferior
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Canvas para el patrón texturizado pegboard, plato de aluminio, estroboscopio y LEDs de estado
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.toPx()
                    val canvasHeight = size.toPx()
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val platterRadius = canvasWidth * 0.43f

                    // Textura de puntos sutiles (Estilo panel perforado industrial)
                    val step = 24f
                    var y = 16f
                    while (y < canvasHeight) {
                        var x = 16f
                        while (x < canvasWidth) {
                            val distToCenter = hypot(x - center.x, y - center.y)
                            if (distToCenter > platterRadius + 16f) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.18f),
                                    radius = 2.1f,
                                    center = Offset(x, y)
                                )
                            }
                            x += step
                        }
                        y += step
                    }

                    // Borde exterior del plato de aluminio con relieve metálico
                    drawCircle(
                        color = Color(0xFFE0E0E0),
                        radius = platterRadius + 2f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFB0B0B0),
                        radius = platterRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF333333),
                        radius = platterRadius,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    // Anillo de puntos estroboscópicos
                    val strobeRadius = platterRadius - 7f
                    val dotCount = 52
                    for (i in 0 until dotCount) {
                        val angle = (i * 360f / dotCount) * (Math.PI / 180f)
                        val x = center.x + (strobeRadius * cos(angle)).toFloat()
                        val yPos = center.y + (strobeRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = Color.Black,
                            radius = 1.8f,
                            center = Offset(x, yPos)
                        )
                    }

                    // Luz estroboscópica roja inferior izquierda
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

                    // 💡 BOMBILLAS / LEDS DE ESTADO (Verde = Reproduciendo, Rojo = Pausa)
                    val ledX = center.x + platterRadius * 0.75f
                    val ledY = center.y - platterRadius * 0.75f

                    // Base oscura del LED
                    drawCircle(
                        color = Color.DarkGray,
                        radius = 6f,
                        center = Offset(ledX, ledY)
                    )

                    // Color de emisión inteligente según estado
                    val ledColor = if (isPlaying) Color(0xFF00E676) else Color(0xFFFF1744)
                    drawCircle(
                        color = ledColor,
                        radius = 3.8f,
                        center = Offset(ledX, ledY)
                    )
                    // Halo de brillo exterior del LED
                    drawCircle(
                        color = ledColor.copy(alpha = 0.45f),
                        radius = 7.5f,
                        center = Offset(ledX, ledY),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // 2. DISCO DE VINILO GIRATORIO (Con Picture Disc, surcos y spindle)
        val vinylSize = size * 0.78f
        Box(
            modifier = Modifier.size(vinylSize),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(vinylSize)
                    .rotate(rotationState.value),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Box(
                    modifier = Modifier.size(vinylSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Imagen de extremo a extremo (Picture Disc dinámico / Artwork)
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

                    // Surcos hiperrealistas, brillo y borde 3D
                    Canvas(modifier = Modifier.size(vinylSize)) {
                        val vSize = vinylSize.toPx()
                        val center = Offset(vSize / 2f, vSize / 2f)
                        val maxRadius = vSize / 2f

                        for (i in 4..12) {
                            val radius = maxRadius * (i / 12f)
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.18f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
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
                                    Color.Black.copy(alpha = 0.9f)
                                ),
                                center = center,
                                radius = maxRadius
                            ),
                            radius = maxRadius - 2f,
                            center = center,
                            style = Stroke(width = 12f)
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray.copy(alpha = 0.9f))
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
