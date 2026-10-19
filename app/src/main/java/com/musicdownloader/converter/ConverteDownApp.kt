package com.musicdownloader.converter

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ConverteDownApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            Log.d("ConverteDownApp", "YoutubeDL / yt-dlp inicializado con exito")
        } catch (e: YoutubeDLException) {
            Log.e("ConverteDownApp", "Error al inicializar YoutubeDL", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
