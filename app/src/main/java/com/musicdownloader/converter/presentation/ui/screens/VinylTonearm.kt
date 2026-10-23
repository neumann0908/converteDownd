package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VinylTonearm(
    isPlaying: Boolean,
    size: Dp = 220.dp
) {
    val rotationRest = 25f
    val rotationPlay = 0f

    val targetRotation = if (isPlaying) rotationPlay else rotationRest

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 600),
        label = "TonearmRotation"
    )

    Box(
        modifier = Modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
        ) {
            val canvasWidth = size.toPx()
            val canvasHeight = size.toPx()

            val pivotX = canvasWidth * 0.82f
            val pivotY = canvasHeight * 0.18f

            drawCircle(
                color = Color(0xFF333333),
                radius = 16f,
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFFCCCCCC),
                radius = 10f,
                center = Offset(pivotX, pivotY)
            )

            val endX = canvasWidth * 0.48f
            val endY = canvasHeight * 0.65f
            
            drawLine(
                color = Color(0xFFDDDDDD),
                start = Offset(pivotX, pivotY),
                end = Offset(endX, endY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF111111),
                start = Offset(endX, endY),
                end = Offset(endX - 12f, endY + 15f),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }
    }
}
