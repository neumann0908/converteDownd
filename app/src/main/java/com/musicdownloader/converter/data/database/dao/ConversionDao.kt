package com.musicdownloader.converter.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicdownloader.converter.data.database.entity.ConversionTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_tasks ORDER BY createdAtMs DESC")
    fun getAllConversions(): Flow<List<ConversionTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(task: ConversionTaskEntity)

    @Query("UPDATE conversion_tasks SET status = :status, progressPercentage = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float)
}
