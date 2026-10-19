package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class HistoryEntry(val date: String, val action: String, val status: String)

@Composable
fun HistoryScreen() {
    val history = listOf(
        HistoryEntry("Hoy, 18:30", "Descarga: Song_01.mp3", "Completado"),
        HistoryEntry("Ayer, 14:15", "Conversión: Video.mp4 -> MP3", "Completado"),
        HistoryEntry("15 Oct, 10:00", "Descarga: Track_02.wav", "Fallido")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de Actividades",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { item ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = item.action, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.date, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.status == "Completado") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
