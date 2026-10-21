package com.musicdownloader.converter.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musicdownloader.converter.data.database.dao.ConversionDao
import com.musicdownloader.converter.data.database.dao.DownloadDao
import com.musicdownloader.converter.data.database.dao.TrackDao
import com.musicdownloader.converter.data.database.entity.ConversionTaskEntity
import com.musicdownloader.converter.data.database.entity.DownloadTaskEntity
import com.musicdownloader.converter.data.database.entity.TrackEntity
import com.musicdownloader.converter.data.database.PlaylistEntity
import com.musicdownloader.converter.data.database.PlaylistSongEntity
import com.musicdownloader.converter.data.database.PlaylistDao

@Database(
    entities = [
        DownloadTaskEntity::class,
        ConversionTaskEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 2, // Incrementamos la versión de la BD a 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun conversionDao(): ConversionDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao // Añadimos el DAO de las playlists
}
