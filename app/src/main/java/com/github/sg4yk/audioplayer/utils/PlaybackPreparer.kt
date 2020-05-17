package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.github.sg4yk.audioplayer.media.MusicSource
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource

// Filter items to play from music source
class PlaybackPreparer(
    private val musicSource: MusicSource,
    private val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DataSource.Factory,
    private val context: Context
) : MediaSessionConnector.PlaybackPreparer {

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).equals(mediaId)
            }

            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$mediaId")
            } else {
                val metadataList = AudioHunter.getAllMetadata(context)
                val mediaSource = buildMediaSource(metadataList, dataSourceFactory)
                val initialWindowIndex = metadataList.indexOf(itemToPlay)
                exoPlayer.prepare(mediaSource)
                exoPlayer.seekTo(initialWindowIndex, 0)
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI) == uri.toString()
            }

            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$uri")
            } else {
                val metadataList = AudioHunter.getAllMetadata(context)
                val mediaSource = buildMediaSource(metadataList, dataSourceFactory)
                val initialWindowIndex = metadataList.indexOf(itemToPlay)
                exoPlayer.prepare(mediaSource)
                exoPlayer.seekTo(initialWindowIndex, 0)
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
        TODO("Not yet implemented")
    }

    override fun onPrepare(playWhenReady: Boolean) {
        TODO("Not yet implemented")
    }


    private fun buildMediaSource(
        metadataList: List<MediaMetadataCompat>,
        factory: DataSource.Factory
    ): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        metadataList.forEach {
            val progressiveSource = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)))
            concatenatingMediaSource.addMediaSource(progressiveSource)
        }
        return concatenatingMediaSource
    }

    private val TAG = "PlaybackPreparer"
}