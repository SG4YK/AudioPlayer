package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.media.AlbumSource
import com.github.sg4yk.audioplayer.media.MetadataSource
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.*

@WorkerThread
// Filter items to play from music source
class PlaybackPreparer(
    ctx: Context,
    source: MetadataSource,
    player: ExoPlayer,
    factory: DefaultDataSourceFactory,
    serviceScope: CoroutineScope
) : MediaSessionConnector.PlaybackPreparer {

    companion object {
        const val MODE_KEY = "MODE"
        const val MODE_PLAY_ALL = 0
        const val MODE_LOAD_ALL_AND_SKIP_TO_AUDIO = 1
        const val MODE_PLAY_ALBUM = 2
        const val MODE_LOAD_ALBUM_AND_SKIP_TO_AUDIO = 3
        const val MODE_SINGLE_AUDIO = 4

        const val ALBUM_ID_TAG = "ALBUM_ID"
        const val AUDIO_ID_TAG = "AUDIO_ID"
    }

    private val context: Context = ctx
    private val mediaSource: MetadataSource = source
    private val exoPlayer: ExoPlayer = player
    private val dataSourceFactory: DataSource.Factory = factory
    private val preparerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentQueue: List<MediaMetadataCompat> = listOf()
    private var serviceScope: CoroutineScope = serviceScope

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle) {
        val mode = extras.get(MODE_KEY)
        when (mode) {
            MODE_PLAY_ALL -> {
                loadAllAndSkipTo()
            }
            MODE_LOAD_ALL_AND_SKIP_TO_AUDIO -> {
                loadAllAndSkipTo(mediaId)
            }
            MODE_PLAY_ALBUM -> {
                loadAlbumAndSkipTo(mediaId)
            }
            MODE_LOAD_ALBUM_AND_SKIP_TO_AUDIO -> {
                loadAlbumAndSkipTo(mediaId, extras.getString(AUDIO_ID_TAG))
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle) {
        mediaSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = mediaSource.find { item ->
                item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI) == uri.toString()
            }

            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaURI=$uri")
            } else {
                val metadataList = MediaHunter.getAllMetadata(context)
                val mediaSource = buildMediaSource(metadataList, dataSourceFactory)
                val initialWindowIndex = metadataList.indexOf(itemToPlay)
                exoPlayer.prepare(mediaSource)
//                exoPlayer.seekTo(initialWindowIndex, 0)
                exoPlayer.seekTo(0)
            }
        }
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle) {
        TODO("Not yet implemented")
    }

    override fun onCommand(
        player: Player,
        controlDispatcher: ControlDispatcher,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?
    ): Boolean {
        Log.d("PlaybackService", "Preparer on command")
        return false
    }

    override fun onPrepare(playWhenReady: Boolean) {
        val mediaSource = buildMediaSource(mediaSource.toList(), dataSourceFactory)
        exoPlayer.prepare(mediaSource)
    }

    private fun loadAllAndSkipTo(mediaId: String? = null) {
        mediaSource.whenReady {
            currentQueue = mediaSource.toList()
            val playlist = buildMediaSource(currentQueue, dataSourceFactory)
            var initialWindowIndex = -1
            if (mediaId != null) {
                val itemToPlay: MediaMetadataCompat? = mediaSource.list.find { item ->
                    item.description.mediaId == mediaId
                }
                if (itemToPlay == null) {
                    Log.d(TAG, "Content not found: MediaID=$mediaId")
                } else {
                    initialWindowIndex = mediaSource.list.indexOf(itemToPlay)
                }
            }
            exoPlayer.prepare(playlist)
            exoPlayer.seekTo(0, 0)
            if (initialWindowIndex != -1) {
                exoPlayer.seekTo(initialWindowIndex, 0)
            }
        }
    }

    private fun loadAlbumAndSkipTo(albumId: String, audioId: String? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            val albumSource = AlbumSource(context, albumId)
            albumSource.load()
            albumSource.whenReady {
                currentQueue = albumSource.list
                var initialWindowIndex = -1

                if (audioId != null) {
                    val itemToPlay: MediaMetadataCompat? = currentQueue.find { item ->
                        item.description.mediaId == audioId
                    }
                    initialWindowIndex = currentQueue.indexOf(itemToPlay)
                }

                val playlist = buildMediaSource(currentQueue, dataSourceFactory)
                serviceScope.launch {
                    exoPlayer.prepare(playlist)
                    if (initialWindowIndex != -1) {
                        exoPlayer.seekTo(initialWindowIndex, 0)
                    }
                }
            }
        }
    }

    private fun buildMediaSource(
        metadataList: List<MediaMetadataCompat>,
        factory: DataSource.Factory
    ): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        metadataList.forEach {
            val progressiveSource = ProgressiveMediaSource.Factory(factory)
                .setTag(it)
                .createMediaSource(Uri.parse(it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)))
            concatenatingMediaSource.addMediaSource(progressiveSource)
        }
        return concatenatingMediaSource
    }

    private val TAG = "PlaybackPreparer"
}

