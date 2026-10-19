package com.musicdownloader.converter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.data.local.PlaylistDao
import com.musicdownloader.converter.data.local.PlaylistEntity
import com.musicdownloader.converter.data.local.PlaylistSongEntity
import com.musicdownloader.converter.player.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    val playerManager: AudioPlayerManager
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

    fun addSongToPlaylist(playlistId: Long, title: String, path: String, sizeMb: String) {
        viewModelScope.launch {
            playlistDao.insertSongToPlaylist(
                PlaylistSongEntity(playlistId = playlistId, title = title, path = path, sizeMb = sizeMb)
            )
        }
    }

    fun removeSongFromPlaylist(songId: Long) {
        viewModelScope.launch {
            playlistDao.deleteSongFromPlaylist(songId)
        }
    }
}
