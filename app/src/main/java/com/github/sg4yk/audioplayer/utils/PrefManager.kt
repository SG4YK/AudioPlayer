package com.github.sg4yk.audioplayer.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.github.sg4yk.audioplayer.R

object PrefManager {
    fun revealAnimationEnabled(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean(context.getString(R.string.reveal_ani_key), true)
    }
}