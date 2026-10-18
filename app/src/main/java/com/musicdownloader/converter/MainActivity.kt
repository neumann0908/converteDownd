package com.musicdownloader.converter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.musicdownloader.converter.presentation.ui.navigation.MainScreenNavGraph
import com.musicdownloader.converter.presentation.ui.theme.MusicDownloaderConverterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicDownloaderConverterTheme {
                MainScreenNavGraph()
            }
        }
    }
}
