package com.github.sg4yk.audioplayer.ui

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toBitmap
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.github.pwittchen.swipe.library.rx2.SwipeListener
import com.github.sg4yk.audioplayer.R
import com.github.sg4yk.audioplayer.extensions.isPlaying
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.MediaHunter
import com.github.sg4yk.audioplayer.playback.PlaybackManager
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.google.android.exoplayer2.ExoPlayer
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_now_playing.*
import kotlinx.coroutines.*
import kotlin.math.hypot


class NowPlayingActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TAG = "REVEAL_ANIM_EXTRA"
    }

    private val LOG_TAG = "NowPlayingActivity"
    private val BUTTON_STATE_PLAYING = 1
    private val BUTTON_STATE_NOT_PLAYING = 0

    private var fabX = 0
    private var fabY = 0
    private var fabD = 0
    private var radius = 0f
    private var backPressLock = false

    private lateinit var imgLoader: RequestManager
    private lateinit var blur: Blurry.Composer
    private lateinit var defaultAlbumArt: Bitmap

    private lateinit var connectionObserver: Observer<Boolean>
    private lateinit var metadataObserver: Observer<MediaMetadataCompat>
    private lateinit var playbackStateObserver: Observer<PlaybackStateCompat>
    private lateinit var repeatModeObserver: Observer<Int>
    private lateinit var shuffleModeObserver: Observer<Int>
    private lateinit var seekbarJob: Job

    private lateinit var audioManager: AudioManager

    private var setBgJob: Job = Job()

    private var skipLock = false
    private var currentButtonState = BUTTON_STATE_NOT_PLAYING

    private lateinit var controller: MediaControllerCompat
    private var enterAniPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        toolbar.post {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        controller = MediaControllerCompat(this, PlaybackManager.sessionToken())

        defaultAlbumArt = getDrawable(R.drawable.default_album_art_blue)!!.toBitmap(512, 512)

        imgLoader = Glide.with(this)
        blur = Blurry.with(this)
            .async()
            .color(Color.argb(76, 0, 0, 0))
            .radius(3)
            .sampling(8)

        seekbar.apply {
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        PlaybackManager.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    try {
                        seekbarJob.cancel()
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, e.message)
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    updateProgress()
                    if (PlaybackManager.playbackState().value?.state == PlaybackStateCompat.STATE_PLAYING) {
                        seekbarJob = GlobalScope.launch {
                            while (isActive) {
                                updateProgress()
                                delay(1000L)
                            }
                        }
                    }
                }
            })
        }

        // set buttons behavior
        buttonPlay.setOnClickListener {
            when (controller.playbackState?.state) {
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

        buttonSkipPrevious.setOnClickListener {
            if (!skipLock) {
                PlaybackManager.skipPrevious()
                PlaybackManager.play()
            }
        }

        buttonSkipNext.setOnClickListener {
            if (!skipLock) {
                PlaybackManager.skipNext()
                PlaybackManager.play()
            }
        }

        buttonRepeatMode.setOnClickListener {
            // switch repeat mode
            when (PlaybackManager.repeatMode().value) {
                ExoPlayer.REPEAT_MODE_OFF -> {
                    PlaybackManager.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL)
                }
                ExoPlayer.REPEAT_MODE_ALL -> {
                    PlaybackManager.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE)
                }
                ExoPlayer.REPEAT_MODE_ONE -> {
                    PlaybackManager.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF)
                }
            }
        }

        buttonShuffleMode.setOnClickListener {
            when (PlaybackManager.shuffleMode().value) {
                PlaybackStateCompat.SHUFFLE_MODE_NONE -> {
                    PlaybackManager.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                }
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> {
                    PlaybackManager.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                }
            }
        }


        // Swipe control
        albumArtSwitcher.post {
            val swipe = Swipe()
            swipe.setListener(object : SwipeListener {
                override fun onSwipingLeft(event: MotionEvent?) {
                }

                override fun onSwipedLeft(event: MotionEvent?): Boolean {
                    buttonSkipNext.performClick()
                    return true
                }

                override fun onSwipingRight(event: MotionEvent?) {

                }

                override fun onSwipedRight(event: MotionEvent?): Boolean {
                    buttonSkipPrevious.performClick()
                    return true
                }

                override fun onSwipingUp(event: MotionEvent?) {

                }

                override fun onSwipedUp(event: MotionEvent?): Boolean {
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                    return true
                }

                override fun onSwipedDown(event: MotionEvent?): Boolean {
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                    return true
                }

                override fun onSwipingDown(event: MotionEvent?) {
                }
            }
            )

            albumArtSwitcher.setOnTouchListener { view, event ->
                swipe.dispatchTouchEvent(event)
                true
            }
        }

        // Initialize metadata and seekbar job
        connectionObserver = Observer { connected ->
            if (connected) {
                PlaybackManager.isConnected().removeObserver(connectionObserver)

                playbackStateObserver = object : Observer<PlaybackStateCompat> {
                    private var inited = false

                    override fun onChanged(state: PlaybackStateCompat?) {
                        if (!inited) {
                            inited = true
                            updateProgress()
                            if (state != null && state.isPlaying) {
                                seekbarJob = GlobalScope.launch {
                                    while (isActive) {
                                        updateProgress()
                                        delay(1000L)
                                    }
                                }
                                setButtonState(BUTTON_STATE_PLAYING)
                            }
                        } else {
                            try {
                                seekbarJob.cancel()
                            } catch (e: Exception) {
                                Log.w(LOG_TAG, e.message)
                            }
                            if (state != null && state.state != PlaybackStateCompat.STATE_BUFFERING) {
                                if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                                    seekbarJob = GlobalScope.launch {
                                        while (isActive) {
                                            updateProgress()
                                            delay(1000L)
                                        }
                                    }
                                    setButtonState(BUTTON_STATE_PLAYING)
                                } else {
                                    setButtonState(BUTTON_STATE_NOT_PLAYING)
                                }
                            }
                        }
                    }
                }
                PlaybackManager.playbackState().observe(this, playbackStateObserver)

                metadataObserver = object : Observer<MediaMetadataCompat> {
                    private var cachedMediaUri: Uri? = Uri.EMPTY
                    private var inited = false

                    override fun onChanged(metadata: MediaMetadataCompat?) {
                        if (metadata == null) {
                            // TODO
                            return
                        }
                        if (metadata.description.mediaUri != cachedMediaUri) {
                            cachedMediaUri = metadata.description.mediaUri
                            if (inited) {
                                updateUI(metadata)
                            } else {
                                inited = true
                                updateUI(PlaybackManager.nowPlaying().value, true, 0)
                            }
                        }
                    }
                }
                PlaybackManager.nowPlaying().observe(this, metadataObserver)

                repeatModeObserver = Observer<Int> { mode ->
                    when (mode) {
                        ExoPlayer.REPEAT_MODE_OFF -> {
                            buttonRepeatMode.setImageResource(R.drawable.exo_icon_repeat_off)
                        }
                        ExoPlayer.REPEAT_MODE_ALL -> {
                            buttonRepeatMode.setImageResource(R.drawable.exo_icon_repeat_all)
                        }
                        ExoPlayer.REPEAT_MODE_ONE -> {
                            buttonRepeatMode.setImageResource(R.drawable.exo_icon_repeat_one)
                        }
                    }
                }
                PlaybackManager.repeatMode().observe(this, repeatModeObserver)

                shuffleModeObserver = Observer<Int> { mode ->
                    when (mode) {
                        PlaybackStateCompat.SHUFFLE_MODE_NONE -> {
                            buttonShuffleMode.setImageResource(R.drawable.exo_icon_shuffle_off)
                        }
                        PlaybackStateCompat.SHUFFLE_MODE_ALL -> {
                            buttonShuffleMode.setImageResource(R.drawable.exo_icon_shuffle_on)
                        }
                    }
                }
                PlaybackManager.shuffleMode().observe(this, shuffleModeObserver)
            }
        }
        PlaybackManager.isConnected().observe(this, connectionObserver)

        // set enter animation
        if (!PrefManager.animationReduced(this)) {
            try {
                val extras = intent.getIntArrayExtra(EXTRA_TAG)
                fabX = extras[0]
                fabY = extras[1]
                fabD = extras[2]
                radius = hypot(fabX.toDouble(), fabY.toDouble()).toFloat()
                rootLayout.visibility = View.INVISIBLE
                rootLayout.post {
                    startRevealAnim(fabX, fabY)
                }
                enterAniPlayed = true
            } catch (e: Exception) {
                Log.d("NowPlayingActivity", e.message)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun startRevealAnim(centerX: Int, centerY: Int) {
        backPressLock = true
        GlobalScope.launch(Dispatchers.Main) {
            // wait for other loading tasks
            delay(175)

            val endRadius = hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
            val anim =
                ViewAnimationUtils.createCircularReveal(
                    rootLayout,
                    centerX, centerY, fabD.toFloat() / 2, endRadius
                )
            anim.duration = 500
            anim.interpolator = FastOutSlowInInterpolator()
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
    }

    override fun onBackPressed() {
        if (backPressLock) {
            return
        }
        try {
            seekbarJob.cancel()
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message)
        }
        backPressLock = true
        if (enterAniPlayed && !PrefManager.animationReduced(this)) {
            val endRadius = 100.0f
            val anim = ViewAnimationUtils.createCircularReveal(
                rootLayout,
                fabX, fabY, radius, fabD.toFloat() / 2
            )
            anim.duration = 400
            anim.interpolator = FastOutLinearInInterpolator()
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
                seekbar.progress = percentage.toInt()
                position.text = Generic.msecToStr(pos)
                duration.text = Generic.msecToStr(dur)
            }
        }
    }

    private fun updateUI(metadata: MediaMetadataCompat?, disableAnim: Boolean = false, bgDelay: Long = 1200) {
        // Disable skip when updating metadata
        skipLock = true

        toolbar.title = metadata?.description?.title ?: "Unknown title"
        val artist = metadata?.description?.subtitle ?: "Unknown artist"
        val album = metadata?.description?.description ?: "Unknown album"
        toolbar.subtitle = "$artist - $album"

        GlobalScope.launch(Dispatchers.IO) {
            val mediaId = metadata?.description?.mediaId
            if (mediaId != null && mediaId != "") {
                val albumId = MediaHunter.getAlbumIdFromAudioId(this@NowPlayingActivity, mediaId.toLong())
                if (albumId != MediaHunter.ALBUM_NOT_EXIST) {
                    val futureTarget = imgLoader.asBitmap()
                        .error(R.drawable.default_album_art_blue)
                        .load(MediaHunter.getArtUriFromAlbumId(albumId))
                        .submit(720, 720)
                    delay(50)
                    try {
                        setAlbumAndBg(futureTarget.get(), disableAnim, bgDelay)
                    } catch (e: Exception) {
                        setAlbumAndBg(null, disableAnim, bgDelay)
                    } finally {
                        imgLoader.clear(futureTarget)
                    }
                }
            } else {
                setAlbumAndBg(defaultAlbumArt, disableAnim, bgDelay)
            }
        }
    }

    private suspend fun setAlbumAndBg(bitmap: Bitmap?, disableAnim: Boolean, bgDelay: Long = 0) {
        GlobalScope.launch(Dispatchers.Main) {
            if (bitmap != null) {
                if (!disableAnim) {
                    val nextView = albumArtSwitcher.nextView as ImageView
                    nextView.setImageBitmap(bitmap)
                    delay(100)
                    albumArtSwitcher.showNext()
                } else {
                    val currentView = albumArtSwitcher.currentView as ImageView
                    currentView.setImageBitmap(bitmap)
                }

                // only the last one will apply
                setBgJob.cancel()
                setBgJob = GlobalScope.launch(Dispatchers.Main) {
                    delay(bgDelay)
                    if (isActive) {
                        if (!disableAnim) {
                            val nextBg = bgSwitcher.nextView as ImageView
                            blur.from(bitmap)
                                .into(nextBg)
                            bgSwitcher.showNext()
                        } else {
                            val curBg = bgSwitcher.currentView as ImageView
                            blur.from(bitmap)
                                .into(curBg)
                        }
                    }
                }

                delay(400)
                skipLock = false

            } else {
                setAlbumAndBg(defaultAlbumArt, disableAnim, bgDelay)
            }
        }
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

}