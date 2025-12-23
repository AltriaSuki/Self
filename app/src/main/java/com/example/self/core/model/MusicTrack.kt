package com.example.self.core.model

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,          // 毫秒
    val uri: Uri,
    val albumArtUri: Uri? = null
)

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: MusicTrack? = null,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false
)

enum class RepeatMode {
    OFF,        // 不循环
    ONE,        // 单曲循环
    ALL         // 列表循环
}
