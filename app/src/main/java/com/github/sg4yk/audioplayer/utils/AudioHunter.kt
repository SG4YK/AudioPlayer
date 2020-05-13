package com.github.sg4yk.audioplayer.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull

object AudioHunter {
    data class Audio(
        val uri: Uri?,
        val title: String?,
        val artist: String?,
        val album: String?
    )

    val audioList = mutableListOf<Audio>()

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM
    )

    fun getAllAudios(ctx: Context): MutableList<Audio> {
        val query = ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )
        query.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.moveToNext()){
                    val idColum = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumnColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            val id  = idColum?.let { cursor.getLong(it) }
                            val title = titleColumn?.let { cursor.getStringOrNull(it) }
                            val artist = artistColumn?.let { cursor.getStringOrNull(it) }
                            val album = albumnColumn?.let { cursor.getStringOrNull(it) }
                            val contentUri: Uri? = id?.let {
                                ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    it
                                )
                            }
                            audioList += Audio(contentUri, title, artist, album)
                        }
                    }
                }
            }
        }
        return audioList
    }
}

