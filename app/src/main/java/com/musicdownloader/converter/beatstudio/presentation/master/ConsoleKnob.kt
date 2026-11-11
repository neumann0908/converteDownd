package com.musicdownloader.converter.beatstudio.presentation.master

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsoleKnob(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    unit: String = "",
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeSpan = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    val angle = -135f + (fraction * 270f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(70.dp)
    ) {
        Text(text = label, color = Color(0xFFFFD700), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val sensitivity = 0.005f
                        val newValue = (value - (dragAmount * sensitivity * rangeSpan)).coerceIn(valueRange)
                        onValueChanged(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 4f

                drawCircle(color = Color(0xFF2A2A3D), radius = radius, center = center)
                drawCircle(color = Color(0xFF161B22), radius = radius - 4f, center = center)

                rotate(angle, pivot = center) {
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(center.x, center.y - radius + 6f),
                        strokeWidth = 3f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        val displayVal = if (value > 0 && valueRange.start < 0) "+${String.format(java.util.Locale.US, "%.1f", value)}" else String.format(java.util.Locale.US, "%.1f", value)
        Text(text = "$displayVal$unit", color = Color.Cyan, fontSize = 9.sp)
    }
}
