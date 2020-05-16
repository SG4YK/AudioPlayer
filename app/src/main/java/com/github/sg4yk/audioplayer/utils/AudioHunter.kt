package com.github.sg4yk.audioplayer.utils

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.utils.PlaybackManager.audioList


@WorkerThread
object AudioHunter {

    private val audioProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.YEAR
    )

    fun getAllAudio(ctx: Context): List<Audio> {
        val audioList = mutableListOf<Audio>()
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

    private val metadataProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID
    )

    @WorkerThread
    fun getAllMetadata(ctx: Context): List<MediaMetadataCompat> {
        val metaList = mutableListOf<MediaMetadataCompat>()
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
                val metadata = MediaMetadataCompat.Builder().also {builder ->
                    builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,artist)
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,album)
                    builder.putString(MediaMetadataCompat.METADATA_KEY_YEAR,year.toString())
                    builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,id.toString())
                }.build()
                metaList += metadata

            }
        }
        return metaList
    }


    private val albumProjection = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.ALBUM,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.FIRST_YEAR,
        MediaStore.Audio.Albums.LAST_YEAR,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS
    )


    fun getAllAlbums(ctx: Context): List<Album> {
        val albumList = mutableListOf<Album>()
        ctx.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumProjection, null, null, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val firstYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)
            val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR)
            val songsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = idColumn.let { cursor.getLong(it) }
                val album = albumColumn.let { cursor.getStringOrNull(it) }
                val artist = artistColumn.let { cursor.getStringOrNull(it) }
                val firstYear = firstYearColumn.let { cursor.getIntOrNull(it) }
                val lastYear = lastYearColumn.let { cursor.getIntOrNull(it) }
                val songs = songsColumn.let { cursor.getIntOrNull(it) }
                val contentUri: Uri? = id.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        it
                    )
                }
                albumList += Album(contentUri, id, album, artist, firstYear, lastYear, songs)
            }
        }
        return albumList
    }


    fun getAlbumOfAudio(ctx: Context, audio: Audio?): Album? {
        if (audio == null || audio.album == null) {
            return null
        }

        val albumList = mutableListOf<Album>()
        val selection = "${MediaStore.Audio.Albums.ALBUM}  = ?"
        val args = arrayOf(audio.album)

        ctx.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumProjection, selection, args, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val firstYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)
            val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR)
            val songsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = idColumn.let { cursor.getLong(it) }
                val album = albumColumn.let { cursor.getStringOrNull(it) }
                val artist = artistColumn.let { cursor.getStringOrNull(it) }
                val firstYear = firstYearColumn.let { cursor.getIntOrNull(it) }
                val lastYear = lastYearColumn.let { cursor.getIntOrNull(it) }
                val songs = songsColumn.let { cursor.getIntOrNull(it) }

                val contentUri: Uri? = id.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        it
                    )
                }
                albumList += Album(contentUri, id, album, artist, firstYear, lastYear, songs)
            }
        }

        return if (albumList.isNotEmpty()) {
            albumList[0]
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAlbumArt(ctx: Context, audio: Audio?): Bitmap? {
        return if (audio?.album == null) {
            null
        } else {
            getAlbumArt(ctx, getAlbumOfAudio(ctx, audio))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAlbumArt(ctx: Context, album: Album?): Bitmap? {
        if (album?.uri == null) {
            return null
        }
        var bitmap: Bitmap? = null
        return ctx.contentResolver.loadThumbnail(album.uri, Size(300, 300), null)
    }
}