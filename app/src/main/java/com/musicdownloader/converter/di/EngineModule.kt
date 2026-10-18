package com.musicdownloader.converter.di

import android.content.Context
import com.musicdownloader.converter.data.engine.Media3AudioConverterEngine
import com.musicdownloader.converter.data.engine.OkHttpDownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideDownloadEngine(okHttpClient: OkHttpClient): OkHttpDownloadEngine {
        return OkHttpDownloadEngine(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAudioConverterEngine(@ApplicationContext context: Context): Media3AudioConverterEngine {
        return Media3AudioConverterEngine(context)
    }
}
