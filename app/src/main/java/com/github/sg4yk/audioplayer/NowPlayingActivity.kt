package com.github.sg4yk.audioplayer

import android.animation.Animator
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toBitmap
import com.github.sg4yk.audioplayer.utils.Generic
import com.github.sg4yk.audioplayer.utils.PrefManager
import com.google.android.exoplayer2.ui.DefaultTimeBar
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
    private lateinit var duration: AppCompatTextView
    private lateinit var position: AppCompatTextView
    private lateinit var timebar: DefaultTimeBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.post {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // set title info
            // TODO
        }


        // set album art and background
        backgroundImg = findViewById(R.id.background)
        backgroundImg2 = findViewById(R.id.background2)
        albumArt = findViewById(R.id.album_art)
        albumArt2 = findViewById(R.id.album_art2)

        // set seekbar behavior
        seekBar = findViewById(R.id.seekbar)
        seekbar.post {
            // set coroutine job for seekbar
            seekbarJob = GlobalScope.launch {
                while (isActive) {
                    delay(500L)
//                    if (PlaybackManager.status() == PlaybackEngine.STATUS_PLAYING) {
//                        updateProgress()
//                    }
                }
            }
            seekbarJob.start()

            // on slide behavior
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // stop updating progress
                    seekbarJob.cancel()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // start a new job
                    seekbarJob = GlobalScope.launch {
                        while (isActive) {
                            delay(500L)
//                            if (PlaybackManager.status() == PlaybackEngine.STATUS_PLAYING) {
//                                updateProgress()
//                            }
                        }
                    }
                    seekbarJob.start()
//                    PlaybackManager.seekTo(seekbar.progress)
                    updateProgress()
                }
            })

            timebar = findViewById(R.id.timebar)
        }

        // set buttons behavior
        val playButton: FloatingActionButton = findViewById(R.id.button_play)
        playButton.post {
            playButton.setOnClickListener {
//                if (PlaybackManager.status() == PlaybackEngine.STATUS_STOPPED) {
//                    if (PlaybackManager.play()) {
//                        updateMetadata()
//                    }
//                } else {
//                    PlaybackManager.playOrPause()
//                }
            }
        }

        duration = findViewById(R.id.duration)
        position = findViewById(R.id.position)

        // update date before enter
//        if (PlaybackManager.status() == PlaybackEngine.STATUS_STOPPED) {
//            setAlbumAndBg(null, 0, 0)
//        } else {
//            updateProgress()
//            updateMetadata(0, 0)
//        }


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
        anim.addListener(object : Animator.AnimatorListener{
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
        seekbarJob.cancel()
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

    @WorkerThread
    private fun updateProgress() {
//        seekBar.post { seekbar.progress = PlaybackManager.percentage() }
//        position.post { position.text = PlaybackManager.positionAsString() }
//        duration.post { duration.text = PlaybackManager.durationAsString() }
    }

    //    @WorkerThread
    private fun updateMetadata(duration: Long = 300, bgDelay: Long = 500) {
//        val metadata = PlaybackManager.currentMetadata ?: return
//        toolbar.post {
//            toolbar.title = metadata.title
//            toolbar.subtitle = "${metadata.artist} - ${metadata.album}"
//        }
//        setAlbumAndBg(metadata.albumArt, duration, bgDelay)
    }

    private fun setAlbumAndBg(bitmap: Bitmap?, duration: Long, bgDelay: Long = 0) {
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
            GlobalScope.launch {
                delay(bgDelay)
                backgroundImg.post { Generic.crossFade(backgroundImg, backgroundImg2, duration) }
            }
        } else {
            setAlbumAndBg(getDrawable(R.drawable.lucas_benjamin_unsplash)!!.toBitmap(300, 300), duration)
        }
    }
}
