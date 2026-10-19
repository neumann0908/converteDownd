package com.musicdownloader.converter

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ConverteDownApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            // Inicializar tanto yt-dlp como FFmpeg de forma nativa
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("ConverteDownApp", "YoutubeDL y FFmpeg inicializados con éxito")
        } catch (e: YoutubeDLException) {
            Log.e("ConverteDownApp", "Error al inicializar YoutubeDL o FFmpeg", e)
        } catch (e: Exception) {
            Log.e("ConverteDownApp", "Error general de inicialización nativa", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
