package com.musicdownloader.converter.data.repository

import com.musicdownloader.converter.data.database.dao.TrackDao
import com.musicdownloader.converter.data.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

class TrackRepositoryImpl(
    private val trackDao: TrackDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    suspend fun insertTrack(track: TrackEntity) {
        trackDao.insertTrack(track)
    }

    suspend fun deleteTrack(id: String) {
        trackDao.deleteTrack(id)
    }
}
