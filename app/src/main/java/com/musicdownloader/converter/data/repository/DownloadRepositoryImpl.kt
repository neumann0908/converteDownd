package com.musicdownloader.converter.data.repository

import com.musicdownloader.converter.data.database.dao.DownloadDao
import com.musicdownloader.converter.data.database.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao
) {
    val allTasks: Flow<List<DownloadTaskEntity>> = downloadDao.getAllTasks()

    suspend fun insertTask(task: DownloadTaskEntity) {
        downloadDao.insertTask(task)
    }

    suspend fun updateProgress(id: String, status: String, bytes: Long) {
        downloadDao.updateProgress(id, status, bytes)
    }

    suspend fun deleteTask(id: String) {
        downloadDao.deleteTask(id)
    }
}
