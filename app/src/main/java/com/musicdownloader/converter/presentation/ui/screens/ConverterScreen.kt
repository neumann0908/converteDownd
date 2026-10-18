package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConverterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Conversor de Audio (Media3)", style = MaterialTheme.typography.headlineMedium)
        Text("Selecciona el formato de salida:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("MP3") }
            OutlinedButton(onClick = {}) { Text("AAC") }
            OutlinedButton(onClick = {}) { Text("FLAC") }
        }
    }
}
