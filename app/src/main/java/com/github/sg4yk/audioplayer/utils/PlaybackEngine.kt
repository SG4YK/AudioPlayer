package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.github.sg4yk.audioplayer.entities.Audio

object PlaybackEngine {
    const val STATUS_STOPPED = 0;
    const val STATUS_PLAYING = 1;
    const val STATUS_PAUSED = 2;

    private var curStatus = STATUS_STOPPED;

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
        curStatus = STATUS_PLAYING
    }

    fun stop() {
        mediaPlayer?.stop()
        curStatus = STATUS_STOPPED
    }

    fun pause() {
        mediaPlayer?.pause()
        curStatus = STATUS_PAUSED
    }


    fun resume() {
        mediaPlayer?.currentPosition?.let { mediaPlayer?.seekTo(it) }
        mediaPlayer?.start()
        curStatus = STATUS_PLAYING
    }

    fun status(): Int {
        return curStatus
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

    fun getDurationString(): String {
        return msecToStr(getDuration())
    }

    fun getPosition(): Int {
        return if (mediaPlayer != null) {
            mediaPlayer!!.currentPosition * 100 / mediaPlayer!!.duration
        } else {
            0
        }
    }

    fun getPosistionString(): String {
        if (mediaPlayer == null) {
            return "00:00"
        }
        return msecToStr(mediaPlayer!!.currentPosition)
    }

    private fun msecToStr(msec: Int): String {
        val seconds = msec / 1000
        val minutes = seconds / 60
        return "%02d:%02d".format(minutes, seconds % 60)
    }
}