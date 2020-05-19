package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.github.sg4yk.audioplayer.extensions.isPlayEnabled
import com.github.sg4yk.audioplayer.extensions.isPlaying
import com.github.sg4yk.audioplayer.extensions.isPrepared

object PlaybackManager {
    private lateinit var connection: PlaybackServiceConnection
    private var connected = false

//    private lateinit var controls: MediaControllerCompat.TransportControls

    fun connectPlaybackService(context: Context) {
        context.startService(Intent(context, PlaybackService::class.java))
        connection = ServiceInjector.getPlaybackServiceConnection(context)
    }

    fun stopPlaybackService(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        connection.transportControls.stop()
        connection.close()
    }

    fun playAudioFromId(mediaId: String, pauseAllowed: Boolean = true) {
        Thread {
            val nowPlaying = connection.nowPlaying.value
            val controls = connection.transportControls
            val isPrepared = connection.playbackState.value?.isPrepared ?: false
            if (isPrepared && mediaId == nowPlaying?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
                connection.playbackState.value?.let { playbackState ->
                    when {
                        playbackState.isPlaying ->
                            if (pauseAllowed) controls.pause() else Unit
                        playbackState.isPlayEnabled -> controls.play()
                        else -> {
                            Log.w(
                                "PlayAudio", "Playable item clicked but neither play nor pause are enabled!" +
                                        " (mediaId=$mediaId)"
                            )
                        }
                    }
                }
            } else {
                controls.playFromMediaId(mediaId, null)
                controls.play()
            }
        }.start()
    }

    fun playAll(mediaId: String, pauseAllowed: Boolean = true) {
        Thread {
            val controls = connection.transportControls
            controls.pause()
            controls.playFromMediaId(mediaId, null)
            controls.play()
        }.start()
    }

    fun pause() {
        connection.transportControls.pause()
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

    fun isConnected(): MutableLiveData<Boolean> {
        return connection.isConnected
    }

    fun sessionToken(): MediaSessionCompat.Token {
        return connection.sessionToken
    }
}