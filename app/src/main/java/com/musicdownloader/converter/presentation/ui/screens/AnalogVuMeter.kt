package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogVuMeter(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransitionLeft = rememberInfiniteTransition(label = "vuLeft")
    val needleOscillationLeft by infiniteTransitionLeft.animateFloat(
        initialValue = -30f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscLeft"
    )

    val infiniteTransitionRight = rememberInfiniteTransition(label = "vuRight")
    val needleOscillationRight by infiniteTransitionRight.animateFloat(
        initialValue = -25f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscRight"
    )

    val targetLeft = if (isPlaying) needleOscillationLeft else -40f
    val targetRight = if (isPlaying) needleOscillationRight else -40f

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
        SingleVuMeter(angle = animatedLeft)
        SingleVuMeter(angle = animatedRight)
    }
}

@Composable
fun SingleVuMeter(angle: Float) {
    val chassisColor = Color(0xFFF4EEDC)
    val bezelColor = Color(0xFF333333)
    val needleColor = Color(0xFFD32F2F)

    Canvas(modifier = Modifier.size(85.dp, 34.dp)) {
        val width = size.width
        val height = size.height

        drawRoundRect(
            color = chassisColor,
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = bezelColor,
            style = Stroke(width = 1.2.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        val pivotX = width / 2f
        val pivotY = height + 18f
        val radius = height * 1.5f

        for (i in -30..30 step 15) {
            val rad = Math.toRadians(i.toDouble())
            val startX = pivotX + ((radius - 4.dp.toPx()) * sin(rad)).toFloat()
            val startY = pivotY - ((radius - 4.dp.toPx()) * cos(rad)).toFloat()
            val endX = pivotX + (radius * sin(rad)).toFloat()
            val endY = pivotY - (radius * cos(rad)).toFloat()

            drawLine(
                color = Color.DarkGray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 0.6.dp.toPx()
            )
        }

        val redZoneRad = Math.toRadians(20.toDouble())
        val rStartX = pivotX + ((radius - 3.dp.toPx()) * sin(redZoneRad)).toFloat()
        val rStartY = pivotY - ((radius - 3.dp.toPx()) * cos(redZoneRad)).toFloat()
        val rEndX = pivotX + (radius * sin(redZoneRad)).toFloat()
        val rEndY = pivotY - (radius * cos(redZoneRad)).toFloat()
        drawLine(
            color = Color.Red,
            start = Offset(rStartX, rStartY),
            end = Offset(rEndX, rEndY),
            strokeWidth = 1.0.dp.toPx()
        )

        val angleRad = Math.toRadians(angle.toDouble())
        val tipX = pivotX + (radius * sin(angleRad)).toFloat()
        val tipY = pivotY - (radius * cos(angleRad)).toFloat()

        drawLine(
            color = needleColor,
            start = Offset(pivotX, pivotY),
            end = Offset(tipX, tipY),
            strokeWidth = 1.0.dp.toPx()
        )

        drawCircle(
            color = Color.Black,
            radius = 2.5.dp.toPx(),
            center = Offset(pivotX, pivotY)
        )
    }
}
