package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
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
                    context?.let { openWebSite(it, "https://github.com/SG4YK/AudioPlayer/releases") }
                }
                "developer" -> {
                    context?.let { openWebSite(it, "https://sg4yk.com") }
                }
                "sourceCode" -> {
                    context?.let { openWebSite(it, "https://github.com/SG4YK/AudioPlayer") }
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