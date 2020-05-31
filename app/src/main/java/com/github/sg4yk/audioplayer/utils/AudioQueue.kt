package com.github.sg4yk.audioplayer.utils

import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.media.Audio

// TODO

@WorkerThread
object AudioQueue {
    const val AUDIO_ALREADY_EXSIT = 1

    const val MODE_DEFAULT = 0
    const val MODE_SHUFFLE = 1
    const val MODE_REPEAT_TRACK = 2
    const val MODE_REPEAT_ALL = 3
    const val MODE_RANDOM = 4

    private var audioList: MutableList<Audio>? = null
    private var played: MutableList<Boolean>? = null
    private var playedCache: MutableList<Int>? = null
    private var playing: Int = -1
    private var mode = MODE_DEFAULT

    fun getQueue(): MutableList<Audio>? {
        return audioList
    }

    fun getMode(): Int {
        return mode
    }

    private fun createQueue() {
        audioList = mutableListOf()
        played = mutableListOf()
        playedCache = mutableListOf()
    }

    fun createQueueFrom(list: MutableList<Audio>) {
        audioList = list
        if (audioList != null) {
            played = MutableList(audioList!!.size) { false }
            playedCache = mutableListOf()
        }
    }

    fun setMode(m: Int) {
        mode = m
    }

    fun append(audio: Audio): Int {
        if (audioList == null) {
            createQueue()
        }
        return if (audioList!!.contains(audio)) {
            AUDIO_ALREADY_EXSIT
        } else {
            audioList!! += audio
            0
        }
    }

    fun next(): Audio? {
        when (mode) {
            MODE_DEFAULT -> {
                if (audioList == null || audioList!!.size == 0) {
                    return null
                }
                return if (playing < audioList!!.size - 1) {
                    playedCache?.plusAssign(playing)
                    played?.set(playing, true)
                    audioList!![++playing]
                } else {
                    null
                }
            }
            else -> {
                return null
            }
        }
    }
}