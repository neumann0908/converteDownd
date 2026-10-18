package com.musicdownloader.converter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.musicdownloader.converter.presentation.MainScreen
import com.musicdownloader.converter.presentation.theme.MusicDownloaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicDownloaderTheme {
                MainScreen()
            }
        }
    }
}
