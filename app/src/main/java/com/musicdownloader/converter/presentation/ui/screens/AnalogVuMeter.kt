package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogVuMeter(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Animación de oscilación para la aguja izquierda
    val infiniteTransitionLeft = rememberInfiniteTransition(label = "vuLeft")
    val needleOscillationLeft by infiniteTransitionLeft.animateFloat(
        initialValue = -35f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscLeft"
    )

    // Animación de oscilación ligeramente desfasada para la aguja derecha (efecto estéreo real)
    val infiniteTransitionRight = rememberInfiniteTransition(label = "vuRight")
    val needleOscillationRight by infiniteTransitionRight.animateFloat(
        initialValue = -30f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscRight"
    )

    // Si está pausado, las agujas bajan al reposo (-45 grados)
    val targetLeft = if (isPlaying) needleOscillationLeft else -45f
    val targetRight = if (isPlaying) needleOscillationRight else -45f

    val animatedLeft by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "smoothLeft"
    )
    val animatedRight by animateFloatAsState(
        targetValue = targetRight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "smoothRight"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vúmetro Canal Izquierdo (L)
        SingleVuMeter(label = "CH-L", angle = animatedLeft)
        // Vúmetro Canal Derecho (R)
        SingleVuMeter(label = "CH-R", angle = animatedRight)
    }
}

@Composable
fun SingleVuMeter(label: String, angle: Float) {
    val chassisColor = androidx.compose.ui.graphics.Color(0xFFF4EEDC) // Color crema retro
    val bezelColor = androidx.compose.ui.graphics.Color(0xFF333333)
    val needleColor = androidx.compose.ui.graphics.Color(0xFFD32F2F)   // Punta roja clásica

    Canvas(modifier = Modifier.size(130.dp, 55.dp)) {
        val width = size.width
        val height = size.height

        // Fondo del medidor (Panel analógico)
        drawRoundRect(
            color = chassisColor,
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        // Marco exterior oscuro
        drawRoundRect(
            color = bezelColor,
            style = Stroke(width = 2.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Líneas de escala simuladas en el fondo
        val pivotX = width / 2f
        val pivotY = height + 40f
        val radius = height * 1.8f

        // Dibujar marcas de la escala VU
        for (i in -30..30 step 15) {
            val rad = Math.toRadians(i.toDouble())
            val startX = pivotX + ((radius - 8.dp.toPx()) * sin(rad)).toFloat()
            val startY = pivotY - ((radius - 8.dp.toPx()) * cos(rad)).toFloat()
            val endX = pivotX + (radius * sin(rad)).toFloat()
            val endY = pivotY - (radius * cos(rad)).toFloat()

            drawLine(
                color = androidx.compose.ui.graphics.Color.DarkGray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Zona roja de saturación a la derecha
        val redZoneRad = Math.toRadians(20.toDouble())
        val rStartX = pivotX + ((radius - 6.dp.toPx()) * sin(redZoneRad)).toFloat()
        val rStartY = pivotY - ((radius - 6.dp.toPx()) * cos(redZoneRad)).toFloat()
        val rEndX = pivotX + (radius * sin(redZoneRad)).toFloat()
        val rEndY = pivotY - (radius * cos(redZoneRad)).toFloat()
        drawLine(
            color = androidx.compose.ui.graphics.Color.Red,
            start = Offset(rStartX, rStartY),
            end = Offset(rEndX, rEndY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Dibujar la Aguja Dinámica
        val angleRad = Math.toRadians(angle.toDouble())
        val tipX = pivotX + (radius * sin(angleRad)).toFloat()
        val tipY = pivotY - (radius * cos(angleRad)).toFloat()

        drawLine(
            color = needleColor,
            start = Offset(pivotX, pivotY),
            end = Offset(tipX, tipY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Eje central de la aguja (Tornillo pivote negro)
        drawCircle(
            color = androidx.compose.ui.graphics.Color.Black,
            radius = 4.dp.toPx(),
            center = Offset(pivotX, pivotY)
        )
    }
}
