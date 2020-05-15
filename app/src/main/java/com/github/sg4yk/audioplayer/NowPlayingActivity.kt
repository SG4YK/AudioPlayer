package com.github.sg4yk.audioplayer

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toBitmap
import com.github.sg4yk.audioplayer.utils.AudioHunter
import com.github.sg4yk.audioplayer.utils.PlaybackEngine
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_now_playing.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

class NowPlayingActivity : AppCompatActivity() {

    private var fabX = 0
    private var fabY = 0
    private var fabD = 0
    private var radius = 0f
    private var backPressed = false

    private lateinit var rootLayout: View
    private lateinit var backgroundImg: ImageView
    private lateinit var albumArt: ImageView
    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var seekbarJob: Job

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
        albumArt = findViewById(R.id.album_art)
        backgroundImg.post {
            val drawable: Drawable = albumArt.drawable
            val bitmap = drawable.toBitmap()
            Blurry.with(this).radius(1).sampling(2).color(Color.argb(76, 0, 0, 0)).from(bitmap)
                .into(backgroundImg)
        }


        // set enter animation
        fabX = intent.getIntExtra("fabX", 0)
        fabY = intent.getIntExtra("fabY", 0)
        fabD = intent.getIntExtra("fabD", 0)
        radius = hypot(fabX.toDouble(), fabY.toDouble()).toFloat()
        rootLayout = findViewById(R.id.rootLayout)
        rootLayout.visibility = View.INVISIBLE
        rootLayout.post {
            startRevealAnim(fabX, fabY)
        }


        // set seekbar behavior
        seekBar = findViewById(R.id.seekbar)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d("seekbar", seekBar?.progress.toString())
                PlaybackEngine.seekTo(seekbar.progress)
                Log.d("Position", PlaybackEngine.getPosition().toString())
            }
        })

        // set buttons behavior
        val playButton: FloatingActionButton = findViewById(R.id.button_play)
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

        seekbarJob = GlobalScope.launch {
            while (true) {
                delay(500L)
                if (PlaybackEngine.status() == PlaybackEngine.STATUS_PLAYING) {
                    seekBar.progress = PlaybackEngine.getPosition()
                }
            }
        }
        seekbarJob.start()
        Log.d("Job","Seekbar job start")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun startRevealAnim(centerX: Int, centerY: Int) {
        val startRadius = 0.0f
        val endRadius = Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, centerX, centerY, fabD.toFloat() / 2, endRadius)
        anim.duration = 1000
        anim.interpolator = DecelerateInterpolator(2.4f)
        rootLayout.visibility = View.VISIBLE
        anim.start()
    }

    override fun onBackPressed() {
        if (backPressed) {
            return
        }
        seekbarJob.cancel()
        if (seekbarJob.isCancelled){
            Log.d("Job","Seekbar job canceled")
        }
        backPressed = true
        val endRadius = 100.0f
        val anim = ViewAnimationUtils.createCircularReveal(rootLayout, fabX, fabY, radius, fabD.toFloat() / 2)
        anim.duration = 500
        anim.interpolator = AccelerateInterpolator()
        anim.doOnEnd {
            rootLayout.visibility = View.INVISIBLE
            finishAfterTransition()
        }
        anim.start()
    }

    private fun setBG() {
        // TODO
    }

    override fun onResume() {
        super.onResume()
        backPressed = false
    }
}
