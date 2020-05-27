package com.github.sg4yk.audioplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.sg4yk.audioplayer.utils.Generic

class LegalInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Legal Information"
        setSupportActionBar(toolbar)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.legalinfo, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            val url = when (preference?.key) {
                "AOSP" -> {
                    "https://source.android.com/license"
                }
                "audioPlayer" -> {
                    "https://github.com/SG4YK/AudioPlayer"
                }
                "blurry" -> {
                    "https://github.com/wasabeef/Blurry"
                }
                "circleImgView" -> {
                    "https://github.com/hdodenhof/CircleImageView"
                }
                "exoPlayer" -> {
                    "https://github.com/google/ExoPlayer"
                }
                "glide" -> {
                    "https://github.com/bumptech/glide"
                }
                "mdcAndroid" -> {
                    "https://github.com/material-components/material-components-android"
                }
                "mdFont" -> {
                    "https://github.com/templarian/MaterialDesign"
                }
                "recyclerAni" -> {
                    "https://github.com/wasabeef/recyclerview-animators"
                }
                "swipe" -> {
                    "https://github.com/pwittchen/swipe"
                }
                "uamp" -> {
                    "https://github.com/android/uamp"
                }
                else -> {
                    null
                }
            }
            if (url != null) {
                activity?.let { Generic.openWebsite(it, url) }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

}