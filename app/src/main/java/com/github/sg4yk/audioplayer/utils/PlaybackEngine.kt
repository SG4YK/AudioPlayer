package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.entities.Audio
import com.github.sg4yk.audioplayer.utils.Generic.msecToStr

// Do not use this class out of PlaybackManager
@WorkerThread
object PlaybackEngine {
    const val STATUS_STOPPED = 0
    const val STATUS_PLAYING = 1
    const val STATUS_PAUSED = 2

    private lateinit var currentAudio: Audio
    private var status = STATUS_STOPPED

    private var mediaPlayer: MediaPlayer? = null

    fun play(ctx: Context, audio: Audio?) {
        if (audio?.uri == null) {
            return
        }
        Thread(Runnable {
            mediaPlayer?.stop()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(ctx, audio.uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        }).start()
        currentAudio = audio
        status = STATUS_PLAYING
    }

    fun getCurrentAudio():Audio{
        return currentAudio
    }

    fun stop() {
        mediaPlayer?.stop()
        status = STATUS_STOPPED
    }

    fun pause() {
        mediaPlayer?.pause()
        status = STATUS_PAUSED
    }


    fun resume() {
        mediaPlayer?.currentPosition?.let { mediaPlayer?.seekTo(it) }
        mediaPlayer?.start()
        status = STATUS_PLAYING
    }

    fun status(): Int {
        return status
    }

    fun seekTo(percentage: Int) {
        if (mediaPlayer == null) {
            return
        }
        val duration = mediaPlayer!!.duration

        val msec = duration * percentage / 100
        Log.d("PlaybackEngine", "Seekto " + msec.toString())
        mediaPlayer?.seekTo(msec)
        if (mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
        }
    }

    fun getDuration(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.duration
        } else {
            0
        }
    }

    fun getDurationStr(): String {
        return msecToStr(getDuration())
    }

    fun getPercentage(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.currentPosition * 100 / mediaPlayer!!.duration
        } else {
            0
        }
    }

    fun getPosition(): Int {
        if (mediaPlayer == null) {
            return 0
        }
        return mediaPlayer!!.currentPosition
    }

    fun getPositionStr(): String {
        if (mediaPlayer == null) {
            return "00:00"
        }
        return msecToStr(mediaPlayer!!.currentPosition)
    }
}