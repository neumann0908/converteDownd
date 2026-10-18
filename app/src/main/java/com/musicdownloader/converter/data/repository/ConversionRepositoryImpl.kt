package com.musicdownloader.converter.data.repository

import com.musicdownloader.converter.data.database.dao.ConversionDao
import com.musicdownloader.converter.data.database.entity.ConversionTaskEntity
import kotlinx.coroutines.flow.Flow

class ConversionRepositoryImpl(
    private val conversionDao: ConversionDao
) {
    val allConversions: Flow<List<ConversionTaskEntity>> = conversionDao.getAllConversions()

    suspend fun insertConversion(task: ConversionTaskEntity) {
        conversionDao.insertConversion(task)
    }

    suspend fun updateProgress(id: String, status: String, progress: Float) {
        conversionDao.updateProgress(id, status, progress)
    }
}
