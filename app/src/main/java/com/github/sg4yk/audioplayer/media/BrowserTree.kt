package com.github.sg4yk.audioplayer.media

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.R
import java.net.URLEncoder
import java.nio.charset.Charset

@WorkerThread
class BrowseTree(context: Context, musicSource: MusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        val rootList = mediaIdToChildren[BROWSABLE_ROOT] ?: mutableListOf()
        val libraryList = mediaIdToChildren[BROWSABLE_ROOT] ?: mutableListOf()

        val recommendedMetadata = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RECOMMENDED_ROOT)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Recommended")
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, RESOURCE_ROOT_URI +
                        context.resources.getResourceEntryName(R.drawable.lucas_benjamin_unsplash)
            )
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        val albumsMetadata = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, ALBUMS_ROOT)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Albums")
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                RESOURCE_ROOT_URI + context.resources.getResourceEntryName(R.drawable.lucas_benjamin_unsplash)
            )
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        rootList += recommendedMetadata
        rootList += albumsMetadata
        mediaIdToChildren[BROWSABLE_ROOT] = rootList

        musicSource.forEach { mediaItem ->
            val albumMediaId = encodeUri(mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            albumChildren += mediaItem

            // Add the first track of each album to the 'Recommended' category
            if (mediaItem.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER) == 1L) {
                val recommendedChildren = mediaIdToChildren[RECOMMENDED_ROOT]
                    ?: mutableListOf()
                recommendedChildren += mediaItem
                mediaIdToChildren[RECOMMENDED_ROOT] = recommendedChildren
            }
        }
    }

    /**
     * Provide access to the list of children with the `get` operator.
     * i.e.: `browseTree\[UAMP_BROWSABLE_ROOT\]`
     */
    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    /**
     * Builds a node, under the root, that represents an album, given
     * a [MediaMetadataCompat] object that's one of the songs on that album,
     * marking the item as [MediaItem.FLAG_BROWSABLE], since it will have child
     * node(s) AKA at least 1 song.
     */
    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                encodeUri(mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            )
            putBitmap(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                mediaItem.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            )
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[ALBUMS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)] = it
        }
    }

    private fun encodeUri(uri: String): String = if (Charset.isSupported("UTF-8")) {
        URLEncoder.encode(uri ?: "", "UTF-8")
    } else {
        @Suppress("deprecation")
        URLEncoder.encode(uri ?: "")
    }
}

const val BROWSABLE_ROOT = "/"
const val EMPTY_ROOT = "@empty@"
const val RECOMMENDED_ROOT = "__RECOMMENDED__"
const val ALBUMS_ROOT = "__ALBUMS__"
const val LIBRARY_ROOT = "__LIBRARY__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.github.sg4yk.AudioPLayer/drawable/"

const val METADATA_KEY_FLAGS = "com.github.sg4yk.media.METADATA_KEY_FLAGS"

@MediaItem.Flags
inline val MediaMetadataCompat.flag
    get() = this.getLong(METADATA_KEY_FLAGS).toInt()

@MediaItem.Flags
inline var MediaMetadataCompat.Builder.flag: Int
    @Deprecated("Property does not have a 'get'", level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(METADATA_KEY_FLAGS, value.toLong())
    }
