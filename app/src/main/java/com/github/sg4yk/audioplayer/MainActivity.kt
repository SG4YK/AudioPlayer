package com.github.sg4yk.audioplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.github.sg4yk.audioplayer.utils.PrefManager
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
    private lateinit var playButton: FloatingActionButton
    private lateinit var skipPreviousButton: FloatingActionButton
    private lateinit var skipNextButton: FloatingActionButton
    private var controller: MediaControllerCompat? = null
    private lateinit var connectionObserver: Observer<Boolean>
    private lateinit var metadataObserver: Observer<MediaMetadataCompat>

    private val permissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity","create")
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

        val appBarLayout: ConstraintLayout = findViewById(R.id.app_bar_layout_light)
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
                        finishAffinity()
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
            playButton = findViewById(R.id.nav_button_play)
            skipPreviousButton = findViewById(R.id.nav_button_previous)
            skipNextButton = findViewById(R.id.nav_button_next)
        }

        if (checkPermissionStatus()) {
            PlaybackManager.connectPlaybackService(this)
        } else {
            grantPermissions()
        }

        // setup control when connected
        connectionObserver = Observer<Boolean> {
            if (it && controller == null) {
                // Stop observing connection
                PlaybackManager.isConnected().removeObserver(connectionObserver)

                playButton.setOnClickListener {
                    when (PlaybackManager.playbackState().value?.state) {
                        PlaybackStateCompat.STATE_NONE -> {
                            PlaybackManager.playAll()
                        }
                        PlaybackStateCompat.STATE_PLAYING -> {
                            PlaybackManager.pause()
                        }
                        PlaybackStateCompat.STATE_PAUSED -> {
                            PlaybackManager.play()
                        }
                    }
                }

                skipNextButton.setOnClickListener { PlaybackManager.skipNext() }

                skipPreviousButton.setOnClickListener { PlaybackManager.skipPrevious() }

                // Start observing metadata
                metadataObserver = object : Observer<MediaMetadataCompat> {
                    private var cachedMediaUri: Uri? = Uri.EMPTY

                    override fun onChanged(metadata: MediaMetadataCompat?) {
                        if (metadata == null) {
                            // TODO
                            return
                        }
                        if (metadata.description.mediaUri != cachedMediaUri) {
                            cachedMediaUri = metadata.description.mediaUri
                            updateUI(metadata)
                        }
                    }
                }
                PlaybackManager.nowPlaying().observe(this, metadataObserver)
            }
        }
        PlaybackManager.isConnected().observe(this, connectionObserver)

    }

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
        GlobalScope.launch(Dispatchers.Main) {
            if (on) {
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
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
                Blurry.with(this@MainActivity).async().radius(2).color(Color.argb(76, 0, 0, 0))
                    .from(bitmap).into(targetBg)
                crossFade(navHeaderBg, navHeaderBg2, 250)
            } else {
                navHeaderBg.setBackgroundColor(R.color.colorAccent)
            }
        }
    }

    private fun updateUI(metadata: MediaMetadataCompat?) {
        GlobalScope.launch {
            if (metadata == null || metadata.description.mediaUri == null) {
                GlobalScope.launch(Dispatchers.Main) {
                    navHeaderTitle.text = "Nothing playing"
                    navHeaderArtist.text = null
                    navHeaderAlbum.text = null
                }
            } else {
                var artist =  metadata.description?.subtitle?:"Unknown artist"
                var album =  metadata.description?.description?:"Unknown album"
                async {
                    val bitmap = metadata.description?.mediaUri?.let {
                        MediaHunter.getThumbnail(this@MainActivity, it, 48)
                    }
                    setDrawerBg(bitmap)
                }
                async {
                    withContext(Dispatchers.Main) {
                        navHeaderTitle.text = metadata.description?.title ?: "Unknown Title"
                        navHeaderArtist.text = artist
                        navHeaderAlbum.text = album
                    }
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        PlaybackManager.nowPlaying().removeObservers(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackManager.closeConnection()
    }

    override fun onBackPressed() {
        finish()
    }
}
