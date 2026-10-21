package com.musicdownloader.converter.di

import android.content.Context
import androidx.room.Room
import com.musicdownloader.converter.data.database.AppDatabase
import com.musicdownloader.converter.data.database.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Usamos Room.databaseBuilder con fallbackToDestructiveMigration para 
        // manejar de forma segura el salto de la versión 1 a la versión 2.
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "music_converter_database" // Asegúrate de que este sea el nombre exacto de tu archivo de base de datos
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }
}
