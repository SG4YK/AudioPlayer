package com.github.sg4yk.audioplayer.media

import android.net.Uri

data class Playlist(
    val uri: Uri,
    val id: Long,
    val name: String?,
    val dateAdded: Long?,
    val dateModified: Long?
)