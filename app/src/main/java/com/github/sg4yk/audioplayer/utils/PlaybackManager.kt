package com.github.sg4yk.audioplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.github.sg4yk.audioplayer.entities.Album
import com.github.sg4yk.audioplayer.entities.Audio

@WorkerThread
object PlaybackManager {
    var context: Context? = null
    var audioList = mutableListOf<Audio>()
    var albumList = mutableListOf<Album>()
    var currentTrack: Audio? = null
    var currentAlbum: Album? = null
    var mediaPlayer = MediaPlayer()
    var currentMetadata: PlaybackManager.Metadata? = null

    fun init(ctx: Context) {
        context = null
        audioList.clear()
        albumList.clear()
        currentMetadata = null
        context = ctx

        Thread(Runnable {
            audioList = AudioHunter.getAllAudio(ctx)
            Log.d("PlaybackManager", "Audio scan complete")
//            audioList.forEach {
//                Log.d("PlaybackManager", it.toString())
//            }
        }).start()

        Thread(Runnable {
            albumList = AudioHunter.getAllAlbums(ctx)
            Log.d("PlaybackManager", "Album scan complete")
//            albumList.forEach {
//                Log.d("PlaybackManager", it.toString())
//            }
        }).start()
    }

    fun playOrPause(): Int {
        when (PlaybackEngine.status()) {
            PlaybackEngine.STATUS_PLAYING -> {
                pause()
                return 0
            }
            PlaybackEngine.STATUS_STOPPED -> {
                play()
                return 1
            }
            PlaybackEngine.STATUS_PAUSED -> {
                resume()
                return 1
            }
            else -> return 0
        }
    }

    fun play(): Boolean {
        return if (audioList.isEmpty()) {
            false
        } else {
            context?.let { PlaybackEngine.play(it, audioList[0]) }
            currentMetadata = PlaybackManager.Metadata(context!!, audioList[0])
            true
        }

    }

    fun pause() {
        if (PlaybackEngine.status() == PlaybackEngine.STATUS_PLAYING) {
            PlaybackEngine.pause()
        }
    }

    fun resume() {
        if (PlaybackEngine.status() == PlaybackEngine.STATUS_PAUSED) {
            PlaybackEngine.resume()
        }
    }

    fun stop() {
        PlaybackEngine.stop()
    }

    fun seekTo(percentage: Int) {
        if (percentage < 0 || percentage > 100) {
            return
        }
        PlaybackEngine.seekTo(percentage)
    }

    fun status(): Int {
        return PlaybackEngine.status()
    }

    fun percentage(): Int {
        return PlaybackEngine.getPercentage()
    }

    fun position(): Int {
        return PlaybackEngine.getPosition()
    }

    fun positionAsString(): String {
        return PlaybackEngine.getPositionStr()
    }

    fun duration(): Int {
        return PlaybackEngine.getDuration()
    }

    fun durationAsString(): String {
        return PlaybackEngine.getDurationStr()
    }

    class Metadata constructor(context: Context, audio: Audio) {
        var title: String = audio.title ?: "null"
        var artist: String = audio.artist ?: "Unknown artist"
        var album: String = audio.album ?: "Unknown Album"
        var year: Int = audio.year ?: 0

        @RequiresApi(Build.VERSION_CODES.Q)
        var albumArt: Bitmap? = AudioHunter.getAlbumArt(context, audio)
    }
}