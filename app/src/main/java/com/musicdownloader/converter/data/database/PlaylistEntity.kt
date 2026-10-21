package com.musicdownloader.converter.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songPath"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songPath: String,
    val songName: String
)
