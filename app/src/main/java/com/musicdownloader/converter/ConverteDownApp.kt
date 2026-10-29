package com.musicdownloader.converter

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.musicdownloader.converter.converter.engine.AssetDownloader
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ConverteDownApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Scope dedicado al ciclo de vida de la aplicación para tareas asíncronas
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // 1. Inicialización de librerías multimedia (YoutubeDL y FFmpeg)
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("ConverteDownApp", "YoutubeDL y FFmpeg inicializados correctamente")
        } catch (e: YoutubeDLException) {
            Log.e("ConverteDownApp", "Error al inicializar YoutubeDL o FFmpeg", e)
        } catch (e: Exception) {
            Log.e("ConverteDownApp", "Error general de inicialización nativa", e)
        }

        // 2. Disparamos la verificación y descarga reactiva de los kits de audio (671MB)
        applicationScope.launch {
            AssetDownloader.downloadAndUnzipIfNeeded(applicationContext)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
