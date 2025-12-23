package com.example.self.feature_music

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.self.core.model.MusicTrack
import com.example.self.core.model.PlaybackState
import com.example.self.core.model.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                MusicViewModel(application)
            }
        }
    }
    
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks: StateFlow<List<MusicTrack>> = _tracks.asStateFlow()
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _currentPlaylist = MutableStateFlow<List<MusicTrack>>(emptyList())
    val currentPlaylist: StateFlow<List<MusicTrack>> = _currentPlaylist.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val filteredTracks: StateFlow<List<MusicTrack>> = combine(tracks, searchQuery) { trackList, query ->
        if (query.isBlank()) {
            trackList
        } else {
            trackList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private var exoPlayer: ExoPlayer? = null
    private var currentTrackIndex = 0
    
    init {
        initializePlayer()
        loadMusicFromDevice()
        startProgressUpdate()
    }
    
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            playNext()
                        }
                        Player.STATE_READY -> {
                            _playbackState.update { 
                                it.copy(duration = exoPlayer?.duration ?: 0) 
                            }
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.update { it.copy(isPlaying = isPlaying) }
                }
            })
        }
    }
    
    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.update { 
                            it.copy(currentPosition = player.currentPosition) 
                        }
                    }
                }
            }
        }
    }
    
    private fun loadMusicFromDevice() {
        viewModelScope.launch {
            val musicList = withContext(Dispatchers.IO) {
                val tracks = mutableListOf<MusicTrack>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                
                getApplication<Application>().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "未知歌曲"
                        val artist = cursor.getString(artistColumn) ?: "未知艺术家"
                        val album = cursor.getString(albumColumn) ?: "未知专辑"
                        val duration = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        
                        val albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                        
                        tracks.add(
                            MusicTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                uri = contentUri,
                                albumArtUri = albumArtUri
                            )
                        )
                    }
                }
                tracks
            }
            _tracks.value = musicList
            _currentPlaylist.value = musicList
        }
    }
    
    fun playTrack(track: MusicTrack) {
        currentTrackIndex = _currentPlaylist.value.indexOfFirst { it.id == track.id }
        if (currentTrackIndex == -1) currentTrackIndex = 0
        
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(track.uri))
            prepare()
            play()
        }
        
        _playbackState.update { 
            it.copy(
                currentTrack = track,
                currentPosition = 0,
                duration = track.duration
            )
        }
    }
    
    fun playPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (_playbackState.value.currentTrack == null && _currentPlaylist.value.isNotEmpty()) {
                    playTrack(_currentPlaylist.value.first())
                } else {
                    player.play()
                }
            }
        }
    }
    
    fun playNext() {
        if (_currentPlaylist.value.isEmpty()) return
        
        val nextIndex = if (_playbackState.value.isShuffleEnabled) {
            (0 until _currentPlaylist.value.size).random()
        } else {
            (currentTrackIndex + 1) % _currentPlaylist.value.size
        }
        
        playTrack(_currentPlaylist.value[nextIndex])
    }
    
    fun playPrevious() {
        if (_currentPlaylist.value.isEmpty()) return
        
        val prevIndex = if (currentTrackIndex > 0) {
            currentTrackIndex - 1
        } else {
            _currentPlaylist.value.size - 1
        }
        
        playTrack(_currentPlaylist.value[prevIndex])
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playbackState.update { it.copy(currentPosition = position) }
    }
    
    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }
    
    fun toggleRepeat() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        exoPlayer?.repeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun refreshMusicList() {
        loadMusicFromDevice()
    }
    
    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
