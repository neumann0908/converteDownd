package com.musicdownloader.converter.di

import android.content.Context
import androidx.room.Room
import com.musicdownloader.converter.data.database.AppDatabase
import com.musicdownloader.converter.data.database.dao.ConversionDao
import com.musicdownloader.converter.data.database.dao.DownloadDao
import com.musicdownloader.converter.data.database.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "converter_downd_db"
        ).build()
    }

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideConversionDao(database: AppDatabase): ConversionDao = database.conversionDao()

    @Provides
    fun provideTrackDao(database: AppDatabase): TrackDao = database.trackDao()
}
