package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.ui.viewmodel.DownloadState
import com.musicdownloader.converter.presentation.ui.viewmodel.DownloadViewModel

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf("") }
    val downloadState by viewModel.downloadState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Descargador de Música",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Pega el enlace / URL directa del audio") },
            placeholder = { Text("https://ejemplo.com/audio.mp3") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            enabled = downloadState !is DownloadState.Downloading
        )

        Button(
            onClick = { viewModel.startDownload(urlText) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = downloadState !is DownloadState.Downloading
        ) {
            if (downloadState is DownloadState.Downloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Iniciar Descarga")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = downloadState) {
            is DownloadState.Success -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is DownloadState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> {}
        }
    }
}
