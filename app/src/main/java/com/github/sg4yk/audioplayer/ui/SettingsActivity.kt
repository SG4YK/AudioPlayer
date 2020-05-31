package com.github.sg4yk.audioplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.sg4yk.audioplayer.ui.LegalInfoActivity
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.utils.Generic
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.post {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        val frag = SettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, frag)
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            when (preference?.key) {
                "version" -> {
//                    activity?.let { Generic.openWebsite(it, "https://github.com/SG4YK/AudioPlayer/releases" ) }
                }
                "developer" -> {
                    activity?.let { Generic.openWebsite(it, "https://sg4yk.com") }
                }
                "sourceCode" -> {
                    activity?.let { Generic.openWebsite(it, "https://github.com/SG4YK/AudioPlayer") }
                }
                "legalInfo" -> {
                    val intent = Intent(context, LegalInfoActivity::class.java)
                    context?.startActivity(intent)
                }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}