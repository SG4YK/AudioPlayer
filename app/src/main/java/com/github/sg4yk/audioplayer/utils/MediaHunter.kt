package com.github.sg4yk.audioplayer.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.github.sg4yk.audioplayer.media.Album
import com.github.sg4yk.audioplayer.media.Artist
import com.github.sg4yk.audioplayer.media.Audio
import com.github.sg4yk.audioplayer.media.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext


@WorkerThread
object MediaHunter {

    const val VOLUME_EXTERNAL = "external"

    const val ALBUM_NOT_EXIST = -1L

    val ALBUM_ART_ROOT = Uri.parse("content://media/external/audio/albumart")

    private val cachedIdMapping: MutableMap<Long, Long> = mutableMapOf()

    private val audioProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DURATION
    )

    private val albumProjection = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.ALBUM,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.FIRST_YEAR,
        MediaStore.Audio.Albums.LAST_YEAR,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS
    )

    private val artistAlbumProjection = arrayOf(
        MediaStore.Audio.Artists.Albums.ALBUM_ID,
        MediaStore.Audio.Artists.Albums.ALBUM,
        MediaStore.Audio.Artists.Albums.ARTIST,
        MediaStore.Audio.Artists.Albums.FIRST_YEAR,
        MediaStore.Audio.Artists.Albums.LAST_YEAR,
        MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS
    )

    private val playlistProjection = arrayOf(
        MediaStore.Audio.Playlists._ID,
        MediaStore.Audio.Playlists.NAME,
        MediaStore.Audio.Playlists.DATE_ADDED,
        MediaStore.Audio.Playlists.DATE_MODIFIED
    )

    private val metadataProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID
    )

    fun getAllAudio(ctx: Context): MutableList<Audio> {
        return queryAudio(ctx, sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC")
    }

    fun getAllMetadata(ctx: Context): List<MediaMetadataCompat> {
        val metadata = queryMetadata(ctx, sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC")
        return metadata
    }

    fun getAudioByAlbumId(ctx: Context, albumId: String): MutableList<MediaMetadataCompat> {
//        val metadata = queryMetadata(ctx, sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC")
        val metadata = queryMetadata(
            ctx,
            selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?",
            args = arrayOf(albumId),
            sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )
        return metadata
    }

    fun getAllAlbums(ctx: Context): MutableList<Album> {
        return queryAlbum(ctx)
    }

    suspend fun getAlbumIdFromAudioId(ctx: Context, mediaId: String, noCache: Boolean = false): Long {
        return getAlbumIdFromAudioId(ctx, mediaId.toLong())
    }

    suspend fun getAlbumIdFromAudioId(ctx: Context, audioId: Long, noCache: Boolean = false): Long {
        if (!noCache) {
            try {
                // album id is cached
                val audioId = cachedIdMapping.getValue(audioId)
                return audioId
            } catch (e: Exception) {
                Log.w("MediaHunter", e.message)
            }
        }
        var albumId = ALBUM_NOT_EXIST
        // album id not cached
        ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.ALBUM_ID),
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(audioId.toString()),
            null
        )?.use { cursor ->
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            if (cursor.moveToNext()) {
                albumId = cursor.getLongOrNull(albumIdColumn) ?: ALBUM_NOT_EXIST
            }
        }
        return albumId
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAlbumArt(ctx: Context, audioId: String, size: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioId.toLong()
        )
        return ctx.contentResolver.loadThumbnail(uri, Size(size, size), null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getThumbnail(ctx: Context, uri: String, size: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            ctx.contentResolver.loadThumbnail(Uri.parse(uri), Size(size, size), null)
        }
    }

    suspend fun getThumbnail(ctx: Context, uri: Uri, size: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            withContext(Dispatchers.IO + SupervisorJob()) {
                ctx.contentResolver.loadThumbnail(uri, Size(size, size), null)
            }
        } else {
            withContext(Dispatchers.IO + SupervisorJob()) {
                var albumArt: Bitmap? = null
                val audioId = uri.lastPathSegment
                val albumId = getAlbumIdFromAudioId(ctx, audioId!!)
                if (albumId != null) {
                    albumArt = getAlbumArtFromAlbumId(ctx, albumId.toString())
                }
                albumArt
            }
        }

    }

    suspend fun getAlbumArtFromAlbumId(ctx: Context, albumId: String): Bitmap? {
        var albumArt: Bitmap? = null
        ctx.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
            "${MediaStore.Audio.Albums._ID} =?",
            arrayOf(albumId),
            null
        )?.use { cursor ->
            val albumArtColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)
            if (cursor.moveToNext()) {
                val albumArtPath = cursor.getString(albumArtColumn)
                if (albumArt != null || albumArtPath != "") {
                    try {
                        albumArt = BitmapFactory.decodeFile(albumArtPath)
                    } catch (e: Exception) {
                        Log.e("MediaHunter", e.message)
                    }
                }
            }
        }
        return albumArt
    }

    fun getArtUriFromAlbumId(albumId: Long): Uri {
        return ContentUris.withAppendedId(ALBUM_ART_ROOT, albumId)
    }

    val artistProjection = arrayOf(
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST
    )

    fun getAllArtists(ctx: Context): MutableList<Artist> {
        return queryArtist(ctx)
    }

    fun getAlbumsByArtistId(ctx: Context, artistId: Long): MutableList<Album> {
        return queryAlbum(
            ctx,
            uri = MediaStore.Audio.Artists.Albums.getContentUri(VOLUME_EXTERNAL, artistId),
            projection = artistAlbumProjection
        )
    }

    fun getAllPlaylists(ctx: Context): MutableList<Playlist> {
        return queryPlaylist(ctx)
    }

    private fun queryMetadata(
        ctx: Context,
        uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection: Array<String> = audioProjection,
        selection: String? = null,
        args: Array<String>? = null,
        sortOrder: String? = null
    ): MutableList<MediaMetadataCompat> {
        val metadataList = mutableListOf<MediaMetadataCompat>()
        ctx.contentResolver.query(
            uri, projection, selection, args, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            var id: Long? = null
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var year: Int? = null
            var audioUri: Uri? = null
            var duration: Int? = null

            while (cursor.moveToNext()) {
                id = cursor.getLong(idColumn)
                title = cursor.getStringOrNull(titleColumn)
                artist = cursor.getStringOrNull(artistColumn)
                album = cursor.getStringOrNull(albumColumn)
                year = cursor.getIntOrNull(yearColumn)
                duration = cursor.getIntOrNull(durationColumn)
                audioUri = id.let {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        it
                    )
                }

                val metadata = MediaMetadataCompat.Builder().apply {
                    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    year?.toLong()?.let { putLong(MediaMetadataCompat.METADATA_KEY_YEAR, it) }
                    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, audioUri.toString())
                    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "$artist - $album")
                    putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration!!.toLong())
                }.build()
                metadataList += metadata
            }
        }
        return metadataList
    }

    fun createPlaylist(ctx: Context, name: String) {
        val resolver = ctx.contentResolver
        val playListDetail = ContentValues().apply {
            put(MediaStore.Audio.Playlists.NAME, name)
            put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())
            put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis())
        }
        val cusor = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, playListDetail)
        Log.d("MediaHunter", "Playlist added")
    }

    private fun queryAudio(
        ctx: Context,
        uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection: Array<String> = audioProjection,
        selection: String? = null,
        args: Array<String>? = null,
        sortOrder: String? = null
    ): MutableList<Audio> {
        val audioList = mutableListOf<Audio>()
        ctx.contentResolver.query(
            uri, projection, selection, args, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            var id: Long = 0L
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var albumId: Long? = null
            var year: Int? = null
            var duration: Long? = null
            var contentUri: Uri = Uri.EMPTY

            cachedIdMapping[id] = albumId ?: ALBUM_NOT_EXIST

            while (cursor.moveToNext()) {
                id = cursor.getLong(idColumn)
                title = cursor.getStringOrNull(titleColumn)
                artist = cursor.getStringOrNull(artistColumn)
                album = cursor.getStringOrNull(albumColumn)
                albumId = cursor.getLongOrNull(albumIdColumn)
                year = cursor.getIntOrNull(yearColumn)
                duration = cursor.getLongOrNull(durationColumn)
                contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                try {
                    cachedIdMapping.put(id, albumId ?: ALBUM_NOT_EXIST)
                } catch (e: Exception) {
                    Log.w("MediaHunter", e.message)
                }
                audioList.add(Audio(contentUri, id, title, artist, album, albumId, year, duration))
            }

            title = null
            artist = null
            album = null
            albumId = null
            year = null
            duration = null
        }
        return audioList
    }

    private fun queryAlbum(
        ctx: Context,
        uri: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        projection: Array<String> = albumProjection,
        selection: String? = null,
        args: Array<String>? = null,
        sortOrder: String? = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
    ): MutableList<Album> {
        val albumList = mutableListOf<Album>()
        ctx.contentResolver.query(
            uri,
            projection, selection, args,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(projection[0])
            val albumColumn = cursor.getColumnIndexOrThrow(projection[1])
            val artistColumn = cursor.getColumnIndexOrThrow(projection[2])
            val firstYearColumn = cursor.getColumnIndexOrThrow(projection[3])
            val lastYearColumn = cursor.getColumnIndexOrThrow(projection[4])
            val songsColumn = cursor.getColumnIndexOrThrow(projection[5])

            while (cursor.moveToNext()) {
                val id = idColumn.let { cursor.getLong(it) }
                val album = albumColumn.let { cursor.getStringOrNull(it) }
                val artist = artistColumn.let { cursor.getStringOrNull(it) }
                val firstYear = firstYearColumn.let { cursor.getIntOrNull(it) }
                val lastYear = lastYearColumn.let { cursor.getIntOrNull(it) }
                val songs = songsColumn.let { cursor.getIntOrNull(it) }
                val contentUri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        id
                    )
                albumList += Album(contentUri, id, album, artist, firstYear, lastYear, songs)
            }
        }
        return albumList
    }

    private fun queryArtist(
        ctx: Context,
        uri: Uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
        projection: Array<String> = artistProjection,
        selection: String? = null,
        args: Array<String>? = null,
        sortOrder: String? = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
    ): MutableList<Artist> {
        val artistList = mutableListOf<Artist>()
        ctx.contentResolver.query(
            uri, projection, selection, args, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)

            var id: Long? = null
            var artist: String? = null

            while (cursor.moveToNext()) {
                id = idColumn.let { cursor.getLong(it) }
                artist = artistColumn.let { cursor.getStringOrNull(it) }
                val contentUri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        id
                    )
                artistList += Artist(contentUri, id, artist)
            }
        }
        return artistList
    }

    private fun queryPlaylist(
        ctx: Context,
        uri: Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
        projection: Array<String> = playlistProjection,
        selection: String? = null,
        args: Array<String>? = null,
        sortOrder: String? = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
    ): MutableList<Playlist> {
        val playlistList = mutableListOf<Playlist>()
        ctx.contentResolver.query(
            uri, projection, selection, args, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getStringOrNull(nameColumn)
                val dateAdded = cursor.getLongOrNull(dateAddedColumn)
                val dateModified = cursor.getLongOrNull(dateModifiedColumn)
                val contentUri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        id
                    )
                playlistList += Playlist(contentUri, id, name, dateAdded, dateModified)
            }
        }
        return playlistList
    }
}