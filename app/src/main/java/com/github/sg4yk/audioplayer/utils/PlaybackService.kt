package com.github.sg4yk.audioplayer.utils

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompatExtras
import androidx.lifecycle.LiveData
import com.github.sg4yk.audioplayer.MainActivity
import com.github.sg4yk.audioplayer.R

@WorkerThread
class PlaybackService : Service() {

    private val mBinder = PlaybackService.PlaybackBinder()

    class PlaybackBinder : Binder() {
        fun play() {
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        // initialize playback manager
        PlaybackManager.init(applicationContext)
        Log.d("PlaybackService", "Service started")


        // create notification
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), 0
        )

//        val notification =  NotificationCompat.Builder()
//            .setSmallIcon(R.drawable.ic_play_circle_filled_white_24dp)
//            .setContentTitle("Track title")
//            .setContentText("Artist - Album")
//        .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
//            .setMediaSession(
//                Mediase
//            ))
//            .build();
    }

    override fun onDestroy() {
        Log.d("PlaybackService", "Stopping playback...")
        PlaybackManager.stop()
        Log.d("PlaybackService", "Service stopped")
        super.onDestroy()
    }

    class metadata : LiveData<PlaybackManager.Metadata>() {
        override fun onActive() {
            super.onActive()
        }

        override fun onInactive() {
            super.onInactive()
        }
    }


}
