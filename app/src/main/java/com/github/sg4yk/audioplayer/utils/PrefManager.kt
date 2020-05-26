package com.github.sg4yk.audioplayer.utils

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.github.sg4yk.audioplayer.R

@WorkerThread
object PrefManager {
    fun animationReduced(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean(context.getString(R.string.reduce_animation), false)
    }

    fun keepBgService(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean(context.getString(R.string.keep_bg_service), true)
    }
}