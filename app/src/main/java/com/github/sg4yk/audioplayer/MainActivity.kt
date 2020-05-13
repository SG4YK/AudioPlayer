package com.github.sg4yk.audioplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
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
            toolbar = findViewById(R.id.tool_bar)
            val navHost: View = findViewById(R.id.nav_host)
            nav_host.post {
                val navView: NavigationView = findViewById(R.id.nav_drawer)

                navControl = findNavController(R.id.nav_host)
                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.nav_playlist, R.id.nav_library, R.id.nav_album, R.id.nav_folder, R.id.nav_artist
                    ), findViewById<DrawerLayout>(R.id.drawer_layout)
                )
                toolbar.setupWithNavController(navControl, appBarConfiguration)
                findViewById<NavigationView>(R.id.nav_drawer).setupWithNavController(navControl)
                navControl.addOnDestinationChangedListener { controller, destination, arguments ->
                    toolbar.title = when (destination.id) {
                        R.id.nav_library -> getString(R.string.library)
                        R.id.nav_playlist -> getString(R.string.playlist)
                        R.id.nav_album -> getString(R.string.album)
                        R.id.nav_folder -> getString(R.string.folder)
                        R.id.nav_artist -> getString(R.string.artist)
                        else -> getString(R.string.app_name)
                    }
                }
            }

            toolbar.inflateMenu(R.menu.app_bar_menu_main)
            toolbar.setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.menu_exit -> {
                        finishAfterTransition()
                        true
                    }
                    else -> true
                }
            }
        }

        val drawer: NavigationView = findViewById(R.id.nav_drawer)
    }
}
