package com.musicdownloader.converter

import android.app.Application
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConverteDownApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("ConverteDownApp", "yt-dlp y FFmpeg inicializados correctamente")
        } catch (e: YoutubeDLException) {
            Log.e("ConverteDownApp", "Fallo al inicializar youtubedl-android", e)
        }
    }
}
