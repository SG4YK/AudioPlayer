package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

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
            when (preference?.key) {
                "AOSP" -> {
                    context?.let { SettingsActivity.openWebSite(it, "https://source.android.com/license") }
                }
                "audioPlayer" -> {
                    context?.let { SettingsActivity.openWebSite(it, "https://github.com/SG4YK/AudioPlayer") }
                }
                "blurry" -> {
                    context?.let { SettingsActivity.openWebSite(it, "https://github.com/wasabeef/Blurry") }
                }
                "circleImgView" -> {
                    context?.let { SettingsActivity.openWebSite(it, "https://github.com/hdodenhof/CircleImageView") }
                }
                "exoPlayer" ->{
                    context?.let { SettingsActivity.openWebSite(it, "https://github.com/google/ExoPlayer") }
                }
                "mdcAndroid" -> {
                    context?.let {
                        SettingsActivity.openWebSite(
                            it,
                            "https://github.com/material-components/material-components-android"
                        )
                    }

                }
                "mdFont" -> {
                    context?.let {
                        SettingsActivity.openWebSite(
                            it,
                            "https://github.com/templarian/MaterialDesign"
                        )
                    }
                }
                "recyclerAni" -> {
                    context?.let {
                        SettingsActivity.openWebSite(
                            it,
                            "https://github.com/wasabeef/recyclerview-animators"
                        )
                    }
                }
                "uamp" ->{
                    context?.let { SettingsActivity.openWebSite(it, "https://github.com/android/uamp") }
                }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    companion object {
        fun openWebSite(ctx: Context, url: String) {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(ctx.packageManager) != null) {
                ctx.startActivity(intent)
            }
        }
    }
}