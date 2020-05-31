package com.github.sg4yk.audioplayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.Generic.setLightStatusBar
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PrefManager
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_layout_light.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val BUTTON_STATE_PLAYING = 1
        private const val BUTTON_STATE_NOT_PLAYING = 0
    }

    private lateinit var decorView: View
    private var controller: MediaControllerCompat? = null
    private lateinit var connectionObserver: Observer<Boolean>
    private lateinit var metadataObserver: Observer<MediaMetadataCompat>
    private lateinit var playbackStateObserver: Observer<PlaybackStateCompat>
    private var skipLock = false
    private lateinit var imgLoader: RequestManager
    private lateinit var blur: Blurry.Composer
    private lateinit var viewModel: AppViewModel
    private var currentButtonState = BUTTON_STATE_NOT_PLAYING

    private val permissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // change status bar color when open drawer
        decorView = window.decorView

        imgLoader = Glide.with(this)
        blur = Blurry.with(this).async().radius(2)

        viewModel = ViewModelProvider(this).get(AppViewModel::class.java).apply {
            refreshAll()
        }

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

            infoArea.setOnClickListener {
                this@MainActivity.startActivity(Intent(this@MainActivity, NowPlayingActivity::class.java))
            }

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
                if (destination.id == R.id.nav_playlist) {
                    toolbar.menu.clear()
                    toolbar.inflateMenu(R.menu.menu_playlist_main)
                } else {
                    toolbar.menu.clear()
                    toolbar.inflateMenu(R.menu.app_bar_menu_main)
                }
            }

            // setup menu
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_search -> {
                        // TODO
                        true
                    }
                    R.id.menu_create_playlist -> {
                        createPlaylist(this)
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

        if (checkPermissionStatus()) {
            PlaybackManager.connectPlaybackService(this)
            GlobalScope.launch(Dispatchers.Main) {
                delay(1000)
                fab.show()
            }
            startObserve()
        } else {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun startObserve() {
        // setup control when service is connected
        connectionObserver = Observer<Boolean> {
            if (it && controller == null) {
                // Stop observing connection
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

                buttonSkipNext.setOnClickListener {
                    if (!skipLock) {
                        PlaybackManager.skipNext()
                        PlaybackManager.play()
                    }
                }

                buttonSkipPrevious.setOnClickListener {
                    if (!skipLock) {
                        PlaybackManager.skipPrevious()
                        PlaybackManager.play()
                    }
                }

                // observe metadata
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

                // observe playback state
                playbackStateObserver = object : Observer<PlaybackStateCompat> {
                    private var cachedState = PlaybackStateCompat.STATE_NONE
                    override fun onChanged(state: PlaybackStateCompat?) {
                        if (state?.state == cachedState) {
                            return
                        }
                        cachedState = state?.state ?: PlaybackStateCompat.STATE_NONE
                        when (state?.state) {
                            PlaybackStateCompat.STATE_PLAYING -> {
                                setButtonState(BUTTON_STATE_PLAYING)
                            }
                            PlaybackStateCompat.STATE_BUFFERING -> {
                                // DO NOTHING
                            }
                            else -> {
                                setButtonState(BUTTON_STATE_NOT_PLAYING)
                            }
                        }
                    }
                }
                PlaybackManager.playbackState().observe(this, playbackStateObserver)
            }
        }
        PlaybackManager.isConnected().observe(this, connectionObserver)
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
                    // refresh data
                    viewModel.refreshAll()
                    GlobalScope.launch(Dispatchers.Main) {
                        startObserve()
                        delay(1000)
                        fab.show()
                    }
                } else {
                    showToast("Permission denied")
                }
            }
        }
    }

    private fun updateUI(metadata: MediaMetadataCompat?) {
        skipLock = true
        if (metadata == null || metadata.description.mediaUri == null) {
            navHeaderTitle.text = "Nothing playing"
            navHeaderArtist.text = null
            navHeaderAlbum.text = null
        } else {
            GlobalScope.launch {
                // set album art
                val albumId = MediaHunter.getAlbumIdFromAudioId(
                    this@MainActivity,
                    metadata.description.mediaId!!.toLong()
                )
                if (albumId != MediaHunter.ALBUM_NOT_EXIST) {
                    try {
                        val futureTarget = imgLoader.asBitmap()
                            .load(MediaHunter.getArtUriFromAlbumId(albumId))
                            .submit(50, 50)

                        delay(50)
                        val bitmap = futureTarget.get()
                        imgLoader.clear(futureTarget)
                        setDrawerBg(bitmap)
                    } catch (e: Exception) {
                        Log.w("MainActivity", e.message)
                        setDrawerBg(null)

                    }
                } else {
                    setDrawerBg(null)
                }
            }

            // set metadata
            navHeaderTitle.text = metadata.description?.title ?: "Unknown Title"
            navHeaderArtist.text = metadata.description?.subtitle ?: "Unknown artist"
            navHeaderAlbum.text = metadata.description?.description ?: "Unknown album"
        }
    }

    private fun setDrawerBg(bitmap: Bitmap?) {
        val nextView = viewSwitcher.nextView as ImageView

        GlobalScope.launch(Dispatchers.Main) {
            // prepare next background
            if (bitmap != null) {
                try {
                    Palette.from(bitmap).generate { palette ->
                        val luminance = Generic.luminance(palette?.getDominantColor(Color.WHITE) ?: 200)

                        // Darken the image depending on its dominant color
                        // Alpha is between 20 and 76
                        var alpha = 20
                        if (luminance > 170) {
                            alpha = if (luminance > 226) {
                                76
                            } else {
                                luminance - 150
                            }
                        }
                        blur.color(Color.argb(alpha, 0, 0, 0)).from(bitmap).into(nextView)
                    }
                } catch (e: Exception) {
                    Log.w("MainActivity", e.message)
                    nextView.setImageResource(R.color.colorAccent)
                }
            } else {
                nextView.setImageResource(R.color.colorAccent)
            }
            viewSwitcher.showNext()

            delay(300)
            skipLock = false
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

    private fun setButtonState(state: Int) {
        if (state == currentButtonState) {
            return
        }
        currentButtonState = state
        when (state) {
            BUTTON_STATE_NOT_PLAYING -> {
                buttonPlay.setImageResource(R.drawable.avd_pause_to_play)
                val icon = buttonPlay.drawable as AnimatedVectorDrawable
                icon.start()
            }

            BUTTON_STATE_PLAYING -> {
                buttonPlay.setImageResource(R.drawable.avd_play_to_pause)
                val icon = buttonPlay.drawable as AnimatedVectorDrawable
                icon.start()
            }
        }
    }

    private fun createPlaylist(ctx: Context) {
        val maxLength = 30
        MaterialDialog(ctx).show {
            input(maxLength = maxLength)
            getInputLayout().apply {
                hint = "Playlist name"
                this.setStartIconDrawable(R.drawable.ic_playlist_add_gray_24dp)
                this.isCounterEnabled = true
            }
            title(R.string.create_playlist, null)
            negativeButton(R.string.cancel)
            positiveButton(R.string.create) { dialog ->
                val name = dialog.getInputField().text.toString()
                GlobalScope.launch(Dispatchers.IO) {
                    MediaHunter.createPlaylist(ctx, name)
                    viewModel.refreshPlaylistItems()
                }
            }
        }
    }
}

const val PERMISSION_REQUEST_CODE = 1