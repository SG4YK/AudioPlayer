package com.github.sg4yk.audioplayer.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder

object PlaybackServiceManager {

    private class PlaybackService : Service() {
        override fun onCreate() {
            super.onCreate()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onDestroy() {
            if (PlaybackEngine.status() != PlaybackEngine.STATUS_STOPPED) {
                PlaybackEngine.stop()
            }
            super.onDestroy()
        }

        override fun onBind(intent: Intent): IBinder {
            TODO("Return the communication channel to the service.")
        }
    }

}