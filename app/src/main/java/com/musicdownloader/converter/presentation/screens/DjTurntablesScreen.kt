package com.musicdownloader.converter.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musicdownloader.converter.presentation.viewmodel.DjViewModel

@Composable
fun DjTurntablesScreen(viewModel: DjViewModel) {
    val isPlayingA by viewModel.isPlayingDeckA.collectAsState()
    val isPlayingB by viewModel.isPlayingDeckB.collectAsState()

    val infiniteTransitionA = rememberInfiniteTransition(label = "deckA")
    val angleA by infiniteTransitionA.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationA"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text("🎚️ DJ PRO CONSOLE 🎛️", style = MaterialTheme.typography.headlineMedium, color = Color.Cyan)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TurntableDeck(
                title = "Deck A",
                isPlaying = isPlayingA,
                rotationAngle = if (isPlayingA) angleA else 0f,
                onTogglePlay = { viewModel.toggleDeckA() }
            )

            TurntableDeck(
                title = "Deck B",
                isPlaying = isPlayingB,
                rotationAngle = if (isPlayingB) angleA else 0f,
                onTogglePlay = { viewModel.toggleDeckB() }
            )
        }
    }
}

@Composable
fun TurntableDeck(
    title: String,
    isPlaying: Boolean,
    rotationAngle: Float,
    onTogglePlay: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(rotationAngle)) {
                drawCircle(color = Color.Black)
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 3)
                drawCircle(color = Color.Red, radius = size.minDimension / 10)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Red else Color.Green),
            onClick = onTogglePlay
        ) {
            Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.White)
        }
    }
}
