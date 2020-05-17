package com.github.sg4yk.audioplayer.media

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat

class Audio (
    val uri: Uri?,
    val id: Long?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?
)