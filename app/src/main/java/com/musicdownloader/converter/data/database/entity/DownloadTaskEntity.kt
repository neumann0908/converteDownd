package com.musicdownloader.converter.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val status: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localFilePath: String?,
    val createdAtMs: Long
)
