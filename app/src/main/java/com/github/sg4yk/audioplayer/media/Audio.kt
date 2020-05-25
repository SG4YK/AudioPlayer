package com.github.sg4yk.audioplayer.media

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat

data class Audio(
    val uri: Uri,
    val id: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumId: Long?,
    val year: Int?,
    val duration: Long?
)