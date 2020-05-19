package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.extensions.isPlayEnabled
import com.github.sg4yk.audioplayer.extensions.isPlaying
import com.github.sg4yk.audioplayer.extensions.isPrepared

@WorkerThread
object PlaybackManager {
    private lateinit var connection: PlaybackServiceConnection

    fun startPlaybackService(context: Context) {
        context.startService(Intent(context, PlaybackService::class.java))
        connection = ServiceInjector.getPlaybackServiceConnection(context)
    }

    fun stopPlaybackService(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        context.stopService(intent)
        connection.close()
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun playAudioFromId(mediaId: String, pauseAllowed: Boolean = true) {
        val nowPlaying = connection.nowPlaying.value
        val transportControls = connection.transportControls

        val isPrepared = connection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
            connection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            "PlayAudio", "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=$mediaId)"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaId, null)
            transportControls.play()
        }
    }
}