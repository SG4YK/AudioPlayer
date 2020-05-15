package com.github.sg4yk.audioplayer

//import androidx.test.espresso.core.internal.deps.guava.base.Joiner.on
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.github.sg4yk.audioplayer.utils.AudioHunter
import com.github.sg4yk.audioplayer.utils.PlaybackEngine
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var navControl: NavController
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navDrawer: NavigationView
    private var lastNavSlideOffset: Float = 0f
    private lateinit var decorView: View
    private lateinit var navHeaderBg: ImageView

    private val permissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // change status bar color when open drawer
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.post {
            decorView = window.decorView
            drawerLayout.addDrawerListener(

                object : DrawerLayout.DrawerListener {
                    override fun onDrawerStateChanged(newState: Int) {}
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                        if (lastNavSlideOffset <= 0.2f && slideOffset >= 0.2f) {
                            setLightStatusBar(decorView, false)
                        } else if (lastNavSlideOffset >= 0.2f && slideOffset <= 0.2f) {
                            setLightStatusBar(decorView, true)
                        }
                        lastNavSlideOffset = slideOffset
                    }

                    override fun onDrawerClosed(drawerView: View) {
                        setLightStatusBar(decorView, true)
                    }

                    override fun onDrawerOpened(drawerView: View) {
                        setLightStatusBar(decorView, false)
                    }
                }

            )
        }


        val appBarLayout: AppBarLayout = findViewById(R.id.app_bar_layout_light)
        appBarLayout.post {
            toolbar = findViewById(R.id.toolbar)
            nav_host.post {

                // setup navigation
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

                navDrawer = findViewById(R.id.nav_drawer)
                navDrawer.post {
                    navHeaderBg = findViewById(R.id.nav_header_bg)
                    navHeaderBg.post {
                        setDrawerBg(getDrawable(R.drawable.lucas_benjamin_unsplash))
                    }
                }

                // setup nav header
                val playButton: FloatingActionButton = findViewById(R.id.nav_button_play)
                playButton.post {
                    playButton.setOnClickListener { v ->
                        val audioList = AudioHunter.audioList
                        Log.d("AudioHunter", "clicked")
                        audioList.forEach {
                            val audioList = AudioHunter.audioList
                            audioList.forEach {
                                Log.d("AudioHunter", it.toString())
                            }
                        }
                PlaybackEngine.play(this, audioList[0])
                    }
                }
            }

            // setup menu
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
                        PlaybackEngine.stop()
                        finish()
                        true
                    }
                    else -> true
                }
            }
        }

        // setup fab
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

        if (checkPermissionStatus()) {
            scanAllAudio()
        } else {
            grantPermissions()
        }
    }

    private fun checkPermissionStatus(): Boolean {
        permissions.forEach {
            if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun grantPermissions() {
        requestPermissions(permissions, 1)
    }

    private fun scanAllAudio() {
        Thread(Runnable {
            val audioList = AudioHunter.getAllAudio(this)
//            audioList.forEach { audio ->
//                Log.d("AudioHunter", audio.toString())
//            }
            Log.d("AudioHunter", "Scan Complete")
        }).start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanAllAudio()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLightStatusBar(view: View, on: Boolean) {
        if (on) {
            view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    private fun setDrawerBg(drawable: Drawable?) {
        if (drawable != null) {
            Blurry.with(this).radius(1).sampling(4).color(Color.argb(76, 0, 0, 0))
                .from(drawable.toBitmap()).into(navHeaderBg)
        } else {
            navHeaderBg.setBackgroundColor(R.color.colorAccent)
        }

    }

    private fun setDrawerBg(bitmap: Bitmap?) {
        if (bitmap != null) {
            Blurry.with(this).radius(1).sampling(4).color(Color.argb(76, 0, 0, 0))
                .from(bitmap).into(navHeaderBg)
        } else {
            navHeaderBg.setBackgroundColor(R.color.colorAccent)
        }
    }
}
