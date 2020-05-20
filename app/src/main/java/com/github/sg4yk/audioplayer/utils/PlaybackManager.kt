package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.github.sg4yk.audioplayer.extensions.isPrepared
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

    fun playAudioFromId(mediaId: String, pauseAllowed: Boolean = true) {
        GlobalScope.launch {
            val nowPlaying = connection.nowPlaying.value
            val controls = connection.transportControls
            val isPrepared = connection.playbackState.value?.isPrepared ?: false
//            if (isPrepared && mediaId == nowPlaying?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
//                connection.playbackState.value?.let { playbackState ->
//                    when {
//                        playbackState.isPlaying ->
//                            if (pauseAllowed) controls.pause() else Unit
//                        playbackState.isPlayEnabled -> controls.play()
//                        else -> {
//                            Log.w(
//                                "PlayAudio", "Playable item clicked but neither play nor pause are enabled!" +
//                                        " (mediaId=$mediaId)"
//                            )
//                        }
//                    }
//                }
//            } else {
            controls.prepareFromMediaId(mediaId, null)
            controls.play()
            Log.d("TOKEN", connection.sessionToken.toString())
//            }
        }

    }

    fun playAll() {
        GlobalScope.launch {
            val controls = connection.transportControls
            controls.prepareFromMediaId(MEDIA_ID_PLAY_ALL, null)
            controls.play()
        }
    }

    fun loadAllAndSkipTo(id: Long) {
        GlobalScope.launch {
            val controls = connection.transportControls
            controls.playFromMediaId(MEDIA_ID_PLAY_ALL, null)
            controls.skipToQueueItem(id)
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