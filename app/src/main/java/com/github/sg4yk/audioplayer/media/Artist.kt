package com.github.sg4yk.audioplayer.media

import android.net.Uri

data class Artist(
    val uri: Uri,
    val id: Long,
    val artist: String?
)