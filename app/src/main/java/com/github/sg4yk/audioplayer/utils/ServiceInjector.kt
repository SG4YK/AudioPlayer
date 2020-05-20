package com.github.sg4yk.audioplayer.utils

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread

object ServiceInjector {
    fun getPlaybackServiceConnection(context: Context): PlaybackServiceConnection {
        val connection =  PlaybackServiceConnection.getInstance(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        Log.d("Injector","Getting instance")
        return connection
    }

//    fun provideMainActivityViewModel(context: Context): MainActivityViewModel.Factory {
//        val applicationContext = context.applicationContext
//        val musicServiceConnection = provideMusicServiceConnection(applicationContext)
//        return MainActivityViewModel.Factory(musicServiceConnection)
//    }

//    fun provideMediaItemFragmentViewModel(context: Context, mediaId: String)
//            : MediaItemFragmentViewModel.Factory {
//        val applicationContext = context.applicationContext
//        val musicServiceConnection = provideMusicServiceConnection(applicationContext)
//        return MediaItemFragmentViewModel.Factory(mediaId, musicServiceConnection)
//    }

//    fun provideNowPlayingFragmentViewModel(context: Context)
//            : NowPlayingFragmentViewModel.Factory {
//        val applicationContext = context.applicationContext
//        val musicServiceConnection = provideMusicServiceConnection(applicationContext)
//        return NowPlayingFragmentViewModel.Factory(
//            applicationContext as Application, musicServiceConnection)
//    }
}