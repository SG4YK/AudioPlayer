package com.github.sg4yk.audioplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var navControl: NavController
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarConfiguration: AppBarConfiguration

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
                }
            }

            toolbar.inflateMenu(R.menu.app_bar_menu_main)
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
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
                intent.putExtra("startX", location[0] + v.width / 2)
                intent.putExtra("startY", location[1] + v.height / 2)
                startActivity(intent, activityOptions.toBundle())
            }
        }


    }
}
