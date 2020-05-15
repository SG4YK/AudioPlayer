package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.github.sg4yk.audioplayer.entities.Audio

object PlaybackEngine {
    const val STATUS_STOPPED = 0;
    const val STATUS_PLAYING = 1;

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
    }

    fun stop() {
        mediaPlayer?.stop()
    }

    fun pause() {
        mediaPlayer?.pause()
    }


    fun resume() {
        mediaPlayer?.start()
    }

    fun status(): Int {
        return if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            STATUS_PLAYING
        } else {
            STATUS_STOPPED
        }
    }

    fun seekTo(percentage: Int) {
        if (mediaPlayer == null) {
            return
        }
        val duration = mediaPlayer!!.duration

        val msec = duration * percentage / 100
        Log.d("PlaybackEngine", "Seekto " + msec.toString())
        mediaPlayer?.seekTo(msec)
        mediaPlayer?.start()
    }

    fun getDuration(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.duration
        } else {
            0
        }
    }

    fun getPosition(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.currentPosition * 100 / mediaPlayer!!.duration
        } else {
            0
        }
    }
}