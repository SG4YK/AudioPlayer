package com.github.sg4yk.audioplayer.media

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.utils.MediaHunter

interface MusicSource : Iterable<MediaMetadataCompat> {

    suspend fun load()

    fun whenReady(performAction: (Boolean) -> Unit): Boolean

    fun search(query: String, extras: Bundle): List<MediaMetadataCompat>
}

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class State

const val STATE_CREATED = 1
const val STATE_INITIALIZING = 2
const val STATE_INITIALIZED = 3
const val STATE_ERROR = 4

@WorkerThread
abstract class AbstractMusicSource : MusicSource {
    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            else -> {
                performAction(state != STATE_ERROR)
                true
            }
        }

    override fun search(query: String, extras: Bundle): List<MediaMetadataCompat> {
        // First attempt to search with the "focus" that's provided in the extras.
        val focusSearchResult = when (extras[MediaStore.EXTRA_MEDIA_FOCUS]) {
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                val genre = extras[EXTRA_MEDIA_GENRE]
                Log.d(TAG, "Focused genre search: '$genre'")
                filter { metadata ->
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE) == genre
                }
            }
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused artist search: '$artist'")
                filter { metadata ->
                    (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) == artist
                            || metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART) == artist)
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                // For an Album focused search, album and artist are set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                Log.d(TAG, "Focused album search: album='$album' artist='$artist")
                filter { metadata ->
                    (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) == artist
                            || metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST) == artist)
                            && metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                // For a metadata (aka Media) focused search, title, album, and artist are set.
                val title = extras[MediaStore.EXTRA_MEDIA_TITLE]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused media search: title='$title' album='$album' artist='$artist")
                filter { metadata ->
                    (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) == artist
                            || metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST) == artist)
                            && metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) == album
                            && metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) == album
                }
            }
            else -> {
                emptyList()
            }
        }

        if (focusSearchResult.isEmpty()) {
            return if (query.isNotBlank()) {
                Log.d(TAG, "Unfocused search for '$query'")
                filter { metadata ->
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE).contains(query)
                            || metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE).contains(query)
                }
            } else {
                Log.d(TAG, "Unfocused search without keyword")
                return shuffled()
            }
        } else {
            return focusSearchResult
        }
    }

    private val EXTRA_MEDIA_GENRE
        get() = MediaStore.EXTRA_MEDIA_GENRE
}

private const val TAG = "MediaSource"

class MetadataSource(private val context: Context) : AbstractMusicSource() {

    var list: List<MediaMetadataCompat> = listOf()

    init {
        state = STATE_INITIALIZING
    }

    override suspend fun load() {
        try {
            list = MediaHunter.getAllMetadata(context)
            state = STATE_INITIALIZED
        } catch (e: Exception) {
            Log.e("MetadataSource", e.message)
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> {
        return list.iterator()
    }
}

class AlbumSource(private val context: Context, private val albumId: String) : AbstractMusicSource() {

    var list: List<MediaMetadataCompat> = listOf()

    init {
        state = STATE_INITIALIZING
    }

    override suspend fun load() {
        try {
            list = MediaHunter.getMetadataByAlbumId(context, albumId).toList()
            state = STATE_INITIALIZED
        } catch (e: Exception) {
            Log.e("AlbumSource", e.message)
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> {
        return list.iterator()
    }
}

class PlaylistSource(private val context: Context, private val playlistId: String) : AbstractMusicSource() {

    var list: List<MediaMetadataCompat> = listOf()

    init {
        state = STATE_INITIALIZING
    }

    override suspend fun load() {
        try {
            list = MediaHunter.getMetadataByPlayListId(context, playlistId).toList()
            state = STATE_INITIALIZED
        } catch (e: Exception) {
            Log.e("AlbumSource", e.message)
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> {
        return list.iterator()
    }
}