package com.github.sg4yk.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.github.sg4yk.audioplayer.extensions.isPlayEnabled
import com.github.sg4yk.audioplayer.extensions.isPlaying
import com.github.sg4yk.audioplayer.extensions.isPrepared
import com.github.sg4yk.audioplayer.utils.Generic.crossFade
import com.github.sg4yk.audioplayer.utils.PlaybackService
import com.github.sg4yk.audioplayer.utils.PlaybackServiceConnection
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.github.sg4yk.audioplayer.utils.ServiceInjector
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
    private lateinit var navHeaderBg2: ImageView
    private lateinit var navHeaderTitle: TextView
    private lateinit var navHeaderArtist: TextView
    private lateinit var navHeaderAlbum: TextView
    private lateinit var connection: PlaybackServiceConnection

    private val permissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

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
//                        if (PlaybackManager.status() == PlaybackEngine.STATUS_STOPPED) {
//                            if (PlaybackManager.play()) {
//                                updateMetadata()
//                            }
//                        } else {
//                            PlaybackManager.playOrPause()
//                        }
                        playMedia("26")
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
                        stopService(Intent(this, PlaybackService::class.java))
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

        if (checkPermissionStatus()) {
            launchService()
        } else {
            grantPermissions()
        }
    }
    // end of onCreate()


    override fun onStart() {
        super.onStart()
        navDrawer = findViewById(R.id.nav_drawer)
        navDrawer.post {
            navHeaderBg = findViewById(R.id.nav_header_bg)
            navHeaderBg2 = findViewById(R.id.nav_header_bg2)
            navHeaderTitle = findViewById(R.id.nav_header_title)
            navHeaderArtist = findViewById(R.id.nav_header_artist)
            navHeaderAlbum = findViewById(R.id.nav_header_album)
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

    @WorkerThread
    private fun grantPermissions() {
        requestPermissions(permissions, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchService()
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

    private fun setDrawerBg(bitmap: Bitmap?) {
        if (bitmap != null) {
            val targetBg = if (navHeaderBg.visibility == View.GONE) {
                navHeaderBg
            } else {
                navHeaderBg2
            }
            Blurry.with(this).async().radius(4).sampling(4).color(Color.argb(128, 0, 0, 0))
                .from(bitmap).into(targetBg)
            crossFade(navHeaderBg, navHeaderBg2, 300)
        } else {
            navHeaderBg.setBackgroundColor(R.color.colorAccent)
        }
    }

    @WorkerThread
    private fun updateMetadata() {
//        val meta = PlaybackManager.currentMetadata ?: return
//        val drawerLayout: View = findViewById(R.id.drawer_layout)
//        drawerLayout.post {
//            val navDrawer: NavigationView = findViewById(R.id.nav_drawer)
//            navDrawer.post {
//                navHeaderBg.post { setDrawerBg(meta.albumArt) }
//                navHeaderBg2.post { setDrawerBg(meta.albumArt) }
//                navHeaderTitle.post { navHeaderTitle.text = meta.title }
//                navHeaderArtist.post { navHeaderArtist.text = meta.artist }
//                navHeaderAlbum.post { navHeaderAlbum.text = meta.album }
//            }
//        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateMetadata()
    }

    private fun launchService() {
        val intent = Intent(this, PlaybackService::class.java)
        startService(intent)
        connection = ServiceInjector.getPlaybackServiceConnection(this)
        Log.d("PlaybackService", "Launching")
//        bindService(
//            intent, serviceConnection, Context.BIND_AUTO_CREATE
//        )
    }

    fun playMedia(mediaId: String, pauseAllowed: Boolean = true) {
        val nowPlaying = connection.nowPlaying.value
        val transportControls = connection.transportControls

        val isPrepared = connection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
            connection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            "PlayMedia", "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=$mediaId)"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaId, null)
        }
    }
}
