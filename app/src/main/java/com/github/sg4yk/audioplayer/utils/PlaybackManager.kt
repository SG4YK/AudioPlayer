package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object PlaybackManager {
    private lateinit var connection: PlaybackServiceConnection

//    private lateinit var controls: MediaControllerCompat.TransportControls

    fun connectPlaybackService(context: Context) {
        context.startService(Intent(context, PlaybackService::class.java))
        connection = ServiceInjector.getPlaybackServiceConnection(context)
    }

    fun stopPlaybackService(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        connection.transportControls.stop()
        connection.close()
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun playAll() {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_PLAY_ALL)
            }
            controls.prepareFromMediaId("PLAYALL", bundle)
            controls.play()
        }
    }

    fun loadAllAndSkipTo(audioId: String) {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_LOAD_ALL_AND_SKIP_TO_AUDIO)
            }
            controls.prepareFromMediaId(audioId, bundle)
            controls.play()
        }
    }

    fun playAlbum(albumId: String) {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_PLAY_ALBUM)
            }
            controls.prepareFromMediaId(albumId, bundle)
            controls.play()
        }
    }

    fun loadAlbumAndSkipTo(albumId: String, position: Int) {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_LOAD_ALBUM_AND_SKIP_TO_AUDIO)
                putInt(PlaybackPreparer.POSITION_TAG, position)
            }
            controls.prepareFromMediaId(albumId, bundle)
            controls.play()
        }
    }

    fun playPlaylist(playlistId: String) {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_PLAY_PLAYLIST)
            }
            controls.prepareFromMediaId(playlistId, bundle)
            controls.play()
        }
    }

    fun loadPlaylistAndSkipTo(playlistId: String, position: Int) {
        GlobalScope.launch {
            val controls = connection.transportControls
            val bundle = Bundle().apply {
                putInt(PlaybackPreparer.MODE_KEY, PlaybackPreparer.MODE_LOAD_PLAYLIST_AND_SKIP_TO)
                putInt(PlaybackPreparer.POSITION_TAG, position)
            }
            controls.prepareFromMediaId(playlistId, bundle)
            controls.play()
        }
    }

    fun pause() {
        connection.transportControls.pause()
    }

    fun play() {
        connection.transportControls.play()
    }

    fun skipNext() {
        connection.transportControls.skipToNext()
    }

    fun skipPrevious() {
        connection.transportControls.skipToPrevious()
    }

    fun skipToId(id: Long) {
        connection.transportControls.skipToQueueItem(id)
    }

    fun seekTo(percentage: Int) {
        val state = playbackState().value?.state ?: PlaybackStateCompat.STATE_NONE
        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            val duration = connection.nowPlaying.value?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
            val position = duration / 100 * percentage
            connection.transportControls.seekTo(position)
        }
    }

    fun isConnected(): MutableLiveData<Boolean> {
        return connection.isConnected
    }

    fun playbackState(): MutableLiveData<PlaybackStateCompat> {
        return connection.playbackState
    }

    fun nowPlaying(): MutableLiveData<MediaMetadataCompat> {
        return connection.nowPlaying
    }

    fun sessionToken(): MediaSessionCompat.Token {
        return connection.sessionToken
    }

    fun closeConnection() {
        connection.close()
    }
}