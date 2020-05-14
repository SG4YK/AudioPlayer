package com.github.sg4yk.audioplayer.entities

import android.net.Uri

data class Audio(
    val uri: Uri?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?
)