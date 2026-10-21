package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.data.database.PlaylistDao
import com.musicdownloader.converter.data.database.PlaylistEntity
import com.musicdownloader.converter.data.database.PlaylistSongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    val playlistDao: PlaylistDao
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlistId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    fun addSongToPlaylist(playlistId: Long, songPath: String, songName: String) {
        viewModelScope.launch {
            playlistDao.addSongToPlaylist(
                PlaylistSongEntity(playlistId = playlistId, songPath = songPath, songName = songName)
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songPath)
        }
    }
}
