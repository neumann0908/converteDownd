package com.musicdownloader.converter.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_tasks")
data class ConversionTaskEntity(
    @PrimaryKey val id: String,
    val sourceFilePath: String,
    val targetFilePath: String,
    val targetFormat: String,
    val status: String,
    val progressPercentage: Float,
    val createdAtMs: Long
)
