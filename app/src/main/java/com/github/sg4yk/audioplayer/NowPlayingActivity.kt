package com.github.sg4yk.audioplayer

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.appbar.MaterialToolbar
import jp.wasabeef.blurry.Blurry
import kotlin.math.hypot

class NowPlayingActivity : AppCompatActivity() {

    private var fabX: Int = 0
    private var fabY: Int = 0
    private var fabD: Int = 0
    private var radius: Float = 0f
    private lateinit var rootLayout: View
    private lateinit var backgroundImg: ImageView
    private lateinit var albumArt: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.post {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        backgroundImg = findViewById(R.id.background)
        albumArt = findViewById(R.id.album_art)
        backgroundImg.post {
            val drawable: Drawable = albumArt.drawable
            val bitmap = drawable.toBitmap()
            Blurry.with(this).radius(1).sampling(2).color(Color.argb(76, 0, 0, 0)).from(bitmap)
                .into(backgroundImg)
        }

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
}
