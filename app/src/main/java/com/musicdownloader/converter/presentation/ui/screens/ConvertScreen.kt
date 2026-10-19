package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen() {
    var selectedFormat by remember { mutableStateOf("MP3") }
    var expandedFormat by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Convertidor de Formatos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seleccionar Archivo de Origen:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* Seleccionar archivo local */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Examinar Archivo Local")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expandedFormat,
            onExpandedChange = { expandedFormat = !expandedFormat },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedFormat,
                onValueChange = {},
                readOnly = true,
                label = { Text("Formato de Salida") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormat) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedFormat,
                onDismissRequest = { expandedFormat = false }
            ) {
                listOf("MP3", "M4A", "AAC", "WAV", "OGG").forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format) },
                        onClick = {
                            selectedFormat = format
                            expandedFormat = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Iniciar conversión */ },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Iniciar Conversión")
        }
    }
}
