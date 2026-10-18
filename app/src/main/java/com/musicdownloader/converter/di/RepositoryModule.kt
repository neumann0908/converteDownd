package com.musicdownloader.converter.di

import com.musicdownloader.converter.data.database.dao.ConversionDao
import com.musicdownloader.converter.data.database.dao.DownloadDao
import com.musicdownloader.converter.data.database.dao.TrackDao
import com.musicdownloader.converter.data.repository.ConversionRepositoryImpl
import com.musicdownloader.converter.data.repository.DownloadRepositoryImpl
import com.musicdownloader.converter.data.repository.TrackRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDownloadRepository(downloadDao: DownloadDao): DownloadRepositoryImpl {
        return DownloadRepositoryImpl(downloadDao)
    }

    @Provides
    @Singleton
    fun provideConversionRepository(conversionDao: ConversionDao): ConversionRepositoryImpl {
        return ConversionRepositoryImpl(conversionDao)
    }

    @Provides
    @Singleton
    fun provideTrackRepository(trackDao: TrackDao): TrackRepositoryImpl {
        return TrackRepositoryImpl(trackDao)
    }
}
