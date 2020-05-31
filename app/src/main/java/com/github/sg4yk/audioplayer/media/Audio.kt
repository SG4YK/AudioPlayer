package com.github.sg4yk.audioplayer.media

import android.net.Uri

data class Audio(
    val uri: Uri,
    val id: Long,
    val title: String?,
    val artist: String?,
    val artistId: Long?,
    val album: String?,
    val albumId: Long?,
    val track: Int?,
    val year: Int?,
    val duration: Long?
)

class PlaylistAudio(
    val audio: Audio,
    val id: Long,
    val playOrder: Int?
)