package com.github.sg4yk.audioplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import com.github.sg4yk.audioplayer.utils.Generic.crossFade
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PrefManager
import io.alterac.blurkit.BlurKit
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout_light.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    companion object {
        private val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        const val PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var decorView: View
    private lateinit var defaultBg: Drawable
    private var controller: MediaControllerCompat? = null
    private lateinit var connectionObserver: Observer<Boolean>
    private lateinit var metadataObserver: Observer<MediaMetadataCompat>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BlurKit.init(application);
        // change status bar color when open drawer
        decorView = window.decorView
        drawerLayout.addDrawerListener(
            object : DrawerLayout.DrawerListener {
                private var lastNavSlideOffset: Float = 0f

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


        // setup toolbar and navigation
        navHost.post {
            val navControl = findNavController(R.id.navHost)

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_playlist, R.id.nav_library, R.id.nav_album, R.id.nav_artist
                ),
                drawerLayout
            )

            toolbar.setupWithNavController(navControl, appBarConfiguration)

            navDrawer.setupWithNavController(navControl)

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

            // setup menu
            toolbar.inflateMenu(R.menu.app_bar_menu_main)
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_search -> {
                        // TODO
                        true
                    }
                    R.id.menu_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
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

        if (checkPermissionStatus()) {
            PlaybackManager.connectPlaybackService(this)
        } else {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            return
        }

        // setup fab
        fab.hide()
        fab.setOnClickListener { v ->
            val intent = Intent(this, NowPlayingActivity::class.java)
            if (!PrefManager.animationReduced(this)) {
                val location = IntArray(2)
                v.getLocationInWindow(location)

                val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "reveal")
                val extras = intArrayOf(
                    location[0] + fab.width / 2,
                    location[1] + fab.height / 2,
                    fab.width
                )
                intent.putExtra(NowPlayingActivity.EXTRA_TAG, extras)

                startActivity(intent, activityOptions.toBundle())
            } else {
                startActivity(intent)
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            delay(500)
            fab.show()
        }

        // setup control when service is connected
        connectionObserver = Observer<Boolean> {
            if (it && controller == null) {
                // Stop observing connection
                defaultBg = getDrawable(R.color.colorAccent)!!

                PlaybackManager.isConnected().removeObserver(connectionObserver)

                buttonPlay.setOnClickListener {
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

                buttonSkipNext.setOnClickListener { PlaybackManager.skipNext() }

                buttonSkipPrevious.setOnClickListener { PlaybackManager.skipPrevious() }

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

        defaultBg = getDrawable(R.color.colorAccent)!!
    }

    private fun checkPermissionStatus(): Boolean {
        permissions.forEach {
            if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (PERMISSION_REQUEST_CODE) {
            1 -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // restart activity
                    finish()
                    startActivity(intent)
                } else {
                    showToast("Permission denied")
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

    private fun updateUI(metadata: MediaMetadataCompat?) {
        if (metadata == null || metadata.description.mediaUri == null) {
            navHeaderTitle.text = "Nothing playing"
            navHeaderArtist.text = null
            navHeaderAlbum.text = null
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                val artist = metadata.description?.subtitle ?: "Unknown artist"
                val album = metadata.description?.description ?: "Unknown album"

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

    private fun setDrawerBg(bitmap: Bitmap?) {

        GlobalScope.launch(Dispatchers.Main) {
            // define which image view should be shown
            val targetBg = if (navHeaderBg.visibility == View.GONE) {
                navHeaderBg
            } else {
                navHeaderBg2
            }

            // prepare next background
            if (bitmap != null) {
//                Blurry.with(this@MainActivity)
//                    .async()
//                    .radius(2)
//                    .color(Color.argb(76, 0, 0, 0))
//                    .from(bitmap).into(targetBg)
                targetBg.setImageBitmap(BlurKit.getInstance().blur(bitmap, 2))
            } else {
                targetBg.setImageDrawable(defaultBg)
            }

            // start transition
            crossFade(navHeaderBg, navHeaderBg2, 250)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackManager.nowPlaying().removeObservers(this)
        PlaybackManager.closeConnection()
    }
}