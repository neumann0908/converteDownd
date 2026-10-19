package com.musicdownloader.converter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicdownloader.converter.presentation.viewmodel.DownloadUiState
import com.musicdownloader.converter.presentation.viewmodel.DownloadViewModel

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Descargador de Música",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Pega el enlace / URL directa del audio") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = {
                if (urlText.isNotBlank()) {
                    viewModel.startDownload(urlText)
                }
            },
            enabled = uiState !is DownloadUiState.Loading && urlText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState is DownloadUiState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = "Iniciar Descarga", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is DownloadUiState.Success -> {
                Text(
                    text = "¡Descarga completada con éxito!",
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            is DownloadUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {}
        }
    }
}
