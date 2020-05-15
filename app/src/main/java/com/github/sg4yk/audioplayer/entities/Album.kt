package com.github.sg4yk.audioplayer.entities

import android.net.Uri

data class Album(
    val uri: Uri?,
    val id: Long?,
    val album: String?,
    val artist: String?,
    val firstYear: Int?,
    val lastYear: Int?,
    val songs: Int?
)