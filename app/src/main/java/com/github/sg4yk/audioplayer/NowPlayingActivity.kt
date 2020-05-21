package com.github.sg4yk.audioplayer

import android.animation.Animator
import android.graphics.Bitmap
import android.graphics.Color
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Observer
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.utils.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_now_playing.*
import kotlinx.coroutines.*
import kotlin.math.hypot


class NowPlayingActivity : AppCompatActivity() {

    private var fabX = 0
    private var fabY = 0
    private var fabD = 0
    private var radius = 0f
    private var backPressLock = false

    private lateinit var rootLayout: View
    private lateinit var backgroundImg: ImageView
    private lateinit var backgroundImg2: ImageView
    private lateinit var albumArt: ImageView
    private lateinit var albumArt2: ImageView
    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var seekbarJob: Job
    private lateinit var mediaDuration: AppCompatTextView
    private lateinit var position: AppCompatTextView
    private lateinit var timebar: DefaultTimeBar
    private lateinit var connectionObserver: Observer<Boolean>
    private lateinit var metadataObserver: Observer<MediaMetadataCompat>
    private lateinit var playbackStateObserver: Observer<PlaybackStateCompat>

    private var controller = MediaControllerCompat(this, PlaybackManager.sessionToken())
    private var skipLock = false
    private var setBgJob: Job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar).also {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        backgroundImg = findViewById(R.id.background)
        backgroundImg2 = findViewById(R.id.background2)
        albumArt = findViewById(R.id.album_art)
        albumArt2 = findViewById(R.id.album_art2)
        mediaDuration = findViewById(R.id.duration)
        position = findViewById(R.id.position)
        seekBar = findViewById(R.id.seekbar)

//        timebar = findViewById(R.id.defa)


        seekbar.post {
            // set coroutine job for seekbar

//            seekbarJob.start()

            // on slide behavior
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        PlaybackManager.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    try {
                        seekbarJob.cancel()
                    } catch (e: Exception) {
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekbarJob = GlobalScope.launch {
                        while (isActive) {
                            updateProgress()
                            delay(1000L)
                        }
                    }
                }
            })
        }

        // set buttons behavior
        val playButton = findViewById<FloatingActionButton>(R.id.button_play).apply {
            setOnClickListener {
                when (controller?.playbackState?.state) {
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
        }

        val skipPreviousButton = findViewById<FloatingActionButton>(R.id.button_previous).apply {
            setOnClickListener {
                if (!skipLock) {
                    seekbar.progress = 0
                    position.text = "00:00"
                    PlaybackManager.skipPrevious()
                    PlaybackManager.play()
                }
            }
        }

        val skipNextButton = findViewById<FloatingActionButton>(R.id.button_next).apply {
            setOnClickListener {
                if (!skipLock) {
                    seekbar.progress = 0
                    position.text = "00:00"
                    PlaybackManager.skipNext()
                    PlaybackManager.play()
                }
            }
        }

        controller.registerCallback(object : MediaControllerCompat.Callback() {
            private var cachedMediaUri = ""
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                if (metadata != null && metadata.description.title != null
                    && metadata.description.mediaUri.toString() != cachedMediaUri
                ) {
                    updateMetadata(metadata)
                }
            }
        })

        // Initialize metadata and seekbar
        connectionObserver = Observer { connected ->
            if (connected) {
                PlaybackManager.isConnected().removeObserver(connectionObserver)
                val metadata = PlaybackManager.nowPlaying().value
                updateMetadata(metadata, 0, 0)

                playbackStateObserver = Observer<PlaybackStateCompat> { state ->
                    try {
                        seekbarJob.cancel()
                    } catch (e: Exception) {
                    }
                    when (state.state) {
                        PlaybackState.STATE_PLAYING -> {
                            seekbarJob = GlobalScope.launch {
                                while (isActive) {
                                    updateProgress()
                                    delay(1000L)
                                }
                            }
                        }
                    }
                }
                PlaybackManager.playbackState().observe(this, playbackStateObserver)
            }
        }
        PlaybackManager.isConnected().observe(this, connectionObserver)

        // set enter animation
        if (PrefManager.revealAnimationEnabled(this)) {
            fabX = intent.getIntExtra("fabX", 0)
            fabY = intent.getIntExtra("fabY", 0)
            fabD = intent.getIntExtra("fabD", 0)
            radius = hypot(fabX.toDouble(), fabY.toDouble()).toFloat()
            rootLayout = findViewById(R.id.rootLayout)
            rootLayout.visibility = View.INVISIBLE
            rootLayout.post {
                startRevealAnim(fabX, fabY)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }


    private fun startRevealAnim(centerX: Int, centerY: Int) {
        backPressLock = true
        val endRadius = hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, centerX, centerY, fabD.toFloat() / 2, endRadius)
        anim.duration = 1000
        anim.interpolator = DecelerateInterpolator(2.4f)
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                backPressLock = false
            }

        })
        rootLayout.visibility = View.VISIBLE
        anim.start()
    }

    override fun onBackPressed() {
        if (backPressLock) {
            return
        }
        try {
            seekbarJob.cancel()
        } catch (e: Exception) {
        }
        backPressLock = true
        if (PrefManager.revealAnimationEnabled(this)) {
            val endRadius = 100.0f
            val anim = ViewAnimationUtils.createCircularReveal(rootLayout, fabX, fabY, radius, fabD.toFloat() / 2)
            anim.duration = 500
            anim.interpolator = AccelerateInterpolator()
            anim.doOnEnd {
                rootLayout.visibility = View.INVISIBLE
                finishAfterTransition()
            }
            anim.start()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        backPressLock = false
    }

    private fun updateProgress() {
        GlobalScope.launch(Dispatchers.Main) {
            val pos = controller.playbackState.position
            val dur = controller.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            if (dur != 0L) {
                val percentage = 100 * pos / dur
                seekBar.progress = percentage.toInt()
                position.text = Generic.msecToStr(pos)
            }
        }
    }

    //    @WorkerThread
    private fun updateMetadata(metadata: MediaMetadataCompat?, duration: Long = 300, bgDelay: Long = 1000) {
        GlobalScope.launch(Dispatchers.Main) {
            // Disable skip when updating metadata
            skipLock = true
            async {
                val uri = metadata?.description?.mediaUri ?: null
                var bitmap: Bitmap? = null
                if (uri != null) {
                    bitmap = MediaHunter.getThumbnail(
                        this@NowPlayingActivity,
                        uri, 320
                    )
                }
                setAlbumAndBg(bitmap, duration, bgDelay)

                // wait for animation to finish
                delay(duration + 100)
                skipLock = false
            }
            toolbar.title = metadata?.description?.title ?: "Unknown title"
            val artist = metadata?.description?.subtitle ?: "Unknown artist"
            val album = metadata?.description?.description ?: "Unknown album"
            toolbar.subtitle = "$artist - $album"
            mediaDuration.text =
                Generic.msecToStr(controller.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        }
    }

    private suspend fun setAlbumAndBg(bitmap: Bitmap?, duration: Long, bgDelay: Long = 0) {
        if (bitmap != null) {
            val targetAlbumArt = if (albumArt.visibility == View.GONE) {
                albumArt
            } else {
                albumArt2
            }
            val targetBG = if (backgroundImg.visibility == View.GONE) {
                backgroundImg
            } else {
                backgroundImg2
            }
            Blurry.with(this).async().radius(2).sampling(4).color(Color.argb(128, 0, 0, 0))
                .from(bitmap).into(targetBG)
            targetAlbumArt.setImageBitmap(bitmap)
            Generic.crossFade(albumArt, albumArt2, duration)

            // only the last one will apply
            setBgJob.cancel()
            setBgJob = GlobalScope.launch {
                delay(bgDelay)
                if (isActive) {
                    backgroundImg.post { Generic.crossFade(backgroundImg, backgroundImg2, duration) }
                }
            }
        } else {
            setAlbumAndBg(getDrawable(R.drawable.default_album_art_blue)!!.toBitmap(300, 300), duration)
        }
    }
}
