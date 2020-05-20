package com.github.sg4yk.audioplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.github.sg4yk.audioplayer.utils.Generic.crossFade
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PlaybackService
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var navControl: NavController
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navDrawer: NavigationView
    private var lastNavSlideOffset: Float = 0f
    private lateinit var decorView: View
    private lateinit var navHeaderBg: ImageView
    private lateinit var navHeaderBg2: ImageView
    private lateinit var navHeaderTitle: TextView
    private lateinit var navHeaderArtist: TextView
    private lateinit var navHeaderAlbum: TextView
    private var controller: MediaControllerCompat? = null
    private lateinit var serviceIntent: Intent
    private lateinit var observer: Observer<Boolean>

    private val permissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "Oncreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

        serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)
        if (checkPermissionStatus()) {
            PlaybackManager.connectPlaybackService(this)

        } else {
            grantPermissions()
        }
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


                // setup nav header button
                val playButton: FloatingActionButton = findViewById(R.id.nav_button_play)
                playButton.post {
                    playButton.setOnClickListener {
                        PlaybackManager.playAll()
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
                        Thread {
                            val intent = Intent(this, SettingsActivity::class.java)
                            startActivity(intent)
                        }.start()
                        true
                    }
                    R.id.menu_exit -> {
                        PlaybackManager.stopPlaybackService(this)
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
                val intent = Intent(this, NowPlayingActivity::class.java)

                if (PrefManager.revealAnimationEnabled(this)) {
                    val location = IntArray(2)
                    v.getLocationInWindow(location)
                    val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "reveal")
                    intent.putExtra("fabX", location[0] + fab.width / 2)
                    intent.putExtra("fabY", location[1] + fab.height / 2)
                    intent.putExtra("fabD", fab.width)
                    startActivity(intent, activityOptions.toBundle())
                } else {
                    startActivity(intent)
                }
            }
        }

        navDrawer = findViewById(R.id.nav_drawer)
        navDrawer.post {
            navHeaderBg = findViewById(R.id.nav_header_bg)
            navHeaderBg2 = findViewById(R.id.nav_header_bg2)
            navHeaderTitle = findViewById(R.id.nav_header_title)
            navHeaderArtist = findViewById(R.id.nav_header_artist)
            navHeaderAlbum = findViewById(R.id.nav_header_album)

            // set controller when connected
            observer = Observer<Boolean> {
                if (it && controller == null) {
                    controller = MediaControllerCompat(this, PlaybackManager.sessionToken()).apply {
                        registerCallback(object : MediaControllerCompat.Callback() {
                            private var cachedMediaUri = ""
                            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                                if (metadata != null && metadata.description.title != null
                                    && metadata.description.mediaUri.toString() != cachedMediaUri
                                ) {
                                    cachedMediaUri = metadata.description.mediaUri.toString()
                                    updateUI(metadata)
                                }
                            }
                        })
                    }
                    // Stop observing when controller set
                    PlaybackManager.isConnected().removeObserver(observer)
                }
            }

            PlaybackManager.isConnected().observe(this, observer)
        }


    }
    // end of onCreate()


    override fun onStart() {
        super.onStart()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Thread {
                        PlaybackManager.connectPlaybackService(this)
                        controller = MediaControllerCompat(this, PlaybackManager.sessionToken())
                    }.start()
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

    private suspend fun setDrawerBg(bitmap: Bitmap?) {
        withContext(Dispatchers.Main) {
            if (bitmap != null) {
                val targetBg = if (navHeaderBg.visibility == View.GONE) {
                    navHeaderBg
                } else {
                    navHeaderBg2
                }
                Blurry.with(applicationContext).async().radius(2).color(Color.argb(100, 0, 0, 0))
                    .from(bitmap).into(targetBg)
                crossFade(navHeaderBg, navHeaderBg2, 300)
            } else {
                navHeaderBg.setBackgroundColor(R.color.colorAccent)
            }
        }
    }

    private fun updateUI(metadata: MediaMetadataCompat?) {
        GlobalScope.launch {
            val subtitle = metadata?.description?.subtitle
            var artist = "Unknwon Artist"
            var album = "Unknown Album"
            if (subtitle != null) {
                val splited = subtitle.split(" - ")
                artist = splited[0]
                album = splited[1]
            }
            async {
                val bitmap = metadata?.description?.mediaUri?.let {
                    MediaHunter.getThumbnail(applicationContext, it, 64)
                }
                setDrawerBg(bitmap)
            }
            async {
                withContext(Dispatchers.Main) {
                    navHeaderTitle.text = metadata?.description?.title ?: "Unknown Title"
                    navHeaderArtist.text = artist
                    navHeaderAlbum.text = album
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    override fun onBackPressed() {
        finish()
    }


}
