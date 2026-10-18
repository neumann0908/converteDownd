package com.musicdownloader.converter.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF003913),
    primaryContainer = Color(0xFF00521D),
    onPrimaryContainer = Color(0xFF69F49E),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE2E2E2),
    onSurface = Color(0xFFE2E2E2)
)

@Composable
fun MusicDownloaderConverterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EmeraldDarkColorScheme,
        content = content
    )
}
