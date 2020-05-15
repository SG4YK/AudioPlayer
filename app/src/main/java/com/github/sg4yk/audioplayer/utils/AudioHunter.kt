package com.github.sg4yk.audioplayer.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.github.sg4yk.audioplayer.entities.Audio

object AudioHunter {

    val audioList = mutableListOf<Audio>()

    private val audioProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.YEAR
    )

    fun getAllAudio(ctx: Context): MutableList<Audio> {
        ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioProjection, null, null, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            while (cursor.moveToNext()) {
                val id = idColumn.let { cursor.getLong(it) }
                val title = titleColumn.let { cursor.getStringOrNull(it) }
                val artist = artistColumn.let { cursor.getStringOrNull(it) }
                val album = albumColumn.let { cursor.getStringOrNull(it) }
                val year = yearColumn.let { cursor.getIntOrNull(it) }
                val contentUri: Uri? = id.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it
                    )
                }
                audioList += Audio(contentUri, id, title, artist, album, year)
            }
        }
        return audioList
    }
}

