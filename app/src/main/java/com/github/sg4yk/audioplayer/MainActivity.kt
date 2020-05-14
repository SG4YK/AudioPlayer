package com.github.sg4yk.audioplayer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.github.sg4yk.audioplayer.utils.AudioHunter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_now_playing.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var navControl: NavController
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navDrawer: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appBarLayout: AppBarLayout = findViewById(R.id.app_bar_layout_light)
        appBarLayout.post {
            toolbar = findViewById(R.id.toolbar)
            nav_host.post {
                navControl = findNavController(R.id.nav_host)

                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        // top level fragments
                        R.id.nav_playlist, R.id.nav_library, R.id.nav_album, R.id.nav_folder, R.id.nav_artist
                    ),
                    findViewById<DrawerLayout>(R.id.drawer_layout)
                )

                navDrawer = findViewById(R.id.nav_drawer)
                navDrawer.post {
                    val navHeaderBg: ImageView = findViewById(R.id.nav_header_bg)
                    navHeaderBg.post {
                        val drawable: Drawable? = getDrawable(R.drawable.lucas_benjamin_unsplash)
                        if (drawable != null) {
                            Blurry.with(this).radius(1).sampling(4).color(Color.argb(76, 0, 0, 0))
                                .from(drawable.toBitmap()).into(navHeaderBg)
                        } else {
                            navHeaderBg.setBackgroundColor(R.color.colorAccent)
                        }
                    }
                }

                toolbar.setupWithNavController(navControl, appBarConfiguration)

                findViewById<NavigationView>(R.id.nav_drawer).setupWithNavController(navControl)
                navControl.addOnDestinationChangedListener { controller, destination, arguments ->

                    // Change toolbar title when navigate between fragments
                    toolbar.title = when (destination.id) {
                        R.id.nav_playlist -> getString(R.string.playlist)
                        R.id.nav_library -> getString(R.string.library)
                        R.id.nav_album -> getString(R.string.album)
                        R.id.nav_folder -> getString(R.string.folder)
                        R.id.nav_artist -> getString(R.string.artist)
                        else -> getString(R.string.app_name)
                    }

//                    navDrawer.menu.getItem(0).actionView.get
                }
            }

            toolbar.inflateMenu(R.menu.app_bar_menu_main)
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_search -> {
                        true
                    }
                    R.id.menu_settings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
//                        val activityOptions = ActivityOptionsCompat.makeClipRevealAnimation(toolbar,0,0,
//                            toolbar.width,toolbar.height)
                        startActivity(intent)
                        true
                    }
                    R.id.menu_exit -> {
                        finish()
                        true
                    }
                    else -> true
                }
            }
        }

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.post {
            fab.setOnClickListener { v ->
                val location = IntArray(2)
                v.getLocationInWindow(location)
//                val activityOptions = ActivityOptionsCompat.makeClipRevealAnimation(v, v.width / 2, v.height / 2, 0, 0)
                val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "reveal")
                val intent = Intent(this, NowPlayingActivity::class.java)
                intent.putExtra("fabX", location[0] + fab.width / 2)
                intent.putExtra("fabY", location[1] + fab.height / 2)
                intent.putExtra("fabD", fab.width)
                startActivity(intent, activityOptions.toBundle())
            }
        }


        val audioList = AudioHunter.getAllAudios(this)
        audioList.forEach { audio ->
            Log.d("scanaudio", audio.title)
        }
        Log.d("scanaudio", "scancomplete")
    }
}
